package com.whoeverlovely.mychatapp;

import android.util.Log;

import com.whoeverlovely.mychatapp.Util.Security.RSAKeyStoreUtil;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

/**
 * Created by yan on 2/12/18.
 */


public class RSATest {
    final private static String TAG = "RSATest";

    @Test
    public void test() {
        RSAKeyStoreUtil.generateKeyPairWithKeyStore();
        String publicKey = RSAKeyStoreUtil.getPublicKeyStr();
        Log.d("public key is: ",publicKey);

        String text = "today is a good day";
        String sign = RSAKeyStoreUtil.sign(text);
        JSONObject data = new JSONObject();
        try {
            data.put("text", text);
            data.put("sign", sign);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String dataStr = data.toString();
        String encryptedData = RSAKeyStoreUtil.encryptWithPublicKey(publicKey,dataStr);
        String decryptedData = RSAKeyStoreUtil.decryptWithPrivateKey(encryptedData);
        Log.d("encrypted data: ", encryptedData);
        Log.d("decrypted data: ", decryptedData);

        try {
            JSONObject newData = new JSONObject(decryptedData);
            String textNew = newData.getString("text");
            String signNew = newData.getString("sign");

            boolean verify = RSAKeyStoreUtil.verify(textNew,signNew,publicKey);
            Log.d("verification result: ", String.valueOf(verify));
        } catch (JSONException e) {
            e.printStackTrace();
        }


    }
}
