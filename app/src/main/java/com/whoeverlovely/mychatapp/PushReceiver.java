package com.whoeverlovely.mychatapp;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.content.Context;
import android.media.RingtoneManager;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.keyczar.Crypter;
import org.keyczar.exceptions.KeyczarException;

/**
 * Created by yan on 2/8/18.
 */

public class PushReceiver extends BroadcastReceiver {
    final private static String TAG = "PushReceiver";

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

            try {
                Crypter crypter = new Crypter(new MyKeyczarReader(context));
                String aesKeyStr = crypter.decrypt(encryptedKey);
                Log.d(getClass().getSimpleName(),"received aes key: " + aesKeyStr);

                //encrypted aesKeyStr and save in shared_preference
                SharedPreferences.Editor editor = context.getSharedPreferences(context.getString(R.string.user_key),Context.MODE_PRIVATE).edit();
                editor.putString(senderId+"_AES",AESKeyStoreUtil.encryptAESKeyStore(aesKeyStr));
                editor.apply();

                //TODO verify signature
            } catch (KeyczarException e) {
                e.printStackTrace();
            }

        }

        // payload for msg
        if (intent.getStringExtra("msgContent") != null) {
            String encryptedMesgContent = intent.getStringExtra("msgContent");
            String senderId = intent.getStringExtra("from");

            String decryptedMsgContent = new AESKeyczarUtil(context).decrypt(senderId,encryptedMesgContent);

            Intent chatBoxIntent = new Intent(senderId);
            // You can also include some extra data.
            chatBoxIntent.putExtra("decryptedMsgContent", decryptedMsgContent);
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        }

    }
}
