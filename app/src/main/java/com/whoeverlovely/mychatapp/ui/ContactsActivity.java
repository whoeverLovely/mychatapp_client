package com.whoeverlovely.mychatapp.ui;

import android.content.Intent;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.whoeverlovely.mychatapp.R;
import com.whoeverlovely.mychatapp.data.ChatAppDBContract;

public class ContactsActivity extends AppCompatActivity implements ContactListAdapter.ContactItemClickHandler, LoaderManager.LoaderCallbacks<Cursor> {

    final private static String TAG = "ContactsActivity";

    private RecyclerView contactListRecyclerView;
    private ContactListAdapter contactListAdapter;
    final private static int ID_CONTACT_LOADER = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);

        contactListRecyclerView = findViewById(R.id.contact_list);
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

            default:
                throw new IllegalArgumentException("The menu item selected is not known.");
        }

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case ID_CONTACT_LOADER:

                //Return a contact list(userId and name) of all contacts whose AES is not null AND !=''
                return new CursorLoader(this,
                        ChatAppDBContract.ContactEntry.CONTENT_URI,
                        new String[]{ChatAppDBContract.ContactEntry.COLUMN_NAME, ChatAppDBContract.ContactEntry.COLUMN_USER_ID},
                        ChatAppDBContract.ContactEntry.COLUMN_AES_KEY + " is not null AND " + ChatAppDBContract.ContactEntry.COLUMN_AES_KEY + " != " + "?",
                        new String[]{""},
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
