package com.jayway.maven.plugins.android.generation2.samples.libraryprojects.aar1;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

/**
 * Activity super class that refers to a resource from this aar.
 */
public class AbstractActivityUsingResources extends Activity {

    private static String TAG = "AbstractActivityUsingResources";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final String aar1Resource = getResources().getString(R.string.aar1resource);
        Log.d(TAG, "Found resource : " + aar1Resource);
    }
}
