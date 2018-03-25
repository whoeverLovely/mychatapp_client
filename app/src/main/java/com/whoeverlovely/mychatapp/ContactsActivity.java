package com.whoeverlovely.mychatapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import com.google.common.base.Strings;
import com.whoeverlovely.mychatapp.data.ChatAppDBContract;
import com.whoeverlovely.mychatapp.data.ChatAppDBHelper;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ContactsActivity extends AppCompatActivity implements ContactListAdapter.ContactItemClickHandler{

    final private static String TAG = "ContactsActivity";

    private RecyclerView contactListRecyclerView;
    private ContactListAdapter contactListAdapter;


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

        List<Contact> contactList = initContactList();
        contactListAdapter.setContactData(contactList);

    }

    private List<Contact> initContactList() {
        SQLiteDatabase db = new ChatAppDBHelper(this).getReadableDatabase();
        Cursor cursor = db.query(ChatAppDBContract.ContactEntry.TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                null);
        int count = cursor.getCount();
        //TODO preference: orderBy

        List<Contact> contactList = new LinkedList<Contact>();
        Contact contact;
        String userId;
        String userName;
        String encryptedAESKey;
        for (int i = 0; i < count; i++) {
            //display all contacts which have AESKey value
            cursor.moveToPosition(i);
            encryptedAESKey = cursor.getString(cursor.getColumnIndex(ChatAppDBContract.ContactEntry.COLUMN_AES_KEY));
            if(!Strings.isNullOrEmpty(encryptedAESKey)) {
                userId = cursor.getString(cursor.getColumnIndex(ChatAppDBContract.ContactEntry.COLUMN_USER_ID));
                userName = cursor.getString(cursor.getColumnIndex(ChatAppDBContract.ContactEntry.COLUMN_NAME));

                if(Strings.isNullOrEmpty(userName))
                    userName = userId;

                contact = new Contact();
                contact.setUserId(userId);
                contact.setName(userName);
                contact.setEncryptedAESKey(encryptedAESKey);

                contactList.add(contact);
            }
        }

        Log.d(TAG, "contactList size: " + contactList.size());
        return contactList;
    }

    @Override
    public void onClick(Contact contact) {
        Intent intent = new Intent(this,ChatBoxActivity.class);
        intent.putExtra("contact", contact);
        startActivity(intent);
    }
}
