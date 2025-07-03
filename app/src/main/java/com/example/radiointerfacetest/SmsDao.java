package com.example.radiointerfacetest;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Update;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SmsDao {
    @Insert
    long insert(SmsEntity sms);

    @Update
    int updateSms(SmsEntity sms);

    @Query("SELECT * FROM sms_messages ORDER BY timestamp DESC")
    List<SmsEntity> getAllSms();

    @Query("SELECT * FROM sms_messages ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    List<SmsEntity> getPagedSms(int limit, int offset);

    @Query("SELECT * FROM sms_messages ORDER BY timestamp DESC LIMIT 1")
    SmsEntity getLatestSms();
}