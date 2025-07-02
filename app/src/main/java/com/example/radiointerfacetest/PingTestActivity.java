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

    EditText ipET;
    Button pingBtn;

    Spinner ipSpinner;
    private ArrayAdapter<String> ipSpinnerAdapter;
    private List<String> ipSpinnerList;

    Handler mainHandler = new Handler(Looper.getMainLooper());

    View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int id = v.getId();
            if (id == R.id.btn_clear_ip) {
                ipET.setText("");
            } else if (id == R.id.btn_ping) {
                ping();
            }
        }
    };

    View.OnLongClickListener mLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            int id = v.getId();
            if (id == R.id.btn_clear_ip) {
                Dialog dialog = new android.app.AlertDialog.Builder(PingTestActivity.this)
                    .setTitle("Clear History")
                    .setMessage("Are you sure you want to clear all IP history?")
                    .setPositiveButton("Yes", (dialogInterface, i) -> {
                        InputHistoryCache numberCache = InputHistoryCache.getInstance(PingTestActivity.this);
                        numberCache.clear(InputHistoryCache.KEY_IP);
                        Toast.makeText(PingTestActivity.this, "IP history cleared", Toast.LENGTH_LONG).show();
                    })
                    .setNegativeButton("No", null)
                    .create();
                dialog.show();
            }
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

        ipET = findViewById(R.id.remote_ip);
        pingBtn = findViewById(R.id.btn_ping);

        findViewById(R.id.btn_clear_ip).setOnClickListener(mClickListener);
        findViewById(R.id.btn_clear_ip).setOnLongClickListener(mLongClickListener);
        pingBtn.setOnClickListener(mClickListener);

        ipSpinner = findViewById(R.id.spinner_ip);
        InputHistoryCache numberCache = InputHistoryCache.getInstance(this);
        ipSpinnerList = numberCache.getAll(InputHistoryCache.KEY_IP);
        ipSpinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, ipSpinnerList);
        ipSpinner.setAdapter(ipSpinnerAdapter);

        numberCache.addListener(() -> {
            Log.d(TAG, "cached number changed");

            mainHandler.post(()->{
                List<String> caches = numberCache.getAll(InputHistoryCache.KEY_IP);
                ipSpinnerList.clear();
                for (String number : caches) {
                    ipSpinnerList.add(number);
                }
//                numberSpinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, numberSpinnerList);
//                numberSpinner.setAdapter(numberSpinnerAdapter);
                ipSpinnerAdapter.notifyDataSetChanged();
            });
        });

        ipSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // 获取选择的项目
                String selectedItem = ipSpinnerList.get(position);
//                Toast.makeText(MainActivity.this, "选择了: " + selectedItem, Toast.LENGTH_SHORT).show();
                ipET.setText(selectedItem);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 没有选择任何项目
            }
        });
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
        String ip = ipET.getText().toString();
        if (!isValidIPv4(ip)) {
            Toast.makeText(this, "Invalid IP address", Toast.LENGTH_SHORT).show();
            return;
        }

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
            InputHistoryCache.getInstance(this).add(InputHistoryCache.KEY_IP, mCurrentPingIP);
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