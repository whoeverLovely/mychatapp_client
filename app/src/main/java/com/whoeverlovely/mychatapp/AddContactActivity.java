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

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.whoeverlovely.mychatapp.ui.EditContactNameDialogFragment;
import com.whoeverlovely.mychatapp.util.Security.FriendKeyczarReader;

import org.json.JSONException;
import org.json.JSONObject;

public class AddContactActivity extends AppCompatActivity {

    private EditText profileEditText;
    private Button scanQRCodeBtn;
    private Button confirmProfileBtn;

    private String friendUserId;
    private String profile;

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
                profile = profileEditText.getText().toString();
                handleProfile();
            }
        });

        scanQRCodeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new IntentIntegrator(AddContactActivity.this).initiateScan();
            }
        });

        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                new IntentFilter(ContactsService.ACTION_RECEIVE_PROFILE));

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == IntentIntegrator.REQUEST_CODE) {
            // Process scanning result
            IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
            if(result != null) {
                if(result.getContents() == null) {
                    Snackbar.make(scanQRCodeBtn, "Cancelled!", Snackbar.LENGTH_LONG).show();
                } else {
                    profile = result.getContents();
                    Log.d(TAG, "the scanning result is " + profile);
                    handleProfile();
                }
            } else {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    private void handleProfile() {

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
            Snackbar.make(profileEditText, R.string.invalid_profile, Snackbar.LENGTH_LONG).show();
        }
    }

    private void showEditNameFragment() {
        FragmentManager fm = getSupportFragmentManager();
        EditContactNameDialogFragment editNameDialogFragment = EditContactNameDialogFragment.newInstance(Long.parseLong(friendUserId));
        editNameDialogFragment.show(fm, "fragment_edit_name");
    }
}
