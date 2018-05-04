package com.whoeverlovely.mychatapp.util.Security;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Created by yan on 2/4/18.
 */

public class AESKeyStoreUtil {

    final private static String TAG = "AESKeyStoreUtil";
    final private static String defaultAlias = "mySecretKey";

    //generate AESKey for chatting
    public static String generateAESKey() {
        String secretKeyStr = null;
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(256);
            SecretKey secretKey = keyGenerator.generateKey();
            secretKeyStr = Base64.encodeToString(secretKey.getEncoded(), Base64.DEFAULT);

        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return secretKeyStr;
    }

    //if aliaName doesn't exist in KeyStore, generate an AES key aliaName.
    private static void generateAESKeyWithKeyStore(String alias) {
        KeyStore ks = null;
        SecretKey keyStoreKey = null;
        try {
            ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            keyStoreKey = (SecretKey) ks.getKey(alias, null);
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }

        if (keyStoreKey == null) {
            try {
                KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
                keyGenerator.init(
                        new KeyGenParameterSpec.Builder(alias,
                                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                                .setRandomizedEncryptionRequired(false)   //no need to provide random iv for each encryption/decryption operation
                                .build());
                keyGenerator.generateKey();
            } catch (GeneralSecurityException e) {
                e.printStackTrace();
            }
        }
    }

    //generate "mySecretKey" as default AES secretKey
    public static void generateAESKeyWithKeyStore() {
        generateAESKeyWithKeyStore(defaultAlias);
    }

    public static String encryptAESKeyStore(String plainText) {
        String encryptedText = null;
        Cipher cipher = prepareDefaultCipher(Cipher.ENCRYPT_MODE);
        byte[] encryptedByte = new byte[0];
        try {
            encryptedByte = cipher.doFinal(plainText.getBytes("UTF-8"));
            encryptedText = Base64.encodeToString(encryptedByte, Base64.DEFAULT);
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "encrypt with AES key from keystore. encryptedText: " + encryptedText);
        return encryptedText;
    }

    public static String decryptAESKeyStore(String encryptedText) {
        String decryptedText = null;
        Cipher cipher = prepareDefaultCipher(Cipher.DECRYPT_MODE);
        byte[] decryptedByte = new byte[0];
        try {
            Log.d(TAG, "encrypted msg is : " + encryptedText);
            decryptedByte = cipher.doFinal(Base64.decode(encryptedText, Base64.DEFAULT));
            decryptedText = new String(decryptedByte, "UTF-8");
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "decrypt with AES key from keystore. decryptedText: " + decryptedText);
        return decryptedText;
    }

    private static Cipher prepareDefaultCipher(int mode) {
        String encryptedText = null;

        KeyStore ks = null;
        Cipher cipher = null;
        try {
            ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            SecretKey keyStoreKey = (SecretKey) ks.getKey(defaultAlias, null);
            cipher = Cipher.getInstance("AES/GCM/NoPadding");

            //prepare iv
            String FIXED_IV = "It's just a fixed iv.";
            byte[] iv = new byte[12];
            for (int i = 0; i < 12; i++)
                iv[i] = FIXED_IV.getBytes()[i];

            cipher.init(mode, keyStoreKey, new GCMParameterSpec(128, iv));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cipher;
    }
}
