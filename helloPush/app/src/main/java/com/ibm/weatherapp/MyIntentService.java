package com.ibm.weatherapp;


import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.ibm.mobilefirstplatform.clientsdk.android.logger.api.Logger;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPPush;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPPushIntentService;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPPushNotificationHandler;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPPushNotificationOptions;
import com.ibm.mobilefirstplatform.clientsdk.android.push.internal.MFPInternalPushMessage;
import com.ibm.mobilefirstplatform.clientsdk.android.push.internal.MFPPushBroadcastReceiver;
import com.ibm.mobilefirstplatform.clientsdk.android.push.internal.MFPPushConstants;
import com.ibm.mobilefirstplatform.clientsdk.android.push.internal.MFPPushUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.Random;

import static com.ibm.mobilefirstplatform.clientsdk.android.push.internal.MFPPushConstants.ACTION;
import static com.ibm.mobilefirstplatform.clientsdk.android.push.internal.MFPPushConstants.BIGTEXT_NOTIFICATION;
import static com.ibm.mobilefirstplatform.clientsdk.android.push.internal.MFPPushConstants.DISMISS_NOTIFICATION;
import static com.ibm.mobilefirstplatform.clientsdk.android.push.internal.MFPPushConstants.DRAWABLE;
import static com.ibm.mobilefirstplatform.clientsdk.android.push.internal.MFPPushConstants.INBOX_NOTIFICATION;
import static com.ibm.mobilefirstplatform.clientsdk.android.push.internal.MFPPushConstants.LINES;
import static com.ibm.mobilefirstplatform.clientsdk.android.push.internal.MFPPushConstants.NID;
import static com.ibm.mobilefirstplatform.clientsdk.android.push.internal.MFPPushConstants.NOTIFICATIONID;
import static com.ibm.mobilefirstplatform.clientsdk.android.push.internal.MFPPushConstants.PICTURE_NOTIFICATION;
import static com.ibm.mobilefirstplatform.clientsdk.android.push.internal.MFPPushConstants.RAW;
import static com.ibm.mobilefirstplatform.clientsdk.android.push.internal.MFPPushConstants.TEXT;
import static com.ibm.mobilefirstplatform.clientsdk.android.push.internal.MFPPushConstants.TITLE;
import static com.ibm.mobilefirstplatform.clientsdk.android.push.internal.MFPPushConstants.TYPE;
import static com.ibm.mobilefirstplatform.clientsdk.android.push.internal.MFPPushConstants.URL;
/**
 * Created by jojustin on 14/10/16.
 */
public class MyIntentService extends MFPPushIntentService{
    public static final String IBM_PUSH_NOTIFICATION = ".IBMPushNotification";
    public static final String GCM_MESSAGE = ".C2DM_MESSAGE";
    public static final String GCM_EXTRA_MESSAGE = "message";
    static final String PREFS_NOTIFICATION_MSG = "LatestNotificationMsg";
    static final String PREFS_NOTIFICATION_COUNT = "NotificationCount";

    public static boolean isAppForeground = true;

    private static Random randomObj = new Random();

    private LinkedList<Intent> intentsQueue = new LinkedList<Intent>();

    private static Logger logger = Logger.getLogger(Logger.INTERNAL_PREFIX + MyIntentService.class.getSimpleName());

    public MyIntentService() {
        super();
    }

    public static boolean isAppForeground() {
        return isAppForeground;
    }

    public static void setAppForeground(boolean isAppForeground) {
        MFPPushIntentService.isAppForeground = isAppForeground;
    }

    private BroadcastReceiver resultReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            System.out.println("---- broadcast receiver");
            if (getResultCode() == Activity.RESULT_FIRST_USER
                    || !isAppForeground()) {
                logger.debug("MFPPushIntentService: App is not running in foreground. Create a notification.");
                onUnhandled(context, intent);
            }
        }
    };

    private void saveInSharedPreferences(MFPInternalPushMessage message) {
        SharedPreferences sharedPreferences = getSharedPreferences(
                MFPPush.PREFS_NAME, Context.MODE_PRIVATE);
        String msgString = message.toJson().toString();
        //PREFS_NOTIFICATION_COUNT value provides the count of number of undelivered notifications stored in the sharedpreferences
        int count = sharedPreferences.getInt(PREFS_NOTIFICATION_COUNT, 0);
        //Increment the count and use it for the next notification
        count++;
        MFPPushUtils.storeContentInSharedPreferences(sharedPreferences, PREFS_NOTIFICATION_MSG + count, msgString);

        MFPPushUtils.storeContentInSharedPreferences(sharedPreferences, PREFS_NOTIFICATION_COUNT, count);
    }

    private void onUnhandled(Context context, Intent intent) {
        String action = intent.getAction();
        if ((MFPPushUtils.getIntentPrefix(context) + GCM_MESSAGE).equals(action)) {
            MFPInternalPushMessage message = intent
                    .getParcelableExtra(GCM_EXTRA_MESSAGE);

            //intent.putExtra(GCM_EXTRA_MESSAGE,)
            int notificationId = randomObj.nextInt();

            message.setNotificationId(notificationId);
            saveInSharedPreferences(message);
            System.out.println("--------- message object is "+message);

            intent = new Intent(MFPPushUtils.getIntentPrefix(context)
                    + IBM_PUSH_NOTIFICATION);

            intent.setClass(context, MFPPushNotificationHandler.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            intent.putExtra(NOTIFICATIONID, message.getNotificationId());

            generateNotification(context, message.getAlert(),
                    getNotificationTitle(context), message.getAlert(),
                    getCustomNotificationIcon(context, ""), intent, getNotificationSound(message), notificationId, message);
        }
    }

    private String getNotificationTitle(Context context) {
        // Check if push_notification_title is defined, if not get the
        // application name
        int notificationTitle = -1;
        try {
            notificationTitle = MFPPushUtils.getResourceId(getApplicationContext(),
                    "string", "push_notification_title");
            return context.getString(notificationTitle);
        } catch (Exception e) {
            // ignore the exception
        }

        if (notificationTitle == -1) {
            ApplicationInfo appInfo = null;
            PackageManager packManager = context.getPackageManager();
            try {
                appInfo = packManager.getApplicationInfo(
                        context.getPackageName(), 0);
            } catch (Exception e) {
                logger.warn("MFPPushIntentService:getNotificationTitle() - Notification will not have a title because application name is not available.");
            }

            if (appInfo != null) {
                return (String) packManager.getApplicationLabel(appInfo);
            }
        }

        return "";
    }


    private void generateNotification(Context context, String ticker,
                                      String title, String msg, int icon, Intent intent, String sound, int notificationId, MFPInternalPushMessage message) {

        int androidSDKVersion = Build.VERSION.SDK_INT;
        long when = System.currentTimeMillis();
        Notification notification = null;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                this);

        if (message.getGcmStyle() != null && androidSDKVersion > 21) {
            NotificationCompat.Builder mBuilder = null;
            NotificationManager notificationManager = (NotificationManager) context
                    .getSystemService(Context.NOTIFICATION_SERVICE);

            try {
                JSONObject gcmStyleObject = new JSONObject(message.getGcmStyle());
                String type = gcmStyleObject.getString(TYPE);

                if (type != null && type.equalsIgnoreCase(PICTURE_NOTIFICATION)) {
                    Bitmap remote_picture = null;
                    NotificationCompat.BigPictureStyle notificationStyle = new NotificationCompat.BigPictureStyle();
                    notificationStyle.setBigContentTitle(ticker);
                    notificationStyle.setSummaryText(gcmStyleObject.getString(TITLE));

                    try {
                        remote_picture = new getBitMapBigPictureNotification().execute(gcmStyleObject.getString(URL)).get();
                    } catch (Exception e) {
                        logger.error("MFPPushIntentService:generateNotification() - Error while fetching image file.");
                    }
                    if (remote_picture != null) {
                        notificationStyle.bigPicture(remote_picture);
                    }

                    mBuilder = new NotificationCompat.Builder(
                            context);
                    notification = mBuilder
                            .setSmallIcon(icon)
                            .setLargeIcon(remote_picture)
                            .setAutoCancel(true)
                            .setContentTitle(title)
                            .setContentIntent(PendingIntent
                                    .getActivity(context, notificationId, intent,
                                            PendingIntent.FLAG_UPDATE_CURRENT))
                            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                            .setContentText(msg)
                            .setStyle(notificationStyle).build();

                } else if (type != null && type.equalsIgnoreCase(BIGTEXT_NOTIFICATION)) {
                    NotificationCompat.BigTextStyle notificationStyle = new NotificationCompat.BigTextStyle();
                    notificationStyle.setBigContentTitle(ticker);
                    notificationStyle.setSummaryText(gcmStyleObject.getString(TITLE));
                    notificationStyle.bigText(gcmStyleObject.getString(TEXT));

                    mBuilder = new NotificationCompat.Builder(
                            context);
                    notification = mBuilder
                            .setSmallIcon(icon)
                            .setAutoCancel(true)
                            .setContentTitle(title)
                            .setContentIntent(PendingIntent
                                    .getActivity(context, notificationId, intent,
                                            PendingIntent.FLAG_UPDATE_CURRENT))
                            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                            .setContentText(msg)
                            .setStyle(notificationStyle).build();
                } else if (type != null && type.equalsIgnoreCase(INBOX_NOTIFICATION)) {
                    NotificationCompat.InboxStyle notificationStyle = new NotificationCompat.InboxStyle();
                    notificationStyle.setBigContentTitle(ticker);
                    notificationStyle.setSummaryText(gcmStyleObject.getString(TITLE));

                    String lines = gcmStyleObject.getString(LINES).replaceAll("\\[", "").replaceAll("\\]", "");
                    String[] lineArray = lines.split(",");

                    for (String line : lineArray) {
                        notificationStyle.addLine(line);
                    }

                    mBuilder = new NotificationCompat.Builder(
                            context);
                    notification = mBuilder
                            .setSmallIcon(icon)
                            .setAutoCancel(true)
                            .setContentTitle(title)
                            .setContentIntent(PendingIntent
                                    .getActivity(context, notificationId, intent,
                                            PendingIntent.FLAG_UPDATE_CURRENT))
                            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                            .setContentText(msg)
                            .setStyle(notificationStyle).build();
                }

                notification.flags = Notification.FLAG_AUTO_CANCEL;
                notificationManager.notify(notificationId, notification);
            } catch (JSONException e) {
                logger.error("MFPPushIntentService:generateNotification() - Error while parsing JSON.");
            }

        } else {
            if (androidSDKVersion > 10) {
                builder.setContentIntent(PendingIntent
                        .getActivity(context, notificationId, intent,
                                PendingIntent.FLAG_UPDATE_CURRENT))
                        .setSmallIcon(icon).setTicker(ticker).setWhen(when)
                        .setAutoCancel(true).setContentTitle(title)
                        .setContentText(msg).setSound(getNotificationSoundUri(context, sound));

                if (androidSDKVersion > 15) {
                    int priority = getPriorityOfMessage(message);
                    builder.setPriority(priority);
                    notification = builder.build();
                }

                if (androidSDKVersion > 19) {
                    //As new material theme is very light, the icon is not shown clearly
                    //hence setting the background of icon to black
                    builder.setColor(Color.BLACK);
                    Boolean isBridgeSet = message.getBridge();
                    if (!isBridgeSet) {
                        // show notification only on current device.
                        builder.setLocalOnly(true);
                    }

                    notification = builder.build();
                    int receivedVisibility = 1;
                    String visibility = message.getVisibility();
                    if (visibility != null && visibility.equalsIgnoreCase(MFPPushConstants.VISIBILITY_PRIVATE)) {
                        receivedVisibility = 0;
                    }
                    if (receivedVisibility == Notification.VISIBILITY_PRIVATE && message.getRedact() != null) {
                        builder.setContentIntent(PendingIntent
                                .getActivity(context, notificationId, intent,
                                        PendingIntent.FLAG_UPDATE_CURRENT))
                                .setSmallIcon(icon).setTicker(ticker).setWhen(when)
                                .setAutoCancel(true).setContentTitle(title)
                                .setContentText(message.getRedact()).setSound(getNotificationSoundUri(context, sound));

                        notification.publicVersion = builder.build();
                    }
                }

                if (androidSDKVersion > 21) {
                    String setPriority = message.getPriority();
                    if (setPriority != null && setPriority.equalsIgnoreCase(MFPPushConstants.PRIORITY_MAX)) {
                        //heads-up notification
                        builder.setContentText(msg)
                                .setFullScreenIntent(PendingIntent.getActivity(context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT), true);
                        notification = builder.build();
                    }
                }

            } else {
                notification = builder.setContentIntent(PendingIntent
                        .getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT))
                        .setSmallIcon(icon).setTicker(ticker).setWhen(when)
                        .setAutoCancel(true).setContentTitle(title)
                        .setContentText(msg).setSound(getNotificationSoundUri(context, sound))
                        .build();
            }

            NotificationManager notificationManager = (NotificationManager) context
                    .getSystemService(Context.NOTIFICATION_SERVICE);

            notificationManager.notify(notificationId, notification);
        }
    }

    private int getPriorityOfMessage (MFPInternalPushMessage message) {
        String priorityFromServer = message.getPriority();
        MFPPushNotificationOptions options = MFPPush.getInstance().getNotificationOptions();
        int priorityPreSetValue = 0;

        if (options != null && options.getPriority()!=null) {
            priorityPreSetValue = options.getPriority().getValue();
        }

        if (priorityFromServer != null) {
            if (priorityFromServer.equalsIgnoreCase(MFPPushConstants.PRIORITY_MAX)) {
                return Notification.PRIORITY_MAX;
            } else if (priorityFromServer.equalsIgnoreCase(MFPPushConstants.PRIORITY_MIN)) {
                return Notification.PRIORITY_MIN;
            } else if (priorityFromServer.equalsIgnoreCase(MFPPushConstants.PRIORITY_HIGH)) {
                return Notification.PRIORITY_HIGH;
            } else if (priorityFromServer.equalsIgnoreCase(MFPPushConstants.PRIORITY_LOW)) {
                return Notification.PRIORITY_LOW;
            }
        } else if (priorityPreSetValue != 0){
            return priorityPreSetValue;
        }
        return Notification.PRIORITY_DEFAULT;
    }

    private String getNotificationSound(MFPInternalPushMessage message) {
        String soundFromServer = message.getSound();
        String soundPreSet = null;
        MFPPushNotificationOptions options = MFPPush.getInstance().getNotificationOptions();

        if (options != null && options.getSound()!=null){
            soundPreSet = options.getSound();
        }
        if (soundFromServer != null) {
            return soundFromServer;
        } else if (soundPreSet != null) {
            return soundPreSet;
        }
        return null;
    }

    private Uri getNotificationSoundUri(Context context, String sound) {
        Uri uri = null;

        if (sound == null) {
            uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        } else if (!(sound.trim().isEmpty())) {
            String soundResourceString = sound;
            try {
                if (soundResourceString.contains(".")) {
                    soundResourceString = soundResourceString.substring(0, soundResourceString.indexOf("."));
                }
                int resourceId = getResourceId(context, RAW, soundResourceString);
                if (resourceId == -1) {
                    logger.error("MFPPushIntentService:getNotificationSoundUri() - Specified sound file is not found in res/raw");
                }
                uri = Uri.parse("android.resource://" + context.getPackageName() + "/" + resourceId);
            } catch (Exception e) {
                logger.error("MFPPushIntentService:getNotificationSoundUri() - Exception while parsing sound file");
            }
        }

        return uri;
    }

    public int getCustomNotificationIcon(Context context, String resourceName) {
        int resourceId = -1;

        try {
            resourceId = getResourceIdForCustomIcon(context, DRAWABLE, resourceName);
        } catch (Exception e) {
            logger.error("MFPPushIntentService: getCustomNotification() - Exception while parsing icon file.");
            resourceId = android.R.drawable.btn_star;
        }

        if (resourceId == 0) {
            resourceId = android.R.drawable.btn_star;
        }
        return resourceId;
    }

    public static int getResourceId(Context context, String resourceCategory, String resourceName) {
        int resourceId = -1;
        try {
            resourceId = context.getResources().getIdentifier(resourceName, RAW, context.getPackageName());
        } catch (Exception e) {
            logger.error("MFPPushIntentService: getResourceId() - Failed to find resource R." + resourceCategory + "." + resourceName, e);
        }
        return resourceId;
    }

    public static int getResourceIdForCustomIcon(Context context, String resourceCategory, String resourceName) {
        int resourceId = -1;

        try {
            resourceId = context.getResources().getIdentifier(DRAWABLE+"/" + resourceName, DRAWABLE, context.getPackageName());
        } catch (Exception e) {
            logger.error("MFPPushIntentService: Failed to find resource R." + resourceCategory + "." + resourceName, e);
        }
        return resourceId;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        intent = handleMessageIntent(intent, extras);
        MFPPushBroadcastReceiver.completeWakefulIntent(intent);
    }

    private Intent handleMessageIntent(Intent intent, Bundle extras) {
        String action = extras.getString(ACTION);
        if (action != null && action.equals(DISMISS_NOTIFICATION)) {
            logger.debug("MFPPushIntentService:handleMessageIntent() - Dismissal message from GCM Server");
            dismissNotification(extras.getString(NID));
        } else {
           GoogleCloudMessaging gcm = GoogleCloudMessaging
                    .getInstance(getApplicationContext());
            String messageType = gcm.getMessageType(intent);

            if (!extras.isEmpty()) {
                if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                    try {
                        String alert = extras.getString("alert");
                        alert = URLEncoder.encode(alert, "UTF-8");
                        SharedPreferences pref = getSharedPreferences("transLangFile", MODE_PRIVATE);
                        String selectedLangFromPref = pref.getString("transLang", "en");
                        String language = URLEncoder.encode(selectedLangFromPref,"UTF-8");
                        alert = "content=" + alert +"&language="+language;
                        alert = new RetrieveTranslationTask().execute("https://<node.js app route>/translate?" + alert).get();
                        extras.putString("alert", alert);
                        intent.putExtras(extras);
                    }
                    catch(Exception ex)
                    {
                        ex.printStackTrace();
                    }
                    MFPInternalPushMessage message = new MFPInternalPushMessage(intent);
                    intent = new Intent(MFPPushUtils.getIntentPrefix(getApplicationContext())
                            + GCM_MESSAGE);
                    intent.putExtra(GCM_EXTRA_MESSAGE, message);

                    if (!isAppForeground()) {
                        logger.debug("MFPPushIntentService:handleMessageIntent() - App is not on foreground. Queue the intent for later re-sending when app is on foreground");
                        intentsQueue.add(intent);
                    }
                    getApplicationContext().sendOrderedBroadcast(intent, null,
                            resultReceiver, null, Activity.RESULT_FIRST_USER, null,
                            null);
                }
            }
        }
        return intent;
    }

    protected void dismissNotification(String nid) {
        System.out.println("dismiss notification");
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(MFPPush.PREFS_NAME, Context.MODE_PRIVATE);
        int countOfStoredMessages = sharedPreferences.getInt(PREFS_NOTIFICATION_COUNT, 0);

        if (countOfStoredMessages > 0) {
            for (int index = 1; index <= countOfStoredMessages; index++) {

                String key = PREFS_NOTIFICATION_MSG + index;
                try {
                    String msg = sharedPreferences.getString(key, null);
                    if (msg != null) {
                        JSONObject messageObject = new JSONObject(msg);
                        if (messageObject != null && !messageObject.isNull(NID)) {
                            String id = messageObject.getString(NID);
                            if (id != null && id.equals(nid)) {
                                MFPPushUtils.removeContentFromSharedPreferences(sharedPreferences, key);
                                MFPPushUtils.storeContentInSharedPreferences(sharedPreferences, PREFS_NOTIFICATION_COUNT, countOfStoredMessages - 1);
                                NotificationManager mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
                                mNotificationManager.cancel(messageObject.getInt(NOTIFICATIONID));
                            }
                        }
                    }
                } catch (JSONException e) {
                    logger.error("MFPPushIntentService: dismissNotification() - Failed to dismiss notification.");
                }
            }
        }
    }

    class getBitMapBigPictureNotification extends AsyncTask<String, Void, Bitmap> {

        protected Bitmap doInBackground(String... urls) {
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                Bitmap myBitmap = BitmapFactory.decodeStream(input);
                return myBitmap;
            } catch (Exception e) {
                return null;
            }
        }

    }
}
