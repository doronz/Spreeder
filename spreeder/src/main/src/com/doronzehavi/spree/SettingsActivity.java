package com.doronzehavi.spree;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.preference.PreferenceActivity;



public class SettingsActivity extends PreferenceActivity {
	

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //this.getListView().setBackgroundDrawable(this.getResources().getDrawable(R.drawable.background_holo_dark));
        PrefFragment prefFragment = new PrefFragment();
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(android.R.id.content, prefFragment);
        fragmentTransaction.commit();

    }
    
    @Override
    public void onBackPressed() {
        setResult(RESULT_OK);
        finish();
    }
 
}