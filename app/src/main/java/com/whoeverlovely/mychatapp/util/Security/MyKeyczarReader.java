package com.whoeverlovely.mychatapp.util.Security;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.google.common.base.Strings;
import com.whoeverlovely.mychatapp.R;

import org.keyczar.DefaultKeyType;
import org.keyczar.KeyMetadata;
import org.keyczar.KeyVersion;
import org.keyczar.KeyczarPublicKey;
import org.keyczar.RsaPrivateKey;
import org.keyczar.enums.KeyPurpose;
import org.keyczar.enums.KeyStatus;
import org.keyczar.enums.RsaPadding;
import org.keyczar.exceptions.KeyczarException;
import org.keyczar.interfaces.KeyczarReader;
import org.keyczar.keyparams.RsaKeyParameters;


/**
 * Created by yan on 2/11/18.
 */

public class MyKeyczarReader implements KeyczarReader {

    private final static String TAG = "MyKeyczarKeyReader";
    private KeyMetadata meta;
    private RsaPrivateKey key;

    public MyKeyczarReader(Context context) {
        meta = new KeyMetadata("my_key", KeyPurpose.DECRYPT_AND_ENCRYPT, DefaultKeyType.RSA_PRIV);
        KeyVersion v = new KeyVersion(0, KeyStatus.PRIMARY, true);
        meta.addVersion(v);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String encryptedPrivateKey = sharedPreferences.getString(context.getString(R.string.pref_my_private_key), null);
        if (Strings.isNullOrEmpty(encryptedPrivateKey)) {
            Log.i(getClass().getSimpleName(), "No stored my key. Creating new one.");
            key = createKey(context);
            Log.i(TAG, "New key is saved.");
            Toast.makeText(context, "New key is saved.", Toast.LENGTH_LONG).show();
        } else {
            try {
                key = (RsaPrivateKey) DefaultKeyType.RSA_PRIV.getBuilder().read(AESKeyStoreUtil.decryptAESKeyStore(encryptedPrivateKey));
                Log.i(getClass().getSimpleName(), "Loaded my key.");
            } catch (KeyczarException e) {
                throw new RuntimeException(e);
            }
        }
    }


    /**
     * @param context
     * @return create default private key and return, if already exists, return null
     */
    public static RsaPrivateKey createKey(Context context) {
        try {
            //generate a new RSA key pair
            RsaPrivateKey privateKey = (RsaPrivateKey) DefaultKeyType.RSA_PRIV.getBuilder().generate(new RsaKeyParameters() {
                @Override
                public int getKeySize() throws KeyczarException {
                    return 2048;
                }

                @Override
                public RsaPadding getRsaPadding() throws KeyczarException {
                    return null;
                }
            });

            //save encrypted private key and public key in default shared preference
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            if (!sharedPreferences.contains(context.getString(R.string.pref_my_private_key))) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(context.getString(R.string.pref_my_private_key), AESKeyStoreUtil.encryptAESKeyStore(privateKey.toString()));
                editor.putString(context.getString(R.string.pref_my_public_key), privateKey.getPublic().toString());
                editor.apply();
                Log.i(TAG, "New key is saved.");
                Toast.makeText(context, "New key is saved.", Toast.LENGTH_LONG).show();
                return privateKey;
            }
            return null;
        } catch (KeyczarException e) {
            throw new RuntimeException(e);
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
