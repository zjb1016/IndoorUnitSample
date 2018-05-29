package com.glasssix.zjb.indoorunitsample;

import android.app.Application;

import com.glasssix.zjb.indoorunitsample.database.PreferenceAccess;

/**
 * Created by shenjingyuan002 on 2018/5/22.
 */

public class myApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        PreferenceAccess.getInstance().init(getApplicationContext());
    }
}
