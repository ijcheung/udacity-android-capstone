package com.karaokyo.android.app.player.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

import com.karaokyo.android.app.player.R;
import com.karaokyo.android.app.player.service.LyricService;
import com.karaokyo.android.app.player.util.Constants;

public class ControlWidget extends AppWidgetProvider {

    private static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.control_widget);

        views.setOnClickPendingIntent(R.id.back, generateLyricServicePendingIntent(context, appWidgetId, Constants.ACTION_BACK));
        views.setOnClickPendingIntent(R.id.play, generateLyricServicePendingIntent(context, appWidgetId, Constants.ACTION_TOGGLE_PLAYBACK));
        views.setOnClickPendingIntent(R.id.forward, generateLyricServicePendingIntent(context, appWidgetId, Constants.ACTION_FORWARD));

        appWidgetManager.updateAppWidget(appWidgetId, views);

        sendUpdateRequest(context, appWidgetId);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {

    }

    @Override
    public void onDisabled(Context context) {

    }

    public static PendingIntent generateLyricServicePendingIntent(Context context, int appWidgetId, String command){
        Intent intent = new Intent(context,LyricService.class);
        intent.setAction(command);
        Uri data = Uri.withAppendedPath(Uri.parse("controlwidget://widget/id/#"+command+appWidgetId), String.valueOf(appWidgetId));
        intent.setData(data);
        return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static void sendUpdateRequest(Context context, int appWidgetId){
        Intent intent = new Intent(Constants.ACTION_WIDGET_UPDATE_REQUEST);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        context.sendBroadcast(intent);
    }
}

