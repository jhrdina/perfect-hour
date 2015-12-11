package cz.hrdinajan.perfecthour;


import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.util.Calendar;

public class NotificationReceiver extends BroadcastReceiver {
    public static final int INTENT_ID = 0;
    public static final int NOTIFICATION_ID = 1;
    public static final int CONTENT_INTENT_ID = 1;

    public static final String KEY_PREF_NOTIFICATION_SOUND = "notification_sound";
    public static final String KEY_PREF_DEBUG_ENABLED = "debug_enabled";

    public NotificationReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String notificationSoundUriStr = sharedPref.getString(KEY_PREF_NOTIFICATION_SOUND, "");

        Uri notificationSoundUri = notificationSoundUriStr != "" ?
                Uri.parse(notificationSoundUriStr) : RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);


        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_timelapse_white_24dp)
                        .setContentTitle(context.getResources().getString(R.string.message_box_title))
                        .setContentText(context.getResources().getString(R.string.message_timesheet_not_up_to_date))
                        .setDefaults(NotificationCompat.DEFAULT_VIBRATE | NotificationCompat.DEFAULT_LIGHTS)
                        .setSound(notificationSoundUri)
                        .setColor(ContextCompat.getColor(context, R.color.colorPrimary));

        Intent resultIntent = new Intent(context, MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(CONTENT_INTENT_ID, PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder.setContentIntent(resultPendingIntent);
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

            Calendar next = Utils.findNext(Utils.getFixedMinutePoints(debugEnabled));
            Log.d("perfect", next.toString());

            alarmManager.set(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), pendingIntent);

        } else {
            alarmManager.cancel(pendingIntent);
        }
    }

    public static boolean isEnabled(Context context) {
        Intent alarmIntent = new Intent(context, NotificationReceiver.class);
        return PendingIntent.getBroadcast(context, INTENT_ID, alarmIntent, PendingIntent.FLAG_NO_CREATE) != null;
    }
}
