package com.ibm.weatherapp;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

/**
 * Created by jojustin on 06/10/16.
 */
public class RetrieveTranslationTask extends AsyncTask<String, Integer, String>{

    private Exception exception;

    protected String doInBackground(String... urls) {
        String translatedStr = "";
        // Send data
        try
        {
            HttpClient httpclient = new DefaultHttpClient();
           // Log.i("URL1", urls[0]);


            // Prepare a request object
            HttpGet httpget = new HttpGet(urls[0]);

            // Execute the request
            HttpResponse response;
            try {
                response = httpclient.execute(httpget);
                // Examine the response status
                Log.i("Praeda", response.getStatusLine().toString());

                // Get hold of the response entity
                HttpEntity entity = response.getEntity();
                translatedStr = EntityUtils.toString(entity);
                // If the response does not enclose an entity, there is no need
                // to worry about connection release

                System.out.println("Response of GET request"+translatedStr);
            }
            catch(Exception ex)
            {
                ex.printStackTrace();
            }
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
        return translatedStr;
    }
}
