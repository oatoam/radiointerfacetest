package com.example.radiointerfacetest;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.telecom.Call;
import android.telecom.CallAudioState;
import android.telecom.CallEndpoint;
import android.telecom.InCallService;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;

public class MyIncallServiceImpl extends InCallService {



    private String CHANNEL_ID = "channel_incalls";
    private final String TAG = MyIncallServiceImpl.class.getName();

    private Call.Callback mCallback = new Call.Callback() {
        @Override
        public void onStateChanged(Call call, int state) {
//            Log.d(TAG, "MyIncallServiceImpl's onStateChanged " + InCallActivity.state2String(state));
            super.onStateChanged(call, state);
            if (call == DataHolderSingleton.getInstance().mCurrentCall) {
                sendNotification(call);
            }
            Executors.newSingleThreadExecutor();
            DataHolderSingleton.CallInfo callInfo = DataHolderSingleton.getInstance().mCurrentCallInfos.get(call);
            if (callInfo != null) {
                callInfo.mStates.add(new DataHolderSingleton.CallInfo.State(state, Instant.now()));
            }
        }

        @Override
        public void onCallDestroyed(Call call) {
            if (call == DataHolderSingleton.getInstance().mCurrentCall) {
                NotificationManager mgr = getSystemService(NotificationManager.class);
                mgr.cancel(TAG, 0);
            }
//            call.unregisterCallback(mCallback);
        }
    };

    Intent mIncallIntent;
    Notification.Builder mNotificationBuilder;

    public final static String ACTION_REQUEST_TOGGLE_MUTE = "action_request_toggle_mute";
    public final static String ACTION_REQUEST_TOGGLE_ROUTE = "action_request_toggle_route";

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "received action = " + action);
            switch (action) {
                case ACTION_REQUEST_TOGGLE_MUTE:
                    boolean reqMute = intent.getBooleanExtra("mute", false);
                    setMuted(reqMute);
                    break;
                case ACTION_REQUEST_TOGGLE_ROUTE:
                    int route = intent.getIntExtra("route", 0);
                    setAudioRoute(route);
                    break;
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    public void onCreate() {
        super.onCreate();
        setupNotification();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_REQUEST_TOGGLE_MUTE);
        filter.addAction(ACTION_REQUEST_TOGGLE_ROUTE);
        registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(receiver);

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        List<Call> calls = getCalls();
        for (Call call : calls) {
            DataHolderSingleton.getInstance().mCurrentCall = call;
            DataHolderSingleton.CallInfo callInfo = new DataHolderSingleton.CallInfo();
            callInfo.mCall = call;
            callInfo.mStates.add(new DataHolderSingleton.CallInfo.State(call.getDetails().getState(), Instant.now()));
            DataHolderSingleton.getInstance().mCurrentCallInfos.put(call, callInfo);
        }
        return super.onBind(intent);
    }


    @Override
    public void onAvailableCallEndpointsChanged(@NonNull List<CallEndpoint> availableEndpoints) {
        Log.d(TAG, "onAvailableCallEndpointsChanged");
        super.onAvailableCallEndpointsChanged(availableEndpoints);
        DataHolderSingleton.getInstance().mAvailableCallEndpoints = availableEndpoints;
    }

    @Override
    public void onCallEndpointChanged(@NonNull CallEndpoint callEndpoint) {
        Log.d(TAG, "onCallEndpointChanged");

        super.onCallEndpointChanged(callEndpoint);
        DataHolderSingleton.getInstance().mCurrentCallEndpoint = callEndpoint;
    }

    @Override
    public void onCallAudioStateChanged(CallAudioState audioState) {
        Log.d(TAG, "onCallAudioStateChanged");

        super.onCallAudioStateChanged(audioState);
        DataHolderSingleton.getInstance().mCurrentCallAudioState = audioState;
    }

    void setupNotification() {
        NotificationManager mgr = getSystemService(NotificationManager.class);

        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Incoming Calls", NotificationManager.IMPORTANCE_MAX);
        mgr.createNotificationChannel(channel);

        mIncallIntent = new Intent(Intent.ACTION_MAIN, null);
        mIncallIntent.setFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION | Intent.FLAG_ACTIVITY_NEW_TASK);
        mIncallIntent.setClass(getApplicationContext(), InCallActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 1, mIncallIntent, PendingIntent.FLAG_MUTABLE);
        final Notification.Builder builder = new Notification.Builder(getApplicationContext(), CHANNEL_ID);
        builder.setOngoing(true);
        builder.setPriority(Notification.PRIORITY_HIGH);
        builder.setContentIntent(pendingIntent);
        builder.setFullScreenIntent(pendingIntent, true);

        mNotificationBuilder = builder;
    }

    void sendNotification(Call call) {
        int direction = call.getDetails().getCallDirection();
        String direct = "unknown";
        switch (direction) {
            case Call.Details.DIRECTION_INCOMING: direct = "INCOMING"; break;
            case Call.Details.DIRECTION_OUTGOING: direct = "OUTGOING"; break;
        }

        String text = String.format("%s: %s", direct, call.getDetails().getHandle());

        mNotificationBuilder.setSmallIcon(R.drawable.ic_launcher_foreground);
        mNotificationBuilder.setContentTitle(text);
        mNotificationBuilder.setContentText("Click to open IncallUI\n" + InCallActivity.state2String(call.getDetails().getState()));

        NotificationManager mgr = getSystemService(NotificationManager.class);
        mgr.notify(TAG, 0, mNotificationBuilder.build());


    }

    @Override
    public void onCallAdded(Call call) {
        super.onCallAdded(call);

        InputHistoryCache.getInstance(getApplicationContext()).
                put(InputHistoryCache.KEY_NUMBERS, call.getDetails().getHandle().toString());

        DataHolderSingleton.getInstance().mCurrentCall = call;
        DataHolderSingleton.CallInfo callInfo = new DataHolderSingleton.CallInfo();
        callInfo.mCall = call;
        callInfo.mStates.add(new DataHolderSingleton.CallInfo.State(call.getDetails().getState(), Instant.now()));
        DataHolderSingleton.getInstance().mCurrentCallInfos.put(call, callInfo);

        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
        if (tasks != null && !tasks.isEmpty() && 
            tasks.get(0).topActivity.getPackageName().equals(getPackageName())) {
            startActivity(mIncallIntent);
        }

        sendNotification(call);

        call.registerCallback(mCallback);
    }

    @Override
    public void onCallRemoved(Call call) {
        super.onCallRemoved(call);
        call.unregisterCallback(mCallback);
        DataHolderSingleton.getInstance().mCurrentCallInfos.remove(call);
    }
}
