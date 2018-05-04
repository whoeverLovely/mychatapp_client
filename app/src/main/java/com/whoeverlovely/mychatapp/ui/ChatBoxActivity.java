package com.whoeverlovely.mychatapp.ui;

import android.content.DialogInterface;
import android.support.v4.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.support.v4.app.NavUtils;
import android.support.v4.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.common.base.Strings;
import com.whoeverlovely.mychatapp.PushReceiver;
import com.whoeverlovely.mychatapp.R;
import com.whoeverlovely.mychatapp.util.NetworkUtil;
import com.whoeverlovely.mychatapp.util.Security.AESKeyStoreUtil;
import com.whoeverlovely.mychatapp.util.Security.AESKeyczarUtil;
import com.whoeverlovely.mychatapp.data.ChatAppDBContract;
import com.whoeverlovely.mychatapp.data.ChatAppDBHelper;

import org.json.JSONException;
import org.json.JSONObject;

public class ChatBoxActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    final private static String TAG = "ChatBoxActivity'";
    final private static int ID_MESSAGE_LOADER = 1;
    final private static int ID_CONTACT_LOADER = 2;

    private EditText inputEditText;
    private MessageAdapter adapter;
    private RecyclerView messageListRecyclerView;

    private long friendId;
    private Cursor friendCursor;
    private String friendName;
    private String myId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_box);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        inputEditText = findViewById(R.id.chatbox_textEditor);

        Intent intent = getIntent();
        friendId = intent.getLongExtra("userId", 0);
        if (friendId == 0)
            throw new RuntimeException("Can't get userId from intent");

        getSupportLoaderManager().initLoader(ID_CONTACT_LOADER, null, this);

        myId = PreferenceManager.getDefaultSharedPreferences(this).getString("myUserId", null);

        adapter = new MessageAdapter(this);
        messageListRecyclerView = findViewById(R.id.msg_list_recyclerView);
        messageListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        messageListRecyclerView.setAdapter(adapter);


        IntentFilter filter = new IntentFilter(PushReceiver.NEW_MSG_ACTION);
        filter.setPriority(1);
        registerReceiver(mMessageReceiver, filter);

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
    protected void onStop() {
        unregisterReceiver(mMessageReceiver);
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_chat_box, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;

            case R.id.nickname_chatbox_menu_item:
                ChatAppDBHelper.setUserNameWithAlertDialog(this, friendId);
                Log.d(TAG, "nickname changed.");
                getSupportLoaderManager().restartLoader(ID_MESSAGE_LOADER, null, this);
                return true;

            default:
                throw new IllegalArgumentException("The menu item selected is not found.");
        }
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

        switch (loaderId) {
            case ID_MESSAGE_LOADER:
                adapter.swapCursor(data);        //this will be running after the user name is updated, because the URI associated to the loader is updated.
                if (adapter.getItemCount() > 0)
                    messageListRecyclerView.smoothScrollToPosition(adapter.getItemCount() - 1);
                Log.d(TAG, "message loader finished.");
                break;

            case ID_CONTACT_LOADER:

                if (!data.moveToFirst())
                    throw new RuntimeException("The contact doesn't exist.");

                String friendNameNew = data.getString(data.getColumnIndex(ChatAppDBContract.ContactEntry.COLUMN_NAME));
                if (!friendNameNew.equals(friendName)) {
                    adapter.updateName(friendNameNew);
                }

                friendName = friendNameNew;
                setTitle(friendName);
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
                param.put("receiverUserId", Long.toString(friendId));
                param.put("data", data.toString());

                JSONObject result = NetworkUtil.executePost(url, param);

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


    //receive broadcast sent from PushReceiver
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            long senderId = intent.getLongExtra(PushReceiver.INTENT_LONG_EXTRA_SENDERID, 0);
            Log.d(TAG, "Got message: " + " from " + senderId);

            if (senderId == friendId) {
                getSupportLoaderManager().restartLoader(ID_MESSAGE_LOADER, null, ChatBoxActivity.this);
                abortBroadcast();
            }
        }
    };
}
