package com.whoeverlovely.mychatapp;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by liyan on 2/22/18.
 */

public class Contact implements Serializable{

    private String name;
    private String userId;
    private String encryptedAESKey;

    public String getName() {
        return name;
    }

    public String getUserId() {
        return userId;
    }

    public String getEncryptedAESKey() {
        return encryptedAESKey;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setEncryptedAESKey(String encryptedAESKey) {
        this.encryptedAESKey = encryptedAESKey;
    }
}
