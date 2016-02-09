package com.karaokyo.android.app.player.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import com.google.android.gms.analytics.HitBuilders;
import com.karaokyo.android.app.player.R;

public class OpenSourceActivity extends SelfClosingActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_source);
    }

    @Override
    protected void onResume() {
        super.onResume();

        getTracker().setScreenName("Open Source");
        getTracker().send(new HitBuilders.ScreenViewBuilder().build());
    }

    public void openUrl(View v){
        String url = v.getTag().toString();
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        startActivity(i);
    }
}
