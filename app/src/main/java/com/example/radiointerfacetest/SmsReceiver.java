package com.example.radiointerfacetest;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build; // 新增导入
import android.os.Bundle;
import android.provider.Telephony;
import android.telecom.Call;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionInfo;
import androidx.core.app.NotificationCompat; // 新增导入

import java.util.ArrayList;
import java.util.List;
import java.util.Date;


public class SmsReceiver extends BroadcastReceiver {
    final String TAG = SmsReceiver.class.getName();
    private static final String CHANNEL_ID = "sms_notification_channel";
    private static final int NOTIFICATION_ID = 1;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "get " + intent.getAction().toString());
        if (intent.getAction().equals(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {
            Log.d(TAG, "SMS received intent detected");
            SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);

            if (messages != null && messages.length > 0) {
                SmsDatabase db = SmsDatabase.getDatabase(context);
                SmsDao smsDao = db.smsDao();

                StringBuilder notificationMessage = new StringBuilder(); // 用于构建通知消息

                for (SmsMessage message : messages) {
                    String sender = message.getDisplayOriginatingAddress();
                    String messageBody = message.getMessageBody();
                    long timestamp = message.getTimestampMillis();
                    String phoneNumber = ""; // 接收短信的手机号
                    int simSlot = -1; // 所属SIM卡槽

                    // 尝试获取接收短信的手机号和SIM卡槽信息
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                            int subId = intent.getIntExtra("subscription", -1);
                            if (subId != -1) {
                                SubscriptionManager subscriptionManager = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
                                if (subscriptionManager != null) {
                                    SubscriptionInfo info = subscriptionManager.getActiveSubscriptionInfo(subId);
                                    if (info != null) {
                                        simSlot = info.getSimSlotIndex();
                                    }
                                }
                            }
                        }
                        phoneNumber = "DevicePhoneNumber"; // Placeholder, actual implementation is more complex
                    } catch (Exception e) {
                        Log.e(TAG, "Error getting phone number or SIM slot: " + e.getMessage());
                    }

                    SmsEntity sms = new SmsEntity(sender, messageBody, timestamp, phoneNumber, simSlot, false); // isSent = false for received SMS
                    sms.isHighlighted = true;

                    SmsDatabase.databaseWriteExecutor.execute(() -> {
                        // 插入短信到数据库
                        long id = smsDao.insert(sms);
                        sms.id = id; // 更新ID
                        Log.d(TAG, "SMS saved to DB: " + sender + " - " + messageBody);
                    });
                    

                    notificationMessage.append("来自 ").append(sender).append(": ").append(messageBody).append("\n");
                }

                // 发送通知
                sendSmsNotification(context, notificationMessage.toString().trim());
            }

            // 广播现有逻辑，如果需要的话
            // 将数据库查询操作放到单独的线程中

            SmsDatabase.databaseWriteExecutor.execute(() -> {
                SmsDatabase db = SmsDatabase.getDatabase(context);
                SmsDao smsDao = db.smsDao();
                // 由于可能有多条短信同时到达，这里需要获取所有新插入的短信
                // 简单起见，这里假设每次广播只处理一条短信，或者获取最近的N条短信
                // 更严谨的做法是，在保存短信时，将新短信的ID收集起来，然后根据ID查询
                // 这里先获取最近的messages.length条短信
                List<SmsEntity> latestSmsList = smsDao.getPagedSms(messages.length, 0);
                ArrayList<SmsEntity> receivedSmsList = new ArrayList<>();
                for (SmsEntity sms : latestSmsList) {
                    receivedSmsList.add(sms);
                }

                if (!receivedSmsList.isEmpty()) {
                    Intent showSmsIntent = new Intent("SMS_RECEIVED_ACTION");
                    showSmsIntent.putParcelableArrayListExtra("new_sms_list", receivedSmsList);
                    context.sendBroadcast(showSmsIntent);
                }
            });
        }
    }

    private void sendSmsNotification(Context context, String message) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // 创建通知渠道 (适用于Android 8.0及以上)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(context.getString(R.string.notification_channel_description));
            notificationManager.createNotificationChannel(channel);
        }

        // 点击通知后跳转到SmsListActivity
        Intent intent = new Intent(context, SmsListActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // 构建通知
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_email) // 设置小图标
                .setContentTitle(context.getString(R.string.notification_title))
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH) // 设置为高优先级，可能弹出
                .setOngoing(true) // 设置为持久通知
                .setContentIntent(pendingIntent)
                .setAutoCancel(true); // 点击通知后自动清除

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }
}