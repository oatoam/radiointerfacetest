package com.example.radiointerfacetest;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Telephony;
import android.telecom.Call;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;


import java.util.ArrayList;
import java.util.List;

public class SmsReceiver extends BroadcastReceiver {
    final String TAG = SmsReceiver.class.getName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "get " + intent.getAction().toString());
        if (intent.getAction().equals(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {
            Log.d(TAG, "SMS received intent detected");
            SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);

            Bundle[] bundles = new Bundle[messages.length];
            int count = 0;
            for (SmsMessage message : messages) {
                String sender = message.getDisplayOriginatingAddress();
                String messageBody = message.getMessageBody();
                Bundle bundle = new Bundle();
                bundle.putString("sender", sender);
                bundle.putString("message", messageBody);
                bundles[count++] = bundle;

                Log.d(TAG, "sendBroadcast for sender " + sender);
            }
            if (count > 0) {
                Intent showSmsIntent = new Intent("SMS_RECEIVED_ACTION");
                showSmsIntent.putExtra("sms", bundles);
                context.sendBroadcast(showSmsIntent);
            }

        }
    }
}