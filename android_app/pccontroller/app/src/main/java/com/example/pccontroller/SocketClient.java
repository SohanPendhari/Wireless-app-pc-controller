package com.example.pccontroller;

import android.util.Log;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class SocketClient {

    private static final String TAG = "PC_CONTROLLER";

    private static SocketClient instance;
    private Socket socket;
    private PrintWriter out;
    private volatile boolean connected = false;

    // 🔥 Command queue
    private final BlockingQueue<String> sendQueue = new LinkedBlockingQueue<>();

    private SocketClient() {}

    public static synchronized SocketClient getInstance() {
        if (instance == null)
            instance = new SocketClient();
        return instance;
    }

    public void connect(String ip, int port, String token) {
        new Thread(() -> {
            try {
                Log.d(TAG, "Connecting to PC...");
                socket = new Socket(ip, port);
                out = new PrintWriter(socket.getOutputStream(), true);

                out.println(token);
                connected = true;
                Log.i(TAG, "Connected, token sent");

                // 🔥 Sender loop (BACKGROUND THREAD)
                while (connected) {
                    String msg = sendQueue.take();
                    out.println(msg);
                    Log.d(TAG, "Sent -> " + msg);
                }

            } catch (Exception e) {
                connected = false;
                Log.e(TAG, "Socket error", e);
            }
        }).start();
    }

    // 🔥 SAFE: UI THREAD CAN CALL THIS
    public void send(String msg) {
        if (connected) {
            sendQueue.offer(msg);
        } else {
            Log.w(TAG, "Send ignored (not connected)");
        }
    }

    public boolean isConnected() {
        return connected;
    }
}
