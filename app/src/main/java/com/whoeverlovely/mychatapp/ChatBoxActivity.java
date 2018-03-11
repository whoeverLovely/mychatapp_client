package com.whoeverlovely.mychatapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Strings;

import org.json.JSONException;
import org.json.JSONObject;

public class ChatBoxActivity extends AppCompatActivity {

    final private static String TAG = "ChatBoxActivity'";

    private SharedPreferences shared_preference;

    private EditText inputEditText;
    private TextView displayMsgTextView;

    String Friend_Id;
    String Friend_Name;
    String My_Id;
    String My_Name;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_box);
        shared_preference = getSharedPreferences(getString(R.string.default_shared_preference), MODE_PRIVATE);

        inputEditText = findViewById(R.id.chatbox_textEditor);
        displayMsgTextView = findViewById(R.id.chatbox_displayMsg);

        Intent intent = getIntent();
        Contact contact = (Contact) intent.getSerializableExtra("contact");
        Friend_Id = contact.getUserId();
        Friend_Name = contact.getName();

        My_Id = shared_preference.getString("myUserId", null);
        My_Name = "Me";
        TextView userNameTextView = (TextView)findViewById(R.id.chatbox_friendName);
        userNameTextView.setText(Friend_Name);

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(Friend_Id));
        Log.d(TAG,"activity intent name: " + Friend_Id);

        Button sendButton = findViewById(R.id.chatbox_sendButton);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msgContent = inputEditText.getText().toString();
                if(!Strings.isNullOrEmpty(msgContent)) {
                    new SendMsgTask(getApplicationContext()).execute(msgContent);
                    inputEditText.setText("");
                }
            }
        });

    }

    public class SendMsgTask extends AsyncTask<String, Void, JSONObject> {
        //TODO locally save friends' id and run background service to update friends' name when launching the app
        String url = getString(R.string.base_url) + "Forward";
        private Context context;

        private SendMsgTask(Context context) {
            this.context = context;
        }

        @Override
        protected JSONObject doInBackground(String... strings) {
            String msgContent = strings[0];

            //encrypt msgContent with userId_AES
            String encryptedMsg = new AESKeyczarUtil(context).encrypt(Friend_Id, msgContent);

            try {
                JSONObject data = new JSONObject();
                data.put("from", My_Id);
                data.put("msgContent", encryptedMsg);

                JSONObject param = new JSONObject();
                param.put("userId", My_Id);
                param.put("chat_token", AESKeyStoreUtil.decryptAESKeyStore(shared_preference.getString("chat_token", null)));
                param.put("receiverUserId", Friend_Id);
                param.put("data", data.toString());

                JSONObject result = NetworkUtil.executePost(url,param);
                Log.d(TAG, "Sent message: " + msgContent + " to " + Friend_Name);
                //if no error received from server, pass the plain msgContent to onPostExecute
                if(result == null) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("msgContent", msgContent);
                    return jsonObject;
                } else
                    return result;
            } catch (JSONException e) {
                //TODO make a toast
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject jsonObject) {

            if(jsonObject == null || "".equals(jsonObject)) {
                Toast.makeText(ChatBoxActivity.this, "Please check your internet", Toast.LENGTH_SHORT).show();
            }
            else {
                try {
                    String msgContent = jsonObject.getString("msgContent");
                    if(msgContent != null) {
                        displayMsgTextView.append(My_Name + " : " + msgContent + System.getProperty("line.separator"));

                    } else {
                        String error = jsonObject.getString("error");
                        Toast.makeText(ChatBoxActivity.this, error, Toast.LENGTH_SHORT).show();
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //receive msg
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            String msgContent = intent.getStringExtra("decryptedMsgContent");
            Log.d(TAG, "Got message: " + msgContent + " from " + Friend_Name);
            displayMsgTextView.append(Friend_Name + ": " + msgContent + System.getProperty("line.separator"));

        }
    };
}
