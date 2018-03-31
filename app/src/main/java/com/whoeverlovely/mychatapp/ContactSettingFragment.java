package com.whoeverlovely.mychatapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;
import android.widget.EditText;

import java.util.List;

/**
 * Created by yan on 3/27/18.
 */

public class ContactSettingFragment extends PreferenceFragmentCompat implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    /**
     * Set preference summary rules for each kind of preference: CheckBoxPreference, EditTextPreference, ListPreference
     */
    private void setPreferenceSummary(Preference preference, String preferenceValue) {

        if (preference instanceof CheckBoxPreference || preference instanceof EditTextPreference) {
            preference.setSummary(preferenceValue);
        }

        if (preference instanceof ListPreference) {
            int prefIndex = ((ListPreference) preference).findIndexOfValue(preferenceValue);
            if (prefIndex >= 0)
                preference.setSummary(((ListPreference) preference).getEntries()[prefIndex]);
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.contact_setting);

        //Set preference summary for each preference item
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        SharedPreferences sharedPreferences = preferenceScreen.getSharedPreferences();
        for (int i = 0; i < preferenceScreen.getPreferenceCount(); i++) {
            Preference p = preferenceScreen.getPreference(i);
            setPreferenceSummary(p, sharedPreferences.getString(p.getKey(), null));
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        //If username changed,
        // 1) update the name in db via content provider
        // 2) sync the update to sharedPreference
        // 3) and sync chatbox display immediately
    }

    @Override
    public void onStop() {
        super.onStop();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }
}

