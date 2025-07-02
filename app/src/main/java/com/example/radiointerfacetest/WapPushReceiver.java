package com.example.radiointerfacetest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

public class WapPushReceiver extends BroadcastReceiver {
    final String TAG = WapPushReceiver.class.getName();
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "get " + intent.getAction().toString());
        if (intent.getAction().equals(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {
            Log.d(TAG, "SMS received intent detected");
            SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
            for (SmsMessage message : messages) {
                String sender = message.getDisplayOriginatingAddress();
                String messageBody = message.getMessageBody();
                Intent showSmsIntent = new Intent("SMS_RECEIVED_ACTION");
                showSmsIntent.putExtra("sender", sender);
                showSmsIntent.putExtra("message", messageBody);
                context.sendBroadcast(showSmsIntent);
                Log.d(TAG, "sendBroadcast for sender " + sender);
            }
        }
    }
}