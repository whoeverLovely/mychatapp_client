package com.whoeverlovely.mychatapp;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.whoeverlovely.mychatapp.util.Security.AESKeyStoreUtil;
import com.whoeverlovely.mychatapp.util.Security.AESKeyczarUtil;
import com.whoeverlovely.mychatapp.util.Security.MyKeyczarReader;
import com.whoeverlovely.mychatapp.util.Security.VerifierKeyReader;
import com.whoeverlovely.mychatapp.data.ChatAppDBContract;
import com.whoeverlovely.mychatapp.data.ChatAppDBHelper;

import org.keyczar.Crypter;
import org.keyczar.DefaultKeyType;
import org.keyczar.RsaPublicKey;
import org.keyczar.Verifier;
import org.keyczar.exceptions.KeyczarException;

/**
 * Created by yan on 2/8/18.
 */

public class PushReceiver extends BroadcastReceiver {
    final private static String TAG = "PushReceiver";
    public final static String NEW_MSG_ACTION = "com.whoeverlovely.mychatapp.action.action_new_message";
    public final static String INTENT_LONG_EXTRA_SENDERID = "senderId";
    public final static String INTENT_STRING_EXTRA_TEXT = "msgText";

    @Override
    public void onReceive(Context context, Intent intent) {

        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            for (String key : bundle.keySet()) {
                Object value = bundle.get(key);
                Log.d(TAG, String.format("%s %s (%s)", key,
                        value.toString(), value.getClass().getName()));
            }
        }

        // payload for key exchange
        if (intent.getStringExtra("key") != null) {
            String encryptedKey = intent.getStringExtra("key");
            String senderId = intent.getStringExtra("from");
            String signature = intent.getStringExtra("signature");

            SQLiteDatabase db = new ChatAppDBHelper(context).getWritableDatabase();

            try {
                //decrypt received key and retrieve key and signature
                Crypter crypter = new Crypter(new MyKeyczarReader(context));
                String key = crypter.decrypt(encryptedKey);
                Log.d(getClass().getSimpleName(), "received aes key: " + key);

                //verify signature against key
                Cursor cursor = context.getContentResolver().query(ContentUris.withAppendedId(ChatAppDBContract.ContactEntry.CONTENT_URI, Integer.parseInt(senderId)),
                        new String[]{ChatAppDBContract.ContactEntry.COLUMN_PUBLIC_KEY},
                        null,
                        null,
                        null);
                cursor.moveToFirst();

                //TODO loop the
                String senderPubKey = cursor.getString(0);
                RsaPublicKey senderRsaPubkey = (RsaPublicKey) DefaultKeyType.RSA_PUB.getBuilder().read(senderPubKey);
                VerifierKeyReader verifierReader = new VerifierKeyReader(senderRsaPubkey);
                Verifier verifier = new Verifier(verifierReader);
                boolean verified = verifier.verify(key, signature);
                Log.d(TAG, "key signature verified: " + String.valueOf(verified));

                //save userId_AES key to table Contact
                if (verified) {
                    ContentValues cv = new ContentValues();
                    cv.put(ChatAppDBContract.ContactEntry.COLUMN_AES_KEY, AESKeyStoreUtil.encryptAESKeyStore(key));
                    db.update(ChatAppDBContract.ContactEntry.TABLE_NAME,
                            cv,
                            ChatAppDBContract.ContactEntry.COLUMN_USER_ID + "=" + Integer.parseInt(senderId),
                            null);

                }

            } catch (KeyczarException e) {
                e.printStackTrace();
            }

        }

        // payload for msg
        if (intent.getStringExtra("msgContent") != null) {
            String encryptedMesgContent = intent.getStringExtra("msgContent");
            String senderIdStr = intent.getStringExtra("from");
            long senderId = Long.parseLong(senderIdStr);

            String decryptedMsgContent = new AESKeyczarUtil(context).decrypt(senderId, encryptedMesgContent);
            Log.d(TAG, "decryptedMsgContent: " + decryptedMsgContent);

            // save message to table message, status=>10
            ContentValues cv = new ContentValues();
            cv.put(ChatAppDBContract.MessageEntry.COLUMN_MESSAGE_CONTENT, decryptedMsgContent);
            cv.put(ChatAppDBContract.MessageEntry.COLUMN_SENDER_ID, senderId);
            cv.put(ChatAppDBContract.MessageEntry.COLUMN_RECEIVER_ID, Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString("myUserId", null)));
            cv.put(ChatAppDBContract.MessageEntry.COLUMN_STATUS, 10);
            context.getContentResolver().insert(ChatAppDBContract.MessageEntry.CONTENT_URI, cv);



            Intent chatBoxIntent = new Intent();
            // You can also include some extra data.
            chatBoxIntent.putExtra(INTENT_LONG_EXTRA_SENDERID, senderId);
            chatBoxIntent.putExtra(INTENT_STRING_EXTRA_TEXT, decryptedMsgContent);
            chatBoxIntent.setAction(NEW_MSG_ACTION);

            context.sendOrderedBroadcast(chatBoxIntent, null);
            /* LocalBroadcastManager.getInstance(context).sendBroadcast(chatBoxIntent);*/
            // Display notification
//            NotificationUtils.remindMsgReceived(context, decryptedMsgContent, senderId, senderName);
        }

    }
}
