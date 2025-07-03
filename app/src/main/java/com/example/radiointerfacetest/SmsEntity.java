package com.example.radiointerfacetest;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "sms_messages")
public class SmsEntity implements Parcelable {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public String sender;
    public String messageBody;
    public long timestamp; // 接收时间，使用Unix时间戳
    public String phoneNumber; // 接收短信的手机号
    public int simSlot; // 所属SIM卡槽 (0 for SIM 1, 1 for SIM 2, etc.)
    public boolean isHighlighted; // 是否高亮显示
    public boolean isSent; // 是否为发送的短信 (true for sent, false for received)

    public SmsEntity(String sender, String messageBody, long timestamp, String phoneNumber, int simSlot, boolean isSent) {
        this.sender = sender;
        this.messageBody = messageBody;
        this.timestamp = timestamp;
        this.phoneNumber = phoneNumber;
        this.simSlot = simSlot;
        this.isHighlighted = false; // 默认不高亮
        this.isSent = isSent;
    }

    // 为发送短信添加一个简化构造函数
    @Ignore
    public SmsEntity(long timestamp, String phoneNumber, String messageBody, int simSlot, boolean isSent) {
        this.sender = phoneNumber; // 发送短信时，sender就是接收方号码
        this.messageBody = messageBody;
        this.timestamp = timestamp;
        this.phoneNumber = phoneNumber;
        this.simSlot = simSlot;
        this.isHighlighted = false;
        this.isSent = isSent;
    }

    protected SmsEntity(Parcel in) {
        id = in.readLong();
        sender = in.readString();
        messageBody = in.readString();
        timestamp = in.readLong();
        phoneNumber = in.readString();
        simSlot = in.readInt();
        isHighlighted = in.readByte() != 0; // 读取boolean
        isSent = in.readByte() != 0; // 读取boolean
    }

    public static final Creator<SmsEntity> CREATOR = new Creator<SmsEntity>() {
        @Override
        public SmsEntity createFromParcel(Parcel in) {
            return new SmsEntity(in);
        }

        @Override
        public SmsEntity[] newArray(int size) {
            return new SmsEntity[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(sender);
        dest.writeString(messageBody);
        dest.writeLong(timestamp);
        dest.writeString(phoneNumber);
        dest.writeInt(simSlot);
        dest.writeByte((byte) (isHighlighted ? 1 : 0)); // 写入boolean
        dest.writeByte((byte) (isSent ? 1 : 0)); // 写入boolean
    }
}