package com.whoeverlovely.mychatapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.whoeverlovely.mychatapp.ui.EditContactNameDialogFragment;
import com.whoeverlovely.mychatapp.util.Security.FriendKeyczarReader;

import org.json.JSONException;
import org.json.JSONObject;

public class AddContactActivity extends AppCompatActivity {

    private EditText profileEditText;
    private Button scanQRCodeBtn;
    private Button confirmProfileBtn;

    private String friendUserId;

    private final static String TAG = AddContactActivity.class.getSimpleName();

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int status = intent.getIntExtra(ContactsService.EXTRA_RECEIVE_PROFILE_STATUS, -1);
            if (status == 1)
                showEditNameFragment();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_contact);

        profileEditText = findViewById(R.id.editText_input_profile);
        confirmProfileBtn = findViewById(R.id.btn_confirm_input_profile);
        scanQRCodeBtn = findViewById(R.id.btn_scan_qrcode);

        confirmProfileBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String profile = profileEditText.getText().toString();
                handleProfile(profile);


            }
        });

        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                new IntentFilter(ContactsService.ACTION_RECEIVE_PROFILE));

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);

    }

    private void handleProfile(String profile) {

        try {
            JSONObject profileJSON = new JSONObject(profile);
            friendUserId = profileJSON.getString("myUserId");
            String publicKey = profileJSON.getString("publicKey");
            publicKey = FriendKeyczarReader.createRsaPublicKey(this, publicKey);
            Log.d(TAG, "myUserId scanned is " + friendUserId);
            Log.d(TAG, "publicKey scanned is " + publicKey);

            ContactsService.startReceiveProfileService(this, publicKey, Long.parseLong(friendUserId));

        } catch (JSONException e) {
            e.printStackTrace();
            Snackbar.make(profileEditText, R.string.invalid_profile, Snackbar.LENGTH_LONG);
        }
    }

    private void showEditNameFragment() {
        FragmentManager fm = getSupportFragmentManager();
        EditContactNameDialogFragment editNameDialogFragment = EditContactNameDialogFragment.newInstance(Long.parseLong(friendUserId));
        editNameDialogFragment.show(fm, "fragment_edit_name");
    }
}
