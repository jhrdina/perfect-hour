package cz.hrdinajan.perfecthour;


import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;
import android.util.Log;

import java.util.Calendar;
import java.util.TreeSet;

import static androidx.core.content.ContextCompat.getSystemService;

public class NotificationReceiver extends BroadcastReceiver {
    public static final int INTENT_ID = 0;
    public static final int NOTIFICATION_ID = 1;
    public static final int CONTENT_INTENT_ID = 1;
    public static final String CHANNEL_ID = "main_channel";

    public static final String KEY_PREF_VIBRATION_ENABLED = "vibration_enabled";
    public static final String KEY_PREF_NOTIFICATION_SOUND = "notification_sound";
    public static final String KEY_PREF_DEBUG_ENABLED = "debug_enabled";
    public static final String KEY_PREF_MINUTE_POINTS = "minute_points";

    public NotificationReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String notificationSoundUriStr = sharedPref.getString(KEY_PREF_NOTIFICATION_SOUND, "");
        Boolean vibrationEnabled = sharedPref.getBoolean(KEY_PREF_VIBRATION_ENABLED, true);

        Uri notificationSoundUri = notificationSoundUriStr != "" ?
                Uri.parse(notificationSoundUriStr) : RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = context.getString(R.string.channel_name);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
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
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(CONTENT_INTENT_ID, PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder.setContentIntent(resultPendingIntent);

        // Finally show the notification
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());

        // Plan next notification
        setEnabled(true, context, intent);
    }

    public static void setEnabled(boolean enabled, Context context) {
        Intent alarmIntent = new Intent(context, NotificationReceiver.class);
        setEnabled(enabled, context, alarmIntent);
    }

    public static void setEnabled(boolean enabled, Context context, Intent alarmIntent) {
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, INTENT_ID, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (enabled) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            Boolean debugEnabled = sharedPref.getBoolean(KEY_PREF_DEBUG_ENABLED, false);
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
        return PendingIntent.getBroadcast(context, INTENT_ID, alarmIntent, PendingIntent.FLAG_NO_CREATE) != null;
    }
}
