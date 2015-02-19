package com.example.fueleconomyassistant;

import android.app.Activity;
import android.app.UiModeManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

/**
 * Created by brophywa on 2/19/2015.
 * base activity that sets up the theme when creating the activities views
 */
public class BaseActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String colorScheme = prefs.getString("color_scheme_pref", "0");
        UiModeManager manager = (UiModeManager) getSystemService(Context.UI_MODE_SERVICE);
        manager.enableCarMode(0);
        if (colorScheme.equals("1")) {
            manager.setNightMode(UiModeManager.MODE_NIGHT_YES);
        } else if (colorScheme.equals("2")) {
            manager.setNightMode(UiModeManager.MODE_NIGHT_NO);
        } else {
            manager.setNightMode(UiModeManager.MODE_NIGHT_AUTO);
        }
        setTheme(R.style.ModeTheme);
    }
}
