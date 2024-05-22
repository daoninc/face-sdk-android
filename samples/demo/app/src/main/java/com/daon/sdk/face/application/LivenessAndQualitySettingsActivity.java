package com.daon.sdk.face.application;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;


/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class LivenessAndQualitySettingsActivity extends SettingsActivity {


    @Override
    PreferenceFragment getPreferenceFragment() {
        return new SettingsFragment();
    }

    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.settings);

            // Bind the summaries of EditText settings
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("pref_blink_threshold"));
            bindPreferenceSummaryToValue(findPreference("pref_hmd_cutoff_value"));
            bindPreferenceSummaryToValue(findPreference("pref_hmd_threshold"));
            bindPreferenceSummaryToValue(findPreference("pref_hmd_time_limit_nod"));
            bindPreferenceSummaryToValue(findPreference("pref_hmd_time_limit_shake"));
        }
    }

}
