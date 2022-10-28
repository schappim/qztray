package qz.ws;

import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import qz.common.Constants;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.awt.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.prefs.Preferences;

public class APIClient {

    private static final Logger log = LogManager.getLogger(APIClient.class);

    final String TOKEN = "api_token";
    final String API_URL = "api_URL";
    private Preferences prefs;

    private APIWebSocket ws;

    private static APIClient client;

    public static APIClient init() {
        if (client == null) {
            client = new APIClient();
        }
        return client;
    }

    public APIClient() {
        prefs = Preferences.userNodeForPackage(APIClient.class);

        if(getToken().length() == 0) {
            loadKoi();
        } else {
            initSocket();
        }
    }

    private void loadKoi () {
        try {
            Desktop desk = Desktop.getDesktop();
            desk.browse(new URI(Constants.API_ADMIN_URL));
        } catch (Exception e) {
            log.error(e);
        }
    }

    private String getToken () {
        String token = prefs.get(TOKEN, "");

        try {
            String decrypted = decrypt(Constants.ENCRYPT_KEY, Constants.ENCRYPT_INIT, token);
            return decrypted;
        } catch (Exception e) {
            resetToken();
        }

        return "";
    }

    private String getUrl () {
        return prefs.get(API_URL, Constants.DEFAULT_API_DOMAIN);
    }

    public void resetToken () {
        prefs.put(TOKEN, encrypt(Constants.ENCRYPT_KEY, Constants.ENCRYPT_INIT, ""));
        if (ws != null) {
            ws.close();
        }
        loadKoi();
    }

    public void setToken (String token, String url) {
        String enc = encrypt(Constants.ENCRYPT_KEY, Constants.ENCRYPT_INIT, token);
        prefs.put(TOKEN, enc);
        prefs.put(API_URL, url);
        initSocket();
    }

    public String encrypt(String key, String initVector, String value) {
        try {
            IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);

            byte[] encrypted = cipher.doFinal(value.getBytes());
            System.out.println("encrypted string: "
                                       + Base64.encodeBase64String(encrypted));

            return Base64.encodeBase64String(encrypted);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    public String decrypt(String key, String initVector, String encrypted) {
        try {
            IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);

            byte[] original = cipher.doFinal(Base64.decodeBase64(encrypted));

            return new String(original);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }


    private void initSocket() {
        try {
            if (ws == null) {
                ws = new APIWebSocket(new URI("wss://" + getUrl() + "/cable?token=" + getToken()));
            }

            if (!ws.isOpen()) {
                ws.connect();
            }
        } catch (URISyntaxException e) {
            log.error("APIClient Socket Error");
            log.error(e);
        }
    }

    public void send (String message) {
        if (ws == null) {
            initSocket();
        }

        if (ws != null && ws.isOpen()) {

            JSONObject obj = new JSONObject();

            try {
                obj.put("command", "message");
                obj.put("identifier", "{\"channel\":\"" + Constants.API_CHANNEL + "\"}");
                obj.put("data", message);
            } catch(Exception e) {
                log.error(e);
            }

            //log.error(obj.toString());

            ws.send(obj.toString());
        } else {
            log.info("API socket is closed");
        }
    }
}
