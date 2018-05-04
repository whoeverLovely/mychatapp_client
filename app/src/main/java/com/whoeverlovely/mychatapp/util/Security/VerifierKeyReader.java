package com.whoeverlovely.mychatapp.util.Security;

import org.keyczar.DefaultKeyType;
import org.keyczar.KeyMetadata;
import org.keyczar.KeyVersion;
import org.keyczar.RsaPublicKey;
import org.keyczar.enums.KeyPurpose;
import org.keyczar.enums.KeyStatus;
import org.keyczar.exceptions.KeyczarException;
import org.keyczar.interfaces.KeyczarReader;

/**
 * Created by liyan on 2/21/18.
 */

public class VerifierKeyReader implements KeyczarReader {
    private KeyMetadata meta;
    private RsaPublicKey key;

    public VerifierKeyReader(RsaPublicKey key) {
        this.key = key;

        meta = new KeyMetadata("friend_key", KeyPurpose.SIGN_AND_VERIFY, DefaultKeyType.RSA_PUB);
        KeyVersion v = new KeyVersion(0, KeyStatus.PRIMARY, true);
        meta.addVersion(v);
    }

    /*public static VerifierKeyReader loadFromSharedPreference(Context context, String userId) {
        SharedPreferences user_key = context.getSharedPreferences(context.getString(R.string.user_key), Context.MODE_PRIVATE);
        String publicKey = user_key.getString(userId, null);

        if (!Strings.isNullOrEmpty(publicKey)) {
            try {
                RsaPublicKey key = (RsaPublicKey) DefaultKeyType.RSA_PUB.getBuilder().read(publicKey);
                VerifierKeyReader reader = new VerifierKeyReader(key);
                Log.i(FriendKeyczarReader.class.getSimpleName(), "Loaded friend key for verification.");
                return reader;
            } catch (KeyczarException e) {
                throw new RuntimeException(e);
            }
        }

        Log.i(FriendKeyczarReader.class.getSimpleName(), "No saved friend key loaded.");
        return null;
    }*/

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
