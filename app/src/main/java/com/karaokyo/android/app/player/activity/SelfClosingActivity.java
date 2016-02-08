package com.karaokyo.android.app.player.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;

import com.karaokyo.android.app.player.util.Constants;

public class SelfClosingActivity extends AppCompatActivity {
    private final BroadcastReceiver closeBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);
            finish();
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(closeBroadcastReceiver, new IntentFilter(Constants.ACTION_CLOSE));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(closeBroadcastReceiver);
    }
}
