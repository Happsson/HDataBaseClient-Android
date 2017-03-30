package se.spree.festen;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends AppCompatActivity {

    Button btConnect, btSend, btTakeImage;
    TextView tvIn;

    HDataBaseClient db;
    String mCurrentPhotoPath;
    static final int REQUEST_TAKE_PHOTO = 1;

    static final int REQUEST_IMAGE_CAPTURE = 1;


    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }


    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File

            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "se.spree.festen.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);

            }
        }
    }

    private Bitmap getPic() {
        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = 4;

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        Matrix matrix = new Matrix();
        matrix.postRotate(90);

        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(),
                matrix, true);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btConnect = (Button) findViewById(R.id.btConnect);
        btSend = (Button) findViewById(R.id.btSendImage);
        tvIn = (TextView) findViewById(R.id.tvInput);
        btTakeImage = (Button) findViewById(R.id.btTakePicture);


        btSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                db.SendImageFile("Photo", getPic());
            }
        });


        btTakeImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });

        btConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dbConnect();
            }
        });



    }

    public void dbConnect() {
        if (db != null) {
            db.disconnect();
            db = null;
        }
        try {
            db = new HDataBaseClient("10.10.40.33", 8090, "RFID register application", new HDataBaseMessageHandler() {
                @Override
                public void onImageReceived(String img) {
                    Runnable r = new Runnable() {
                        @Override
                        public void run() {
                            tvIn.setText("Image recieved");

                        }
                    };
                    runOnUiThread(r);
                }

                @Override
                public void onData(String in) {
                    final String _in = in;
                    Runnable r = new Runnable() {
                        @Override
                        public void run() {
                            tvIn.setText(_in);

                        }
                    };
                    runOnUiThread(r);

                }
            });


        }catch(Exception e){
            tvIn.setText("Connection refused. Server offline?");
        }
    }
}
