package se.spree.festen;

/**
 * Created by hannes on 27/03/17.
 */
public interface HDataBaseMessageHandler {
    /**
     * Created by hannes on 27/03/17.
     */

        void onImageReceived(String img);

        void onData(String in);


}
