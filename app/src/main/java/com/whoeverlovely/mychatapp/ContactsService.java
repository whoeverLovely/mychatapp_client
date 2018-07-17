package com.whoeverlovely.mychatapp;

import android.app.IntentService;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.common.base.Strings;
import com.whoeverlovely.mychatapp.data.ChatAppDBContract;
import com.whoeverlovely.mychatapp.util.NetworkUtil;
import com.whoeverlovely.mychatapp.util.Security.AESKeyStoreUtil;
import com.whoeverlovely.mychatapp.util.Security.FriendKeyczarReader;
import com.whoeverlovely.mychatapp.util.Security.MyKeyczarReader;
import com.whoeverlovely.mychatapp.util.Security.SignKeyReader;
import com.whoeverlovely.mychatapp.util.Security.VerifierKeyReader;

import org.json.JSONException;
import org.json.JSONObject;
import org.keyczar.Crypter;
import org.keyczar.DefaultKeyType;
import org.keyczar.Encrypter;
import org.keyczar.RsaPublicKey;
import org.keyczar.Signer;
import org.keyczar.Verifier;
import org.keyczar.exceptions.KeyczarException;

public class ContactsService extends IntentService {

    private static final String TAG = ContactsService.class.getSimpleName();

    public static final String ACTION_RECEIVE_PROFILE = "com.whoeverlovely.mychatapp.action.action_receive_profile";
    public static final String ACTION_SEND_KEY = "com.whoeverlovely.mychatapp.action.action_send_key";
    public static final String ACTION_RECEIVE_KEY = "com.whoeverlovely.mychatapp.action.action_receive_key";
    public static final String ACTION_VERIFY_KEY = "com.whoeverlovely.mychatapp.action.action_verify_key";

    public static final String EXTRA_FRIEND_USER_ID = "com.whoeverlovely.mychatapp.extra.friend_user_id";
    public static final String EXTRA_FRIEND_PUBLIC_KEY = "com.whoeverlovely.mychatapp.extra.friend_public_key";
    public static final String EXTRA_KEY = "com.whoeverlovely.mychatapp.extra.key";
    public static final String EXTRA_SIGNATURE = "com.whoeverlovely.mychatapp.extra.signature";
    public static final String EXTRA_RECEIVE_PROFILE_STATUS = "com.whoeverlovely.mychatapp.extra.status";

    public ContactsService() {
        super("ContactsService");
    }

    public static void startReceiveProfileService(Context context, String publicKey, long userId) {
        Intent intent = new Intent(context, ContactsService.class);
        intent.setAction(ACTION_RECEIVE_PROFILE);
        intent.putExtra(EXTRA_FRIEND_USER_ID, userId);
        intent.putExtra(EXTRA_FRIEND_PUBLIC_KEY, publicKey);
        context.startService(intent);
    }

    public static void startSendKeyService(Context context, long friendUserId, String friendPublicKey) {
        Intent intent = new Intent(context, ContactsService.class);
        intent.setAction(ACTION_SEND_KEY);
        intent.putExtra(EXTRA_FRIEND_USER_ID, friendUserId);
        intent.putExtra(EXTRA_FRIEND_PUBLIC_KEY, friendPublicKey);
        context.startService(intent);
    }

    public static void startReceiveKeyService(Context context, long friendUserId, String key, String signature) {
        Intent intent = new Intent(context, ContactsService.class);
        intent.setAction(ACTION_RECEIVE_KEY);
        intent.putExtra(EXTRA_FRIEND_USER_ID, friendUserId);
        intent.putExtra(EXTRA_KEY, key);
        intent.putExtra(EXTRA_SIGNATURE, signature);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            long friendUserId = intent.getLongExtra(EXTRA_FRIEND_USER_ID, 0);
            String friendPublicKey = intent.getStringExtra(EXTRA_FRIEND_PUBLIC_KEY);
            switch (action) {
                case ACTION_SEND_KEY:
                    handleActionSendKey(friendUserId, friendPublicKey);
                    break;

                case ACTION_RECEIVE_PROFILE:
                    handleActionReceiveProfile(friendUserId, friendPublicKey);
                    break;

                case ACTION_RECEIVE_KEY:
                    String key = intent.getStringExtra(EXTRA_KEY);
                    String signature = intent.getStringExtra(EXTRA_SIGNATURE);
                    handleActionReceiveKey(friendUserId, key, signature);
                    break;

                default:
                    throw new IllegalArgumentException("The action is unknown.");

            }
        }
    }

    private void handleActionReceiveKey(long friendUserId, String key, String signature) {

        try {
            //decrypt received key
            Crypter crypter = new Crypter(new MyKeyczarReader(getApplicationContext()));
            key = crypter.decrypt(key);
            Log.d(getClass().getSimpleName(), "received aes key: " + key);

            // Query the contact
            Cursor cursor = getContentResolver().query(ContentUris.withAppendedId(ChatAppDBContract.ContactEntry.CONTENT_URI, friendUserId),
                    null,
                    null,
                    null,
                    null);

            if (cursor.moveToFirst()) {
                String publicKey = cursor.getString(cursor.getColumnIndex(ChatAppDBContract.ContactEntry.COLUMN_PUBLIC_KEY));

                // If the contact's public key exists, verify the signature
                if (!Strings.isNullOrEmpty(publicKey)) {
                    verifySignature(signature, key, friendUserId, publicKey);
                }
            }
            // If the Contact doesn't exists, insert one with userId, AESKey and signature
            else {
                ContentValues cv = new ContentValues();
                cv.put(ChatAppDBContract.ContactEntry.COLUMN_USER_ID, friendUserId);
                cv.put(ChatAppDBContract.ContactEntry.COLUMN_AES_KEY, AESKeyStoreUtil.encryptAESKeyStore(key));
                cv.put(ChatAppDBContract.ContactEntry.COLUMN_VERIFIED_FLAG, 0);
                cv.put(ChatAppDBContract.ContactEntry.COLUMN_SIGNATURE, signature);
                getContentResolver().insert(ChatAppDBContract.ContactEntry.CONTENT_URI, cv);

            }

        } catch (KeyczarException e) {
            e.printStackTrace();
        }
    }

    private void handleActionReceiveProfile(long friendUserId, String friendPublicKey) {
        // Query if the friend already exists in the Contact table
        Cursor cursor = getContentResolver().query(ChatAppDBContract.ContactEntry.CONTENT_URI,
                null,
                ChatAppDBContract.ContactEntry.COLUMN_USER_ID + "=" + friendUserId,
                null,
                null);

        boolean existed = false;
        // If the friend doesn't exist, insert it in Contact table
        if (!cursor.moveToFirst()) {
            ContentValues cv = new ContentValues();
            cv.put(ChatAppDBContract.ContactEntry.COLUMN_USER_ID, friendUserId);
            cv.put(ChatAppDBContract.ContactEntry.COLUMN_PUBLIC_KEY, friendPublicKey);
            cv.put(ChatAppDBContract.ContactEntry.COLUMN_VERIFIED_FLAG, 0);
            getContentResolver().insert(ChatAppDBContract.ContactEntry.CONTENT_URI, cv);
        }
        // If the friend does exist, and if the new public key is different from the one in DB, update table
        else {
            existed = true;
            String publicKey = cursor.getString(cursor.getColumnIndex(ChatAppDBContract.ContactEntry.COLUMN_PUBLIC_KEY));
            if (!friendPublicKey.equals(publicKey)) {
                Uri uri = ContentUris.withAppendedId(ChatAppDBContract.ContactEntry.CONTENT_URI, friendUserId);
                ContentValues cv = new ContentValues();
                cv.put(ChatAppDBContract.ContactEntry.COLUMN_PUBLIC_KEY, friendPublicKey);
                getContentResolver().update(uri, cv, null, null);
            }

        }

        String myUserId = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                .getString(getString(R.string.pref_my_user_id), null);

        // If myUserId is less than friend's userId, send AES key
        if (Long.parseLong(myUserId) < friendUserId) {
            handleActionSendKey(friendUserId, friendPublicKey);

            // Set verifiedFlag -> 1
            Uri uri = ContentUris.withAppendedId(ChatAppDBContract.ContactEntry.CONTENT_URI, friendUserId);
            ContentValues cv = new ContentValues();
            cv.put(ChatAppDBContract.ContactEntry.COLUMN_VERIFIED_FLAG, 1);
            getContentResolver().update(uri, cv, null, null);
        }

            // Else if the friend exists in DB, verify signature
        else {
            if(existed) {
                String signature = cursor.getString(cursor.getColumnIndex(ChatAppDBContract.ContactEntry.COLUMN_SIGNATURE));
                if (!Strings.isNullOrEmpty(signature)) {
                    String key = cursor.getString(cursor.getColumnIndex(ChatAppDBContract.ContactEntry.COLUMN_AES_KEY));
                    key = AESKeyStoreUtil.decryptAESKeyStore(key);
                    verifySignature(signature, key, friendUserId, friendPublicKey);
                }
            }

        }

        Intent intent = new Intent(ACTION_RECEIVE_PROFILE);
        intent.putExtra(EXTRA_RECEIVE_PROFILE_STATUS, 1);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void handleActionSendKey(long friendUserId, String friendPublicKey) {
        // Extract my user id and my chat token from shared preference
        String myUserId = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                .getString(getString(R.string.pref_my_user_id), null);
        String chat_token = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                .getString(getString(R.string.pref_my_chat_token), null);

        // Generate aeskey for the friend
        String aesKey = AESKeyStoreUtil.generateAESKey();

        // Encrypt and save in table Contact
        ContentValues cv = new ContentValues();
        cv.put(ChatAppDBContract.ContactEntry.COLUMN_AES_KEY, AESKeyStoreUtil.encryptAESKeyStore(aesKey));
        getContentResolver().update(ContentUris.withAppendedId(ChatAppDBContract.ContactEntry.CONTENT_URI, friendUserId),
                cv, null, null);

        JSONObject data;
        try {
            // Sign AES key
            Signer signer = new Signer(new SignKeyReader(getApplicationContext()));
            String signature = signer.sign(aesKey);

            // Encrypt AES key
            RsaPublicKey key = (RsaPublicKey) DefaultKeyType.RSA_PUB.getBuilder().read(friendPublicKey);
            FriendKeyczarReader friendKeyczarReader = new FriendKeyczarReader(key);
            Encrypter enc = new Encrypter(friendKeyczarReader);
            String encryptedAESKey = enc.encrypt(aesKey);

            data = new JSONObject();
            data.put("key", encryptedAESKey);
            data.put("from", myUserId);
            data.put("signature", signature);


        } catch (JSONException e) {
            throw new RuntimeException("Json is wrong during AES key encryption an signing.");
        } catch (KeyczarException e) {
            throw new RuntimeException("Keyczar is wrong during AES key encryption an signing.");
        }

        String url = getString(R.string.base_url) + "Forward";


        JSONObject result;
        JSONObject parameter = new JSONObject();
        try {
            parameter.put("data", data.toString());
            parameter.put("receiverUserId", String.valueOf(friendUserId));
            parameter.put("chat_token", AESKeyStoreUtil.decryptAESKeyStore(chat_token));
            parameter.put("userId", myUserId);
            result = NetworkUtil.executePost(url, parameter);

        } catch (JSONException e) {
            throw new RuntimeException("Json is wrong during sending key to server.");
        }

        if (result != null && result.has("error")) {
            String error;
            try {
                error = result.getString("error");
                Toast.makeText(getApplicationContext(), error, Toast.LENGTH_LONG).show();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void verifySignature(String signature, String key, long friendUserId, String publicKey) {
        boolean verified = false;
        try {
            // Verify signature against key
            RsaPublicKey senderRsaPubkey = (RsaPublicKey) DefaultKeyType.RSA_PUB.getBuilder().read(publicKey);
            VerifierKeyReader verifierReader = new VerifierKeyReader(senderRsaPubkey);
            Verifier verifier = new Verifier(verifierReader);
            verified = verifier.verify(key, signature);
            Log.d(TAG, "key signature verified: " + String.valueOf(verified));
        } catch (KeyczarException e) {
            throw new RuntimeException("Keyczar is wrong during signature verification.");
        }

        // Update contact(aesKey and verifiedFlag) if verified
        if (verified) {
            ContentValues cv = new ContentValues();
            cv.put(ChatAppDBContract.ContactEntry.COLUMN_AES_KEY, AESKeyStoreUtil.encryptAESKeyStore(key));
            cv.put(ChatAppDBContract.ContactEntry.COLUMN_VERIFIED_FLAG, 1);
            getContentResolver().update(ContentUris.withAppendedId(ChatAppDBContract.ContactEntry.CONTENT_URI, friendUserId),
                    cv,
                    null,
                    null);
        }
    }

}
