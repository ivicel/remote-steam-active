package info.ivicel.steam.slowdowng;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

/**
 * Created by sedny on 27/08/2017.
 */

public class SocketController {
    private static final String TAG = "SocketController";
    private static SocketController mController;
    private WebSocket mWebSocket;
    private OnLoginListener mLoginListener;
    private OnRedeemListener mRedeemListener;
    private OnConnectFailure mConnectFailure;
    private boolean connected = false;
    
    public interface OnLoginListener {
        void onLogin(final JSONObject object);
        void onAuthCode(JSONObject object);
    }
    
    public interface OnConnectFailure {
        void onConnectFailure(Throwable t, Response response);
    }
    
    public interface OnRedeemListener {
        void onRedeem(JSONObject object);
    }
    
    public void setOnConnectFailure(OnConnectFailure listener) {
        mConnectFailure = listener;
    }
    
    public void setOnRedeemListener(OnRedeemListener listener) {
        mRedeemListener = listener;
    }
    
    public void setOnLoginListener(OnLoginListener listener) {
        mLoginListener = listener;
    }
    
    public static SocketController getInstance() {
        if (mController == null) {
            mController = new SocketController();
        }
        return mController;
    }
    
    public boolean login(String url, String username, String password) {
        initConnection(url, username, password);
        return true;
    }
    
    public boolean login(String username, String password) {
        boolean result = false;
        try {
            JSONObject requestData = new JSONObject();
            requestData.put("action", "logOn");
            requestData.put("username", username);
            requestData.put("password", password);
            requestData.put("authcode", "");
            result = mWebSocket.send(requestData.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }
    
    public boolean sendAuthCode(String authCode) {
        boolean result = false;
        try {
            JSONObject requestData = new JSONObject();
            requestData.put("action", "authCode");
            requestData.put("authCode", authCode);
            result = mWebSocket.send(requestData.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }
    
    public boolean redeemCode(List<String> keys) {
        boolean result = false;
        try {
            JSONArray arrayKeys = new JSONArray();
            JSONObject requestData = new JSONObject();
            requestData.put("action", "redeem");
            for (String key : keys) {
                arrayKeys.put(key);
            }
            requestData.put("keys", arrayKeys);
            result = mWebSocket.send(requestData.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }
    
    public void initConnection(final String url, final String username, final String password) {
        String ends = url.endsWith("/") ? "ws" : "/ws";
        final String serverUrl = url.replaceFirst("http", "ws") + ends;
        
        OkHttpClient client = new OkHttpClient.Builder().build();
        Request request = new Request.Builder()
                .url(serverUrl)
                .build();
        
        client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                Log.d(TAG, "onOpen: connecting to " + serverUrl);
                mWebSocket = webSocket;
            }
    
            @Override
            public void onMessage(WebSocket webSocket, String text) {
                Log.d(TAG, "onMessage: receive -> " + text);
                try {
                    JSONObject responseJson = new JSONObject(text);
                    String action = responseJson.getString("action");
                    if ("connect".equalsIgnoreCase(action)) {
                        connected = true;
                        if (username != null && password != null) {
                            login(username, password);
                        }
                    } else if ("logOn".equalsIgnoreCase(action)) {
                        mLoginListener.onLogin(responseJson);
                    } else if ("authCode".equalsIgnoreCase(action)) {
                        mLoginListener.onAuthCode(responseJson);
                    } else if ("redeem".equalsIgnoreCase(action)) {
                        Log.d(TAG, "onMessage: " + text);
                        mRedeemListener.onRedeem(responseJson);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
    
            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                Log.d(TAG, "onMessage: receive bytes message");
                super.onMessage(webSocket, bytes);
            }
    
            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                Log.d(TAG, "onClosed: websocket closed");
                super.onClosed(webSocket, code, reason);
                connected = false;
            }
    
            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                Log.d(TAG, "onClosing: websocket closing");
                super.onClosing(webSocket, code, reason);
            }
    
            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                super.onFailure(webSocket, t, response);
                if (mConnectFailure != null) {
                    mConnectFailure.onConnectFailure(t, response);
                }
                connected = false;
            }
        });
    }
    
    public void close() {
        if (mWebSocket != null) {
            mWebSocket.close(1000, null);
        }
    }
    
    public boolean isConnected() {
        return this.connected;
    }
    
    public void connectToServer(String url) {
        close();
        initConnection(url, null, null);
    }
}
