package cz.hrdinajan.perfecthour;


import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;

import java.util.Calendar;
import java.util.TreeSet;

public class NotificationReceiver extends BroadcastReceiver {
    public static final int INTENT_ID = 0;
    public static final int NOTIFICATION_ID = 1;
    public static final int NOTIFICATION_PERM_REQUEST_CODE = 0;
    public static final int CONTENT_INTENT_ID = 1;
    public static final String CHANNEL_ID = "main_channel";

    public static final String KEY_PREF_VIBRATION_ENABLED = "vibration_enabled";
    public static final String KEY_PREF_NOTIFICATION_SOUND = "notification_sound";
    public static final String KEY_PREF_DEBUG_ENABLED = "debug_enabled";
    public static final String KEY_PREF_MINUTE_POINTS = "minute_points";

    public NotificationReceiver() {
    }

    private static void ensureNotificationPermissions(Context context) {
        if (Build.VERSION.SDK_INT < 33) {
            return;
        }

        if (
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED && context instanceof Activity
        ) {
            ActivityCompat.requestPermissions(
                    (Activity) context,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_PERM_REQUEST_CODE
            );
        }
    }

    public static void setEnabled(boolean enabled, Context context) {
        Intent alarmIntent = new Intent(context, NotificationReceiver.class);
        setEnabled(enabled, context, alarmIntent);
    }

    public static void setEnabled(boolean enabled, Context context, Intent alarmIntent) {
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, INTENT_ID, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT | getImmutableFlag());
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (enabled) {
            ensureNotificationPermissions(context);

            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            boolean debugEnabled = sharedPref.getBoolean(KEY_PREF_DEBUG_ENABLED, false);
            TreeSet<Integer> minPoints = debugEnabled
                    ? Utils.getDebugMinutePoints()
                    : Utils.minutePointsFromStringSet(sharedPref.getStringSet(KEY_PREF_MINUTE_POINTS, null));

            if (minPoints == null) {
                minPoints = Utils.getDefaultMinutePoints();
            }

            Calendar next = Utils.findNext(minPoints);
            Log.d("perfect", next.toString());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), pendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), pendingIntent);
            }

        } else {
            alarmManager.cancel(pendingIntent);
        }
    }

    public static boolean isEnabled(Context context) {
        Intent alarmIntent = new Intent(context, NotificationReceiver.class);
        return PendingIntent.getBroadcast(
                context,
                INTENT_ID,
                alarmIntent,
                PendingIntent.FLAG_NO_CREATE | getImmutableFlag()
        ) != null;
    }

    private static int getImmutableFlag() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_IMMUTABLE : 0;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static NotificationChannel ensureNotificationChannel(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = notificationManager.getNotificationChannel(CHANNEL_ID);
        if (channel == null) {
            CharSequence name = context.getString(R.string.channel_name);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            channel = new NotificationChannel(CHANNEL_ID, name, importance);
            notificationManager.createNotificationChannel(channel);
        }
        return channel;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        Uri notificationSoundUri = Uri.parse(sharedPref.getString(
                KEY_PREF_NOTIFICATION_SOUND,
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString()
        ));
        boolean vibrationEnabled = sharedPref.getBoolean(KEY_PREF_VIBRATION_ENABLED, true);

        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ensureNotificationChannel(context);
        }

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_timelapse_white_24dp)
                        .setContentTitle(context.getResources().getString(R.string.message_box_title))
                        .setContentText(context.getResources().getString(R.string.message_timesheet_not_up_to_date))
                        .setDefaults((vibrationEnabled ? NotificationCompat.DEFAULT_VIBRATE : 0) | NotificationCompat.DEFAULT_LIGHTS)
                        .setSound(notificationSoundUri)
                        .setColor(ContextCompat.getColor(context, R.color.colorPrimary));

        // Setup activity that is opened after notification tap
        Intent resultIntent = new Intent(context, MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(CONTENT_INTENT_ID, PendingIntent.FLAG_UPDATE_CURRENT | getImmutableFlag());

        mBuilder.setContentIntent(resultPendingIntent);

        // Finally show the notification
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());

        // Plan next notification
        setEnabled(true, context, intent);
    }
}
