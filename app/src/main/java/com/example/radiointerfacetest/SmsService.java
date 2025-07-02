package com.example.radiointerfacetest;

import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.telecom.TelecomManager;
import android.telephony.SmsManager;
import androidx.annotation.Nullable;

public class SmsService extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Handle the service actions here if necessary
        return START_NOT_STICKY;
    }
}