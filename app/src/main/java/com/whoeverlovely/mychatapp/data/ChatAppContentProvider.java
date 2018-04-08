package com.whoeverlovely.mychatapp.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Created by yan on 3/24/18.
 */

public class ChatAppContentProvider extends ContentProvider {

    public static final String TAG = "ChatAppContentProvider";

    public static final int ID_CONTACT = 100;
    public static final int ID_CONTACT_WITH_USERID = 101;

    public static final int ID_MESSAGE = 200;
    public static final int ID_MESSAGE_ID = 201;

    private static final UriMatcher uriMatcher = buildUriMatcher();
    private ChatAppDBHelper dbHelper;

    public static UriMatcher buildUriMatcher() {
        UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        String authority = ChatAppDBContract.CONTENT_AUTHORITY;

        uriMatcher.addURI(authority, ChatAppDBContract.PATH_CONTACT, ID_CONTACT);
        uriMatcher.addURI(authority, ChatAppDBContract.PATH_CONTACT + "/#", ID_CONTACT_WITH_USERID);
        uriMatcher.addURI(authority, ChatAppDBContract.PATH_MESSAGE, ID_MESSAGE);
        uriMatcher.addURI(authority, ChatAppDBContract.PATH_MESSAGE + "/#", ID_MESSAGE_ID);

        return uriMatcher;
    }

    @Override
    public boolean onCreate() {
        dbHelper = new ChatAppDBHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        int match = uriMatcher.match(uri);
        Cursor cursor;
        switch (match) {
            case ID_CONTACT:
                cursor = db.query(ChatAppDBContract.ContactEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                break;

            case ID_CONTACT_WITH_USERID:
                cursor = db.query(ChatAppDBContract.ContactEntry.TABLE_NAME,
                        projection,
                        ChatAppDBContract.ContactEntry.COLUMN_USER_ID + "=?",
                        new String[]{uri.getLastPathSegment()},
                        null,
                        null,
                        sortOrder);
                break;

            case ID_MESSAGE:
                cursor = db.query(ChatAppDBContract.MessageEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                break;

            case ID_MESSAGE_ID:
                cursor = db.query(ChatAppDBContract.MessageEntry.TABLE_NAME,
                        projection,
                        ChatAppDBContract.MessageEntry._ID + "=?",
                        new String[]{uri.getLastPathSegment()},
                        null,
                        null,
                        sortOrder);
                break;

            default:
                throw new UnsupportedOperationException("Unknown URI: " + uri);
        }

        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    /**
     *
     * @param uri
     * @param values
     * @return the inserted URI
     */
    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        int match = uriMatcher.match(uri);

        long id = 0L;

        switch (match) {

            case ID_CONTACT:
                id = db.insert(ChatAppDBContract.ContactEntry.TABLE_NAME,
                        null,
                        values);
                break;

            case ID_MESSAGE:
                id = db.insert(ChatAppDBContract.MessageEntry.TABLE_NAME,
                        null,
                        values);
                break;

            default:
                throw new UnsupportedOperationException("Unknown URI: " + uri);
        }
        Uri returnedUri;
        if (id > 0) {
            returnedUri = ContentUris.withAppendedId(uri, id);
        } else
            throw new SQLException("Failed to insert into " + uri);

        getContext().getContentResolver().notifyChange(uri, null);
        return returnedUri;
    }

    /**
     * @param uri
     * @param selection
     * @param selectionArgs
     * @return the number of rows affected if a whereClause is passed in, 0 otherwise. To remove all rows and get a count pass "1" as the whereClause
     */
    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        if (selection == null)
            selection = "1";

        int match = uriMatcher.match(uri);
        int returnedId = 0;
        switch (match) {
            case ID_CONTACT:
                returnedId = db.delete(ChatAppDBContract.ContactEntry.TABLE_NAME,
                        selection,
                        selectionArgs);
                break;

            case ID_MESSAGE:
                returnedId = db.delete(ChatAppDBContract.MessageEntry.TABLE_NAME,
                        selection,
                        selectionArgs);
                break;

            default:
                throw new UnsupportedOperationException("Unknown URI: " + uri);

        }

        if (returnedId > 0)
            getContext().getContentResolver().notifyChange(uri, null);

        return returnedId;
    }

    /**
     *
     * @param uri
     * @param values
     * @param selection
     * @param selectionArgs
     * @return the number of rows affected
     */
    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        int match = uriMatcher.match(uri);
        int returnedId;
        switch (match) {
            case ID_CONTACT_WITH_USERID:
                returnedId = db.update(ChatAppDBContract.ContactEntry.TABLE_NAME,
                        values,
                        ChatAppDBContract.ContactEntry.COLUMN_USER_ID + "=?",
                        new String[]{uri.getLastPathSegment()}
                );
                break;

            case ID_MESSAGE_ID:
                returnedId = db.update(ChatAppDBContract.MessageEntry.TABLE_NAME,
                        values,
                        ChatAppDBContract.MessageEntry._ID + "=?",
                        new String[]{uri.getLastPathSegment()}
                );
                break;

            default:
                throw new UnsupportedOperationException("Unknown URI: " + uri);
        }

        if (returnedId > 0)
            getContext().getContentResolver().notifyChange(uri, null);

        return returnedId;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }
}
