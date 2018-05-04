package com.whoeverlovely.mychatapp.util.Security;

import org.keyczar.DefaultKeyType;
import org.keyczar.KeyMetadata;
import org.keyczar.KeyVersion;
import org.keyczar.enums.KeyPurpose;
import org.keyczar.enums.KeyStatus;
import org.keyczar.exceptions.KeyczarException;
import org.keyczar.interfaces.KeyczarReader;

/**
 * Created by yan on 2/12/18.
 */

public class AESKeyczarReader implements KeyczarReader{

    String aesKeyStr;
    private final KeyMetadata metadata;

    public AESKeyczarReader(String aesKeyStr) {
        this.metadata = new KeyMetadata(
                "My Reader", KeyPurpose.DECRYPT_AND_ENCRYPT, DefaultKeyType.AES);
        KeyVersion version = new KeyVersion(0, KeyStatus.PRIMARY, false);
        this.metadata.addVersion(version);
        this.aesKeyStr = aesKeyStr;
    }

    @Override
    public String getKey(int version) throws KeyczarException {
        return aesKeyStr;
    }

    @Override
    public String getKey() throws KeyczarException {
        return aesKeyStr;
    }

    @Override
    public String getMetadata() throws KeyczarException {
        return metadata.toString();
    }
}
