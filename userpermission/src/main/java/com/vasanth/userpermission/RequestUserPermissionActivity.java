package com.vasanth.userpermission;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

/**
 * 1. A activity abstracts the android permission stuff from our application.
 * <p>
 * Usage
 * 1. Start this activity for result, with the following data
 * 1.a. KEY_PERMISSION_NAME - String - Permission name to request.
 * <p>
 * 2. The app will receive the result in onActivityResult, with the following data,
 * 2.a. KEY_IS_PERMISSION_GRANTED - Boolean - Whether user has granted permission or not.
 *
 * @author Vasanth
 * @see ContextCompat#checkSelfPermission(Context, String)
 * @see ActivityCompat#requestPermissions(Activity, String[], int)
 */
public class RequestUserPermissionActivity extends AppCompatActivity {

    @NonNull
    public static Intent getIntent(@NonNull Context context, @NonNull final String permissionName) {
        Intent intent = new Intent(context, RequestUserPermissionActivity.class);
        intent.putExtra(KEY_PERMISSION_NAME, permissionName);
        return intent;
    }

    public static final String KEY_IS_PERMISSION_GRANTED = "IS_PERMISSION_GRANTED";

    private static final String KEY_PERMISSION_NAME = "KEY_PERMISSION_NAME";
    private static final int REQUEST_CODE_REQUEST_PERMISSION = 501;

    private String permissionName;

    // ACTIVITY METHODS.
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initializeExtraData(getIntent().getExtras());

        checkIfUserGrantedPermissionElseRequestPermission();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_REQUEST_PERMISSION) {
            processRequestPermissionResult(permissions, grantResults);
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    // PRIVATE METHODS.
    private void initializeExtraData(@Nullable final Bundle extras) {
        if (extras != null) {
            permissionName = extras.getString(KEY_PERMISSION_NAME, null);
        }
    }

    private void checkIfUserGrantedPermissionElseRequestPermission() {
        if (TextUtils.isEmpty(permissionName)) {
            this.finish();
        }

        if (ContextCompat.checkSelfPermission(this, permissionName) == PackageManager.PERMISSION_GRANTED) {
            sendCallbackAndFinishTheActivity(true);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{permissionName}, REQUEST_CODE_REQUEST_PERMISSION);
        }
    }

    private void processRequestPermissionResult(@NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            sendCallbackAndFinishTheActivity(true);
        } else {
            sendCallbackAndFinishTheActivity(false);
        }
    }

    private void sendCallbackAndFinishTheActivity(final boolean isPermissionGranted) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(KEY_IS_PERMISSION_GRANTED, isPermissionGranted);
        setResult(RESULT_OK, resultIntent);
        finish();
    }
}
