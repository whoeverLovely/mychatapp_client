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

import org.json.JSONException;
import org.json.JSONObject;
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
            SharedPreferences user_key = context.getSharedPreferences(context.getString(R.string.user_key), Context.MODE_PRIVATE);

            try {
                //decrypt received key and retrieve key and signature
                Crypter crypter = new Crypter(new MyKeyczarReader(context));
                String key = crypter.decrypt(encryptedKey);
                Log.d(getClass().getSimpleName(), "received aes key: " + key);

                //verify signature against key
                String senderPubKey = user_key.getString(senderId,null);
                RsaPublicKey senderRsaPubkey = (RsaPublicKey) DefaultKeyType.RSA_PUB.getBuilder().read(senderPubKey);
                VerifierKeyReader verifierReader = new VerifierKeyReader(senderRsaPubkey);
                Verifier verifier = new Verifier(verifierReader);
                boolean verified = verifier.verify(key, signature);
                Log.d(TAG, "key signature verified: " + String.valueOf(verified));

                //save userId_AES key in user_key sharedPreference, and delete public key
                if(verified) {
                    SharedPreferences.Editor editor = user_key.edit();
                    editor.putString(senderId + "_AES", AESKeyStoreUtil.encryptAESKeyStore(key));
                    editor.remove(senderId);
                    editor.apply();
                }

            } catch (KeyczarException e) {
                e.printStackTrace();
            }

        }

        // payload for msg
        if (intent.getStringExtra("msgContent") != null) {
            String encryptedMesgContent = intent.getStringExtra("msgContent");
            String senderId = intent.getStringExtra("from");

            String decryptedMsgContent = new AESKeyczarUtil(context).decrypt(senderId, encryptedMesgContent);
            Log.d(TAG, "decryptedMsgContent: " + decryptedMsgContent);
            Log.d(TAG, "create intent name: " + senderId);
            Intent chatBoxIntent = new Intent(senderId);
            // You can also include some extra data.
            chatBoxIntent.putExtra("decryptedMsgContent", decryptedMsgContent);
            LocalBroadcastManager.getInstance(context).sendBroadcast(chatBoxIntent);
        }

    }
}
