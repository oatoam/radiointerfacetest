package com.example.radiointerfacetest;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telecom.Call;
import android.telecom.CallAudioState;
import android.telecom.CallEndpoint;
import android.telecom.TelecomManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InCallActivity extends AppCompatActivity {

    private final String TAG = InCallActivity.class.getName();
    private Button acceptButton;
    private Button declineButton;
    private Button holdButton;
    private Button unholdButton;
    private Button muteButton;
    private Button routeButton;
    private TextView callStateText;
    private TextView callInfoText;

    private TextView allCallInfoText;

    private Call mCurrentCall;

    public static String state2String(int state) {
        switch (state) {
            case Call.STATE_NEW: return "STATE_NEW";
            case Call.STATE_DIALING: return "STATE_DIALING";
            case Call.STATE_RINGING: return "STATE_RINGING";
            case Call.STATE_HOLDING: return "STATE_HOLDING";
            case Call.STATE_ACTIVE: return "STATE_ACTIVE";
            case Call.STATE_DISCONNECTED: return "STATE_DISCONNECTED";
            case Call.STATE_SELECT_PHONE_ACCOUNT: return "STATE_SELECT_PHONE_ACCOUNT";
            case Call.STATE_CONNECTING: return "STATE_CONNECTING";
            case Call.STATE_DISCONNECTING: return "STATE_DISCONNECTING";
            case Call.STATE_PULLING_CALL: return "STATE_PULLING_CALL";
            case Call.STATE_AUDIO_PROCESSING: return "STATE_AUDIO_PROCESSING";
            case Call.STATE_SIMULATED_RINGING: return "STATE_SIMULATED_RINGING";
        }
        return "UNKNOWN";
    }

    private Call.Callback mCallback = new Call.Callback() {
        @Override
        public void onStateChanged(Call call, int state) {
            callStateText.setText(state2String(state));
//            Log.d(TAG, "InCallActivity's onStateChanged " + state2String(state));
            updateAllCallStates();
        }

        @Override
        public void onCallDestroyed(Call call) {
            super.onCallDestroyed(call);
            finish();
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incall);

        callInfoText = findViewById(R.id.call_info_text);
        acceptButton = findViewById(R.id.accept_button);
        declineButton = findViewById(R.id.decline_button);
        holdButton = findViewById(R.id.hold_button);
        muteButton = findViewById(R.id.mute_button);
        routeButton = findViewById(R.id.audio_route_button);
        unholdButton = findViewById(R.id.unhold_button);
        callStateText = findViewById(R.id.call_state_text);
        allCallInfoText = findViewById(R.id.all_call_info_text);

        mCurrentCall = DataHolderSingleton.getInstance().mCurrentCall;

        mCurrentCall.registerCallback(mCallback);
        callStateText.setText(state2String(mCurrentCall.getDetails().getState()));
        callInfoText.setText(mCurrentCall.getDetails().getHandle().toString());
        setupListeners();

        timerThread.start();
    }

    Handler mainHandler = new Handler(Looper.getMainLooper());
    boolean requestTimerThreadExit = false;
    private Thread timerThread = new Thread(new Runnable() {
        @Override
        public void run() {
            while (!requestTimerThreadExit) {

                updateAllCallStates();

                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    });

    @Override
    protected void onDestroy() {
        super.onDestroy();
        requestTimerThreadExit = true;
        try {
            timerThread.join(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressLint("DefaultLocale")
    void updateAllCallStates() {
        Map<Call, DataHolderSingleton.CallInfo> map = DataHolderSingleton.getInstance().mCurrentCallInfos;
        if (map == null) {
            return;
        }
        Instant now = Instant.now();
        String info = "";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            info += String.format("Mute: %s\nEndpoint: %s\n\n", false, DataHolderSingleton.getInstance().mCurrentCallEndpoint.getEndpointName());
        } else {
            CallAudioState state = DataHolderSingleton.getInstance().mCurrentCallAudioState;
            info += String.format("Mute: %s\nRoute: %s\n\n", state.isMuted(), CallAudioState.audioRouteToString(state.getRoute()));
        }

        for (DataHolderSingleton.CallInfo c : map.values()) {
            DataHolderSingleton.CallInfo.State lastState = c.mStates.get(c.mStates.size()-1);
            Duration duration = Duration.between(lastState.mStart, now);
            info += String.format("%s: %s: %d\n", c.mCall.getDetails().getHandle().toString(),
                    InCallActivity.state2String(lastState.mState),
                    duration.getSeconds());

        }


        final String finalInfo = info;

        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                allCallInfoText.setText(finalInfo);
            }
        });

    }


    private void setupListeners() {
        acceptButton.setOnClickListener(v -> {
            // Handle accept call
            acceptCall();
        });



        declineButton.setOnClickListener(v -> {
            // Handle decline call
            declineCall();
        });

        holdButton.setOnClickListener(v -> {
            // Handle accept call
            holdCall();
        });

        unholdButton.setOnClickListener(v -> {
            // Handle accept call
            unholdCall();
        });

        muteButton.setOnClickListener(v -> {
            toggleMicrophoneMute();
        });

        routeButton.setOnClickListener(v -> {
            toggleSpeaker();
        });
    }

    private void acceptCall() {
        // Implement call acceptance logic here
        mCurrentCall.answer(0);
    }

    @SuppressLint("MissingPermission")
    private void declineCall() {
        Log.d(TAG, "declineCall");
        // Implement call decline logic here
        int callState = 0;
        callState = mCurrentCall.getDetails().getState();
        if (callState == Call.STATE_DIALING || callState == Call.STATE_RINGING) {
            mCurrentCall.reject(Call.REJECT_REASON_DECLINED);
        } else {
            TelecomManager tm = (TelecomManager) getSystemService(TELECOM_SERVICE);
            tm.endCall();
        }
    }

    void holdCall() {
        mCurrentCall.hold();
    }

    void unholdCall() {
        mCurrentCall.unhold();
    }

//    boolean microphoneMute = false;
    void toggleMicrophoneMute() {
        Log.d(TAG, "microphoneMute");
        CallAudioState state = DataHolderSingleton.getInstance().mCurrentCallAudioState;
        boolean microphoneMuted = state.isMuted();
//        AudioManager audioManager = getSystemService(AudioManager.class);
//        audioManager.setMicrophoneMute(microphoneMute);
//        muteButton.setText(microphoneMuted ? "Unmute" : "Mute");

        Intent intent = new Intent();
        intent.setAction(MyIncallServiceImpl.ACTION_REQUEST_TOGGLE_MUTE);
        intent.putExtra("mute", !microphoneMuted);

        sendBroadcast(intent);

//        info += String.format("Mute: %s\nRoute: %s\n\n", , CallAudioState.audioRouteToString(state.getRoute()));
    }

    void toggleSpeaker() {
        Log.d(TAG, "toggleAudioRoute");
        CallAudioState state = DataHolderSingleton.getInstance().mCurrentCallAudioState;

        int route = 0;

        if (state.getRoute() != CallAudioState.ROUTE_SPEAKER) {
            route = CallAudioState.ROUTE_SPEAKER;
        }

        Intent intent = new Intent();
        intent.setAction(MyIncallServiceImpl.ACTION_REQUEST_TOGGLE_ROUTE);
        intent.putExtra("route", route);
        sendBroadcast(intent);

    }
}
