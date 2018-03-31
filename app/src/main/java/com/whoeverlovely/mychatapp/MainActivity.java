package com.whoeverlovely.mychatapp;

import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.whoeverlovely.mychatapp.Util.NetworkUtil;
import com.whoeverlovely.mychatapp.Util.Security.AESKeyStoreUtil;
import com.whoeverlovely.mychatapp.Util.Security.FriendKeyczarReader;
import com.whoeverlovely.mychatapp.Util.Security.SignKeyReader;
import com.whoeverlovely.mychatapp.data.ChatAppDBContract;
import com.whoeverlovely.mychatapp.data.ChatAppDBHelper;

import org.json.JSONException;
import org.json.JSONObject;
import org.keyczar.DefaultKeyType;
import org.keyczar.Encrypter;
import org.keyczar.RsaPublicKey;
import org.keyczar.Signer;
import org.keyczar.exceptions.KeyczarException;

import me.pushy.sdk.Pushy;

public class MainActivity extends AppCompatActivity {

    final private static String TAG = "MainActivity";

    String myUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myUserId = PreferenceManager.getDefaultSharedPreferences(this).getString("myUserId", null);
        if (myUserId == null) {
            Intent intent = new Intent(this, SignUpActivity.class);
            startActivity(intent);
        }

        Pushy.listen(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.generate_key_item) {

            //prepare data for generating profile
            String publicKey = PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.my_public_key), null);
            Log.d(TAG, "my public key is " + publicKey);
            JSONObject profileJSON = new JSONObject();
            try {
                profileJSON.put("myUserId", myUserId);
                profileJSON.put("publicKey", publicKey);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            String profile = profileJSON.toString();

            TextView profileStrTextView = findViewById(R.id.profile_string);
            ImageView profileQRcodeImageView = findViewById(R.id.profile_qrcode);
            profileStrTextView.setText(profile);
            MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
            try {
                BitMatrix bitMatrix = multiFormatWriter.encode(profile, BarcodeFormat.QR_CODE, 200, 200);
                BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
                Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);
                profileQRcodeImageView.setImageBitmap(bitmap);
            } catch (WriterException e) {
                e.printStackTrace();
            }
            return true;
        }

        if (item.getItemId() == R.id.scan_qrcode_item) {
            try {
                Intent intent = new Intent("com.google.zxing.client.android.SCAN");
                intent.putExtra("SCAN_MODE", "QR_CODE_MODE"); // "PRODUCT_MODE for bar codes
                startActivityForResult(intent, 0);
            } catch (Exception e) {
                Uri marketUri = Uri.parse("market://details?id=com.google.zxing.client.android");
                Intent marketIntent = new Intent(Intent.ACTION_VIEW, marketUri);
                startActivity(marketIntent);
            }
            return true;
        }

        if (item.getItemId() == R.id.test_item_main) {
            Intent intent = new Intent(this, TestActivity.class);
            startActivity(intent);
            return true;
        }

        if (item.getItemId() == R.id.contact_list_item) {
            Intent intent = new Intent(this, ContactsActivity.class);
            startActivity(intent);
            return true;
        }

        if (item.getItemId() == R.id.clear_data_item) {
            //clear both table message and table contact
            int messageCount = getContentResolver().delete(ChatAppDBContract.MessageEntry.CONTENT_URI, null, null);
            int contactCount = getContentResolver().delete(ChatAppDBContract.ContactEntry.CONTENT_URI, null, null);
            Toast.makeText(this, "deleted message no.: " + messageCount, Toast.LENGTH_LONG).show();
            Toast.makeText(this, "deleted contact no.: " + contactCount, Toast.LENGTH_LONG).show();

        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        //scan profile result
        if (requestCode == 0) {
            if (resultCode == RESULT_OK) {
                String profile = data.getStringExtra("SCAN_RESULT");

                try {
                    JSONObject profileJSON = new JSONObject(profile);
                    final String userId = profileJSON.getString("myUserId");
                    String publicKey = profileJSON.getString("publicKey");
                    publicKey = FriendKeyczarReader.createRsaPublicKey(this, publicKey);
                    Log.d(TAG, "myUserId scanned is " + userId);
                    Log.d(TAG, "publicKey scanned is " + publicKey);

                    ContentValues cv = new ContentValues();
                    cv.put(ChatAppDBContract.ContactEntry.COLUMN_USER_ID, Integer.parseInt(userId));
                    cv.put(ChatAppDBContract.ContactEntry.COLUMN_PUBLIC_KEY, publicKey);
                    getContentResolver().insert(ChatAppDBContract.ContactEntry.CONTENT_URI, cv);

                    //if my user id is less than the other user, I create an AES key and send to the other user
                    if (myUserId.compareTo(userId) < 0)
                        new ExchangeKey(getApplicationContext()).execute(userId, publicKey, myUserId);

                    //display dialog for user name
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

                    // get username_dialog.xml view
                    LayoutInflater inflater = LayoutInflater.from(this);
                    View promptsView = inflater.inflate(R.layout.username_dialog, null);

                    // set username_dialog.xml to alertdialog builder
                    alertDialogBuilder.setView(promptsView);

                    final EditText userInput = promptsView.findViewById(R.id.username_editText);

                    // set dialog message
                    alertDialogBuilder
                            .setCancelable(false)
                            .setPositiveButton("OK",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            // get user input and set it as NAME in table user
                                            ContentValues cv = new ContentValues();
                                            cv.put(ChatAppDBContract.ContactEntry.COLUMN_NAME, userInput.getText().toString());
                                            getContentResolver().update(ContentUris.withAppendedId(ChatAppDBContract.ContactEntry.CONTENT_URI, Long.parseLong(userId)),
                                                    cv, null, null);
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

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                //TODO delete the other user's public key in an hour

            }
            if (resultCode == RESULT_CANCELED) {
                //handle cancel
            }
        }
    }

    private class ExchangeKey extends AsyncTask<String, Void, JSONObject> {
        Context context;

        ExchangeKey(Context context) {
            this.context = context;
        }

        @Override
        protected JSONObject doInBackground(String... strings) {
            String userId = strings[0];
            String publicKey = strings[1];

            //generate aeskey for the friend, encrypt and save in user_key
            String aesKey = AESKeyStoreUtil.generateAESKey();
            Log.d(TAG, "generated aes key: " + aesKey);

            ContentValues cv = new ContentValues();
            cv.put(ChatAppDBContract.ContactEntry.COLUMN_AES_KEY, AESKeyStoreUtil.encryptAESKeyStore(aesKey));
            getContentResolver().update(ContentUris.withAppendedId(ChatAppDBContract.ContactEntry.CONTENT_URI, Long.parseLong(userId)),
                    cv, null, null);

            JSONObject data = null;
            try {
                //Sign AES key
                Signer signer = new Signer(new SignKeyReader(context));
                String signature = signer.sign(aesKey);
                Log.d("signature: ", signature);

                RsaPublicKey key = (RsaPublicKey) DefaultKeyType.RSA_PUB.getBuilder().read(publicKey);
                FriendKeyczarReader friendKeyczarReader = new FriendKeyczarReader(key);
                Encrypter enc = new Encrypter(friendKeyczarReader);
                String encryptedAESKey = enc.encrypt(aesKey);

                data = new JSONObject();
                data.put("key", encryptedAESKey);
                data.put("from", myUserId);
                data.put("signature", signature);


            } catch (JSONException e) {
                Log.d(TAG, e.toString());
            } catch (KeyczarException e) {
                e.printStackTrace();
            }

            String url = getString(R.string.base_url) + "Forward";
            String chat_token = PreferenceManager.getDefaultSharedPreferences(context).getString("chat_token", null);

            JSONObject result = null;
            JSONObject parameter = new JSONObject();
            try {
                parameter.put("data", data.toString());
                parameter.put("receiverUserId", userId);
                parameter.put("chat_token", AESKeyStoreUtil.decryptAESKeyStore(chat_token));
                parameter.put("userId", myUserId);
                result = NetworkUtil.executePost(url, parameter);
            } catch (JSONException e) {
                Log.d(TAG, e.toString());
            }

            return result;
        }

        @Override
        protected void onPostExecute(JSONObject jsonObject) {
            if (jsonObject != null && jsonObject.has("error")) {
                String error = null;
                try {
                    error = jsonObject.getString("error");
                    Toast.makeText(context, error, Toast.LENGTH_LONG).show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.d(TAG, error);
            }
        }
    }

}
