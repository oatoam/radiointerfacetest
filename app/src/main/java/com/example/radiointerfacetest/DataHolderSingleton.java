package com.example.radiointerfacetest;

import android.telecom.Call;
import android.telecom.CallAudioState;
import android.telecom.CallEndpoint;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DataHolderSingleton {
    private static DataHolderSingleton sInstance;
    public static DataHolderSingleton getInstance() {
        if (sInstance == null) {
            synchronized (DataHolderSingleton.class) {
                if (sInstance == null) {
                    sInstance = new DataHolderSingleton();
                }
            }
        }

        return sInstance;
    }

    Call mCurrentCall;

    public static class CallInfo {
        public Call mCall;
        public static class State {
            public int mState;
            public Instant mStart;
            public State(int state, Instant start) {
                this.mState = state;
                this.mStart = start;
            }
        }
        public List<State> mStates = new ArrayList<>();
    }

    public Map<Call, CallInfo> mCurrentCallInfos = new HashMap<>();

    public List<CallEndpoint> mAvailableCallEndpoints = new ArrayList<>();
    public CallEndpoint mCurrentCallEndpoint;
    public CallAudioState mCurrentCallAudioState;

}
