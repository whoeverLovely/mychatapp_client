package com.whoeverlovely.mychatapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.whoeverlovely.mychatapp.Util.NetworkUtil;
import com.whoeverlovely.mychatapp.Util.Security.AESKeyStoreUtil;
import com.whoeverlovely.mychatapp.Util.Security.MyKeyczarReader;

import org.json.JSONException;
import org.json.JSONObject;

import me.pushy.sdk.Pushy;
import me.pushy.sdk.util.exceptions.PushyException;

public class SignUpActivity extends AppCompatActivity {

    final private static String TAG = "SignUpActivity";
    private TextView errorTextView;
    private ProgressBar mLoadingIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        mLoadingIndicator = findViewById(R.id.signup_loading_indicator);

        Button signUpButton = findViewById(R.id.signup_submit);
        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText passwordEditText = findViewById(R.id.signup_password);
                EditText repeatPasswordEditText = findViewById(R.id.signup_repeat_password);
                String password = passwordEditText.getText().toString();
                String repeatPassword = repeatPasswordEditText.getText().toString();

                errorTextView = findViewById(R.id.signup_error);
                if (password.trim().length() == 0)
                    errorTextView.setText(getString(R.string.empty_password));
                else if (repeatPassword.trim().length() == 0 || !password.equals(repeatPassword))
                    errorTextView.setText(getString(R.string.wrong_repeat_password));
                else if (password.length() < 6)
                    errorTextView.setText(getString(R.string.too_short_password));
                else {
                    new SignUpTask(getApplicationContext()).execute(password);
                }

            }
        });
    }

    private class SignUpTask extends AsyncTask<String, Void, JSONObject> {

        Context context;

        SignUpTask(Context context) {
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mLoadingIndicator.setVisibility(View.VISIBLE);
        }

        @Override
        protected JSONObject doInBackground(String... strings) {
            String password = strings[0];
            String url = getString(R.string.base_url);
            url = url + "SignUp";
            JSONObject result = null;
            // Assign a unique token to this device
            String deviceToken = null;
            try {
                //register a new pushy token and save in sharedPreferences. If it already exists, return the existing one
                deviceToken = Pushy.register(getApplicationContext());
                // Log it for debugging purposes
                Log.d(TAG, "Pushy device token: " + deviceToken);

                JSONObject param = new JSONObject();
                param.put("pushy_token", deviceToken);
                param.put("password", password);

                //send pushy_token and password to server for registration, server will return myUserId and chat_token
                result = NetworkUtil.executePost(url, param);
            } catch (PushyException | JSONException e) {
                Log.d(TAG, e.toString());
            }
            return result;
        }

        @Override
        protected void onPostExecute(JSONObject jsonObject) {
            mLoadingIndicator.setVisibility(View.INVISIBLE);
            try {
                if (jsonObject == null) {
                    errorTextView.setText("Internet error!");
                } else if (jsonObject.has("error")) {
                    errorTextView.setText(jsonObject.getString("error"));
                } else {
                    //register with mychatapp server successfully

                    //generate default AES key for encrypting tokens/keys in sharedPreference
                    AESKeyStoreUtil.generateAESKeyWithKeyStore();

                    //save myUserId and chat_token to sharedPreference, chat_token encrypt with AES key
                    String userId = jsonObject.getString("userId");
                    String chat_token = jsonObject.getString("chat_token");
                    chat_token = AESKeyStoreUtil.encryptAESKeyStore(chat_token);

                    SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
                    editor.putString(getString(R.string.pref_key_my_user_id), userId);
                    editor.putString("chat_token", chat_token);
                    editor.apply();

                    Toast.makeText(context, getString(R.string.signup_success), Toast.LENGTH_LONG).show();

                    //generate RSA key pair for exchanging AES key when adding friends
                    MyKeyczarReader.createKey(context);

                    Intent intent = new Intent(context, MainActivity.class);
                    startActivity(intent);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
