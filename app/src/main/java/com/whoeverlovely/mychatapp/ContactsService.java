package com.whoeverlovely.mychatapp;

import android.app.IntentService;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

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

public class ContactsService extends IntentService{

    public static final String ACTION_SEND_KEY = "com.whoeverlovely.mychatapp.action.action_send_key";
    public static final String ACTION_RECEIVE_KEY = "com.whoeverlovely.mychatapp.action.action_receive_key";
    public static final String ACTION_VERIFY_KEY = "com.whoeverlovely.mychatapp.action.action_verify_key";

    public static final String EXTRA_FRIEND_USER_ID = "com.whoeverlovely.mychatapp.extra.friend_user_id";
    public static final String EXTRA_FRIEND_PUBLIC_KEY = "com.whoeverlovely.mychatapp.extra.friend_public_key";

    public ContactsService() {
        super("ContactsService");
    }

    public static void startSendKeyService(Context context, long friendUserId, String friendPublicKey) {
        Intent intent = new Intent(context, ContactsService.class);
        intent.setAction(ACTION_SEND_KEY);
        intent.putExtra(EXTRA_FRIEND_USER_ID, friendUserId);
        intent.putExtra(EXTRA_FRIEND_PUBLIC_KEY, friendPublicKey);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if(intent != null) {
            final String action = intent.getAction();
            if (ACTION_SEND_KEY.equals(action)) {
                long friendUserId = intent.getLongExtra(EXTRA_FRIEND_USER_ID, 0);
                String friendPublicKey = intent.getStringExtra(EXTRA_FRIEND_PUBLIC_KEY);
                handleActionSendKey(friendUserId, friendPublicKey);
            }
        }

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


        JSONObject result = null;
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


}
