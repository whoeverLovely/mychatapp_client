package com.whoeverlovely.mychatapp.ui;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.TextView;

import com.whoeverlovely.mychatapp.R;

import java.util.Map;
import java.util.Set;

public class TestActivity extends AppCompatActivity {

    TextView testTextView;
    final private static String TAG = "TestActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        testTextView = findViewById(R.id.test_textview);
        testTextView.setMovementMethod(new ScrollingMovementMethod());

        /*SharedPreferences shared_preference = getSharedPreferences(getString(R.string.default_shared_preference),MODE_PRIVATE);*/
        SharedPreferences shared_preference = PreferenceManager.getDefaultSharedPreferences(this);
        Map<String, ?> defaultPrefs = shared_preference.getAll();
        Log.d(TAG,"default_shared_preference");
        testTextView.append("default_shared_preference" + System.getProperty("line.separator"));
        for (String key : defaultPrefs.keySet()) {
            Object pref = defaultPrefs.get(key);
            String printVal = "";
            if (pref instanceof Boolean) {
                printVal =  key + " : " + pref;
            }
            if (pref instanceof Float) {
                printVal =  key + " : " + pref;
            }
            if (pref instanceof Integer) {
                printVal =  key + " : " + pref;
            }
            if (pref instanceof Long) {
                printVal =  key + " : " + pref;
            }
            if (pref instanceof String) {
                printVal =  key + " : " + pref;
            }
            if (pref instanceof Set<?>) {
                printVal =  key + " : " + pref;
            }

            Log.d(TAG,printVal);
            testTextView.append(printVal + System.getProperty("line.separator"));
        }

    }
}
