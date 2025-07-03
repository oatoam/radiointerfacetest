package com.example.radiointerfacetest;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {SmsEntity.class}, version = 3, exportSchema = false)
public abstract class SmsDatabase extends RoomDatabase {
    public abstract SmsDao smsDao();

    private static volatile SmsDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    static final java.util.concurrent.ExecutorService databaseWriteExecutor =
        java.util.concurrent.Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public static SmsDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (SmsDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            SmsDatabase.class, "sms_database")
                            .fallbackToDestructiveMigration() // 允许破坏性迁移
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}