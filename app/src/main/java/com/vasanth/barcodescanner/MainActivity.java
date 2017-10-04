package com.vasanth.barcodescanner;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.vasanth.barcodescannerlib.BarcodeScannerActivity;

/**
 * Main Activity.
 * <p>
 * 1. Responsibility.
 * 1.a. Activity used to try Google Vision Barcode Scanner feature.
 *
 * @author Vasanth
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int REQUEST_CODE_BARCODE_SCANNER = 1001;

    private Button bBarcodeScanner;


    // ACTIVITY METHODS.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bBarcodeScanner = (Button) findViewById(R.id.barcode_scanner);
        bBarcodeScanner.setOnClickListener(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_BARCODE_SCANNER) {
            processBarcodeScannerResult(requestCode, resultCode, data);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // View.OnClickListener METHODS.
    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.barcode_scanner) {
            startBarcodeScanner();
        }
    }

    // PRIVATE METHODS.
    private void startBarcodeScanner() {
        Intent barcodeScannerIntent = new Intent(this, BarcodeScannerActivity.class);
        startActivityForResult(barcodeScannerIntent, REQUEST_CODE_BARCODE_SCANNER);
    }

    private void processBarcodeScannerResult(final int requestCode, final int resultCode, final Intent data) {
        if (requestCode == REQUEST_CODE_BARCODE_SCANNER) {
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    String barcodeData = data.getStringExtra(BarcodeScannerActivity.EXTRAS_RESULT_BARCODE_RAW_VALUE);
                    Toast.makeText(this, "Barcode Found - " + barcodeData, Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, R.string.barcodeScanner_cancelled, Toast.LENGTH_SHORT).show();
            }
        }
    }
}