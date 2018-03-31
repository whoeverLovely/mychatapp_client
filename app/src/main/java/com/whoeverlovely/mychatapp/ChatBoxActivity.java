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
    final private static int ID_CONTACT_LOADER = 2;

    private EditText inputEditText;
    private MessageAdapter adapter;

    private int friendId;
    private Cursor friendCursor;
    private String myId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_box);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        inputEditText = findViewById(R.id.chatbox_textEditor);

        Intent intent = getIntent();
        friendId = intent.getIntExtra("userId", 0);
        if (friendId == 0)
            throw new RuntimeException("Can't get userId from intent");

        getSupportLoaderManager().initLoader(ID_CONTACT_LOADER, null, this);

        myId = PreferenceManager.getDefaultSharedPreferences(this).getString("myUserId", null);

        adapter = new MessageAdapter(this);
        RecyclerView messageListRecyclerView = findViewById(R.id.msg_list_recyclerView);
        messageListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        messageListRecyclerView.setAdapter(adapter);

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(Integer.toString(friendId)));

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

            case ID_CONTACT_LOADER:
                Log.d(TAG, "contact loader started");
                Log.d(TAG, "friend id: " + friendId);

                return new CursorLoader(this,
                        ContentUris.withAppendedId(ChatAppDBContract.ContactEntry.CONTENT_URI, friendId),
                        null,
                        null,
                        null,
                        null);

            default:
                throw new RuntimeException("Unsupported loader id: " + id);
        }
    }

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader, Cursor data) {
        int loaderId = loader.getId();
        Log.d(TAG, "loader id in onLoadFinished: " + loaderId);

        switch (loaderId) {
            case ID_MESSAGE_LOADER:
                Log.d(TAG, "onLoadFinished cursor: " + data.getCount());
                adapter.swapCursor(data);
                break;

            case ID_CONTACT_LOADER:
                Log.d(TAG, "data count: " + data.getCount());

                friendCursor = data;
                Log.d(TAG, "onLoadFinished contact cursor: " + data.getCount());

                if (!friendCursor.moveToFirst())
                    throw new RuntimeException("The contact doesn't exist.");

                for (int columnIndex = 0; columnIndex < friendCursor.getColumnCount(); columnIndex++) {
                    Log.d(TAG, friendCursor.getColumnName(columnIndex) + ": " + friendCursor.getString(columnIndex));
                }

                setTitle(friendCursor.getString(friendCursor.getColumnIndex(ChatAppDBContract.ContactEntry.COLUMN_NAME)));
                break;

            default:
                throw new RuntimeException("Unsupported loader id: " + loaderId);
        }

    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {
        int loaderId = loader.getId();
        switch (loaderId) {
            case ID_MESSAGE_LOADER:
                adapter.swapCursor(null);

            case ID_CONTACT_LOADER:
                friendCursor = null;
        }
    }

    public class SendMsgTask extends AsyncTask<String, Void, JSONObject> {
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
            cv.put(ChatAppDBContract.MessageEntry.COLUMN_RECEIVER_ID, friendId);
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
                param.put("receiverUserId", Integer.toString(friendId));
                param.put("data", data.toString());

                JSONObject result = NetworkUtil.executePost(url, param);
                Log.d(TAG, "Sent message: " + msgContent + " to " + friendCursor.getString(friendCursor.getColumnIndex(ChatAppDBContract.ContactEntry.COLUMN_NAME)));

                //If no error received from server, pass the plain msgContent to onPostExecute
                if (result == null) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("msgContent", msgContent);
                    jsonObject.put("msgId", uri.getLastPathSegment());
                    return jsonObject;
                } else
                    return result;
            } catch (JSONException e) {
                Toast.makeText(context, e.toString(), Toast.LENGTH_LONG).show();
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
                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
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

            String senderId = intent.getStringExtra("senderId");
            Log.d(TAG, "Got message: " + " from " + senderId);

            if (Integer.parseInt(senderId) == friendId)
                getSupportLoaderManager().restartLoader(ID_MESSAGE_LOADER, null, ChatBoxActivity.this);

        }
    };
}
