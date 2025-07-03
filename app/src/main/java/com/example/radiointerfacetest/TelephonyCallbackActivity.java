package com.example.radiointerfacetest;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.BarringInfo;
import android.telephony.CellIdentity;
import android.telephony.CellInfo;
import android.telephony.CellLocation;
import android.telephony.PhysicalChannelConfig;
import android.telephony.PreciseDataConnectionState;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.telephony.emergency.EmergencyNumber;
import android.telephony.ims.ImsReasonInfo;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class TelephonyCallbackActivity extends AppCompatActivity {

    private final static String TAG = TelephonyCallbackActivity.class.getName();

    private TextView tvInfo;

    private class MyTelephonyCallback extends TelephonyCallback implements
            TelephonyCallback.ActiveDataSubscriptionIdListener,
//            TelephonyCallback.BarringInfoListener,
//            TelephonyCallback.CallDisconnectCauseListener,
            TelephonyCallback.CallForwardingIndicatorListener,
            TelephonyCallback.CarrierNetworkListener,
            TelephonyCallback.CallStateListener,
            TelephonyCallback.CellInfoListener,
            TelephonyCallback.CellLocationListener,
            TelephonyCallback.DataActivationStateListener,
            TelephonyCallback.DataActivityListener,
            TelephonyCallback.EmergencyNumberListListener,
//            TelephonyCallback.ImsCallDisconnectCauseListener,
            TelephonyCallback.MessageWaitingIndicatorListener,
//            TelephonyCallback.PhysicalChannelConfigListener,
//            TelephonyCallback.PreciseDataConnectionStateListener,
//            TelephonyCallback.RegistrationFailedListener,
            TelephonyCallback.ServiceStateListener,
            TelephonyCallback.SignalStrengthsListener,
            TelephonyCallback.UserMobileDataStateListener {

        private int mSubId = -1;

        MyTelephonyCallback(int subId) {
            mSubId = subId;
        }

        @Override
        public void onActiveDataSubscriptionIdChanged(int subId) {
            log(String.format("onActiveDataSubscriptionIdChanged subId=%d", subId));
        }

//        @Override
//        public void onBarringInfoChanged(@NonNull BarringInfo barringInfo) {
//            log(String.format("onBarringInfoChanged barringInfo=%s", barringInfo));
//        }

//        @Override
//        public void onCallDisconnectCauseChanged(int disconnectCause, int preciseDisconnectCause) {
//            log(String.format("onCallDisconnectCauseChanged disconnectCause=%d, preciseDisconnectCause=%d", disconnectCause, preciseDisconnectCause));
//        }

        @Override
        public void onCallForwardingIndicatorChanged(boolean cfi) {
            log(String.format("onCallForwardingIndicatorChanged cfi=%b", cfi));
        }

        int lastCallState = TelephonyManager.CALL_STATE_IDLE;

        @Override
        public void onCallStateChanged(int state) {
            log(String.format("onCallStateChanged state=%d", state));
            if (lastCallState == TelephonyManager.CALL_STATE_OFFHOOK
                    && state == TelephonyManager.CALL_STATE_IDLE) {
                // in/out call end
                Log.d(TAG, "in/out call end" + " subid " + mSubId);
            }
            if (lastCallState == TelephonyManager.CALL_STATE_RINGING
                    && state == TelephonyManager.CALL_STATE_IDLE) {
                // in call hangup
                Log.d(TAG, "in call hangup" + " subid " + mSubId);
            }
            if (lastCallState == TelephonyManager.CALL_STATE_RINGING
                    && state == TelephonyManager.CALL_STATE_OFFHOOK) {
                // in call answered
                Log.d(TAG, "in call answered" + " subid " + mSubId);
            }
            if (lastCallState == TelephonyManager.CALL_STATE_IDLE
                    && state == TelephonyManager.CALL_STATE_RINGING) {
                // in call ringing
                Log.d(TAG, "in call ringing" + " subid " + mSubId);
            }
            if (lastCallState == TelephonyManager.CALL_STATE_IDLE
                    && state == TelephonyManager.CALL_STATE_OFFHOOK) {
                // out call answered
                Log.d(TAG, "out call answered" + " subid " + mSubId);
            }
            lastCallState = state;
        }

        @Override
        public void onCarrierNetworkChange(boolean active) {
            log(String.format("onCarrierNetworkChange active=%b", active));
        }

        @Override
        public void onCellInfoChanged(@NonNull List<CellInfo> cellInfo) {
            log(String.format("onCellInfoChanged cellInfo=%s", cellInfo));
        }

        @Override
        public void onCellLocationChanged(@NonNull CellLocation location) {
            log(String.format("onCellLocationChanged location=%s", location));
        }

        @Override
        public void onDataActivationStateChanged(int state) {
            log(String.format("onDataActivationStateChanged state=%d", state));
        }

        @Override
        public void onDataActivity(int direction) {
            log(String.format("onDataActivity direction=%d", direction));
        }

        @Override
        public void onEmergencyNumberListChanged(@NonNull Map<Integer, List<EmergencyNumber>> emergencyNumberList) {
            log(String.format("onEmergencyNumberListChanged emergencyNumberList=%s", emergencyNumberList));
        }

//        @Override
//        public void onImsCallDisconnectCauseChanged(@NonNull ImsReasonInfo imsReasonInfo) {
//            log(String.format("onImsCallDisconnectCauseChanged imsReasonInfo=%s", imsReasonInfo));
//        }

        @Override
        public void onMessageWaitingIndicatorChanged(boolean mwi) {
            log(String.format("onMessageWaitingIndicatorChanged mwi=%b", mwi));
        }

//        @Override
//        public void onPhysicalChannelConfigChanged(@NonNull List<PhysicalChannelConfig> configs) {
//            log(String.format("onPhysicalChannelConfigChanged configs=%s", configs));
//        }

//        @Override
//        public void onPreciseDataConnectionStateChanged(@NonNull PreciseDataConnectionState dataConnectionState) {
//            log(String.format("onPreciseDataConnectionStateChanged dataConnectionState=%s", dataConnectionState));
//        }

//        @Override
//        public void onRegistrationFailed(@NonNull CellIdentity cellIdentity, @NonNull String chosenPlmn, int domain, int causeCode, int additionalCauseCode) {
//            log(String.format("onRegistrationFailed cellIdentity=%s, chosenPlmn=%s, domain=%d, causeCode=%d, additionalCauseCode=%d",
//                cellIdentity, chosenPlmn, domain, causeCode, additionalCauseCode));
//        }

        @Override
        public void onServiceStateChanged(@NonNull ServiceState serviceState) {
            log(String.format("onServiceStateChanged serviceState=%s", serviceState));
        }

        @Override
        public void onSignalStrengthsChanged(@NonNull SignalStrength signalStrength) {
            log(String.format("onSignalStrengthsChanged signalStrength=%s", signalStrength));
        }

        @Override
        public void onUserMobileDataStateChanged(boolean enabled) {
            log(String.format("onUserMobileDataStateChanged enabled=%b", enabled));
        }
    }

//    MyTelephonyCallback mTelephonyCallback = new MyTelephonyCallback();

    TelephonyManager mTelephonyManager;
    SubscriptionManager mSubscriptionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_telephony_callback);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mSubscriptionManager = (SubscriptionManager) getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);

        Log.d(TAG, "registerTelephonyCallback begin");

        // 获取所有活跃的 Subscription IDs
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        List<SubscriptionInfo> activeSubscriptions = mSubscriptionManager.getActiveSubscriptionInfoList();
        if (activeSubscriptions == null) {
            Log.w(TAG, "No active subscriptions found.");
            return;
        }


        for (SubscriptionInfo subInfo : activeSubscriptions) {

            final int subId = subInfo.getSubscriptionId();
            TelephonyManager subTelephonyManager = mTelephonyManager.createForSubscriptionId(subId);
            subTelephonyManager.registerTelephonyCallback(getMainExecutor(), new MyTelephonyCallback(subId));
        }

        Log.d(TAG, "registerTelephonyCallback done");
    }

    void log(String info) {
//        if (tvInfo == null) {
//            Log.d(TAG, "info: " + info);
//            return;
//        }
        tvInfo = findViewById(R.id.callback_info);

        String lastInfo = tvInfo.getText().toString();
        lastInfo += String.format("%s: %s\n--------\n", LocalDateTime.now(), info);
        final String constLastInfo = lastInfo;
        getMainExecutor().execute(new Runnable() {
            @Override
            public void run() {
                tvInfo.setText(constLastInfo);
            }
        });
    }

}