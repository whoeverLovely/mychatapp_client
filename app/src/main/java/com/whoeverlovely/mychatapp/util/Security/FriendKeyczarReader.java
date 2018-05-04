package com.whoeverlovely.mychatapp.util.Security;

import android.content.Context;
import android.util.Log;

import com.google.common.base.Strings;

import org.keyczar.DefaultKeyType;
import org.keyczar.KeyMetadata;
import org.keyczar.KeyVersion;
import org.keyczar.RsaPublicKey;
import org.keyczar.enums.KeyPurpose;
import org.keyczar.enums.KeyStatus;
import org.keyczar.exceptions.KeyczarException;
import org.keyczar.interfaces.KeyczarReader;

/**
 * Created by yan on 2/12/18.
 */

public class FriendKeyczarReader implements KeyczarReader {

    private KeyMetadata meta;
    private RsaPublicKey key;

    public FriendKeyczarReader(RsaPublicKey key) {
        this.key = key;

        meta = new KeyMetadata("friend_key", KeyPurpose.ENCRYPT, DefaultKeyType.RSA_PUB);
        KeyVersion v = new KeyVersion(0, KeyStatus.PRIMARY, true);
        meta.addVersion(v);
    }

    /*public static FriendKeyczarReader loadFromSharedPreference(Context context, String userId) {
        SharedPreferences keystore = context.getSharedPreferences(context.getString(R.string.user_key), Context.MODE_PRIVATE);
        String inKeystore = keystore.getString(userId, null);

        if (!Strings.isNullOrEmpty(inKeystore)) {
            try {
                RsaPublicKey key = (RsaPublicKey) DefaultKeyType.RSA_PUB.getBuilder().read(inKeystore);
                FriendKeyczarReader reader = new FriendKeyczarReader(key);
                Log.i(FriendKeyczarReader.class.getSimpleName(), "Loaded friend key.");
                return reader;
            } catch (KeyczarException e) {
                throw new RuntimeException(e);
            }
        }

        Log.i(FriendKeyczarReader.class.getSimpleName(), "No saved friend key loaded.");
        return null;
    }*/

    public static String createRsaPublicKey(Context context, String input) {
        if (!Strings.isNullOrEmpty(input)) {
            try {
                RsaPublicKey key = (RsaPublicKey) DefaultKeyType.RSA_PUB.getBuilder().read(input);
                Log.i(FriendKeyczarReader.class.getSimpleName(), "Parsed friend key.");

                return key.toString();
            } catch (KeyczarException e) {
                Log.i(FriendKeyczarReader.class.getSimpleName(), "Could not parse friend public key.", e);
            }
        }
        return null;
    }

    @Override
    public String getKey(int i) throws KeyczarException {
        return key.toString();
    }

    @Override
    public String getKey() throws KeyczarException {
        return key.toString();
    }

    @Override
    public String getMetadata() throws KeyczarException {
        return meta.toString();
    }
}
