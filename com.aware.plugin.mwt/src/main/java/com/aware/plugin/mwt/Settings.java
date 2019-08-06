package com.aware.plugin.mwt;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.aware.Aware;

public class Settings extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    //Plugin settings in XML @xml/preferences
    public static final String STATUS_PLUGIN_MWT = "status_plugin_mwt";

    public static final String STATUS_PLUGIN_PING_SERVER = "status_mwt_ping_server";

    public static final String STATUS_ESM_START_HOUR = "status_mwt_esm_start_time";
    public static final String STATUS_ESM_END_HOUR = "status_mwt_esm_end_time";

    public static final String STATUS_MWT_DETECTION = "status_mwt_detection";

    //Plugin settings UI elements
    private static CheckBoxPreference status;
    private static CheckBoxPreference pingServer;
    private static EditTextPreference esmStartHour;
    private static EditTextPreference esmEndHour;
    private static CheckBoxPreference mwtDetection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        status = (CheckBoxPreference) findPreference(STATUS_PLUGIN_MWT);
        if (Aware.getSetting(this, STATUS_PLUGIN_MWT).length() == 0) {
            Aware.setSetting(this, STATUS_PLUGIN_MWT, true); //by default, the setting is true on install
        }
        status.setChecked(Aware.getSetting(getApplicationContext(), STATUS_PLUGIN_MWT).equals("true"));

        // ping server
        pingServer = (CheckBoxPreference) findPreference(STATUS_PLUGIN_PING_SERVER);
        if (Aware.getSetting(this, STATUS_PLUGIN_PING_SERVER).length() == 0) {
            Aware.setSetting(this, STATUS_PLUGIN_PING_SERVER, false); //by default, the setting is false on install
        }
        pingServer.setChecked(Aware.getSetting(getApplicationContext(), STATUS_PLUGIN_PING_SERVER).equals("true"));

        esmStartHour = (EditTextPreference) findPreference(STATUS_ESM_START_HOUR);
        if (Aware.getSetting(this, STATUS_ESM_START_HOUR).length() == 0) {
            Aware.setSetting(this, STATUS_ESM_START_HOUR, 8);
        }
        esmStartHour.setSummary(Aware.getSetting(getApplicationContext(), STATUS_ESM_START_HOUR) + " hour");

        esmEndHour = (EditTextPreference) findPreference(STATUS_ESM_END_HOUR);
        if (Aware.getSetting(this, STATUS_ESM_END_HOUR).length() == 0) {
            Aware.setSetting(this, STATUS_ESM_END_HOUR, 22);
        }
        esmEndHour.setSummary(Aware.getSetting(getApplicationContext(), STATUS_ESM_END_HOUR) + " hour");

        mwtDetection = (CheckBoxPreference) findPreference(STATUS_MWT_DETECTION);
        if (Aware.getSetting(this, STATUS_MWT_DETECTION).length() == 0) {
            Aware.setSetting(this, STATUS_MWT_DETECTION, false); //by default, the setting is false on install
        }
        mwtDetection.setChecked(Aware.getSetting(getApplicationContext(), STATUS_MWT_DETECTION).equals("true"));
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference setting = findPreference(key);
        if (setting.getKey().equals(STATUS_PLUGIN_MWT)) {
            Aware.setSetting(this, key, sharedPreferences.getBoolean(key, false));
            status.setChecked(sharedPreferences.getBoolean(key, false));
        }

        if (setting.getKey().equals(STATUS_PLUGIN_PING_SERVER)) {
            Aware.setSetting(this, key, sharedPreferences.getBoolean(key, false));
            pingServer.setChecked(sharedPreferences.getBoolean(key, false));
        }

        if (setting.getKey().equals(STATUS_ESM_START_HOUR)) {
            Aware.setSetting(getApplicationContext(), key, sharedPreferences.getString(key, "8"));
            esmStartHour.setSummary(Aware.getSetting(getApplicationContext(), STATUS_ESM_START_HOUR) + " hour");
        }

        if (setting.getKey().equals(STATUS_ESM_END_HOUR)) {
            Aware.setSetting(getApplicationContext(), key, sharedPreferences.getString(key, "22"));
            esmEndHour.setSummary(Aware.getSetting(getApplicationContext(), STATUS_ESM_END_HOUR) + " hour");
        }

        if (setting.getKey().equals(STATUS_MWT_DETECTION)) {
            Aware.setSetting(this, key, sharedPreferences.getBoolean(key, false));
            mwtDetection.setChecked(sharedPreferences.getBoolean(key, false));
        }

        if (Aware.getSetting(this, STATUS_PLUGIN_MWT).equals("true")) {
            Aware.startPlugin(getApplicationContext(), "com.aware.plugin.mwt");
        } else {
            Aware.stopPlugin(getApplicationContext(), "com.aware.plugin.mwt");
        }
    }

}
