package com.whoeverlovely.mychatapp.data;

import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.google.common.base.Strings;
import com.whoeverlovely.mychatapp.R;

/**
 * Created by yan on 3/19/18.
 */

public class ChatAppDBHelper extends SQLiteOpenHelper {

    final private static String TAG = "ChatAppDBHelper";

    // The database name
    private static final String DATABASE_NAME = "mychatapp.db";

    // If you change the database schema, you must increment the database version
    private static final int DATABASE_VERSION = 8;

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
                + ChatAppDBContract.ContactEntry.COLUMN_PUBLIC_KEY + " TEXT, "
                + ChatAppDBContract.ContactEntry.COLUMN_VERIFIED_FLAG + " INTEGER NOT NULL, "
                + ChatAppDBContract.ContactEntry.COLUMN_SIGNATURE + " TEXT);";

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

    /*public static void setUserNameWithAlertDialog(final Context context, final long userId) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);

        // get fragment_username_dialog.xmlialog.xml view
        LayoutInflater inflater = LayoutInflater.from(context);
        View promptsView = inflater.inflate(R.layout.fragment_username_dialog, null);

        // set fragment_username_dialog.xmlialog.xml to alertdialog builder
        alertDialogBuilder.setView(promptsView);

        final EditText userInput = promptsView.findViewById(R.id.username_editText);

        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // get user input and set it as NAME in table user if the input is not null
                                String newName = userInput.getText().toString();
                                if(!Strings.isNullOrEmpty(newName)) {
                                    ContentValues cv = new ContentValues();
                                    cv.put(ChatAppDBContract.ContactEntry.COLUMN_NAME, newName);
                                    context.getContentResolver().update(ContentUris.withAppendedId(ChatAppDBContract.ContactEntry.CONTENT_URI, userId),
                                            cv, null, null);
                                    Log.d(TAG, "nick name has been updated.");
                                }
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }*/
}
