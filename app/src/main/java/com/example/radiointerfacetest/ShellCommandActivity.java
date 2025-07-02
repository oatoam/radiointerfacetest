package com.example.radiointerfacetest;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
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

import java.io.InterruptedIOException;
import java.util.List;

public class ShellCommandActivity extends AppCompatActivity {

    private final String TAG = ShellCommandActivity.class.getName();

    EditText shellET;
    Button executeBtn;

    Spinner shellSpinner;
    private ArrayAdapter<String> shellSpinnerAdapter;
    private List<String> shellSpinnerList;

    Handler mainHandler = new Handler(Looper.getMainLooper());

    View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int id = v.getId();
            if (id == R.id.btn_clear_shell_command) {
                shellET.setText("");
            } else if (id == R.id.btn_execute) {
                execute();
            }
        }
    };

    View.OnLongClickListener mLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            int id = v.getId();
            if (id == R.id.btn_clear_shell_command) {
                Dialog dialog = new android.app.AlertDialog.Builder(ShellCommandActivity.this)
                    .setTitle("Clear History")
                    .setMessage("Are you sure you want to clear all shell command history?")
                    .setPositiveButton("Yes", (dialogInterface, i) -> {
                        InputHistoryCache numberCache = InputHistoryCache.getInstance(ShellCommandActivity.this);
                        numberCache.clear(InputHistoryCache.KEY_SHELL);
                        Toast.makeText(ShellCommandActivity.this, "shell history cleared", Toast.LENGTH_LONG).show();
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
        setContentView(R.layout.activity_shell_command);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        shellET = findViewById(R.id.input_shell_command);
        executeBtn = findViewById(R.id.btn_execute);

        findViewById(R.id.btn_clear_shell_command).setOnClickListener(mClickListener);
        findViewById(R.id.btn_clear_shell_command).setOnLongClickListener(mLongClickListener);
        executeBtn.setOnClickListener(mClickListener);

        shellSpinner = findViewById(R.id.spinner_shell_command);
        InputHistoryCache numberCache = InputHistoryCache.getInstance(this);
        shellSpinnerList = numberCache.getAll(InputHistoryCache.KEY_SHELL);
        shellSpinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, shellSpinnerList);
        shellSpinner.setAdapter(shellSpinnerAdapter);

        numberCache.addListener(() -> {
            Log.d(TAG, "cached shell changed");

            mainHandler.post(()->{
                List<String> caches = numberCache.getAll(InputHistoryCache.KEY_SHELL);
                shellSpinnerList.clear();
                for (String number : caches) {
                    shellSpinnerList.add(number);
                }
//                numberSpinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, numberSpinnerList);
//                numberSpinner.setAdapter(numberSpinnerAdapter);
                shellSpinnerAdapter.notifyDataSetChanged();
            });
        });

        shellSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // 获取选择的项目
                String selectedItem = shellSpinnerList.get(position);
//                Toast.makeText(MainActivity.this, "选择了: " + selectedItem, Toast.LENGTH_SHORT).show();
                shellET.setText(selectedItem);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 没有选择任何项目
            }
        });
    }

    String mCurrentCommand;
    Process mCurrentPingProcess;
    Thread mCurrentPingThread;
    boolean mRequestExit;

    void execute() {
        String command = shellET.getText().toString();
        if (command.isBlank()) {
            Toast.makeText(this, "Invalid shell command", Toast.LENGTH_SHORT).show();
            return;
        }

        synchronized (ShellCommandActivity.this) {
            if (mCurrentPingThread != null) {
                Toast.makeText(this, "Stop Execute " + mCurrentCommand, Toast.LENGTH_SHORT).show();
                mRequestExit = true;
                mCurrentPingProcess.destroy();
                try {
                    mCurrentPingThread.join(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                executeBtn.setText("Execute!");

                return;
            }

            mRequestExit = false;
            mCurrentCommand = command;
            InputHistoryCache.getInstance(this).add(InputHistoryCache.KEY_SHELL, mCurrentCommand);
            mCurrentPingThread = new Thread(() -> {
                try {
                    mCurrentPingProcess = Runtime.getRuntime().exec(mCurrentCommand);
                    java.io.BufferedReader stdoutReader = new java.io.BufferedReader(
                            new java.io.InputStreamReader(mCurrentPingProcess.getInputStream()));
                    java.io.BufferedReader stderrReader = new java.io.BufferedReader(
                            new java.io.InputStreamReader(mCurrentPingProcess.getErrorStream()));
                    StringBuilder output = new StringBuilder();
                    StringBuilder errorMsg = new StringBuilder();
                    String line, errorLine;
                    Log.d(TAG, "execute: " + mCurrentCommand + "running");
                    while (!mRequestExit && mCurrentPingProcess.isAlive()) {
                        line = stdoutReader.readLine();
                        if (line == null) { break; }
//                        Log.d(TAG, "shell: " + line);
                        output.append(line).append("\n");
//                        errorLine = stderrReader.read();
//                        if (errorLine != null && !errorLine.isEmpty()) {
//                            errorMsg.append(errorLine).append("\n");
//                        }

                        mainHandler.post(() -> {
                            TextView textView = findViewById(R.id.shell_info_view);
//                        String old = textView.getText().toString();
                            textView.setText(output.toString());
                            ScrollView view = findViewById(R.id.info_view_container);
                            view.post(() -> view.fullScroll(ScrollView.FOCUS_DOWN));
                        });
                    }
                    Log.d(TAG, "Execute " + mCurrentCommand + " request exit " +mRequestExit+ " or read done");
                    String err = errorMsg.toString();
                    if (!err.isEmpty()) { Log.e(TAG, "error: " + err); }
                    mainHandler.post(() -> {
                        TextView textView = findViewById(R.id.shell_info_view);
//                        String old = textView.getText().toString();
                        textView.setText(output.toString());
                        ScrollView view = findViewById(R.id.info_view_container);
                        view.post(() -> view.fullScroll(ScrollView.FOCUS_DOWN));
                    });
                    if (mCurrentPingProcess.isAlive()) {
                        mCurrentPingProcess.destroy();
                    }
                } catch (Exception e) {
                    if (! (e instanceof InterruptedIOException)) {
                        e.printStackTrace();
//                    mainHandler.post(() -> {
//                        TextView textView = findViewById(R.id.shell_info_view);
//                        textView.setText("Execute failed: " + e.getMessage());
//                    });
                        mainHandler.post(()->{
                            Toast.makeText(this, "Execute failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }

                } finally {
                    mCurrentPingProcess = null;
                    mCurrentPingThread = null;
                    executeBtn.setText("Execute!");
                }

            });
            mCurrentPingThread.start();
            executeBtn.setText("Stop Execute");
        }


    }
}