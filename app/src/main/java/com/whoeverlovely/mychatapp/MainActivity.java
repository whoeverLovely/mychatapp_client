package com.whoeverlovely.mychatapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import org.json.JSONException;
import org.json.JSONObject;
import org.keyczar.DefaultKeyType;
import org.keyczar.Encrypter;
import org.keyczar.RsaPublicKey;
import org.keyczar.Signer;
import org.keyczar.Verifier;
import org.keyczar.exceptions.KeyczarException;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import me.pushy.sdk.Pushy;

public class MainActivity extends AppCompatActivity {
    final private static String TAG = "MainActivity";
    private TextView profileStrTextView;
    private ImageView profileQRcodeImageView;
    SharedPreferences shared_preference;
    SharedPreferences user_key;
    String myUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        user_key = getSharedPreferences(getString(R.string.user_key), MODE_PRIVATE);
        shared_preference = getSharedPreferences(getString(R.string.default_shared_preference), MODE_PRIVATE);
        myUserId = shared_preference.getString("myUserId", null);
        if(myUserId == null) {
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

        if(item.getItemId() == R.id.chat) {
            Intent intent = new Intent(this, ChatBoxActivity.class);
            startActivity(intent);
            return true;
        }

        if(item.getItemId() == R.id.generate_key_item) {

            //prepare data for generating profile
            String publicKey = shared_preference.getString(getString(R.string.my_public_key),null);
            Log.d(TAG,"my public key is " + publicKey);
            JSONObject profileJSON = new JSONObject();
            try {
                profileJSON.put("myUserId", myUserId);
                profileJSON.put("publicKey", publicKey);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            String profile = profileJSON.toString();

            profileStrTextView = (TextView) findViewById(R.id.profile_string);
            profileQRcodeImageView = (ImageView) findViewById(R.id.profile_qrcode);
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

        if(item.getItemId() == R.id.scan_qrcode_item) {
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

        if(item.getItemId() == R.id.test_item_main) {
//            try {
//                Signer signer = new Signer(new SignKeyReader(this));
//                String signature = signer.sign("Message with Integrity");
//                Log.d("signature: ", signature);
//
//                String publicKey = shared_preference.getString(getString(R.string.my_public_key),null);
//                RsaPublicKey key = (RsaPublicKey) DefaultKeyType.RSA_PUB.getBuilder().read(publicKey);
//                VerifierKeyReader verifierReader = new VerifierKeyReader(key);
//                Verifier verifier = new Verifier(verifierReader);
//                boolean verified = verifier.verify("Message with Integrity", signature);
//                Log.d("verified: ", String.valueOf(verified));
//            } catch (Exception e) {
//                Log.d(TAG,e.toString());
//            }
            Map<String, ?> defaultPrefs = shared_preference.getAll();
            Log.d(TAG,"default_shared_preference");
            for (String key : defaultPrefs.keySet()) {
                Object pref = defaultPrefs.get(key);
                String printVal = "";
                if (pref instanceof Boolean) {
                    printVal =  key + " : " + (Boolean) pref;
                }
                if (pref instanceof Float) {
                    printVal =  key + " : " + (Float) pref;
                }
                if (pref instanceof Integer) {
                    printVal =  key + " : " + (Integer) pref;
                }
                if (pref instanceof Long) {
                    printVal =  key + " : " + (Long) pref;
                }
                if (pref instanceof String) {
                    printVal =  key + " : " + (String) pref;
                }
                if (pref instanceof Set<?>) {
                    printVal =  key + " : " + (Set<String>) pref;
                }

                Log.d(TAG,printVal);
            }

            Map<String, ?> prefs = user_key.getAll();
            Log.d(TAG,"user_key");
            for (String key : prefs.keySet()) {
                Object pref = prefs.get(key);
                String printVal = "";
                if (pref instanceof Boolean) {
                    printVal =  key + " : " + (Boolean) pref;
                }
                if (pref instanceof Float) {
                    printVal =  key + " : " + (Float) pref;
                }
                if (pref instanceof Integer) {
                    printVal =  key + " : " + (Integer) pref;
                }
                if (pref instanceof Long) {
                    printVal =  key + " : " + (Long) pref;
                }
                if (pref instanceof String) {
                    printVal =  key + " : " + (String) pref;
                }
                if (pref instanceof Set<?>) {
                    printVal =  key + " : " + (Set<String>) pref;
                }

                Log.d(TAG,printVal);
            }


            return true;
        }

        else
            return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        //scan profile result
        if (requestCode == 0) {
            if (resultCode == RESULT_OK) {
                String profile = data.getStringExtra("SCAN_RESULT");

                String userId = null;
                String publicKey = null;
                try {
                    JSONObject profileJSON = new JSONObject(profile);
                    userId = profileJSON.getString("myUserId");
                    publicKey = profileJSON.getString("publicKey");
                    publicKey = FriendKeyczarReader.createRsaPublicKey(this,publicKey);
                    Log.d(TAG,"myUserId scanned is " + userId);
                    Log.d(TAG, "publicKey scanned is " + publicKey);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                final SharedPreferences.Editor editor = user_key.edit();
                editor.putString(userId,publicKey);
                editor.apply();

                //if my user id is less than the other user, I create an AES key and send to the other user
                if(myUserId.compareTo(userId) < 0)
                    new ExchangeKey(getApplicationContext()).execute(userId, publicKey, myUserId);


                //TODO  pop up for user name


                //TODO delete the other user's public key in an hour

            }
            if(resultCode == RESULT_CANCELED){
                //handle cancel
            }
        }
    }

    private class ExchangeKey extends AsyncTask<String,Void,JSONObject> {
        Context context;
        public ExchangeKey(Context context) {
            this.context = context;
        }

        @Override
        protected JSONObject doInBackground(String... strings) {
            String userId = strings[0];
            String publicKey = strings[1];

            //generate aeskey for the friend, encrypt and save in user_key
            String aesKey = AESKeyStoreUtil.generateAESKey();
            Log.d(TAG,"generated aes key: " + aesKey);
            SharedPreferences.Editor editor = user_key.edit();
            editor.putString(userId+"_AES",AESKeyStoreUtil.encryptAESKeyStore(aesKey));
            editor.apply();

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
                Log.d(TAG,e.toString());
            } catch (KeyczarException e) {
                e.printStackTrace();
            }

            String url = getString(R.string.base_url) + "Forward";
            String chat_token = shared_preference.getString("chat_token", null);

            JSONObject result = null;
            JSONObject parameter = new JSONObject();
            try {
                parameter.put("data", data.toString());
                parameter.put("receiverUserId", userId);
                parameter.put("chat_token", AESKeyStoreUtil.decryptAESKeyStore(chat_token));
                parameter.put("userId", myUserId);
                result = NetworkUtil.executePost(url,parameter);
            } catch (JSONException e) {
                Log.d(TAG,e.toString());
            }

            return result;
        }

        @Override
        protected void onPostExecute(JSONObject jsonObject) {
            if(jsonObject != null && jsonObject.has("error")) {
                String error = null;
                try {
                    error = jsonObject.getString("error");
                    Toast.makeText(context,error,Toast.LENGTH_LONG).show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.d(TAG,error);
            }
        }
    }
}
