package com.example.radiointerfacetest;

import android.telecom.CallAudioState;
import android.telecom.Connection;
import android.telecom.TelecomManager;
import android.util.Log;

public class MyConnection extends Connection {
    private final String TAG = MyConnection.class.getName();

    @Override
    public void onShowIncomingCallUi() {
        Log.d(TAG, "onShowIncomingCallUi");
        super.onShowIncomingCallUi();
    }

    @Override
    public void onCallAudioStateChanged(CallAudioState state) {
        Log.d(TAG, "onCallAudioStateChanged");
        super.onCallAudioStateChanged(state);
    }

    @Override
    public void onHold() {
        Log.d(TAG, "onHold");
        super.onHold();
    }

    @Override
    public void onUnhold() {
        Log.d(TAG, "onUnhold");
        super.onUnhold();
    }

    @Override
    public void onAnswer() {
        Log.d(TAG, "onAnswer");
        super.onAnswer();
    }

    @Override
    public void onReject() {
        Log.d(TAG, "onReject");
        super.onReject();
    }

    @Override
    public void onDisconnect() {
        Log.d(TAG, "onDisconnect");
        super.onDisconnect();
    }
}
