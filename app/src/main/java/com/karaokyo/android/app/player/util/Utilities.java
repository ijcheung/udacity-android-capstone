package com.karaokyo.android.app.player.util;

import android.content.Context;
import android.graphics.Paint;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Utilities {
    public static void showError(Context context, int errorCode){
        Toast.makeText(context, context.getString(errorCode), Toast.LENGTH_SHORT).show();
    }
    /**
     * Function to convert milliseconds time to
     * Timer Format
     * Hours:Minutes:Seconds
     * */
    public static String milliSecondsToTimer(long milliseconds){
        String finalTimerString = "";
        String secondsString = "";

        // Convert total duration into time
        int hours = (int)( milliseconds / (1000*60*60));
        int minutes = (int)(milliseconds % (1000*60*60)) / (1000*60);
        int seconds = (int) ((milliseconds % (1000*60*60)) % (1000*60) / 1000);
        // Add hours if there
        if(hours > 0){
            finalTimerString = hours + ":";
        }

        // Prepending 0 to seconds if it is one digit
        if(seconds < 10){
            secondsString = "0" + seconds;
        }else{
            secondsString = "" + seconds;}

        finalTimerString = finalTimerString + minutes + ":" + secondsString;

        // return timer string
        return finalTimerString;
    }

    /**
     * Function to get Progress percentage
     * @param currentDuration
     * @param totalDuration
     * */
    public static int getProgressPercentage(long currentDuration, long totalDuration){
        Double percentage = (double) 0;

        long currentSeconds = (int) (currentDuration / 1000);
        long totalSeconds = (int) (totalDuration / 1000);

        // calculating percentage
        percentage =(((double)currentSeconds)/totalSeconds)*100;

        // return percentage
        return percentage.intValue();
    }

    /**
     * Function to change progress to timer
     * @param progress -
     * @param totalDuration
     * returns current duration in milliseconds
     * */
    public static int progressToTimer(int progress, int totalDuration) {
        int currentDuration = 0;
        totalDuration /= 1000;
        currentDuration = (int) ((((double)progress) / 100) * totalDuration);

        // return current duration in milliseconds
        return currentDuration * 1000;
    }

    public static int timecodeToMilliseconds(String timecode){
        String[] parts = timecode.split(":");

        switch (parts.length){
            case 2:
                int min = Integer.parseInt(parts[0]);
                float sec = Float.parseFloat(parts[1]);
                return (int) ((sec + min * 60) * 1000);
            case 1:
                return (int)(Float.parseFloat(parts[0]) * 1000);
            default:
                throw new NumberFormatException(timecode);
        }
    }

    public static String millisecondsToTimecode(int milliseconds){
        return Float.toString((float)milliseconds/1000);
    }

    public static String inputStreamToString(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    public static String fileToString(File file) throws IOException {
        FileInputStream fin = new FileInputStream(file);
        String ret = inputStreamToString(fin);
        fin.close();
        return ret;
    }

    public static String intToColorCode(int color){
        String hex = Integer.toHexString(color);

        while(hex.length() < 8){
            hex = "0" + hex;
        }

        return hex;
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null;
    }

    public static int calculateWidth(String text, int textSize){
        Paint textPaint = new Paint();
        textPaint.setTextSize(textSize);
        return (int) textPaint.measureText(text);
    }

    private Utilities(){}
}