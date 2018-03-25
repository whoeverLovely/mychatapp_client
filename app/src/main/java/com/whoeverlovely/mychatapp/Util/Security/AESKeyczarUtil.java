package com.whoeverlovely.mychatapp.Util.Security;

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

    public byte[] retrieveAESKey(String userId) {

        SQLiteDatabase db = new ChatAppDBHelper(context).getReadableDatabase();
        String encryptedAESKey = ChatAppDBHelper.retrieveContactWithUserId(db, userId, ChatAppDBContract.ContactEntry.COLUMN_AES_KEY);

        String decryptedAESKey = AESKeyStoreUtil.decryptAESKeyStore(encryptedAESKey);
        return Base64.decode(decryptedAESKey, Base64.DEFAULT);

    }

    public String encrypt(String userId, String plainText) {

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

    public String decrypt(String userId, String encryptedText) {

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
