package cz.hrdinajan.perfecthour;


import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import java.util.Objects;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();

        getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, new GeneralPreferenceFragment())
                .commit();
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    public static class GeneralPreferenceFragment extends PreferenceFragmentCompat
            implements SharedPreferences.OnSharedPreferenceChangeListener {
        public static final String KEY_PREF_DEBUG_ENABLED = "debug_enabled";
        public static final String KEY_PREF_NOTIFICATION_SOUND = "notification_sound";

        @Override
        public void onResume() {
            super.onResume();
            Objects.requireNonNull(getPreferenceScreen().getSharedPreferences())
                    .registerOnSharedPreferenceChangeListener(this);
            syncProps();
        }

        @Override
        public void onPause() {
            super.onPause();
            Objects.requireNonNull(getPreferenceScreen().getSharedPreferences())
                    .unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.pref_general, rootKey);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setHasOptionsMenu(true);
        }

        /**
         * A preference value change listener that updates the preference's summary
         * to reflect its new value.
         */
        private final Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = (preference, value) -> {
            Log.d("perfect", "Pref change " + preference.getKey() + " " + value.toString());
            syncProps();
            return true;
        };

        private void bindPreferenceSummaryToValue(Preference preference) {
            // Set the listener to watch for value changes.
            preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

            // Trigger the listener immediately with the preference's current value.
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getString(preference.getKey(), ""));
        }

        private String getPreferenceString(String propKey, String defValue) {
            return PreferenceManager
                    .getDefaultSharedPreferences(requireContext())
                    .getString(propKey, defValue);
        }

        private void syncNotificationSoundProp() {
            // Notification Sound
            String notificationSound = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    // Sound is set in the specific notification channel
                    ? NotificationReceiver.ensureNotificationChannel(requireActivity())
                    .getSound()
                    .toString()
                    // Or stored in app preferences for older Android versions
                    : getPreferenceString(
                    KEY_PREF_NOTIFICATION_SOUND,
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString()
            );


            Preference notificationSoundPref = findPreference(KEY_PREF_NOTIFICATION_SOUND);
            assert notificationSoundPref != null;
            // For ringtone preferences, look up the correct display value
            // using RingtoneManager.
            if (TextUtils.isEmpty(notificationSound)) {
                // Empty values correspond to 'silent' (no ringtone).
                notificationSoundPref.setSummary(R.string.pref_notification_no_sound);
            } else {
                Ringtone ringtone = RingtoneManager.getRingtone(
                        notificationSoundPref.getContext(), Uri.parse(notificationSound));

                if (ringtone == null) {
                    // Clear the summary if there was a lookup error.
                    notificationSoundPref.setSummary(null);
                } else {
                    // Set the summary to reflect the new ringtone display
                    // name.
                    String name = ringtone.getTitle(notificationSoundPref.getContext());
                    notificationSoundPref.setSummary(name);
                }
            }
        }

        private void syncProps() {
            syncNotificationSoundProp();
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), MainActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals(KEY_PREF_DEBUG_ENABLED)) {
                NotificationReceiver.setEnabled(NotificationReceiver.isEnabled(getActivity()), getActivity());
            }
        }

        private void showRingtonePicker(Preference preference) {
            Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, Settings.System.DEFAULT_NOTIFICATION_URI);

            String existingValue = Objects.requireNonNull(preference.getSharedPreferences())
                    .getString(KEY_PREF_NOTIFICATION_SOUND, null);
            if (existingValue != null) {
                if (existingValue.length() == 0) {
                    // Select "Silent"
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, (Uri) null);
                } else {
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(existingValue));
                }
            } else {
                // No ringtone has been selected, set to the default
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Settings.System.DEFAULT_NOTIFICATION_URI);
            }

            ringtonePickerActivityResultLauncher.launch(intent);
        }

        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
            if (preference.getKey().equals(KEY_PREF_NOTIFICATION_SOUND)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // For notification channels, you configure the sound in the system settings
                    Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
                    intent.putExtra(Settings.EXTRA_APP_PACKAGE, requireActivity().getPackageName());
                    intent.putExtra(Settings.EXTRA_CHANNEL_ID, NotificationReceiver.CHANNEL_ID);
                    startActivity(intent);
                } else {
                    showRingtonePicker(preference);
                }
                return true;
            } else {
                return super.onPreferenceTreeClick(preference);
            }
        }

        ActivityResultLauncher<Intent> ringtonePickerActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri ringtone = result.getData().getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);

                        String newRingtone = ringtone != null ? ringtone.toString() : "";

                        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(requireActivity()).edit();
                        editor.putString(KEY_PREF_NOTIFICATION_SOUND, newRingtone);
                        editor.commit();

                        syncProps();
                    }
                });
    }
}
