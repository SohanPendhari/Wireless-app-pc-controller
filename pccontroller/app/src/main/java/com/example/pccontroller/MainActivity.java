package com.example.pccontroller;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private float lastX, lastY;
    private boolean fingerDown = false;

    private SocketClient socket;

    // Trackpad tuning
    private static final float BASE_SENSITIVITY = 0.22f;
    private static final float MAX_SENSITIVITY  = 1.1f;

    private float lastDx = 0f;
    private float lastDy = 0f;

    private long lastMoveTime = 0;

    // Tap detection
    private long tapStartTime;
    private float tapStartX, tapStartY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        socket = SocketClient.getInstance();

        Button btnConnect = findViewById(R.id.btnConnect);
        View touchPad = findViewById(R.id.touchPad);

        btnConnect.setOnClickListener(v ->
                startActivity(new Intent(this, QRScannerActivity.class))
        );

        touchPad.setOnTouchListener((v, event) -> {

            if (!socket.isConnected()) return true;

            switch (event.getActionMasked()) {

                case MotionEvent.ACTION_DOWN:
                    fingerDown = true;
                    lastX = event.getX();
                    lastY = event.getY();

                    tapStartTime = System.currentTimeMillis();
                    tapStartX = lastX;
                    tapStartY = lastY;

                    lastMoveTime = tapStartTime;
                    lastDx = 0f;
                    lastDy = 0f;
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (!fingerDown) return true;

                    int pointerIndex =
                            event.findPointerIndex(event.getPointerId(0));
                    if (pointerIndex < 0) return true;

                    // 🔥 process historical points
                    int historySize = event.getHistorySize();
                    for (int i = 0; i < historySize; i++) {
                        float hx = event.getHistoricalX(pointerIndex, i);
                        float hy = event.getHistoricalY(pointerIndex, i);
                        sendDelta(hx, hy);
                    }

                    // current point
                    sendDelta(
                            event.getX(pointerIndex),
                            event.getY(pointerIndex)
                    );
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    fingerDown = false;

                    long tapDuration =
                            System.currentTimeMillis() - tapStartTime;

                    float tapDistance =
                            Math.abs(event.getX() - tapStartX) +
                                    Math.abs(event.getY() - tapStartY);

                    if (tapDuration < 180 && tapDistance < 10) {
                        socket.send("CLICK");
                    }
                    break;
            }
            return true;
        });

        findViewById(R.id.btnCopy)
                .setOnClickListener(v ->
                        socket.send("COMBO ctrl c")
                );

        findViewById(R.id.btnPaste)
                .setOnClickListener(v ->
                        socket.send("COMBO ctrl v")
                );

        findViewById(R.id.btnEnter)
                .setOnClickListener(v ->
                        socket.send("KEY enter")
                );
    }

    // ================= TRACKPAD CORE =================

    private void sendDelta(float x, float y) {

        float rawDx = x - lastX;
        float rawDy = y - lastY;

        lastX = x;
        lastY = y;

        // minimal noise filter ONLY
        if (Math.abs(rawDx) < 0.02f && Math.abs(rawDy) < 0.02f)
            return;

        long now = System.currentTimeMillis();
        float dt = Math.max(1, now - lastMoveTime);
        lastMoveTime = now;

        float speed =
                (float) Math.sqrt(rawDx * rawDx + rawDy * rawDy) / dt;

        float sensitivity = BASE_SENSITIVITY +
                Math.min(speed * 0.9f, MAX_SENSITIVITY);

        float dx = rawDx * sensitivity;
        float dy = rawDy * sensitivity;

        // 🔥 axis coupling (prevents straight-line snap)
        float blend = 0.35f;
        dx = lastDx * blend + dx * (1f - blend);
        dy = lastDy * blend + dy * (1f - blend);

        // 🔥 prevent axis collapse
        if (dx != 0 && Math.abs(dy) < Math.abs(dx) * 0.05f)
            dy = lastDy * 0.3f;

        if (dy != 0 && Math.abs(dx) < Math.abs(dy) * 0.05f)
            dx = lastDx * 0.3f;

        lastDx = dx;
        lastDy = dy;

        socket.send(String.format(
                "MOVE %.4f %.4f",
                dx,
                dy
        ));
    }
}
