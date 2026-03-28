package com.example.pccontroller;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import android.util.Log;

public class QRScannerActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST = 100;
    private DecoratedBarcodeView barcodeView;
    private static final String TAG = "PC_CONTROLLER";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr);

        barcodeView = findViewById(R.id.barcodeScanner);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_REQUEST
            );
        } else {
            startScanner();
        }
    }

    private void startScanner() {
        barcodeView.decodeContinuous(callback);
    }

    private final BarcodeCallback callback = new BarcodeCallback() {
        @Override
        public void barcodeResult(BarcodeResult result) {
            barcodeView.pause();

            String[] parts = result.getText().split(":");

            SocketClient.getInstance()
                    .connect(parts[0],
                            Integer.parseInt(parts[1]),
                            parts[2]);

            // 🔥 DO NOT START MAIN ACTIVITY
            Log.d(TAG, "QRScannerActivity finishing");
            finish();
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        barcodeView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        barcodeView.pause();
    }
}
