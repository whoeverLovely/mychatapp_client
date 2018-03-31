package com.whoeverlovely.mychatapp.Util.Security;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Base64;
import android.util.Log;

import com.whoeverlovely.mychatapp.R;
import com.whoeverlovely.mychatapp.data.ChatAppDBContract;
import com.whoeverlovely.mychatapp.data.ChatAppDBHelper;

import org.keyczar.AesKey;
import org.keyczar.Crypter;
import org.keyczar.HmacKey;
import org.keyczar.exceptions.KeyczarException;
import org.keyczar.interfaces.KeyczarReader;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by yan on 2/12/18.
 */

public class AESKeyczarUtil {

    private final static String TAG = "AESKeyczarUtil";
    private Context context;


    public AESKeyczarUtil(Context context) {
        this.context = context;
    }

    public byte[] retrieveAESKey(int userId) {

        Cursor encryptedAESKey = context.getContentResolver().query(ContentUris.withAppendedId(ChatAppDBContract.ContactEntry.CONTENT_URI, userId),
                new String[] {ChatAppDBContract.ContactEntry.COLUMN_AES_KEY},
                null,
                null,
                null);

        if(encryptedAESKey.moveToFirst()) {
            String decryptedAESKey = AESKeyStoreUtil.decryptAESKeyStore(encryptedAESKey.getString(0));
            return Base64.decode(decryptedAESKey, Base64.DEFAULT);
        } else
            return null;

    }

    public String encrypt(int userId, String plainText) {

        byte[] aesKeyByte = retrieveAESKey(userId);
        String cipherText = null;
        try {
            AesKey aesKey = new AesKey(aesKeyByte, new HmacKey(aesKeyByte));
            KeyczarReader reader = new AESKeyczarReader(aesKey.toString());
            Crypter crypter = new Crypter(reader);
            cipherText = crypter.encrypt(plainText);
        } catch (KeyczarException e) {
            e.printStackTrace();
        }

        return cipherText;
    }

    public String decrypt(int userId, String encryptedText) {

        byte[] aesKeyByte = retrieveAESKey(userId);
        String plainText = null;
        try {
            AesKey aesKey = new AesKey(aesKeyByte, new HmacKey(aesKeyByte));
            KeyczarReader reader = new AESKeyczarReader(aesKey.toString());
            Crypter crypter = new Crypter(reader);
            plainText = crypter.decrypt(encryptedText);
        } catch (KeyczarException e) {
            e.printStackTrace();
        }

        return plainText;
    }

}
