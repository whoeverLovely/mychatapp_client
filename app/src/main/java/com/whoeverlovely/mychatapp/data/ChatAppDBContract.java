package com.whoeverlovely.mychatapp.data;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by yan on 3/17/18.
 */

public class ChatAppDBContract {

    public static final String CONTENT_AUTHORITY = "com.whoeverlovely.mychatapp";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    public static final String PATH_CONTACT = "contact";
    public static final String PATH_MESSAGE = "message";

    public static final class MessageEntry implements BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_MESSAGE).build();

        public static final String TABLE_NAME = "message";
        public static final String COLUMN_SENDER_ID = "senderId";
        public static final String COLUMN_RECEIVER_ID = "receiverId";
        //decrypted message content
        public static final String COLUMN_MESSAGE_CONTENT = "messageContent";
        public static final String COLUMN_TIMESTAMP = "timestamp";
        /**
         * 1* message received
         * - 10 message received but unread
         * - 11 message received and read
         *
         * 2* message sent
         * - 20 message sent
         * - 21 message sent successfully
         */
        public static final String COLUMN_STATUS = "status";
    }

    public static final class ContactEntry implements BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_CONTACT).build();

        public static final String TABLE_NAME = "contact";
        public static final String COLUMN_USER_ID = "userId";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_AES_KEY = "AESKey";
        public static final String COLUMN_PUBLIC_KEY = "publicKey";
        /**
         * -0 unverified
         * -1 verified
         */
        public static final String COLUMN_VERIFIED_FLAG = "verifiedFlag";
        public static final String COLUMN_SIGNATURE = "signature";
    }
}
