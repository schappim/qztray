package qz.ws;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import qz.common.Constants;
import qz.utils.PrintingUtilities;

import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;

/**
 * HTTP JSON endpoint for serving QZ Tray information
 */
public class APIWebSocket extends WebSocketClient  {

    private static final Logger log = LogManager.getLogger(PrintSocketClient.class);

    public APIWebSocket(URI serverURI) {
        super(serverURI);
        log.info("Connecting to " + serverURI);
    }

    private Integer reconnectTimeout = 1000;

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        log.info("Opened api connection");
        reconnectTimeout = 500;
        send("{\"command\":\"subscribe\", \"identifier\":\"{\\\"channel\\\":\\\"" + Constants.API_CHANNEL + "\\\"}\"}");
    }

    @Override
    public void onMessage(String message) {
        log.info("received: " + message);

        try {
            JSONObject jsonMsg = new JSONObject(message);
            JSONObject jsonBody = new JSONObject(jsonMsg.optString("message"));

            if (jsonBody.optString("type").equals("print")) {
                PrintingUtilities.processPrintRequest(null, null, new JSONObject(jsonBody.optString("data")));
            }
        } catch (Exception e) {
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        // The close codes are documented in class org.java_websocket.framing.CloseFrame
        log.info(
                "Connection closed by " + (remote ? "remote peer" : "us") + " Code: " + code + " Reason: "
                        + reason);

        // Increase the timeout for each failed attempt
        if (reconnectTimeout > 20000) {
            reconnectTimeout = 20000;
        } else {
            reconnectTimeout = reconnectTimeout + 500;
        }

        tryReconnect();

    }

    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();
    }

    private void tryReconnect () {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                log.info("Attempting reconnect - " + reconnectTimeout + "ms");
                reconnect();
            }
        }, reconnectTimeout);
    }
}