package com.whoeverlovely.mychatapp;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.whoeverlovely.mychatapp.data.ChatAppContentProvider;
import com.whoeverlovely.mychatapp.data.ChatAppDBContract;
import com.whoeverlovely.mychatapp.data.ChatAppDBHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

/**
 * Created by yan on 3/24/18.
 */

@RunWith(AndroidJUnit4.class)
public class ContentProviderTest {

    final private static String TAG = "ContentProviderTest";
    Context mContext = InstrumentationRegistry.getTargetContext();

    /**
     * Because we annotate this method with the @Before annotation, this method will be called
     * before every single method with an @Test annotation. We want to start each test clean, so we
     * delete all entries in the tasks directory to do so.
     */
    @Before
    public void setUp() {
        Log.d(TAG, "setUp started");
        /* Use TaskDbHelper to get access to a writable database */
        ChatAppDBHelper dbHelper = new ChatAppDBHelper(mContext);
        SQLiteDatabase database = dbHelper.getWritableDatabase();
        database.delete(ChatAppDBContract.ContactEntry.TABLE_NAME, null, null);
        database.delete(ChatAppDBContract.MessageEntry.TABLE_NAME, null, null);
    }

    /**
     * This test checks to make sure that the content provider is registered correctly in the
     * AndroidManifest file. If it fails, you should check the AndroidManifest to see if you've
     * added a <provider/> tag and that you've properly specified the android:authorities attribute.
     */
    @Test
    public void testProviderRegistry() {
        Log.d(TAG, "testProviderRegistry started");

        /*
         * A ComponentName is an identifier for a specific application component, such as an
         * Activity, ContentProvider, BroadcastReceiver, or a Service.
         *
         * Two pieces of information are required to identify a component: the package (a String)
         * it exists in, and the class (a String) name inside of that package.
         *
         * We will use the ComponentName for our ContentProvider class to ask the system
         * information about the ContentProvider, specifically, the authority under which it is
         * registered.
         */
        String packageName = mContext.getPackageName();
        String taskProviderClassName = ChatAppContentProvider.class.getName();
        ComponentName componentName = new ComponentName(packageName, taskProviderClassName);

        try {

            /*
             * Get a reference to the package manager. The package manager allows us to access
             * information about packages installed on a particular device. In this case, we're
             * going to use it to get some information about our ContentProvider under test.
             */
            PackageManager pm = mContext.getPackageManager();

            /* The ProviderInfo will contain the authority, which is what we want to test */
            ProviderInfo providerInfo = pm.getProviderInfo(componentName, 0);
            String actualAuthority = providerInfo.authority;
            String expectedAuthority = packageName;

            /* Make sure that the registered authority matches the authority from the Contract */
            String incorrectAuthority =
                    "Error: TaskContentProvider registered with authority: " + actualAuthority +
                            " instead of expected authority: " + expectedAuthority;
            assertEquals(incorrectAuthority,
                    actualAuthority,
                    expectedAuthority);

        } catch (PackageManager.NameNotFoundException e) {
            String providerNotRegisteredAtAll =
                    "Error: TaskContentProvider not registered at " + mContext.getPackageName();
            /*
             * This exception is thrown if the ContentProvider hasn't been registered with the
             * manifest at all. If this is the case, you need to double check your
             * AndroidManifest file
             */
            fail(providerNotRegisteredAtAll);
        }
    }

    //================================================================================
    // Test UriMatcher
    //================================================================================


    private static final Uri TEST_CONTACT = ChatAppDBContract.ContactEntry.CONTENT_URI;
    private static final Uri TEST_MESSAGE = ChatAppDBContract.MessageEntry.CONTENT_URI;
    // Content URI for a single task with id = 1
    private static final Uri TEST_CONTACT_USERID = TEST_CONTACT.buildUpon().appendPath("1").build();


    /**
     * This function tests that the UriMatcher returns the correct integer value for
     * each of the Uri types that the ContentProvider can handle. Uncomment this when you are
     * ready to test your UriMatcher.
     */
    @Test
    public void testUriMatcher() {

        Log.d(TAG, "testUriMatcher started");

        /* Create a URI matcher that the TaskContentProvider uses */
        UriMatcher testMatcher = ChatAppContentProvider.buildUriMatcher();

        /* Test that the code returned from our matcher matches the expected TASKS int */
        String contactUriDoesNotMatch = "Error: The CONTACT URI was matched incorrectly.";
        int actualContactMatchCode = testMatcher.match(TEST_CONTACT);
        int expectedContactMatchCode = ChatAppContentProvider.ID_CONTACT;
        assertEquals(contactUriDoesNotMatch,
                actualContactMatchCode,
                expectedContactMatchCode);

        /* Test that the code returned from our matcher matches the expected TASK_WITH_ID */
        String contactWithUserIdDoesNotMatch =
                "Error: The TEST_CONTACT_USERID URI was matched incorrectly.";
        int actualContactWithUserIdCode = testMatcher.match(TEST_CONTACT_USERID);
        int expectedContactWithUserIdCode = ChatAppContentProvider.ID_CONTACT_WITH_USERID;
        assertEquals(contactWithUserIdDoesNotMatch,
                actualContactWithUserIdCode,
                expectedContactWithUserIdCode);
    }

    //================================================================================
    // Test Insert
    //================================================================================


    /**
     * Tests inserting a single row of data via a ContentResolver
     */
    @Test
    public void testInsert() {

        Log.d(TAG, "testInsert started");

        /* Create values to insert */
        ContentValues testContactValue = new ContentValues();
        testContactValue.put(ChatAppDBContract.ContactEntry.COLUMN_AES_KEY, "test aes key");
        testContactValue.put(ChatAppDBContract.ContactEntry.COLUMN_USER_ID, 5);
        testContactValue.put(ChatAppDBContract.ContactEntry.COLUMN_NAME, "heyheyhey");
        testContactValue.put(ChatAppDBContract.ContactEntry.COLUMN_PUBLIC_KEY, "test pub key");

        /* TestContentObserver allows us to test if notifyChange was called appropriately */
        TestUtil.TestContentObserver taskObserver = TestUtil.getTestContentObserver();

        ContentResolver contentResolver = mContext.getContentResolver();

        /* Register a content observer to be notified of changes to data at a given URI (tasks) */
        contentResolver.registerContentObserver(
                /* URI that we would like to observe changes to */
                ChatAppDBContract.ContactEntry.CONTENT_URI,
                /* Whether or not to notify us if descendants of this URI change */
                true,
                /* The observer to register (that will receive notifyChange callbacks) */
                taskObserver);


        Cursor cursor = new ChatAppDBHelper(mContext).getReadableDatabase().query(ChatAppDBContract.ContactEntry.TABLE_NAME,
                null, null, null, null, null, null);
        Log.d(TAG, "cursor counts before insert: " + cursor.getCount());

        Uri uri = contentResolver.insert(ChatAppDBContract.ContactEntry.CONTENT_URI, testContactValue);


        Uri expectedUri = ContentUris.withAppendedId(ChatAppDBContract.ContactEntry.CONTENT_URI, 1);

        String insertProviderFailed = "Unable to insert item through Provider";
        assertEquals(insertProviderFailed, expectedUri, uri);

        /*
         * If this fails, it's likely you didn't call notifyChange in your insert method from
         * your ContentProvider.
         */
        taskObserver.waitForNotificationOrFail();

        /*
         * waitForNotificationOrFail is synchronous, so after that call, we are done observing
         * changes to content and should therefore unregister this observer.
         */
        contentResolver.unregisterContentObserver(taskObserver);
    }


    //================================================================================
    // Test Query (for tasks directory)
    //================================================================================


    /**
     * Inserts data, then tests if a query for the tasks directory returns that data as a Cursor
     */
    @Test
    public void testQuery() {

        Log.d(TAG, "testQuery started");

        /* Get access to a writable database */
        ChatAppDBHelper dbHelper = new ChatAppDBHelper(mContext);
        SQLiteDatabase database = dbHelper.getWritableDatabase();

        /* Create values to insert */
        ContentValues testContactValue = new ContentValues();
        testContactValue.put(ChatAppDBContract.ContactEntry.COLUMN_AES_KEY, "test aes key");
        testContactValue.put(ChatAppDBContract.ContactEntry.COLUMN_USER_ID, 1);
        testContactValue.put(ChatAppDBContract.ContactEntry.COLUMN_NAME, "heyheyhey");
        testContactValue.put(ChatAppDBContract.ContactEntry.COLUMN_PUBLIC_KEY, "test pub key");

        /* Insert ContentValues into database and get a row ID back */
        long taskRowId = database.insert(
                /* Table to insert values into */
                ChatAppDBContract.ContactEntry.TABLE_NAME,
                null,
                /* Values to insert into table */
                testContactValue);

        String insertFailed = "Unable to insert directly into the database";
        assertTrue(insertFailed, taskRowId != -1);

        /* We are done with the database, close it now. */
        database.close();

        /* Perform the ContentProvider query */
        Cursor taskCursor = mContext.getContentResolver().query(
                ChatAppDBContract.ContactEntry.CONTENT_URI,
                /* Columns; leaving this null returns every column in the table */
                null,
                /* Optional specification for columns in the "where" clause above */
                null,
                /* Values for "where" clause */
                null,
                /* Sort order to return in Cursor */
                null);

        String queryFailed = "Query failed to return a valid Cursor";
        assertTrue(queryFailed, taskCursor != null);

        taskCursor = mContext.getContentResolver().query(ContentUris.withAppendedId(ChatAppDBContract.ContactEntry.CONTENT_URI, 1),
                null, null, null, null);
        assertTrue(queryFailed, taskCursor != null);

        /* We are done with the cursor, close it now. */
        taskCursor.close();
    }

}
