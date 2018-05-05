package com.whoeverlovely.mychatapp.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
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
import android.widget.EditText;
import android.widget.Toast;

import com.whoeverlovely.mychatapp.ContactsService;
import com.whoeverlovely.mychatapp.R;
import com.whoeverlovely.mychatapp.data.ChatAppDBContract;
import com.whoeverlovely.mychatapp.util.NetworkUtil;
import com.whoeverlovely.mychatapp.util.Security.AESKeyStoreUtil;
import com.whoeverlovely.mychatapp.util.Security.FriendKeyczarReader;
import com.whoeverlovely.mychatapp.util.Security.SignKeyReader;

import org.json.JSONException;
import org.json.JSONObject;
import org.keyczar.DefaultKeyType;
import org.keyczar.Encrypter;
import org.keyczar.RsaPublicKey;
import org.keyczar.Signer;
import org.keyczar.exceptions.KeyczarException;

import me.pushy.sdk.Pushy;

public class ContactsActivity extends AppCompatActivity implements ContactListAdapter.ContactItemClickHandler, LoaderManager.LoaderCallbacks<Cursor> {

    final private static String TAG = "ContactsActivity";

    private ContactListAdapter contactListAdapter;
    final private static int ID_CONTACT_LOADER = 1;
    private String myUserId;
    private String friendUserId;

    private boolean qrCodeReturningWithResult = false;


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

            case R.id.scan_qrcode_contacts_menu_item:
                try {
                    Intent zxingIntent = new Intent("com.google.zxing.client.android.SCAN");
                    zxingIntent.putExtra("SCAN_MODE", "QR_CODE_MODE"); // "PRODUCT_MODE for bar codes
                    startActivityForResult(zxingIntent, 0);
                } catch (Exception e) {
                    Uri marketUri = Uri.parse("market://details?id=com.google.zxing.client.android");
                    Intent marketIntent = new Intent(Intent.ACTION_VIEW, marketUri);
                    startActivity(marketIntent);
                }
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        //scan profile result
        if (requestCode == 0) {
            if (resultCode == RESULT_OK) {
                String profile = data.getStringExtra("SCAN_RESULT");

                try {
                    JSONObject profileJSON = new JSONObject(profile);
                    String userId = profileJSON.getString("myUserId");
                    String publicKey = profileJSON.getString("publicKey");
                    friendUserId = userId;
                    publicKey = FriendKeyczarReader.createRsaPublicKey(this, publicKey);
                    Log.d(TAG, "myUserId scanned is " + userId);
                    Log.d(TAG, "publicKey scanned is " + publicKey);

                    ContactsService.startReceiveProfileService(this, publicKey, Long.parseLong(userId));

                        // Display a dialog fragment to edit new contact's name
                        qrCodeReturningWithResult = true;


                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
            if (resultCode == RESULT_CANCELED) {
                //handle cancel
            }
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (qrCodeReturningWithResult) {
            FragmentManager fm = getSupportFragmentManager();
            EditContactNameDialogFragment editNameDialogFragment = EditContactNameDialogFragment.newInstance(Long.parseLong(friendUserId));
            editNameDialogFragment.show(fm, "fragment_edit_name");
        }
        // Reset the boolean flag back to false for next time.
        qrCodeReturningWithResult = false;
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
