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

import java.io.InterruptedIOException;
import java.util.List;

public class ShellCommandActivity extends AppCompatActivity {

    private final String TAG = ShellCommandActivity.class.getName();

    InputWithHistoryAndClear inputShellCommandComponent;
    Button executeBtn;

    Handler mainHandler = new Handler(Looper.getMainLooper());

    View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int id = v.getId();
            if (id == R.id.btn_execute) {
                execute();
            }
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

        inputShellCommandComponent = findViewById(R.id.input_shell_command_component);
        executeBtn = findViewById(R.id.btn_execute);

        // The clear button is now part of the custom component, its click listener is handled internally
        // We only keep the long click listener for clearing history
        executeBtn.setOnClickListener(mClickListener);

        inputShellCommandComponent.setHistoryCacheKey(InputHistoryCache.KEY_SHELL);
        inputShellCommandComponent.setHint("Shell Command:");
        inputShellCommandComponent.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        inputShellCommandComponent.setText("ls"); // Set default value
    }

    String mCurrentCommand;
    Process mCurrentPingProcess;
    Thread mCurrentPingThread;
    boolean mRequestExit;

    void execute() {
        String command = inputShellCommandComponent.getText();
        if (command.isBlank()) {
            Toast.makeText(this, "Invalid shell command", Toast.LENGTH_SHORT).show();
            return;
        }
        inputShellCommandComponent.saveHistory();

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
            InputHistoryCache.getInstance(this).put(InputHistoryCache.KEY_SHELL, mCurrentCommand);
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
                        if (line == null) { continue; }
//                        Log.d(TAG, "shell: " + line);
                        output.append(line).append("\n");
//                        errorLine = stderrReader.readLine();
//                        if (errorLine != null && !errorLine.isEmpty()) {
//                            Log.d(TAG, "errorLine: " + errorLine);
//                        }

                        mainHandler.post(() -> {
                            TextView textView = findViewById(R.id.shell_info_view);
//                        String old = textView.getText().toString();
                            textView.setText(output.toString());
                            ScrollView view = findViewById(R.id.info_view_container);
                            view.post(() -> view.fullScroll(ScrollView.FOCUS_DOWN));
                        });
                    }
                    Log.d(TAG, "Execute " + mCurrentCommand + " requestexit " +mRequestExit+ " exitValue " + mCurrentPingProcess.exitValue());
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
                    synchronized (ShellCommandActivity.this) {
                        mCurrentPingProcess = null;
                        mCurrentPingThread = null;
                    }
                    executeBtn.setText("Execute!");

                }

            });
            mCurrentPingThread.start();
            executeBtn.setText("Stop Execute");
        }


    }
}