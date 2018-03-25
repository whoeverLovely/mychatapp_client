package com.whoeverlovely.mychatapp;

import android.support.v4.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.support.v4.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Strings;
import com.whoeverlovely.mychatapp.Util.NetworkUtil;
import com.whoeverlovely.mychatapp.Util.Security.AESKeyStoreUtil;
import com.whoeverlovely.mychatapp.Util.Security.AESKeyczarUtil;
import com.whoeverlovely.mychatapp.data.ChatAppDBContract;

import org.json.JSONException;
import org.json.JSONObject;

public class ChatBoxActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    final private static String TAG = "ChatBoxActivity'";
    final private static int ID_MESSAGE_LOADER = 1;

    private EditText inputEditText;
    private RecyclerView messageListRecyclerView;
    private MessageAdapter adapter;

    private String friendId;
    private String friendName;
    private String myId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_box);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        inputEditText = findViewById(R.id.chatbox_textEditor);

        Intent intent = getIntent();
        Contact contact = (Contact) intent.getSerializableExtra("contact");
        friendId = contact.getUserId();
        friendName = contact.getName();
        Log.d(TAG, "Launched chatbox for " + friendName + "_" + friendId);
        setTitle(friendName);

        myId = PreferenceManager.getDefaultSharedPreferences(this).getString("myUserId", null);

        adapter = new MessageAdapter(this);
        messageListRecyclerView = findViewById(R.id.msg_list_recyclerView);
        messageListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        messageListRecyclerView.setAdapter(adapter);

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(friendId));

        Button sendButton = findViewById(R.id.chatbox_sendButton);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msgContent = inputEditText.getText().toString();
                if (!Strings.isNullOrEmpty(msgContent)) {
                    new SendMsgTask(getApplicationContext()).execute(msgContent);
                    inputEditText.setText("");
                }
            }
        });

        getSupportLoaderManager().initLoader(ID_MESSAGE_LOADER, null, this);
    }

    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case ID_MESSAGE_LOADER:
                return new CursorLoader(this,
                        ChatAppDBContract.MessageEntry.CONTENT_URI,
                        null,
                        ChatAppDBContract.MessageEntry.COLUMN_RECEIVER_ID + "=" + friendId + " OR "
                                + ChatAppDBContract.MessageEntry.COLUMN_SENDER_ID + "=" + friendId,
                        null,
                        ChatAppDBContract.MessageEntry.COLUMN_TIMESTAMP);

            default:
                throw new RuntimeException("Unsupported loader id: " + id);
        }
    }

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader, Cursor data) {
        Log.d(TAG, "onLoadFinished cursor: " + data.getCount());
        adapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {
        adapter.swapCursor(null);
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

            //Insert message to db, status=>20
            ContentValues cv = new ContentValues();
            cv.put(ChatAppDBContract.MessageEntry.COLUMN_MESSAGE_CONTENT, msgContent);
            cv.put(ChatAppDBContract.MessageEntry.COLUMN_SENDER_ID, Integer.parseInt(myId));
            cv.put(ChatAppDBContract.MessageEntry.COLUMN_RECEIVER_ID, Integer.parseInt(friendId));
            cv.put(ChatAppDBContract.MessageEntry.COLUMN_STATUS, 20);

            Uri uri = getContentResolver().insert(ChatAppDBContract.MessageEntry.CONTENT_URI,
                    cv);
            Log.d(TAG, "New msg inserted to DB, msg Id: " + uri.getLastPathSegment());

            //Encrypt msgContent with userId_AES
            String encryptedMsg = new AESKeyczarUtil(context).encrypt(friendId, msgContent);

            //Send message to server
            try {
                JSONObject data = new JSONObject();
                data.put("from", myId);
                data.put("msgContent", encryptedMsg);

                JSONObject param = new JSONObject();
                param.put("userId", myId);
                param.put("chat_token", AESKeyStoreUtil.decryptAESKeyStore(PreferenceManager.getDefaultSharedPreferences(context).getString("chat_token", null)));
                param.put("receiverUserId", friendId);
                param.put("data", data.toString());

                JSONObject result = NetworkUtil.executePost(url, param);
                Log.d(TAG, "Sent message: " + msgContent + " to " + friendName);

                //If no error received from server, pass the plain msgContent to onPostExecute
                if (result == null) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("msgContent", msgContent);
                    jsonObject.put("msgId", uri.getLastPathSegment());
                    return jsonObject;
                } else
                    return result;
            } catch (JSONException e) {
                Toast.makeText(context, e.toString(), Toast.LENGTH_LONG);
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject jsonObject) {

            if (jsonObject == null || "".equals(jsonObject)) {
                Toast.makeText(ChatBoxActivity.this, "Please check your internet", Toast.LENGTH_SHORT).show();
            } else {
                try {
                    String msgContent = jsonObject.getString("msgContent");

                    //If message sent to server successfully, update the message status => 21
                    if (msgContent != null) {
                        long msgId = Long.parseLong(jsonObject.getString("msgId"));
                        ContentValues cv = new ContentValues();
                        cv.put(ChatAppDBContract.MessageEntry.COLUMN_STATUS, 21);
                        int updatedRows = getContentResolver().update(ContentUris.withAppendedId(ChatAppDBContract.MessageEntry.CONTENT_URI, msgId),
                                cv,
                                null,
                                null);
                        Log.d(TAG, "No of message updated: " + updatedRows);

                        /*adapter.swapCursor(getCurrentContactMessages());*/
                        getSupportLoaderManager().restartLoader(ID_MESSAGE_LOADER, null, ChatBoxActivity.this);
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
            Log.d(TAG, "Got message: " + msgContent + " from " + friendName);
            getSupportLoaderManager().restartLoader(ID_MESSAGE_LOADER, null, ChatBoxActivity.this);

        }
    };
}
