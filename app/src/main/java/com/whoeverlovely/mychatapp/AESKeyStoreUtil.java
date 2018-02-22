package com.whoeverlovely.mychatapp;

import android.content.Context;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableEntryException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Enumeration;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import static android.content.Context.MODE_PRIVATE;

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
