package com.ibm.weatherapp;
/**
 * Copyright 2015, 2016 IBM Corp. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.Activity;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.ibm.mobilefirstplatform.clientsdk.android.core.api.BMSClient;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPPush;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPPushException;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPPushNotificationListener;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPPushResponseListener;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPSimplePushNotification;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;

public class MainActivity extends Activity {

    private Spinner spinner1;
    private Button btnSubmit;
    String selectedLang = "";
    private static final String TAG = MainActivity.class.getSimpleName();

    private MFPPush push; // Push client
    private MFPPushNotificationListener notificationListener; // Notification listener to handle a push sent to the phone

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        addListenerOnSpinnerItemSelection();
        addListenerOnButton();

        // initialize core SDK with IBM Bluemix application Region, TODO: Update region if not using Bluemix US SOUTH
        BMSClient.getInstance().initialize(this, "<Region>");

        // Grabs push client sdk instance
        push = MFPPush.getInstance();
        // Initialize Push client
        // You can find your App Guid and Client Secret by navigating to the Configure section of your Push dashboard, click Mobile Options (Upper Right Hand Corner)
        // TODO: Please replace <APP_GUID> and <CLIENT_SECRET> with a valid App GUID and Client Secret from the Push dashboard Mobile Options
         push.initialize(this, "<APP_GUID>", "<CLIENT_SECRET>");

        registerDevice();

        // Create notification listener and enable pop up notification when a message is received
        notificationListener = new MFPPushNotificationListener() {
            @Override
            public void onReceive(final MFPSimplePushNotification message) {

                    Log.i(TAG, "Received a Push Notification: " + message.toString());
                    runOnUiThread(new Runnable() {
                        public void run() {
                            try {
                                //<node.js app route> is the node app route hosted on the Bluemix account.
                                String nodeAppRoute= "https://<node.js app route>";
                                String temp = URLEncoder.encode(message.getAlert(), "UTF-8");
                                Log.i(TAG,"temp : " + temp);
                                SharedPreferences pref = getSharedPreferences("transLangFile", MODE_PRIVATE);
                                String selectedLangFromPref = pref.getString("transLang", "en");
                                String language = URLEncoder.encode(selectedLangFromPref,"UTF-8");
                                String urlParam = "content=" + temp +"&language="+language;
                                String title = "Received a Push Notification";
                                title = URLEncoder.encode(title, "UTF-8");
                                title = "content=" + title +"&language="+language;
                                String okStr = "Ok";
                                okStr = URLEncoder.encode(okStr,"UTF-8");
                                okStr = "content=" + okStr +"&language="+language;
                                //encodedAlert will translate the Push Notification received
                                String encodedAlert = new RetrieveTranslationTask().execute(nodeAppRoute+"/translate?" + urlParam).get();
                                Log.i(TAG,"encodedAlert : " + encodedAlert);
                                //title will translate the title of the Push Notification
                                title = new RetrieveTranslationTask().execute(nodeAppRoute+"/translate?" + title).get();
                                Log.i(TAG,"encoded alert is " + encodedAlert + " title is " + title);
                                //okStr will translte the ok button in alert popup
                                okStr = new RetrieveTranslationTask().execute(nodeAppRoute+"/translate?"+ okStr).get();
                                Log.i(TAG,"encoded alert is " + encodedAlert + " okStr is " + okStr);
                                new android.app.AlertDialog.Builder(MainActivity.this)
                                        .setTitle(title)
                                        .setMessage(encodedAlert)
                                        .setPositiveButton(okStr, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int whichButton) {

                                            }
                                        })
                                        .show();
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    });



            }
        };

    }
    public void addListenerOnSpinnerItemSelection() {
        spinner1 = (Spinner) findViewById(R.id.spinner1);
        spinner1.setOnItemSelectedListener(new CustomOnItemSelectedListener());
    }

    // get the selected dropdown list value
    public void addListenerOnButton() {

        spinner1 = (Spinner) findViewById(R.id.spinner1);
        btnSubmit = (Button) findViewById(R.id.btnSubmit);

        btnSubmit.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                Toast.makeText(MainActivity.this,
                        "OnClickListener : " +
                                "\nSpinner 1 : "+ String.valueOf(spinner1.getSelectedItem()),
                        Toast.LENGTH_SHORT).show();

                if (spinner1.getSelectedItem().equals("Spanish")){
                    selectedLang = "es";
                }
                else if (spinner1.getSelectedItem().equals("French")){
                    selectedLang="fr";
                }
                else if (spinner1.getSelectedItem().equals("German")){
                    selectedLang="de";
                }
                else if (spinner1.getSelectedItem().equals("Arabic")){
                    selectedLang="ar";
                }
                else if (spinner1.getSelectedItem().equals("Portuguese")){
                    selectedLang="pt";
                }
                else if (spinner1.getSelectedItem().equals("Italian")){
                    selectedLang="it";
                }
                System.out.println("language is "+selectedLang);
                SharedPreferences pref = getSharedPreferences("transLangFile", MODE_PRIVATE);
                SharedPreferences.Editor editor = pref.edit();
                editor.putString("transLang", selectedLang);
                editor.apply();
                System.out.println("after apply");
            }

        });
    }
    /**
     * Called when the register device button is pressed.
     * Attempts to register the device with your push service on Bluemix.
     * If successful, the push client sdk begins listening to the notification listener.
     * Also includes the example option of UserID association with the registration for very targeted Push notifications.
     *
     * //@param view the button pressed
     */
    public void registerDevice() {

        // Checks for null in case registration has failed previously
        if(push==null){
            push = MFPPush.getInstance();
        }

        // Make register button unclickable during registration and show registering text
      Log.i(TAG, "Welcome to Weather Forecast");
        // Creates response listener to handle the response when a device is registered.
        MFPPushResponseListener registrationResponselistener = new MFPPushResponseListener<String>() {
            @Override
            public void onSuccess(String response) {
                // Split response and convert to JSON object to display User ID confirmation from the backend
                String[] resp = response.split("Text: ");
                try {
                    JSONObject responseJSON = new JSONObject(resp[1]);
                    setStatus("Device Registered Successfully with USER ID " + responseJSON.getString("UserId"), true);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.i(TAG, "Successfully registered for push notifications, " + response);
                // Start listening to notification listener now that registration has succeeded
                push.listen(notificationListener);
            }


            @Override
            public void onFailure(MFPPushException exception) {
                String errLog = "Error registering for push notifications: ";
                String errMessage = exception.getErrorMessage();
                int statusCode = exception.getStatusCode();

                // Set error log based on response code and error message
                if(statusCode == 401){
                    errLog += "Cannot authenticate successfully with Bluemix Push instance, ensure your CLIENT SECRET was set correctly.";
                } else if(statusCode == 404 && errMessage.contains("Push GCM Configuration")){
                    errLog += "Push GCM Configuration does not exist, ensure you have configured GCM Push credentials on your Bluemix Push dashboard correctly.";
                } else if(statusCode == 404 && errMessage.contains("PushApplication")){
                    errLog += "Cannot find Bluemix Push instance, ensure your APPLICATION ID was set correctly and your phone can successfully connect to the internet.";
                } else if(statusCode >= 500){
                    errLog += "Bluemix and/or your Push instance seem to be having problems, please try again later.";
                }

                setStatus(errLog, false);
                Log.e(TAG,errLog);
                // make push null since registration failed
                push = null;
            }
        };

        // Attempt to register device using response listener created above
        // Include unique sample user Id instead of Sample UserId in order to send targeted push notifications to specific users
        push.registerDeviceWithUserId("Sample UserID",registrationResponselistener);
        TextView responseText = (TextView) findViewById(R.id.response_text);
        responseText.setText(R.string.Register_successful);
    }

    // If the device has been registered previously, hold push notifications when the app is paused
    @Override
    protected void onPause() {
        super.onPause();

        if (push != null) {
            push.hold();
        }
    }

    // If the device has been registered previously, ensure the client sdk is still using the notification listener from onCreate when app is resumed
    @Override
    protected void onResume() {
        super.onResume();
        if (push != null) {
            push.listen(notificationListener);
        }
    }

    /**
     * Manipulates text fields in the UI based on initialization and registration events
     * @param messageText String main text view
     * @param wasSuccessful Boolean dictates top 2 text view texts
     */
    private void setStatus(final String messageText, boolean wasSuccessful){
        final EditText responseText = (EditText) findViewById(R.id.response_text);
        final String topStatus = wasSuccessful ? "Yay!" : "Bummer";
        final String bottomStatus = wasSuccessful ? "You Are Connected" : "Something Went Wrong";

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                responseText.setText(messageText);
            }
        });
    }
}
