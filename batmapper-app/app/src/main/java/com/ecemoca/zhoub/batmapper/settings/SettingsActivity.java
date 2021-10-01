package com.ecemoca.zhoub.batmapper.settings;

import android.app.Activity;
import android.content.SharedPreferences;
import android.media.audiofx.BassBoost;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.widget.CheckBox;
import android.widget.Toast;

import com.ecemoca.zhoub.batmapper.BatMapper;
import com.ecemoca.zhoub.batmapper.R;

/**
 * Created by zhoub on 11/16/2016.
 */
// Preferences
public class SettingsActivity extends PreferenceActivity {
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
//        getSharedPreferences("mapscannersettings",MODE_PRIVATE).registerOnSharedPreferenceChangeListener(this);
//        getFragmentManager().beginTransaction().replace(android.R.id.content,new BatMapper.settingsFragment()).commit();
        getPreferenceManager().setSharedPreferencesName(BatMapper.SHARED_PREFS_NAME);
        addPreferencesFromResource(R.xml.preferences);
    }
}


