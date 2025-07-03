package com.example.radiointerfacetest;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.List;

public class PingTestActivity extends AppCompatActivity {

    private final String TAG = PingTestActivity.class.getName();

    InputWithHistoryAndClear inputRemoteIpComponent;
    Button pingBtn;

    Handler mainHandler = new Handler(Looper.getMainLooper());

    View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int id = v.getId();
            if (id == R.id.btn_ping) {
                ping();
            }
        }
    };

    View.OnLongClickListener mLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            int id = v.getId();
//            if (id == R.id.btn_clear_ip) { // This button is now part of the custom component, but long click is still handled here
//                Dialog dialog = new android.app.AlertDialog.Builder(PingTestActivity.this)
//                    .setTitle("Clear History")
//                    .setMessage("Are you sure you want to clear all IP history?")
//                    .setPositiveButton("Yes", (dialogInterface, i) -> {
//                        InputHistoryCache numberCache = InputHistoryCache.getInstance(PingTestActivity.this);
//                        numberCache.clear(InputHistoryCache.KEY_IP);
//                        Toast.makeText(PingTestActivity.this, "IP history cleared", Toast.LENGTH_LONG).show();
//                        inputRemoteIpComponent.setText(""); // Clear the text in the component
//                    })
//                    .setNegativeButton("No", null)
//                    .create();
//                dialog.show();
//            }
            return true;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ping_test);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        inputRemoteIpComponent = findViewById(R.id.input_remote_ip_component);
        pingBtn = findViewById(R.id.btn_ping);

        // The clear button is now part of the custom component, its click listener is handled internally
        // We only keep the long click listener for clearing history
        pingBtn.setOnClickListener(mClickListener);

        inputRemoteIpComponent.setHistoryCacheKey(InputHistoryCache.KEY_IP);
        inputRemoteIpComponent.setHint(getString(R.string.remote_ip_info));
        inputRemoteIpComponent.setInputType(android.text.InputType.TYPE_CLASS_PHONE); // Assuming IP is entered as phone number type
        inputRemoteIpComponent.setText("127.0.0.1"); // Set default value
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mCurrentPingThread != null) {
            Toast.makeText(this, "Stop Ping " + mCurrentPingIP, Toast.LENGTH_SHORT).show();
            mRequestExit = true;
            try {
                mCurrentPingThread.join(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            pingBtn.setText("Ping!");
        }
    }

    boolean isValidIPv4(String ip) {
        String regex = "^((25[0-5]|(2[0-4]\\d|1\\d{2}|[1-9]?\\d))(\\.(?!$)|$)){4}$";
        return ip.matches(regex);
    }

    String mCurrentPingIP;
//    Process mCurrentPingProcess;
    Thread mCurrentPingThread;
    boolean mRequestExit;

    void ping() {
        String ip = inputRemoteIpComponent.getText();
        if (!isValidIPv4(ip)) {
            Toast.makeText(this, "Invalid IP address", Toast.LENGTH_SHORT).show();
            return;
        }
        inputRemoteIpComponent.saveHistory();

        synchronized (PingTestActivity.this) {
            if (mCurrentPingThread != null) {
                Toast.makeText(this, "Stop Ping " + mCurrentPingIP, Toast.LENGTH_SHORT).show();
                mRequestExit = true;
                try {
                    mCurrentPingThread.join(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                pingBtn.setText("Ping!");

                return;
            }

            mRequestExit = false;
            mCurrentPingIP = ip;
            InputHistoryCache.getInstance(this).put(InputHistoryCache.KEY_IP, mCurrentPingIP);
            mCurrentPingThread = new Thread(() -> {
                try {
                    Process process = Runtime.getRuntime().exec("ping " + ip);
                    java.io.BufferedReader reader = new java.io.BufferedReader(
                            new java.io.InputStreamReader(process.getInputStream()));
                    StringBuilder output = new StringBuilder();
                    String line;
                    Log.d(TAG, "ping " + ip + "running");
                    while (!mRequestExit && (line = reader.readLine()) != null) {
                        Log.d(TAG, "ping: " + line);
                        output.append(line).append("\n");

                        mainHandler.post(() -> {
                            TextView textView = findViewById(R.id.info_view);
//                        String old = textView.getText().toString();
                            textView.setText(output.toString());
                            ScrollView view = findViewById(R.id.info_view_container);
                            view.post(() -> view.fullScroll(ScrollView.FOCUS_DOWN));
                        });
                    }
                    Log.d(TAG, "ping " + ip + " request exit " +mRequestExit+ " or read done");
                    if (process.isAlive()) {
                        process.destroy();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    mainHandler.post(() -> {
                        TextView textView = findViewById(R.id.info_view);
                        textView.setText("Ping failed: " + e.getMessage());
                    });
                }
                mCurrentPingThread = null;
            });
            mCurrentPingThread.start();
            pingBtn.setText("Stop Ping");
        }


    }
}