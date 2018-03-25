package com.whoeverlovely.mychatapp.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Base64;

import com.whoeverlovely.mychatapp.Util.Security.AESKeyStoreUtil;

/**
 * Created by yan on 3/19/18.
 */

public class ChatAppDBHelper extends SQLiteOpenHelper {

    // The database name
    private static final String DATABASE_NAME = "mychatapp.db";

    // If you change the database schema, you must increment the database version
    private static final int DATABASE_VERSION = 5;

    // Constructor
    public ChatAppDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final String SQL_CREATE_MESSAGE_TABLE = "CREATE TABLE " + ChatAppDBContract.MessageEntry.TABLE_NAME + " ("
                + ChatAppDBContract.MessageEntry._ID + " INTEGER PRIMARY KEY, "
                + ChatAppDBContract.MessageEntry.COLUMN_MESSAGE_CONTENT + " TEXT NOT NULL, "
                + ChatAppDBContract.MessageEntry.COLUMN_SENDER_ID + " INTEGER NOT NULL, "
                + ChatAppDBContract.MessageEntry.COLUMN_RECEIVER_ID + " INTEGER NOT NULL, "
                + ChatAppDBContract.MessageEntry.COLUMN_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP, "
                + ChatAppDBContract.MessageEntry.COLUMN_STATUS + " INTEGER NOT NULL);";

        final String SQL_CREATE_CONTACT_TABLE = "CREATE TABLE " + ChatAppDBContract.ContactEntry.TABLE_NAME + " ("
                + ChatAppDBContract.ContactEntry._ID + " INTEGER PRIMARY KEY, "
                + ChatAppDBContract.ContactEntry.COLUMN_USER_ID + " INTEGER NOT NULL, "
                + ChatAppDBContract.ContactEntry.COLUMN_NAME + " TEXT, "
                + ChatAppDBContract.ContactEntry.COLUMN_AES_KEY + " TEXT, "
                + ChatAppDBContract.ContactEntry.COLUMN_PUBLIC_KEY + " TEXT NOT NULL);";

        db.execSQL(SQL_CREATE_MESSAGE_TABLE);
        db.execSQL(SQL_CREATE_CONTACT_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //db.execSQL("ALTER TABLE foo ADD COLUMN new_column INTEGER DEFAULT 0");

        db.execSQL("DROP TABLE IF EXISTS " + ChatAppDBContract.MessageEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + ChatAppDBContract.ContactEntry.TABLE_NAME);
        onCreate(db);

    }

    public static String retrieveContactWithUserId (SQLiteDatabase db, String userId, String columnName) {
        String[] columns = new String[] {columnName};
        Cursor cursor = db.query(ChatAppDBContract.ContactEntry.TABLE_NAME,
                columns,
                ChatAppDBContract.ContactEntry.COLUMN_USER_ID + "=" + Integer.parseInt(userId),
                null,
                null,
                null,
                null);

        if(cursor.moveToFirst()) {
            String columnValue = cursor.getString(cursor.getColumnIndex(columnName));
            return columnValue;
        }

        return null;
    }
}
