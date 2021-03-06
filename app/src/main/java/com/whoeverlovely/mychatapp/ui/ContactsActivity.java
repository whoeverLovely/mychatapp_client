package com.whoeverlovely.mychatapp.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.whoeverlovely.mychatapp.AddContactActivity;
import com.whoeverlovely.mychatapp.ContactsService;
import com.whoeverlovely.mychatapp.R;
import com.whoeverlovely.mychatapp.data.ChatAppDBContract;
import com.whoeverlovely.mychatapp.util.Security.FriendKeyczarReader;

import org.json.JSONException;
import org.json.JSONObject;

import me.pushy.sdk.Pushy;

public class ContactsActivity extends AppCompatActivity implements ContactListAdapter.ContactItemClickHandler, LoaderManager.LoaderCallbacks<Cursor> {

    final private static String TAG = "ContactsActivity";

    private ContactListAdapter contactListAdapter;
    final private static int ID_CONTACT_LOADER = 1;
    private String myUserId;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);

        myUserId = PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.pref_my_user_id), null);
        if (myUserId == null) {
            Intent intent = new Intent(this, SignUpActivity.class);
            startActivity(intent);
        }

        RecyclerView contactListRecyclerView = findViewById(R.id.contact_list);
        RecyclerView.LayoutManager layoutManager
                = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        contactListRecyclerView.setLayoutManager(layoutManager);
        contactListRecyclerView.setHasFixedSize(true);

        contactListAdapter = new ContactListAdapter(this);
        contactListRecyclerView.setAdapter(contactListAdapter);

        getSupportLoaderManager().initLoader(ID_CONTACT_LOADER, null, this);

        /*
        Calling this during onCreate() ensures that your app is properly initialized with default settings,
        which your app might need to read in order to determine some behaviors
        */
        PreferenceManager.setDefaultValues(this, R.xml.setting, false);
        Pushy.listen(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_contacts, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.settings_contacts_menu_item:
                Intent intent = new Intent(this, SettingActivity.class);
                startActivity(intent);
                return true;

            case R.id.show_profile_contacts_menu_item:
                FragmentManager fm = getSupportFragmentManager();
                QRCodeDialogFragment qrCodeDialogFragment = QRCodeDialogFragment.newInstance(getProfile(), myUserId);
                qrCodeDialogFragment.show(fm, "QRCode_Image");
                return true;

            case R.id.copy_profile_contacts_menu_item:
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("profile", getProfile());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Profile copied!", Toast.LENGTH_LONG).show();
                return true;

            case R.id.add_contact_contacts_menu_item:
                Intent addContactIntent = new Intent(this, AddContactActivity.class);
                startActivity(addContactIntent);
                return true;

            case R.id.delete_data_contacts_menu_item:
                //clear both table message and table contact
                int messageCount = getContentResolver().delete(ChatAppDBContract.MessageEntry.CONTENT_URI, null, null);
                int contactCount = getContentResolver().delete(ChatAppDBContract.ContactEntry.CONTENT_URI, null, null);
                Toast.makeText(this, "deleted message no.: " + messageCount, Toast.LENGTH_LONG).show();
                Toast.makeText(this, "deleted contact no.: " + contactCount, Toast.LENGTH_LONG).show();
                return true;

            default:
                throw new IllegalArgumentException("The menu item selected is not known.");
        }

    }

    //Prepare data for generating profile
    private String getProfile() {
        String publicKey = PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.pref_my_public_key), null);
        JSONObject profileJSON = new JSONObject();
        try {
            profileJSON.put("myUserId", myUserId);
            profileJSON.put("publicKey", publicKey);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return profileJSON.toString();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case ID_CONTACT_LOADER:

                //Return a contact list(userId and name) of all contacts whose verifiedFlag = 1
                return new CursorLoader(this,
                        ChatAppDBContract.ContactEntry.CONTENT_URI,
                        new String[]{ChatAppDBContract.ContactEntry.COLUMN_NAME, ChatAppDBContract.ContactEntry.COLUMN_USER_ID},
                        ChatAppDBContract.ContactEntry.COLUMN_VERIFIED_FLAG + "=1",
                        null,
                        null);

            default:
                throw new RuntimeException("Unsupported loader id: " + id);
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        contactListAdapter.swapCursor(data);
        Log.d(TAG, String.valueOf(data.getCount()));

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        contactListAdapter.swapCursor(null);
    }

    @Override
    public void onClick(long userId) {
        Intent intent = new Intent(this, ChatBoxActivity.class);
        intent.putExtra("userId", userId);
        startActivity(intent);
    }

}
