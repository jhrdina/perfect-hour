package cz.hrdinajan.perfecthour;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;

import java.util.TreeSet;

public class MainActivity extends AppCompatActivity {

    private TreeSet<Integer> minPoints;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final AppCompatActivity activity = this;

        SwitchCompat mainSwitch = findViewById(R.id.main_switch);
        mainSwitch.setChecked(NotificationReceiver.isEnabled(this));
        mainSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                NotificationReceiver.setEnabled(isChecked, activity)
        );

        // Get minutePoints from settings
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        minPoints = Utils.minutePointsFromStringSet(sharedPref.getStringSet("minute_points", null));
        if (minPoints == null) {
            minPoints = Utils.getDefaultMinutePoints();
        }

        final HourDial hourDial = findViewById(R.id.hour_dial);
        hourDial.setStops(minPoints);
        hourDial.setMinPointsChangeListener(minPoints -> {
            SharedPreferences.Editor e = sharedPref.edit();
            e.putStringSet("minute_points", Utils.minutePointsToStringSet(minPoints));
            e.apply();

            if (NotificationReceiver.isEnabled(activity)) {
                NotificationReceiver.setEnabled(false, activity);
                NotificationReceiver.setEnabled(true, activity);
            }
        });

        // TODO: Remove duplicate code

        ImageButton addBtn = findViewById(R.id.addPointButton);
        addBtn.setOnClickListener(v -> {
            SharedPreferences.Editor e = sharedPref.edit();
            e.putStringSet("minute_points", Utils.minutePointsToStringSet(Utils.addPoint(minPoints)));
            e.apply();
            hourDial.invalidate();

            if (NotificationReceiver.isEnabled(activity)) {
                NotificationReceiver.setEnabled(false, activity);
                NotificationReceiver.setEnabled(true, activity);
            }
        });

        ImageButton removeBtn = findViewById(R.id.removePointButton);
        removeBtn.setOnClickListener(v -> {
            SharedPreferences.Editor e = sharedPref.edit();
            e.putStringSet("minute_points", Utils.minutePointsToStringSet(Utils.removePoint(minPoints)));
            e.apply();
            hourDial.invalidate();

            if (NotificationReceiver.isEnabled(activity)) {
                NotificationReceiver.setEnabled(false, activity);
                NotificationReceiver.setEnabled(true, activity);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
