package com.example.radiointerfacetest;

import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.app.PendingIntent; // 新增导入
import android.content.BroadcastReceiver; // 新增导入
import android.content.Context; // 新增导入
import android.content.DialogInterface;
import android.content.Intent; // 新增导入
import android.content.IntentFilter; // 新增导入
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.SmsManager; // 新增导入
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter; // 新增导入
import android.widget.Button; // 新增导入
import android.widget.EditText; // 新增导入
import android.widget.Spinner; // 新增导入
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.AppCompatImageButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SmsListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private SmsAdapter smsAdapter;
    private List<SmsEntity> smsList = new ArrayList<>();
    private LinearLayoutManager layoutManager;

    private InputWithHistoryAndClear inputNumberComponent;
    private InputWithHistoryAndClear inputMessageComponent;
    private Button sendSmsButton;

    private int currentPage = 0;
    private final int PAGE_SIZE = 20;
    private boolean isLoading = false;
    private boolean allSmsLoaded = false;

    private BroadcastReceiver smsReceiver; // 声明BroadcastReceiver

    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms_list);

        recyclerView = findViewById(R.id.sms_recycler_view);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        inputNumberComponent = findViewById(R.id.input_number_component);
        inputMessageComponent = findViewById(R.id.input_message_component);
        sendSmsButton = findViewById(R.id.send_sms_button);

        inputNumberComponent.setHistoryCacheKey(InputHistoryCache.KEY_NUMBERS);
        inputNumberComponent.setHint(getString(R.string.enter_phone_number));
        inputNumberComponent.setInputType(android.text.InputType.TYPE_CLASS_PHONE);

        inputMessageComponent.setHistoryCacheKey(InputHistoryCache.KEY_SMS_MESSAGES);
        inputMessageComponent.setHint(getString(R.string.enter_sms_message));
        inputMessageComponent.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);

        sendSmsButton.setOnClickListener(v -> sendSms());

        smsAdapter = new SmsAdapter(smsList);
        recyclerView.setAdapter(smsAdapter);

        // 添加分隔线
        recyclerView.addItemDecoration(new SmsItemDecoration(getResources().getDrawable(R.drawable.divider, null)));

        // 添加分隔线
        recyclerView.addItemDecoration(new SmsItemDecoration(getResources().getDrawable(R.drawable.divider, null)));

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                if (!isLoading && !allSmsLoaded) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0
                            && totalItemCount >= PAGE_SIZE) {
                        loadMoreSmsMessages();
                    }
                }
            }
        });

        // 初始化并注册BroadcastReceiver
        smsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("SMS_RECEIVED_ACTION".equals(intent.getAction())) {
                    // 收到新短信广播，重新加载数据
                    // 收到新短信广播，从Intent中获取所有新短信并插入到列表顶部
                    ArrayList<SmsEntity> newSmsList = intent.getParcelableArrayListExtra("new_sms_list");
                    if (newSmsList != null && !newSmsList.isEmpty()) {
                        runOnUiThread(() -> {
                            int insertedCount = newSmsList.size();
                            for (SmsEntity newSms : newSmsList) {
                                // 确保接收到的短信 isSent 字段为 false
                                newSms.isSent = false;
                                smsList.add(0, newSms); // 插入到顶部
                            }
                            if (insertedCount > 0) {
                                smsAdapter.notifyItemRangeInserted(0, insertedCount);
                                // 如果列表可见，平滑滚动到顶部
                                if (layoutManager.findFirstVisibleItemPosition() == 0) {
                                    recyclerView.smoothScrollToPosition(0);
                                }
                                Toast.makeText(context, getString(R.string.toast_new_sms_received_list_refreshed), Toast.LENGTH_SHORT).show();

                            }
                        });
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter("SMS_RECEIVED_ACTION");
        registerReceiver(smsReceiver, filter);

        loadMoreSmsMessages(); // 首次加载数据
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 取消注册BroadcastReceiver
        if (smsReceiver != null) {
            unregisterReceiver(smsReceiver);
        }
    }

    private void loadMoreSmsMessages() {
        if (isLoading || allSmsLoaded) {
            return;
        }
        isLoading = true;

        SmsDatabase db = SmsDatabase.getDatabase(this);
        SmsDao smsDao = db.smsDao();

        SmsDatabase.databaseWriteExecutor.execute(() -> {
            List<SmsEntity> newSms = smsDao.getPagedSms(PAGE_SIZE, currentPage * PAGE_SIZE);
            // 确保加载的数据包含 isSent 字段
            for (SmsEntity sms : newSms) {
                // 默认情况下，从数据库加载的短信都是接收的，除非在保存时明确标记为发送
                // 这里不需要额外处理，因为 isSent 字段已经在 SmsEntity 构造函数中处理
            }
            runOnUiThread(() -> {
                if (newSms.isEmpty()) {
                    allSmsLoaded = true;
                    Toast.makeText(SmsListActivity.this, getString(R.string.toast_all_sms_loaded), Toast.LENGTH_SHORT).show();
                } else {
                    smsList.addAll(newSms);
                    smsAdapter.notifyDataSetChanged();
                    currentPage++;
                }
                isLoading = false;
            });
        });
    }

    private class SmsAdapter extends RecyclerView.Adapter<SmsAdapter.SmsViewHolder> {

        private List<SmsEntity> smsList;
        private static final int VIEW_TYPE_SENT = 1;
        private static final int VIEW_TYPE_RECEIVED = 2;

        public SmsAdapter(List<SmsEntity> smsList) {
            this.smsList = smsList;
        }

        @Override
        public int getItemViewType(int position) {
            SmsEntity sms = smsList.get(position);
            return sms.isSent ? VIEW_TYPE_SENT : VIEW_TYPE_RECEIVED;
        }

        @Override
        public SmsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            if (viewType == VIEW_TYPE_SENT) {
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sms_sent, parent, false);
            } else {
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sms_received, parent, false);
            }
            return new SmsViewHolder(view, viewType);
        }

        @Override
        public void onBindViewHolder(SmsViewHolder holder, int position) {
            SmsEntity sms = smsList.get(position);

            if (holder.getItemViewType() == VIEW_TYPE_SENT) {
                holder.receiverTextView.setText(getString(R.string.sms_receiver) + sms.phoneNumber);
                holder.messageBodyTextView.setText(sms.messageBody);
                holder.timestampTextView.setText(formatTimestamp(sms.timestamp));
            } else {
                holder.senderTextView.setText(getString(R.string.sms_sender) + sms.sender);
                holder.messageBodyTextView.setText(sms.messageBody);
                holder.timestampTextView.setText(formatTimestamp(sms.timestamp));
            }

            holder.itemView.setOnClickListener(v -> showSmsDetailDialog(sms));

            // 处理高亮显示
            if (sms.isHighlighted) {
                holder.itemView.setBackgroundColor(getResources().getColor(R.color.highlight_color, null)); // 使用一个自定义的高亮颜色

                // 5秒后取消高亮
                mHandler.postDelayed(() -> {
                    holder.itemView.setBackgroundColor(getResources().getColor(android.R.color.transparent, null)); // 恢复透明背景
                    sms.isHighlighted = false;
                    SmsDatabase.databaseWriteExecutor.execute(() -> {
                        SmsDatabase db = SmsDatabase.getDatabase(SmsListActivity.this);
                        SmsDao smsDao = db.smsDao();
                        smsDao.updateSms(sms);
                    });
                }, 5000); // 5秒
            } else {
                holder.itemView.setBackgroundColor(getResources().getColor(android.R.color.transparent, null)); // 恢复透明背景
            }
        }

        private String formatTimestamp(long timestamp) {
            java.util.Date date = new java.util.Date(timestamp);
            java.util.Date now = new java.util.Date();

            SimpleDateFormat sdf;
            if (android.text.format.DateUtils.isToday(timestamp)) {
                // 当天，只显示时间
                sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            } else if (date.getYear() == now.getYear()) {
                // 当年，显示月日和时间
                sdf = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());
            } else {
                // 往年，显示年月日和时间
                sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            }
            return sdf.format(date);
        }

        @Override
        public int getItemCount() {
            return smsList.size();
        }

        class SmsViewHolder extends RecyclerView.ViewHolder {
            TextView senderTextView; // for received SMS
            TextView receiverTextView; // for sent SMS
            TextView messageBodyTextView;
            TextView timestampTextView;

            public SmsViewHolder(View itemView, int viewType) {
                super(itemView);
                if (viewType == VIEW_TYPE_SENT) {
                    receiverTextView = itemView.findViewById(R.id.text_receiver_sent);
                    messageBodyTextView = itemView.findViewById(R.id.text_message_body_sent);
                    timestampTextView = itemView.findViewById(R.id.text_timestamp_sent);
                } else {
                    senderTextView = itemView.findViewById(R.id.text_sender_received);
                    messageBodyTextView = itemView.findViewById(R.id.text_message_body_received);
                    timestampTextView = itemView.findViewById(R.id.text_timestamp_received);
                }
            }
        }
    }

    private void showSmsDetailDialog(SmsEntity sms) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.sms_detail_title));

        StringBuilder detail = new StringBuilder();
        if (sms.isSent) {
            detail.append(getString(R.string.sms_detail_receiver)).append(sms.phoneNumber).append("\n");
        } else {
            detail.append(getString(R.string.sms_detail_sender)).append(sms.sender).append("\n");
        }
        detail.append(getString(R.string.sms_detail_body)).append(sms.messageBody).append("\n");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        detail.append(getString(R.string.sms_detail_time)).append(sdf.format(new java.util.Date(sms.timestamp))).append("\n");
        detail.append(getString(R.string.sms_detail_phone_number)).append(sms.phoneNumber).append("\n");
        detail.append(getString(R.string.sms_detail_sim_slot)).append(sms.simSlot == -1 ? getString(R.string.sms_detail_sim_slot_unknown) : (sms.simSlot + 1)).append("\n");

        builder.setMessage(detail.toString());
        builder.setPositiveButton(getString(R.string.confirm), (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void sendSms() {
        String phoneNumber = inputNumberComponent.getText().trim();
        String message = inputMessageComponent.getText().trim();

        if (phoneNumber.isEmpty() || message.isEmpty()) {
            Toast.makeText(this, "Phone number or message cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            SmsManager smsManager = SmsManager.getDefault();
            ArrayList<String> parts = smsManager.divideMessage(message);
            // 创建一个用于发送成功的PendingIntent
            PendingIntent sentIntent = PendingIntent.getBroadcast(this, 0, new Intent("SMS_SENT_ACTION"), PendingIntent.FLAG_IMMUTABLE);
            // 创建一个用于发送失败的PendingIntent
            PendingIntent deliveryIntent = PendingIntent.getBroadcast(this, 0, new Intent("SMS_DELIVERED_ACTION"), PendingIntent.FLAG_IMMUTABLE);

            smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null);

            // 将发送的短信保存到数据库
            SmsEntity sentSms = new SmsEntity(
                    System.currentTimeMillis(),
                    phoneNumber, // 发送者为接收方号码
                    message,
                    -1, // SIM卡槽未知
                    true // 标记为发送短信
            );

            SmsDatabase.databaseWriteExecutor.execute(() -> {
                SmsDatabase db = SmsDatabase.getDatabase(this);
                SmsDao smsDao = db.smsDao();
                smsDao.insert(sentSms);

                // 缓存号码和短信内容
                inputNumberComponent.saveHistory();
                inputMessageComponent.saveHistory();

                runOnUiThread(() -> {
                    smsList.add(0, sentSms); // 插入到顶部
                    smsAdapter.notifyItemInserted(0);
                    recyclerView.smoothScrollToPosition(0);
                    inputMessageComponent.setText(""); // 清空输入框
                    Toast.makeText(SmsListActivity.this, "SMS sent and saved", Toast.LENGTH_SHORT).show();
                });
            });

        } catch (Exception e) {
            Toast.makeText(this, "Failed to send SMS: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
}