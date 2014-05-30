package com.doronzehavi.spree;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.widget.Toast;

public class PrefFragment extends PreferenceFragment implements
		OnPreferenceChangeListener {
	ListPreference prefFontSize;
	CheckBoxPreference prefVariableWpm;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.pref_general);
		prefFontSize = (ListPreference) findPreference("pref_fontSize");
		prefVariableWpm = (CheckBoxPreference) findPreference("variable_wpm_checkbox");
		prefFontSize.setOnPreferenceChangeListener(this);
		prefVariableWpm.setOnPreferenceChangeListener(this);
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		// TODO Enter code to change summary
		if (preference.getKey() == "variable_wpm_checkbox")
		{
			if ((Boolean)newValue == true) {
				prefVariableWpm.setChecked(false);
			}
		}
		return true;
	}
}