package cz.hrdinajan.perfecthour;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Switch;

import java.util.TreeSet;

public class MainActivity extends AppCompatActivity {

    private TreeSet<Integer> minPoints;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final AppCompatActivity activity = this;

        Switch mainSwitch = (Switch) findViewById(R.id.main_switch);
        mainSwitch.setChecked(NotificationReceiver.isEnabled(this));
        mainSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                NotificationReceiver.setEnabled(isChecked, activity);
            }
        });

        // Get minutePoints from settings
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        minPoints = Utils.minutePointsFromStringSet(sharedPref.getStringSet("minute_points", null));
        if (minPoints == null) {
            minPoints = Utils.getDefaultMinutePoints();
        }

        final HourDial hourDial = (HourDial) findViewById(R.id.hour_dial);
        hourDial.setStops(minPoints);
        hourDial.setMinPointsChangeListener(new HourDial.MinPointsChangeListener() {
            @Override
            public void onChange(TreeSet<Integer> minPoints) {
                SharedPreferences.Editor e = sharedPref.edit();
                e.putStringSet("minute_points", Utils.minutePointsToStringSet(minPoints));
                e.apply();

                if (NotificationReceiver.isEnabled(activity)) {
                    NotificationReceiver.setEnabled(false, activity);
                    NotificationReceiver.setEnabled(true, activity);
                };
            }
        });

        // TODO: Remove duplicit code

        ImageButton addBtn = (ImageButton) findViewById(R.id.addPointButton);
        addBtn.setOnClickListener(new ImageButton.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor e = sharedPref.edit();
                e.putStringSet("minute_points", Utils.minutePointsToStringSet(Utils.addPoint(minPoints)));
                e.apply();
                hourDial.invalidate();

                if (NotificationReceiver.isEnabled(activity)) {
                    NotificationReceiver.setEnabled(false, activity);
                    NotificationReceiver.setEnabled(true, activity);
                };
            }
        });

        ImageButton removeBtn = (ImageButton) findViewById(R.id.removePointButton);
        removeBtn.setOnClickListener(new ImageButton.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor e = sharedPref.edit();
                e.putStringSet("minute_points", Utils.minutePointsToStringSet(Utils.removePoint(minPoints)));
                e.apply();
                hourDial.invalidate();

                if (NotificationReceiver.isEnabled(activity)) {
                    NotificationReceiver.setEnabled(false, activity);
                    NotificationReceiver.setEnabled(true, activity);
                };
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
