package com.whoeverlovely.mychatapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import com.google.common.base.Strings;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ContactsActivity extends AppCompatActivity implements ContactListAdapter.ContactItemClickHandler{

    final private static String TAG = "ContactsActivity";

    private RecyclerView contactListRecyclerView;
    private ContactListAdapter contactListAdapter;

    private SharedPreferences user_key;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);

        user_key = getSharedPreferences(getString(R.string.user_key), MODE_PRIVATE);

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
        List<Contact> contactList = new LinkedList<Contact>();
        Map<String, ?> prefs = user_key.getAll();
        Contact contact;
        String userId;
        String userName;
        String encryptedAESKey;
        for (String key : prefs.keySet()) {
            if(key.contains("_AES")) {
                encryptedAESKey = (String)prefs.get(key);
                userId = key.substring(0,key.indexOf("_AES"));

                userName = user_key.getString(userId+"_NAME",null);
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
