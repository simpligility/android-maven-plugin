package com.simpligility.android.aar;

import android.os.Bundle;
import com.actionbarsherlock.app.SherlockActivity;

public class AarActivity extends SherlockActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        getSupportActionBar().setTitle(R.string.app_name);
    }
}
