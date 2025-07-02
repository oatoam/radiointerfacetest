package com.example.radiointerfacetest;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class RespondViaMessageService extends Service {

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