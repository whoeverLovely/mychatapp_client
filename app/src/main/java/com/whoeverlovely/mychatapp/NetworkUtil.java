package com.whoeverlovely.mychatapp;

import android.util.Log;

import com.google.common.base.Strings;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by yan on 2/11/18.
 */

public class NetworkUtil {

    private static final String TAG = "NetworkUtil";

    public static JSONObject executePost(String url, JSONObject JSONdata) throws JSONException {
        JSONObject resp = null;

        HttpURLConnection httpURLConnection;
        try {
            httpURLConnection = (HttpURLConnection) new URL(url).openConnection();
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setDoOutput(true);             // need to set it to true if you want to send (output) a request body, for example with POST or PUT requests. With GET, you do not usually send a body, so you do not need it.
            Log.d(TAG, "HttpUrlConnection established");

            DataOutputStream wr = new DataOutputStream(httpURLConnection.getOutputStream());
            wr.writeBytes(JSONdata.toString());
            Log.d(TAG, "Sent POST request: " + JSONdata.toString());
            wr.flush();
            wr.close();

            InputStream in = httpURLConnection.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(in);

            int inputStreamData = inputStreamReader.read();
            String response = "";
            while (inputStreamData != -1) {
                char current = (char) inputStreamData;
                inputStreamData = inputStreamReader.read();
                response += current;
            }

            Log.d(TAG, "Received POST String response: " + response);
            if (!Strings.isNullOrEmpty(response))
                resp = new JSONObject(response);
            Log.d(TAG, "Received POST JSON response: " + resp);
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return resp;
    }
}
