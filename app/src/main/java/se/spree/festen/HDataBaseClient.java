package se.spree.festen;

import android.graphics.Bitmap;
import android.util.Base64;

import org.xsocket.MaxReadSizeExceededException;
import org.xsocket.connection.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.channels.ClosedChannelException;

/**
 * Created by hannes on 20/03/17.
 */
public class HDataBaseClient {
    private String ip;
    private int port;
    private HDataBaseMessageHandler msgHander;

    private INonBlockingConnection bc;
    private boolean available;  //Used as a mutex
    private boolean nextIsImage = false;

    /**
     * Creates a new client connection to the server.
     *
     * @param ip                ip of server
     * @param port              port (usually 8090)
     * @param myName            the name of this device
     * @param msgHander
     */
    public HDataBaseClient(String ip, int port, String myName, HDataBaseMessageHandler msgHander){
        this.msgHander = msgHander;
        this.ip = ip;
        this.port = port;
        available = true;
        runNonBlockingClient();
        SetMe(myName);
    }

    private void runNonBlockingClient(){
        IDataHandler clientHandler = new IDataHandler() {
            @Override
            public boolean onData(INonBlockingConnection iNonBlockingConnection) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
                String in = iNonBlockingConnection.readStringByDelimiter("\r\n");
                if(in.startsWith("/9j/")){
                    //NOTE - this is only true when dealing with .jpeg/.jpg in Base64.
                    msgHander.onImageReceived(in);
                }else {
                    msgHander.onData(in);
                }
                available = true;
                return true;
            }
        };

        try {
            bc = new NonBlockingConnection(ip, port, clientHandler);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void disconnect(){
        if(bc != null){
            if(bc.isOpen()){
                try {
                    bc.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }



    /**
     * Sort a table in the database, based on a specific column number.
     * If column contains only numbers, and should be sorted by those instead of
     * alphabetically, set @param containsNumbers to "TRUE".
     *
     * Server will respond "Table sorted" on success, otherwise "Error".
     *
     * @param table
     * @param column
     * @param containsNumbers
     */
    public void SortTable(String table, int column, String containsNumbers) {
        if(checkInputValidity(table, containsNumbers)) {
            String sendStr = "SORT_TABLE_BY_INDEX|" + table + "|" + column + "|" + containsNumbers;
            sendString(sendStr);
        }
    }

    /**
     * Set a value (String) to a column/field (as a String) with a key.
     * Server will return either "Field Updated" or "New key added" on success, otherwise "Error";
     *
     *
     * @param table
     * @param key
     * @param fieldName
     * @param value
     * @return
     */
    public void SetValue(String table, String key, String fieldName, String value){
        if(checkInputValidity(table, key, fieldName, value)) {
            String sendStr = "SET_BY_NAME|" + table + "|" + key + "|" + fieldName + "|" + value;
            sendString(sendStr);
        }
    }

    /**
     * Set a value (String) to a column/field (as an (int) Index) with a key.
     * Server will return either "Field Updated" or "New key added" on success, otherwise "Error";
     *
     * @param table
     * @param key
     * @param field
     * @param value
     * @return
     */
    public void SetValue(String table, String key, int field, String value){
        if(checkInputValidity(table, key, value)) {
            String sendStr = "SET_BY_INDEX|" + table + "|" + key + "|" + field + "|" + value;
            sendString(sendStr);
        }

    }

    /**
     * Get a value from a table, using key and name of field/column.
     *
     * Server will respond with the value on success, otherwise "Error"
     *
     * @param table
     * @param key
     * @param fieldName
     * @return
     */
    public void GetValue(String table, String key, String fieldName){
        if(checkInputValidity(table, key, fieldName)) {
            String s = "GET_BY_NAME|" + table + "|" + key + "|" + fieldName;
            sendString(s);
        }
    }

    /**
     * Get a value from a table, using key and index of a field/column.
     *
     * Server will respond with the value on sucess, otherwise "Error"
     *
     * @param table
     * @param key
     * @param field
     * @return
     */
    public void GetValue(String table, String key, int field){
        if(checkInputValidity(table, key)) {
            String s = "GET_BY_INDEX|" + table + "|" + key + "|" + field;
            sendString(s);
        }

    }

    /**
     * Get the complete row of a table using index of table.
     * To sort a table by a specific column, use method SortTable.
     *
     * On success, the server will respond as such:
     * "KEY|VALUE|...|VALUE"
     * Empty columns will be filled with "null".
     *
     * If unsuccessful, the server will respond with "Error"
     *
     * @param table
     * @param tableIndex
     * @return
     */
    public void GetEntryByTableOrder(String table, int tableIndex){
        if(checkInputValidity(table)) {
            String s = "GET_ENTRY_BY_TABLE_ORDER|" + table + "|" + tableIndex;
            sendString(s);
        }

    }

    /**
     * Send an image file (.jpg) to the server.
     *
     * @param imgKey the desired key to use. This will be used to retrieve the image from the server again.
     * @param image
     * @return
     */
    public void SendImageFile(String imgKey, Bitmap image){
        if(checkInputValidity(imgKey)){
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            image.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream .toByteArray();
            String encoded = Base64.encodeToString(byteArray, Base64.NO_WRAP);
            sendString("IMG|" + imgKey + "|" + encoded);

        }
        return;
    }


    /**
     * Create a new table with any number of columns/fields on server.
     *
     * Server will respond with "Table created" on success, "Table already exists" if table exist, and "Error"
     * if error occurs.
     *
     * @param tableName
     * @param columns
     * @return
     */
    public void CreateNewTable(String tableName, String... columns){
        if(checkInputValidity(tableName) && checkInputValidity(columns)){
            String s = "CREATE_TABLE|" + tableName;
            for(int i = 0; i < columns.length; i++){
                s += "|" + columns[i];
            }

            sendString(s);
        }
        return;
    }

    /**
     * Get an image as a String with base64 encoding from the server.
     * Use method ConvertToImage to convert this string to a BufferImage object.
     *
     * Server will return the complete string on success, otherwise "Error"
     *
     * @param imageKey
     * @return
     */
    public void GetImage(String imageKey){
        if(checkInputValidity(imageKey)){
            sendString("GET_IMG|" + imageKey);
        }else{
            return;
        }
    }

    /**
     * Delete a table from the server.
     * Note, this action cannot be undone.
     *
     * Server will return "Table deleted" on success, "No such table exists" if no such table exists, or "Error" if
     * error occurs.
     *
     * @param table
     * @return
     */
    public void DeleteTable(String table){
        sendString("DELETE_TABLE|" + table);
    }

    /**
     * Delete a key and all its values from a table.
     * Server will respond "Key deleted" on success, "No such key exists" if key doesn't exist,
     * "No such table exists" if table doesn't exist, and "Error" if error occurs.
     *
     * @param table
     * @param key
     * @return
     */
    public void DeleteKey(String table, String key){
        sendString("DELETE_KEY|" + table + "|" + key);
    }

    public void SetMe(String clientName){
        sendString("SET_ME|" + clientName);
    }

    private void sendString(String s){

        while(!available) Thread.yield();
        available = false;
        try {
            //IBlockingConnection bc = new BlockingConnection(ip, port);
            bc.write(s + "\r\n");
            //String res = bc.readStringByDelimiter("\r\n");
            return;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return;
    }


    private boolean checkInputValidity(String... values){
        for(String s : values){
            if(s.contains("|") || s.contains(";")){
                System.err.println("INVALID QUERY: No field can contain '|' or ';'. Command not sent to server.");
                return false;
            }
        }
        return true;
    }

    /**
     * Convert an image string (those retrieved from the server with method GetImage()), to a
     * BufferedImage object and returns it.
     * Throws IOException if unsuccessful.
     *
     * @param imgString
     * @return
     * @throws IOException
     */

    public void ConvertToImage(String imgString) throws IOException {
    return;
    }



    /**
     * Save image as .jpg locally.
     *
     * @param path  the path where image should be saved.
     * @param fileName the filename of the image. Filename should not include .jpg.
     */
    public static void SaveImageLocally(String path, String fileName) throws IOException {
        //TODO - Not implemented for Android yet.
       return;
    }


}
