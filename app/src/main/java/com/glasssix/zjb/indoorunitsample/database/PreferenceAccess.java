package com.glasssix.zjb.indoorunitsample.database;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by shenjingyuan002 on 2018/5/22.
 */

public class PreferenceAccess {
    private static PreferenceAccess instance;

    public static PreferenceAccess getInstance() {
        if (instance == null) {
            synchronized (PreferenceAccess.class) {
                if (instance == null) instance = new PreferenceAccess();
            }
        }
        return instance;
    }

    @SuppressWarnings("FieldCanBeLocal")
    private static String FILE_NAME = "indoorUnit_settings";
    private static String RoomNumber = "roomnumber";
    private static String RtspAddress = "rtspaddress";

    private SharedPreferences mSharedPreferences;

    public void init(Context context) {
        if (mSharedPreferences == null)
            mSharedPreferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
    }

    public String getRoomNumber() {
        return mSharedPreferences.getString(RoomNumber, "312");
    }

    public void setRoomNumber(String roomNumber) {
        mSharedPreferences.edit()
                .putString(RoomNumber, roomNumber)
                .apply();
    }

    public String getRtspAddress() {
        return mSharedPreferences.getString(RtspAddress, "rtsp://admin:hk123456@192.168.0.64:554/h264/ch1/sub/av_stream");
    }

    public void setRtspAddress(String rtspAddress) {
        mSharedPreferences.edit()
                .putString(RtspAddress, rtspAddress)
                .apply();
    }
}
