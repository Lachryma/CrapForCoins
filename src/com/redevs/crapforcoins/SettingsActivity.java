package com.redevs.crapforcoins;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class SettingsActivity extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
	
    	getFragmentManager().beginTransaction().replace(android.R.id.content,
    			new PrefsFragment()).commit();

    		
    }
}
