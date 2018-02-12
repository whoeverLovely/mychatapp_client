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

import org.json.JSONException;
import org.json.JSONObject;

public class ChatBoxActivity extends AppCompatActivity {

    final private static String TAG = "ChatBoxActivity'";

    private SharedPreferences shared_preference;

    private EditText inputEditText;
    private TextView displayMsgTextView;

    String receiverId;
    String receiverName;
    String senderId;
    String senderName;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_box);
        shared_preference = getSharedPreferences(getString(R.string.default_shared_preference), MODE_PRIVATE);

        receiverId = getString(R.string.friend_id);
        receiverName = getString(R.string.friend_name);
        senderId = shared_preference.getString("myUserId", null);
        senderName = "Me";
        TextView userNameTextView = (TextView)findViewById(R.id.chatbox_friendName);
        userNameTextView.setText(receiverName);

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(receiverId));

        Button sendButton = findViewById(R.id.chatbox_sendButton);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                inputEditText = (EditText) findViewById(R.id.chatbox_textEditor);
                String msgContent = inputEditText.getText().toString();
                new SendMsgTask(getApplicationContext()).execute(msgContent);
                inputEditText.setText("");
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
            String encryptedMsg = new AESKeyczarUtil(context).encrypt(receiverId, msgContent);

            try {
                JSONObject data = new JSONObject();
                data.put("from", senderId);
                data.put("msgContent", encryptedMsg);

                JSONObject param = new JSONObject();
                param.put("userId", senderId);
                param.put("chat_token", AESKeyStoreUtil.decryptAESKeyStore(shared_preference.getString("chat_token", null)));
                param.put("receiverUserId", receiverId);
                param.put("data", data.toString());

                JSONObject result = NetworkUtil.executePost(url,param);
                Log.d(TAG, "Sent message: " + msgContent + " to " + receiverName);
                //if no error received from server, pass the plain msgContent to onPostExecute
                if(!result.has("error")) {
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
                        displayMsgTextView = (TextView) findViewById(R.id.chatbox_displayMsg);
                        displayMsgTextView.append(senderName+ " : " + msgContent + System.getProperty("line.separator"));

                    } else {
                        //TODO make a toast to show error msg
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
            Log.d(TAG, "Got message: " + msgContent + " from " + senderName);
            displayMsgTextView = (TextView) findViewById(R.id.chatbox_displayMsg);
            displayMsgTextView.append(senderName + ": " + msgContent + System.getProperty("line.separator"));

        }
    };
}
