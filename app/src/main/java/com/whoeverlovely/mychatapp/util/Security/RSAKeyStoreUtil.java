package com.whoeverlovely.mychatapp.util.Security;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import org.json.JSONException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

/**
 * Created by yan on 2/12/18.
 */

public class RSAKeyStoreUtil {

    //TODO to be debug

    final private static String TAG = "RSAKeyStoreUtil";
    final private static String defaultAlias = "myKeyPair";

    private static void generateKeyPairWithKeyStore(String alias) {

        try {
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            KeyStore.Entry entry = ks.getEntry(alias, null);

            //alias doesn't exists in KeyStore, generate a new key pair
            if (entry == null) {
                KeyPairGenerator kpg = KeyPairGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore");
                kpg.initialize(new KeyGenParameterSpec.Builder(
                        alias,
                        KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                        .setDigests(KeyProperties.DIGEST_SHA256,
                                KeyProperties.DIGEST_SHA512)
                        .build());

                kpg.generateKeyPair();
            }
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * generate default key pair
     */
    public static void generateKeyPairWithKeyStore() {
        generateKeyPairWithKeyStore(defaultAlias);
    }

    private static PrivateKey getPrivateKey() {
        PrivateKey privateKey = null;
        try {
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            KeyStore.Entry entry = ks.getEntry(defaultAlias, null);

            if (entry != null) {
                privateKey = ((KeyStore.PrivateKeyEntry) entry).getPrivateKey();
            } else {
                generateKeyPairWithKeyStore();
                getPublicKey();
            }
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
        return privateKey;
    }

    private static PublicKey getPublicKey() {

        PublicKey publicKey = null;
        try {
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            KeyStore.Entry entry = ks.getEntry(defaultAlias, null);

            if (entry != null) {
                publicKey = ((KeyStore.PrivateKeyEntry) entry).getCertificate().getPublicKey();
            } else {
                generateKeyPairWithKeyStore();
                getPublicKey();
            }
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
        return publicKey;
    }

    public static String getPublicKeyStr() {
        PublicKey publicKey = getPublicKey();
        KeyFactory fact = null;
        X509EncodedKeySpec spec = null;
        try {
            fact = KeyFactory.getInstance("RSA");
            spec = fact.getKeySpec(publicKey, X509EncodedKeySpec.class);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }

        if (spec != null)
            return Base64.encodeToString(spec.getEncoded(), Base64.DEFAULT);
        else
            return null;
    }

    public static String sign(String data) {

        byte[] signature = null;
        try {
            Signature s = Signature.getInstance("SHA256withRSA");
            s.initSign(getPrivateKey());
            s.update(data.getBytes());
            signature = s.sign();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
        return Base64.encodeToString(signature, Base64.DEFAULT);
    }

    /**
     * @param
     * @return
     * @throws JSONException data is not encrypted properly
     */
    public static boolean verify(String data, String signature, String publicKeyStr) throws JSONException {

        byte[] signatureByte = null;
        Signature s = null;
        boolean verified = false;
        try {
            X509EncodedKeySpec publicSpec = new X509EncodedKeySpec(Base64.decode(publicKeyStr, Base64.DEFAULT));
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(publicSpec);

            s = Signature.getInstance("SHA256withECDSA");
            s.initVerify(publicKey);
            s.update(Base64.decode(data, Base64.DEFAULT));

            signatureByte = Base64.decode(signature, Base64.DEFAULT);
            verified = s.verify(signatureByte);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
        return verified;
    }

    public static String encryptWithPublicKey(String publicKeyStr, String plainText) {

        byte[] data = Base64.decode(publicKeyStr, Base64.DEFAULT);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(data);
        byte[] encryptedByte = null;
        try {
            KeyFactory fact = KeyFactory.getInstance("RSA");
            PublicKey publicKey = fact.generatePublic(spec);

            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            encryptedByte = cipher.doFinal(plainText.getBytes());
        } catch(GeneralSecurityException e) {
            e.printStackTrace();
        }
        return Base64.encodeToString(encryptedByte, Base64.DEFAULT);
    }

    public static String decryptWithPrivateKey(String encryptedText) {
        String decryptedText = null;
        try {
            PrivateKey privateKey = getPrivateKey();

            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] decryptedByte = cipher.doFinal(Base64.decode(encryptedText, Base64.DEFAULT));
            decryptedText = new String(decryptedByte, "UTF-8");
        } catch (GeneralSecurityException | IOException e) {
            Log.d(TAG, e.toString());
        }
        return decryptedText;
    }
}
