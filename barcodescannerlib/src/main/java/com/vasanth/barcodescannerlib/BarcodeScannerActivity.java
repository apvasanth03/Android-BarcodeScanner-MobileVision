package com.vasanth.barcodescannerlib;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.vasanth.barcodescannerlib.ui.BarcodeScannerOverlayView;
import com.vasanth.userpermission.RequestUserPermissionActivity;

/**
 * Barcode Scanner Activity.
 * <p>
 * 1. Responsibility.
 * 1.a. Activity used to scan the barcode.
 * 1.b. Activity launches the camera and scans for barcode.
 * 1.c. Once Barcode is found - Activity sends result back to calling activity.
 * <p>
 * 2. Result.
 * 2.a. Once barcode is detected, we will send result back to calling activity with the following Extra data's,
 * 2.a.1. Barcode Raw Value - EXTRAS_RESULT_BARCODE_RAW_VALUE
 * <p>
 * 3. Notes.
 * 3.a. Add "Camera" permission to manifest file.
 * 3.b. Declare this activity in "Landscape mode" in manifest file.
 * <p>
 * 4. Reference.
 * 4.a. https://developers.google.com/vision/barcodes-overview
 *
 * @author Vasanth
 */
public class BarcodeScannerActivity extends AppCompatActivity {

    @NonNull
    public static Intent getIntent(@NonNull final Context context) {
        Intent intent = new Intent(context, BarcodeScannerActivity.class);
        return intent;
    }

    public static final String EXTRAS_RESULT_BARCODE_RAW_VALUE = "extras_result_barcode_raw_value";

    private static final String TAG = "BarcodeScanner";
    private static final int REQUEST_CODE_REQUEST_CAMERA_PERMISSION = 101;
    private static final int GOOGLE_PLAY_REQUEST_CODE = 2001;

    private SurfaceView surfaceView;
    private BarcodeScannerOverlayView barcodeScannerOverlayView;
    private CameraSource cameraSource;

    private boolean startRequested;
    private boolean surfaceAvailable;
    private String barcodeRawValue;

    // Activity METHODS.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Keep screen always ON.
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_barcode_scanner);

        // Initialize.
        surfaceView = (SurfaceView) findViewById(R.id.activityBarcodeScanner_surfaceView);
        surfaceView.getHolder().addCallback(new SurfaceCallback());
        barcodeScannerOverlayView = (BarcodeScannerOverlayView) findViewById(R.id.activityBarcodeScanner_barcodeScannerOverlayView);
        startRequested = false;
        surfaceAvailable = false;
        barcodeRawValue = null;

        // Check if device has CAMERA.
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            // Check if we have camera permission.
            checkIfWeHavePermissionToCamera();
        } else {
            showErrorDialog(getString(R.string.barcodeScanner_error_noCameraFeature), true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startCameraSource();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopCameraSource();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_REQUEST_CAMERA_PERMISSION) {
            handleRequestCameraPermissionResult(resultCode, data);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseCameraSource();
    }

    // PRIVATE METHODS.

    /**
     * PERMISSION STUFF.
     */
    private void checkIfWeHavePermissionToCamera() {
        Intent requestPermissionIntent = RequestUserPermissionActivity.getIntent(this, Manifest.permission.CAMERA);
        startActivityForResult(requestPermissionIntent, REQUEST_CODE_REQUEST_CAMERA_PERMISSION);
    }

    private void handleRequestCameraPermissionResult(int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK && data != null) {
            boolean isPermissionGranted = data.getBooleanExtra(RequestUserPermissionActivity.KEY_IS_PERMISSION_GRANTED, false);
            if (isPermissionGranted) {
                weHavePermissionToCamera();
            } else {
                weDontHavePermissionToCamera();
            }
        }
    }

    private void weHavePermissionToCamera() {
        createCameraSource();
    }

    private void weDontHavePermissionToCamera() {
        Snackbar.make(surfaceView, getString(R.string.barcodeScanner_cameraPermission_deniedMessage), Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(R.string.settings), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Go to settings.
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivity(intent);
                        finish();
                    }
                }).show();
    }

    /**
     * BARCODE STUFF.
     */
    /**
     * 1. Used to start barcode scanning once surface is ready.
     */
    private class SurfaceCallback implements SurfaceHolder.Callback {
        @Override
        public void surfaceCreated(SurfaceHolder surface) {
            surfaceAvailable = true;
            startIfReady();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surface) {
            surfaceAvailable = false;
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        }
    }

    private void createCameraSource() {
        Context context = getApplicationContext();

        // Create barcode detector to track barcode's.
        BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(context).build();
        BarcodeTrackerFactory barcodeFactory = new BarcodeTrackerFactory();
        barcodeDetector.setProcessor(
                new MultiProcessor.Builder<>(barcodeFactory).build());

        // Check if barcode dependencies are available.
        if (barcodeDetector.isOperational()) {
            // Creates the camera.
            cameraSource = new CameraSource.Builder(context, barcodeDetector)
                    .setFacing(CameraSource.CAMERA_FACING_BACK)
                    .setRequestedPreviewSize(1600, 1024)
                    .setRequestedFps(15.0f)
                    .setAutoFocusEnabled(true)
                    .build();
        } else {
            // Barcode dependencies are not yet available.
            // Check for low storage.  If there is low storage, the native library will not be downloaded, so detection will not become operational.
            IntentFilter lowStorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = registerReceiver(null, lowStorageFilter) != null;
            if (hasLowStorage) {
                showErrorDialog(getString(R.string.barcodeScanner_error_dependenciesNotDownloadedDueToLowMemory), true);
            } else {
                showErrorDialog(getString(R.string.barcodeScanner_error_dependenciesNotDownloaded), true);
            }
        }
    }

    private void startCameraSource() {
        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code == ConnectionResult.SUCCESS) {
            startRequested = true;
            startIfReady();
        } else {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, GOOGLE_PLAY_REQUEST_CODE);
            dlg.show();
        }
    }

    private void stopCameraSource() {
        if (cameraSource != null) {
            cameraSource.stop();
        }
    }

    private void releaseCameraSource() {
        if (cameraSource != null) {
            cameraSource.release();
        }
    }

    /**
     * Start Camera source if every thing is ready.
     */
    private void startIfReady() {
        if (startRequested && surfaceAvailable && cameraSource != null && surfaceView != null) {
            try {
                cameraSource.start(surfaceView.getHolder());
                startRequested = false;
            } catch (Exception e) {
                cameraSource.release();
                cameraSource = null;
                Log.e(TAG, e.getMessage());
            }
        }
    }

    /**
     * Barcode Tracker Factory.
     * <p>
     * Used to create Tracker instance for Barcode item.
     */
    class BarcodeTrackerFactory implements MultiProcessor.Factory<Barcode> {

        @Override
        public Tracker<Barcode> create(Barcode barcode) {
            return new BarcodeTracker();
        }
    }

    /**
     * Barcode Tracker.
     * <p>
     * A tracker is used to receive notifications for a detected barcode item.
     */
    class BarcodeTracker extends Tracker<Barcode> {


        /**
         * Method is called to indicate that barcode item is detected.
         * <p>
         * 1. Process detected barcode.
         */
        public void onUpdate(Detector.Detections<Barcode> detections, final Barcode barcode) {
            processDetectedBarcode(barcode);
        }
    }


    /**
     * Method used to process detected barcode.
     * <p>
     * 1. Check if we can read a barcode or not.
     * 2. Check if detected barcode is lies inside view finder view.
     * 2.a. If YES then send result.
     *
     * @param barcode Detected Barcode.
     */
    private synchronized void processDetectedBarcode(final Barcode barcode) {
        if (barcodeScannerOverlayView != null && cameraSource != null && cameraSource.getPreviewSize() != null &&
                checkIfBarcodePresentInsideViewFinderAndIsOnLaserLine(barcodeScannerOverlayView.getScreenResolution(), barcodeScannerOverlayView.getViewFinderRect(),
                        new Point(cameraSource.getPreviewSize().getWidth(), cameraSource.getPreviewSize().getHeight()), barcode.getBoundingBox(),
                        barcodeScannerOverlayView.getViewFinderMiddleY())) {
            // Send Result.
            sendResultToCallingActivity(barcode.rawValue);
        }
    }

    /**
     * Used to check if barcode is present inside view finder and is on laser line.
     * <p>
     * 1. ScreenResolution & cameraPreviewResolution may be in different size.
     * 2. Get the ratio by with both screenResolution & cameraPreviewResolution vary.
     * 3. Get the updated barcodeRect value depending upon ratio.
     * 4. Check if barcode is inside view finder.
     * 5. Check if barcode is in center of view finder view (i.e on the laser line).
     *
     * @param screenResolution        Screen Resolution.
     * @param viewFinderRect          View finder rectangle coordinates.
     * @param cameraPreviewResolution Camera Preview Resolution.
     * @param barcodeRect             Detected barcode rectangle coordinates.
     * @return TRUE if barcode present inside view finder.
     */
    private boolean checkIfBarcodePresentInsideViewFinderAndIsOnLaserLine(final Point screenResolution, final Rect viewFinderRect, final Point cameraPreviewResolution,
                                                                          final Rect barcodeRect, int viewFinderMiddleY) {
        boolean isBarcodePresentInsideViewFinder = false;

        if (screenResolution != null && viewFinderRect != null && cameraPreviewResolution != null && barcodeRect != null) {
            // Get the ratio by with both screenResolution & cameraPreviewResolution vary.
            double ratioWidth = (double) screenResolution.x / (double) cameraPreviewResolution.x;
            double ratioHeight = (double) screenResolution.y / (double) cameraPreviewResolution.y;

            // Get the updated barcodeRect value depending upon ratio.
            Rect barcodeRectUpdated = new Rect((int) (barcodeRect.left * ratioWidth), (int) (barcodeRect.top * ratioHeight),
                    (int) (barcodeRect.right * ratioWidth), (int) (barcodeRect.bottom * ratioHeight));

            // Check if barcode is inside view finder & is in the middle (Y) of view finder.
            isBarcodePresentInsideViewFinder = (viewFinderRect.contains(barcodeRectUpdated)) &&
                    (barcodeRectUpdated.contains((barcodeRectUpdated.left + 1), viewFinderMiddleY));
        }
        return isBarcodePresentInsideViewFinder;
    }

    /**
     * Used to send result to calling activity.
     */
    private void sendResultToCallingActivity(final String barcodeRawValue) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(EXTRAS_RESULT_BARCODE_RAW_VALUE, barcodeRawValue);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }


    /**
     * HELPER METHODS.
     */
    /**
     * Used to alert dialog.
     *
     * @param message               Alert dialog message.
     * @param confirmButtonText     Alert dialog confirm button text.
     * @param cancelButtonText      Alert dialog cancel button text.
     * @param showCancelButton      TRUE to show cancel button else FALSE.
     * @param confirmButtonListener Listener to get callback on click of confirm button.
     * @param cancelButtonListener  Listener to get callback on cancel button click.
     */
    private void showAlertDialog(final String message, final String confirmButtonText, final String cancelButtonText, final boolean showCancelButton,
                                 final View.OnClickListener confirmButtonListener, final View.OnClickListener cancelButtonListener) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setMessage(message);
        builder.setPositiveButton(confirmButtonText, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (confirmButtonListener != null) {
                    confirmButtonListener.onClick(null);
                }
            }
        });
        if (showCancelButton) {
            builder.setNegativeButton(cancelButtonText, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (cancelButtonListener != null) {
                        cancelButtonListener.onClick(null);
                    }
                }
            });
        }
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Used to show Error Dialog.
     *
     * @param errorMsg         Error Message.
     * @param isFinishActivity TRUE to finish activity after user confirmation.
     */
    private void showErrorDialog(String errorMsg, final boolean isFinishActivity) {
        showAlertDialog(errorMsg, getString(R.string.ok), getString(R.string.cancel), false, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isFinishActivity) {
                    finish();
                }
            }
        }, null);
    }
}
