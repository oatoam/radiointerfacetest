package com.example.radiointerfacetest;

import android.app.Dialog;
import android.app.role.RoleManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Telephony;
import android.telecom.TelecomManager;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int id = v.getId();
            if (id == R.id.btn_dial) {
                dial();
            } else if (id == R.id.btn_accept) {
                accept();
            } else if (id == R.id.btn_decline) {
                decline();
            } else if (id == R.id.btn_hangup) {
                hangup();
            } else if (id == R.id.btn_senddata) {
                senddata();
            } else if (id == R.id.btn_setup) {
                setup();
            } else if (id == R.id.btn_ping_test) {
                Intent intent = new Intent(MainActivity.this, PingTestActivity.class);
                startActivity(intent);
            } else if (id == R.id.btn_shell_test) {
                Intent intent = new Intent(MainActivity.this, ShellCommandActivity.class);
                startActivity(intent);
            } else if (id == R.id.btn_callback_test) {
                Intent intent = new Intent(MainActivity.this, TelephonyCallbackActivity.class);
                startActivity(intent);
            } else if (id == R.id.btn_view_sms_list) { // 新增按钮点击事件
                Intent intent = new Intent(MainActivity.this, SmsListActivity.class);
                startActivity(intent);
            }
        }
    };

    View.OnLongClickListener mLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            int id = v.getId();
            if (id == R.id.btn_setup) {
                Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            }
//            else if (id == R.id.btn_clear_number) {
//                Dialog dialog = new android.app.AlertDialog.Builder(MainActivity.this)
//                    .setTitle("Clear History")
//                    .setMessage("Are you sure you want to clear all number history?")
//                    .setPositiveButton("Yes", (dialogInterface, i) -> {
//                        InputHistoryCache numberCache = InputHistoryCache.getInstance(MainActivity.this);
//                        numberCache.clear(InputHistoryCache.KEY_NUMBERS);
//                        Toast.makeText(MainActivity.this, "number history cleared", Toast.LENGTH_LONG).show();
//                        inputRemoteNumberComponent.setText(""); // Clear the text in the component
//                    })
//                    .setNegativeButton("No", null)
//                    .create();
//                dialog.show();
//            }
            return true;
        }
    };

    TelecomManager telecomManager;
    private InputWithHistoryAndClear inputRemoteNumberComponent;

    Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.btn_dial).setOnClickListener(mClickListener);
        findViewById(R.id.btn_setup).setOnClickListener(mClickListener);
        findViewById(R.id.btn_setup).setOnLongClickListener(mLongClickListener);
        findViewById(R.id.btn_accept).setOnClickListener(mClickListener);
        findViewById(R.id.btn_decline).setOnClickListener(mClickListener);
        findViewById(R.id.btn_hangup).setOnClickListener(mClickListener);
        findViewById(R.id.btn_senddata).setOnClickListener(mClickListener);
//        findViewById(R.id.btn_clear_number).setOnLongClickListener(mLongClickListener); // Keep long click for clearing history

        findViewById(R.id.btn_ping_test).setOnClickListener(mClickListener);
        findViewById(R.id.btn_shell_test).setOnClickListener(mClickListener);
        findViewById(R.id.btn_callback_test).setOnClickListener(mClickListener);
        findViewById(R.id.btn_view_sms_list).setOnClickListener(mClickListener); // 新增

        inputRemoteNumberComponent = findViewById(R.id.input_remote_number_component);
        inputRemoteNumberComponent.setHistoryCacheKey(InputHistoryCache.KEY_NUMBERS);
        inputRemoteNumberComponent.setHint(getString(R.string.remote_number_info));
        inputRemoteNumberComponent.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        inputRemoteNumberComponent.setText("10086"); // Set default value

        telecomManager = (TelecomManager) getSystemService(Context.TELECOM_SERVICE);


        
        Log.d(TAG, "Running");

        updatePermissionUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    final int PERMISSION_REQUEST_CODE = 1001;
    final String TAG = MainActivity.class.getName();

    public void updatePermissionUI() {
        if (checkPermission()) {
            Log.d(TAG, "checkPermission true");
            Toast.makeText(this, R.string.setup_ok, Toast.LENGTH_SHORT).show();
//            findViewById(R.id.setup_container).setVisibility(View.GONE);
            findViewById(R.id.main_container).setVisibility(View.VISIBLE);
        } else {
            Log.d(TAG, "checkPermission false");
            Toast.makeText(this, R.string.setup_fail, Toast.LENGTH_SHORT).show();
//            findViewById(R.id.setup_container).setVisibility(View.VISIBLE);
            findViewById(R.id.main_container).setVisibility(View.GONE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onActivityResult request " + requestCode + " permissions " + permissions + " grantResults" + grantResults);
        updatePermissionUI();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult request " + requestCode + " result " + resultCode);
        if (resultCode == RESULT_OK) {
            updatePermissionUI();
        } else {
            String info = "failed to setup ";
            switch (requestCode) {
                case 101: info += "default SMS APP "; break;
                case 102: info += "default Dialer APP "; break;
            }
            Toast.makeText(this, info, Toast.LENGTH_LONG).show();
            Log.d(TAG, info);
        }

    }

    boolean checkPermission() {
        String myPackageName = getPackageName();
        if (!myPackageName.equals(Telephony.Sms.getDefaultSmsPackage(this))) {
            Log.e(TAG, "This app is not the default SMS app");
            return false;
        }

        if (!myPackageName.equals(getSystemService(TelecomManager.class).getDefaultDialerPackage())) {
            Log.e(TAG, "This app is not the default Dialer app");
            return false;
        }

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ANSWER_PHONE_CALLS) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Required permissions are not granted");
            return false;
        }

        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    boolean requestPermission() {

        String myPackageName = getPackageName();
        Log.d(TAG, String.format("defaultSmsPackage %s this package %s", Telephony.Sms.getDefaultSmsPackage(this), myPackageName));
        if (!myPackageName.equals(Telephony.Sms.getDefaultSmsPackage(this))) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // Android 11 or higher
                RoleManager roleManager = (RoleManager) getSystemService(ROLE_SERVICE);

                boolean isRoleAvailable = roleManager.isRoleAvailable(RoleManager.ROLE_SMS);
                boolean isRoleHeld = roleManager.isRoleHeld(RoleManager.ROLE_SMS);

                Log.d(TAG, "isRoleAvailable SMS " + isRoleAvailable);
                Log.d(TAG, "isRoleHeld SMS " + isRoleHeld);

                // Check if the app can become the default SMS handler
                Intent roleRequestIntent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS);

                // Start an activity to request the user's permission to become the default SMS application
                startActivityForResult(roleRequestIntent, 101); // You can choose any request code
                Log.d(TAG, "change role sms?" + roleRequestIntent.getAction());
            } else {
                // For versions lower than Android 11, you need to use different methods
                Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
                intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, getPackageName());
                startActivityForResult(intent, 101);
                Log.d(TAG, "change default?");
            }
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            RoleManager roleManager = (RoleManager) getSystemService(ROLE_SERVICE);
            boolean isRoleAvailable = roleManager.isRoleAvailable(RoleManager.ROLE_DIALER);
            boolean isRoleHeld = roleManager.isRoleHeld(RoleManager.ROLE_DIALER);

            Log.d(TAG, "isRoleAvailable DIALER " + isRoleAvailable);
            Log.d(TAG, "isRoleHeld DIALER " + isRoleHeld);

            // Check if the app can become the default SMS handler
            Intent roleRequestIntent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER);

            // Start an activity to request the user's permission to become the default SMS application
            startActivityForResult(roleRequestIntent, 102); // You can choose any request code
            Log.d(TAG, "change role dialer?" + roleRequestIntent.getAction());
        }

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ANSWER_PHONE_CALLS) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Toast.makeText(this, "Grant permission before using interface", Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(this, new String[]{
                    android.Manifest.permission.CALL_PHONE,
                    android.Manifest.permission.READ_PHONE_STATE,
                    android.Manifest.permission.ANSWER_PHONE_CALLS,
                    android.Manifest.permission.SEND_SMS
            }, PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    void dial() {
        Log.d(TAG, "Call dial");
        if (!checkPermission()) {
            return;
        }
        if (inputRemoteNumberComponent.getText().isBlank()) {
            Toast.makeText(this, "Call dial with invalid number", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, "Call dial", Toast.LENGTH_SHORT).show();

        Uri uri = Uri.fromParts("tel", inputRemoteNumberComponent.getText(), null);
        Log.d(TAG, "Call dial : " + uri.toString());
        telecomManager.placeCall(uri, null);
    }

    void accept() {
        Log.d(TAG, "Call accept");
        if (!checkPermission()) {
            return;
        }
        Toast.makeText(this, "Call accept", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "isInCall: " + telecomManager.isInCall());
        telecomManager.acceptRingingCall();
    }

    void decline() {
        Log.d(TAG, "Call decline");
        if (!checkPermission()) {
            return;
        }
        Toast.makeText(this, "Call decline", Toast.LENGTH_SHORT).show();
        telecomManager.endCall();
    }

    void hangup() {
        Log.d(TAG, "Call hangup");
        if (!checkPermission()) {
            return;
        }
        Toast.makeText(this, "Call hangup", Toast.LENGTH_SHORT).show();
        telecomManager.endCall();
    }
    void senddata() {
        Log.d(TAG, "Call senddata");

        Toast.makeText(this, "Call senddata", Toast.LENGTH_SHORT).show();

    }

    void setup() {
        if (checkPermission()) {
            updatePermissionUI();
            return;
        }
        requestPermission();
    }


}