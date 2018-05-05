package com.whoeverlovely.mychatapp.util.Security;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.common.base.Strings;
import com.whoeverlovely.mychatapp.R;

import org.keyczar.DefaultKeyType;
import org.keyczar.KeyMetadata;
import org.keyczar.KeyVersion;
import org.keyczar.KeyczarPublicKey;
import org.keyczar.RsaPrivateKey;
import org.keyczar.enums.KeyPurpose;
import org.keyczar.enums.KeyStatus;
import org.keyczar.exceptions.KeyczarException;
import org.keyczar.interfaces.KeyczarReader;

/**
 * Created by liyan on 2/21/18.
 */

public class SignKeyReader implements KeyczarReader{

    private final static String TAG = "SignKeyReader";
    private KeyMetadata meta;
    private RsaPrivateKey key;

    public SignKeyReader(Context context) {
        meta = new KeyMetadata("my_key", KeyPurpose.SIGN_AND_VERIFY, DefaultKeyType.RSA_PRIV);
        KeyVersion v = new KeyVersion(0, KeyStatus.PRIMARY, true);
        meta.addVersion(v);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String encryptedPrivateKey = sharedPreferences.getString(context.getString(R.string.pref_my_private_key), null);
        if (Strings.isNullOrEmpty(encryptedPrivateKey)) {
            Log.i(getClass().getSimpleName(), "No stored my key.");
        } else {
            try {
                key = (RsaPrivateKey) DefaultKeyType.RSA_PRIV.getBuilder().read(AESKeyStoreUtil.decryptAESKeyStore(encryptedPrivateKey));
                Log.i(getClass().getSimpleName(), "Loaded my key.");
            } catch (KeyczarException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public String getKey(int version) throws KeyczarException {
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

    public KeyczarPublicKey getPublicKey() {
        return key.getPublic();
    }
}
