package com.aware.plugin.mwt;


import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.ESM;
import com.aware.ui.esms.ESMFactory;
import com.aware.ui.esms.ESM_Checkbox;
import com.aware.ui.esms.ESM_Freetext;
import com.aware.ui.esms.ESM_Likert;
import com.aware.ui.esms.ESM_PAM;
import com.aware.ui.esms.ESM_Radio;
import com.aware.utils.Aware_Plugin;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

public class Plugin extends Aware_Plugin {

    public static final String ACTION_AWARE_PLUGIN_DEVICE_MWT = "ACTION_AWARE_PLUGIN_DEVICE_MWT";
    public static final String ACTION_AWARE_MWT_TRIGGER = "ACTION_AWARE_MWT_TRIGGER";

    private static final String ACTION_AWARE_MWT_TRIGGER_CAUSE = "ACTION_AWARE_MWT_TRIGGER_CAUSE";

    public static final int ACTIVITY_CODE_IN_VEHICLE = 0;
    public static final int ACTIVITY_CODE_ON_BICYCLE = 1;
    public static final int ACTIVITY_CODE_ON_FOOT = 2;
    public static final int ACTIVITY_CODE_STILL = 3;
    public static final int ACTIVITY_CODE_UNKNOWN = 4;
    public static final int ACTIVITY_CODE_TILTING = 5;
    public static final int ACTIVITY_CODE_WALKING = 7;
    public static final int ACTIVITY_CODE_RUNNING = 8;

    private static final String ACTION_AWARE_APPLICATIONS_CRASHES = "ACTION_AWARE_APPLICATIONS_CRASHES";
    private static final String ACTION_AWARE_APPLICATIONS_FOREGROUND = "ACTION_AWARE_APPLICATIONS_FOREGROUND";
    private static final String ACTION_AWARE_APPLICATIONS_HISTORY = "ACTION_AWARE_APPLICATIONS_HISTORY";
    private static final String ACTION_AWARE_APPLICATIONS_NOTIFICATIONS = "ACTION_AWARE_APPLICATIONS_NOTIFICATIONS";
    private static final String ACTION_AWARE_CALL_ACCEPTED = "ACTION_AWARE_CALL_ACCEPTED";
    private static final String ACTION_AWARE_CALL_MADE = "ACTION_AWARE_CALL_MADE";
    private static final String ACTION_AWARE_CALL_MISSED = "ACTION_AWARE_CALL_MISSED";
    private static final String ACTION_AWARE_CALL_RINGING = "ACTION_AWARE_CALL_RINGING";
    private static final String ACTION_AWARE_GOOGLE_ACTIVITY_RECOGNITION = "ACTION_AWARE_GOOGLE_ACTIVITY_RECOGNITION";
    private static final String ACTION_AWARE_LOCATIONS = "ACTION_AWARE_LOCATIONS";
    private static final String ACTION_AWARE_USER_IN_CALL = "ACTION_AWARE_USER_IN_CALL";
    private static final String ACTION_AWARE_USER_NOT_IN_CALL = "ACTION_AWARE_USER_NOT_IN_CALL";

    private static final String APPLICATION_NAME = "application_name";
    private static final String EXTRA_ACTIVITY = "activity";
    private static final String EXTRA_CONFIDENCE = "confidence";
    private static final String EXTRA_DATA = "data";
    private static final String PACKAGE_NAME = "package_name";

    private static final String MWT_TRIGGER_SOCIAL_MEDIA = "TRIGGER_SOCIAL_MEDIA";
    private static final String MWT_TRIGGER_ACTIVITY_CHANGE = "TRIGGER_ACTIVITY_CHANGE";
    private static final String MWT_TRIGGER_AFTER_CALL = "TRIGGER_AFTER_CALL";
    private static final String MWT_TRIGGER_MANUAL = "TRIGGER_MANUAL";
    private static final String MWT_TRIGGER_SERVER = "TRIGGER_SERVER";

    private static final long MINIMUM_ESM_GAP_IN_MILLIS = 15 * 60 * 1000L;
    private static final int ESM_EXPIRATION_THRESHOLD_SECONDS = 120;
    private static final int ESM_NOTIFICATION_TIMEOUT_SECONDS = 180;

    public static String activityName = "";
    private static String triggerCause = "";
    private static String packageName = "";
    private static long lastEsmMillis;

    private MwtListener eventListener;

    private void registerEventListener() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_AWARE_CALL_ACCEPTED);
        intentFilter.addAction(ACTION_AWARE_CALL_MADE);
        intentFilter.addAction(ACTION_AWARE_APPLICATIONS_FOREGROUND);
        intentFilter.addAction(ACTION_AWARE_GOOGLE_ACTIVITY_RECOGNITION);
        // trigger for MWT
        intentFilter.addAction(ACTION_AWARE_MWT_TRIGGER);

        eventListener = new MwtListener(this);
        registerReceiver(eventListener, intentFilter);
    }

    private void scheduleMWTTrigger(final long millis) {
        new Handler().postDelayed(new Runnable() {
            public void run() {
                long now = System.currentTimeMillis();
                if ((millis <= 0 || now - lastEsmMillis > MINIMUM_ESM_GAP_IN_MILLIS) && isCorrectDurationNow()) {
                    Log.i(TAG, "[MWT ESM] Start: " + now);
                    lastEsmMillis = now;
                    CONTEXT_PRODUCER.onContext();
                    startESM(triggerCause);
                }
            }
        }, millis);
    }

    private boolean isCorrectDurationNow() {
        Calendar calendar = Calendar.getInstance();
        int i = calendar.get(Calendar.HOUR_OF_DAY);
        return i >= getEsmStartHour() && i < getEsmStopHour();
    }

    private void startESM(String trigger) {
        try {
            ESM.queueESM(this, getQuestionnaire(trigger));
        } catch (JSONException jSONException) {
            Log.e(this.TAG, "[MWT_ESM] Error", jSONException);
        }
    }

    private void unregisterEventListener() {
        unregisterReceiver(this.eventListener);
        this.eventListener = null;
    }

    public void onCreate() {
        super.onCreate();

        AUTHORITY = Provider.getAuthority(this);
        TAG = "AWARE::MWT";

        CONTEXT_PRODUCER = new Aware_Plugin.ContextProducer() {
            public void onContext() {
                Log.d(TAG, "[MWT] onContext");
                ContentValues contentValues = new ContentValues();
                contentValues.put(Provider.MWT_Data.TIMESTAMP, System.currentTimeMillis());
                contentValues.put(Provider.MWT_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
                contentValues.put(Provider.MWT_Data.ACTIVITY_NAME, Plugin.activityName);
                contentValues.put(Provider.MWT_Data.TRIGGER_CAUSE, Plugin.triggerCause);
                contentValues.put(Provider.MWT_Data.PACKAGE_NAME, Plugin.packageName);
                if (DEBUG) {
                    Log.d(TAG, contentValues.toString());
                }

                getContentResolver().insert(Provider.MWT_Data.CONTENT_URI, contentValues);
                Intent intent = new Intent(ACTION_AWARE_PLUGIN_DEVICE_MWT);
                intent.putExtra(Provider.MWT_Data.ACTIVITY_NAME, Plugin.activityName);
                intent.putExtra(Provider.MWT_Data.TRIGGER_CAUSE, Plugin.triggerCause);
                intent.putExtra(Provider.MWT_Data.PACKAGE_NAME, Plugin.packageName);
                sendBroadcast(intent);
            }
        };
        registerEventListener();
        startServerTriggers();
        Log.i(TAG, "[MWT] OnCreate");
    }


    /**
     * Allow callback to other applications when data is stored in provider
     */
    private static AWARESensorObserver awareSensor;

    public static void setSensorObserver(AWARESensorObserver observer) {
        awareSensor = observer;
    }

    public static AWARESensorObserver getSensorObserver() {
        return awareSensor;
    }

    public interface AWARESensorObserver {
        void onDataChanged(ContentValues data);
    }

    public int onStartCommand(Intent paramIntent, int paramInt1, int paramInt2) {
        super.onStartCommand(paramIntent, paramInt1, paramInt2);

        if (PERMISSIONS_OK) {
            DEBUG = Aware.getSetting(this, Aware_Preferences.DEBUG_FLAG).equals("true");

            //Initialize our plugin's settings
            Aware.setSetting(this, Settings.STATUS_PLUGIN_MWT, true);

            //Enable our plugin's sync-adapter to upload the data to the server if part of a study
            if (Aware.getSetting(this, Aware_Preferences.FREQUENCY_WEBSERVICE).length() >= 0 && !Aware.isSyncEnabled(this, Provider.getAuthority(this)) && Aware.isStudy(this) && getApplicationContext().getPackageName().equalsIgnoreCase("com.aware.phone") || getApplicationContext().getResources().getBoolean(R.bool.standalone)) {
                ContentResolver.setIsSyncable(Aware.getAWAREAccount(this), Provider.getAuthority(this), 1);
                ContentResolver.setSyncAutomatically(Aware.getAWAREAccount(this), Provider.getAuthority(this), true);
                ContentResolver.addPeriodicSync(
                        Aware.getAWAREAccount(this),
                        Provider.getAuthority(this),
                        Bundle.EMPTY,
                        Long.parseLong(Aware.getSetting(this, Aware_Preferences.FREQUENCY_WEBSERVICE)) * 60
                );
            }
        }
        Log.i(this.TAG, "[MWT] onStartCommand");

        return START_STICKY;
    }

    public void onDestroy() {
        super.onDestroy();

        stopServerTrigger();
        unregisterEventListener();

        ContentResolver.setSyncAutomatically(Aware.getAWAREAccount(this), Provider.getAuthority(this), false);
        ContentResolver.removePeriodicSync(
                Aware.getAWAREAccount(this),
                Provider.getAuthority(this),
                Bundle.EMPTY
        );

        Aware.setSetting(this, Settings.STATUS_PLUGIN_MWT, false);
    }

    private static final long SERVER_PING_DELAY_MILLIS = 10 * 1000L;
    private static final long SERVER_TRIGGER_GAP_MILLIS = 60 * 1000L;

    private Timer timer = null;
    private static long lastServerTriggerMillis = 0;

    public void startServerTriggers() {
        stopServerTrigger();

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                long now = System.currentTimeMillis();
                boolean shouldPingServer = shouldPingServer()
                        && now - lastServerTriggerMillis > SERVER_TRIGGER_GAP_MILLIS
                        && isCorrectDurationNow();
                if (shouldPingServer && isServerTriggerAvailable()) {
                    lastServerTriggerMillis = now;

                    Log.d(Aware.TAG, "[ESM TRIGGER] Server");
                    Intent esmTriggerIntent = new Intent(ACTION_AWARE_MWT_TRIGGER);
                    esmTriggerIntent.putExtra(ACTION_AWARE_MWT_TRIGGER_CAUSE, MWT_TRIGGER_SERVER);
                    sendBroadcast(esmTriggerIntent);
                }
            }
        }, 0, SERVER_PING_DELAY_MILLIS);
    }

    private static final String SERVER_URL = "https://nuwanjanaka.info/test1/devices";

    private boolean isServerTriggerAvailable() {
        try {
            URL url = new URL(SERVER_URL + "/" + Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
            HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
            httpCon.setRequestMethod("GET");
            httpCon.setRequestProperty("Content-Type", "application/json");

            int responseCode = httpCon.getResponseCode();
            StringBuilder response = new StringBuilder();
            if (responseCode == HttpURLConnection.HTTP_OK) { // success
                try (
                        BufferedReader in = new BufferedReader(new InputStreamReader(httpCon.getInputStream()));
                ) {
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error in reading response", e);
                }
            }
            String resBody = response.toString();
            Log.d(TAG, "Server response: " + responseCode + ", " + resBody);
            return resBody.length() > 0 && !resBody.equalsIgnoreCase("[]");

        } catch (Exception e) {
            Log.e(TAG, "Error:isServerTriggerAvailable", e);
        }
        return false;
    }

    private void stopServerTrigger() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private static class MwtListener extends BroadcastReceiver {
        private static final String TAG_AWARE_MWT = "AWARE::MWT";
        private final Plugin plugin;

        private long lastAppChangeMillis = 0L;
        private long lastActivityChangeMillis = 0L;

        private MwtListener(Plugin plugin) {
            this.plugin = plugin;
        }

        public void onReceive(Context param1Context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG_AWARE_MWT, "[ACTION]: " + action);

            boolean expectedActivity = false;

            if (ACTION_AWARE_GOOGLE_ACTIVITY_RECOGNITION.equals(action)) {
                int activity = intent.getIntExtra("activity", ACTIVITY_CODE_UNKNOWN);
                int confidence = intent.getIntExtra("confidence", ACTIVITY_CODE_UNKNOWN);
                String newActivityName = Plugin.getActivityName(activity);
                Log.d(TAG_AWARE_MWT, "[MWT] Activity: " + newActivityName + ", " + confidence);
                if (!newActivityName.equals(activityName) && confidence > 60) {
                    expectedActivity = activity == ACTIVITY_CODE_IN_VEHICLE || activity == ACTIVITY_CODE_STILL || activity == ACTIVITY_CODE_WALKING;
                    activityName = newActivityName;
                    lastActivityChangeMillis = System.currentTimeMillis();
                }
            }
            if (expectedActivity && System.currentTimeMillis() - lastActivityChangeMillis > 20000L) {
                Log.i(TAG_AWARE_MWT, "[MWT TRIGGER] Activity:" + activityName);
                triggerCause = MWT_TRIGGER_ACTIVITY_CHANGE;
                plugin.scheduleMWTTrigger(5000L);
            }

            boolean expectedApp = false;

            if (ACTION_AWARE_APPLICATIONS_FOREGROUND.equals(action)) {
                ContentValues contentValues = intent.getParcelableExtra(EXTRA_DATA);
                Log.d(TAG_AWARE_MWT, "[MWT] App: " + contentValues.toString());

                String currentPackageName = contentValues.getAsString(PACKAGE_NAME);
                expectedApp = isExpectedSocialMediaPackage(currentPackageName);

                if (!packageName.equals(currentPackageName)) {
                    packageName = currentPackageName;
                    lastAppChangeMillis = System.currentTimeMillis();
                }
            }
            if (expectedApp && System.currentTimeMillis() - lastAppChangeMillis > 60000L) {
                Log.i(TAG_AWARE_MWT, "[MWT TRIGGER] App");
                triggerCause = MWT_TRIGGER_SOCIAL_MEDIA;
                plugin.scheduleMWTTrigger(10000L);
            }

            if (ACTION_AWARE_CALL_ACCEPTED.equals(action) || ACTION_AWARE_CALL_MADE.equals(action)) {
                Log.i(TAG_AWARE_MWT, "[MWT TRIGGER] Call");
                triggerCause = MWT_TRIGGER_AFTER_CALL;
                plugin.scheduleMWTTrigger(5000L);
            }

            if (ACTION_AWARE_MWT_TRIGGER.equals(action)) {
                String trigger = intent.getStringExtra(ACTION_AWARE_MWT_TRIGGER_CAUSE);
                if (trigger == null) {
                    trigger = MWT_TRIGGER_MANUAL;
                }
                Log.i(TAG_AWARE_MWT, "[MWT TRIGGER] " + trigger);
                triggerCause = trigger;
                plugin.scheduleMWTTrigger(0);
            }
        }

    }

    private static boolean isExpectedSocialMediaPackage(String currentPackageName) {
        switch (currentPackageName) {
            case "com.google.android.talk":
            case "com.facebook.katana":
            case "com.android.chrome":
            case "com.google.android.gm":
            case "com.tencent.mm":
            case "com.whatsapp":
            case "com.google.android.youtube":
                return true;
            default:
                return false;
        }
    }

    public static String getActivityName(int paramInt) {
        switch (paramInt) {
            case ACTIVITY_CODE_IN_VEHICLE:
                return "on_vehicle";
            case ACTIVITY_CODE_ON_BICYCLE:
                return "on_bicycle";
            case ACTIVITY_CODE_ON_FOOT:
                return "on_foot";
            case ACTIVITY_CODE_STILL:
                return "still";
            case ACTIVITY_CODE_TILTING:
                return "tilting";
            case ACTIVITY_CODE_WALKING:
                return "walking";
            case ACTIVITY_CODE_RUNNING:
                return "running";
            case ACTIVITY_CODE_UNKNOWN:
            default:
                return "unknown";
        }
    }

    private static String getQuestionnaire(String trigger) throws JSONException {
        ESMFactory eSMFactory = new ESMFactory();

        ESM_Likert languageReceptivityLikert = new ESM_Likert();
        languageReceptivityLikert
                .setLikertMax(7)
                .setLikertMaxLabel("Very High")
                .setLikertMinLabel("Very Low")
                .setLikertStep(1.0D)
                .setTitle("Receptivity to learn")
                .setInstructions("How interested you are in learning a new language now?")
                .setSubmitButton("Next")
                .setTrigger(trigger)
                .setExpirationThreshold(ESM_EXPIRATION_THRESHOLD_SECONDS)
                .setNotificationTimeout(ESM_NOTIFICATION_TIMEOUT_SECONDS);
        ESM_Freetext otherLearningActivitiesFreeText = new ESM_Freetext();
        otherLearningActivitiesFreeText
                .setTitle("Learning activity preferences")
                .setInstructions("Any other desired things you like to learn now? If not why?")
                .setSubmitButton("Next");
        ESM_Radio roadFamiliarityRadio = new ESM_Radio();
        roadFamiliarityRadio
                .addRadio("Unfamiliar")
                .addRadio("Moderately familiar")
                .addRadio("Very familiar")
                .setTitle("Familiarity")
                .setInstructions("How familiar is the road/path?")
                .setSubmitButton("Next");
        ESM_Radio waitingReasonRadio = new ESM_Radio();
        waitingReasonRadio
                .addRadio("For bus/MRT/taxi")
                .addRadio("For the lift")
                .addRadio("For a friend/friends")
                .addRadio("For food to be ready")
                .addRadio("For a turn")
                .addRadio("Other")
                .setTitle("Reason for waiting")
                .setInstructions("Why/What are you waiting for?")
                .setSubmitButton("Next");
        ESM_Radio primaryActivityRadio = new ESM_Radio();
        primaryActivityRadio
                .addRadio("Walking")
                .addRadio("Waiting")
                .addRadio("Consuming")
                .addRadio("Commuting")
                .addRadio("Exercising")
                .addRadio("Working/Studying")
                .addRadio("Other")
                .addFlow("Walking", roadFamiliarityRadio.build())
                .addFlow("Commuting", roadFamiliarityRadio.build())
                .addFlow("Waiting", waitingReasonRadio.build())
                .setTitle("Primary Activity")
                .setInstructions("What is the MAIN activity you are doing now?")
                .setSubmitButton("Next");
        ESM_Checkbox secondaryActivityCheckbox = new ESM_Checkbox();
        secondaryActivityCheckbox
                .addCheck("Conversing")
                .addCheck("Exploring")
                .addCheck("Observing")
                .addCheck("Pondering")
                .addCheck("Resting")
                .addCheck("Mobile Checking")
                .addCheck("Other")
                .setTitle("Secondary Activity")
                .setInstructions("What other activities you are doing now?")
                .setSubmitButton("Next");
        ESM_Likert mentalDemandLikert = new ESM_Likert();
        mentalDemandLikert
                .setLikertMax(7)
                .setLikertMaxLabel("Very High")
                .setLikertMinLabel("Very Low")
                .setLikertStep(1.0D)
                .setTitle("Mental Demand")
                .setInstructions("How mentally demanding is the task you are doing now?")
                .setSubmitButton("Next");
        ESM_Likert physicalDemandLikert = new ESM_Likert();
        physicalDemandLikert
                .setLikertMax(7)
                .setLikertMaxLabel("Very High")
                .setLikertMinLabel("Very Low")
                .setLikertStep(1.0D)
                .setTitle("Physical Demand")
                .setInstructions("How physically demanding is the task you are doing now?")
                .setSubmitButton("Next");
        ESM_Radio socialContextRadio = new ESM_Radio();
        socialContextRadio
                .addRadio("Alone")
                .addRadio("With 1 person")
                .addRadio("With 2 people")
                .addRadio("With 3 people")
                .addRadio("More than 3 people")
                .setTitle("Social Context")
                .setInstructions("How many acquaintances are there with you now?")
                .setSubmitButton("Next");
        ESM_Radio crowdRadio = new ESM_Radio();
        crowdRadio
                .addRadio("Less crowded")
                .addRadio("Medium crowded")
                .addRadio("Highly crowded")
                .setTitle("Surrounding crowd")
                .setInstructions("How crowded are your surroundings?")
                .setSubmitButton("Next");
        ESM_Radio physicalConditionRadio = new ESM_Radio();
        physicalConditionRadio
                .addRadio("Active")
                .addRadio("Tired")
                .addRadio("Normal")
                .addRadio("Other")
                .setTitle("Physical Condition")
                .setInstructions("How do you feel physically?")
                .setSubmitButton("Next");
        ESM_PAM moodGrid = new ESM_PAM();
        moodGrid
                .setTitle("Mood")
                .setInstructions("What is your mood right now? Choose the most appropriate image.")
                .setSubmitButton("OK");

        eSMFactory.addESM(languageReceptivityLikert);
        eSMFactory.addESM(otherLearningActivitiesFreeText);
        eSMFactory.addESM(primaryActivityRadio);
        eSMFactory.addESM(secondaryActivityCheckbox);
        eSMFactory.addESM(mentalDemandLikert);
        eSMFactory.addESM(physicalDemandLikert);
        eSMFactory.addESM(socialContextRadio);
        eSMFactory.addESM(crowdRadio);
        eSMFactory.addESM(physicalConditionRadio);
        eSMFactory.addESM(moodGrid);

        return eSMFactory.build();
    }

    private boolean shouldPingServer() {
        return Aware.getSetting(getApplicationContext(), Settings.STATUS_PLUGIN_PING_SERVER).equals("true");
    }

    private int getEsmStartHour() {
        return Integer.valueOf(Aware.getSetting(getApplicationContext(), Settings.STATUS_ESM_START_HOUR));
    }

    private int getEsmStopHour() {
        return Integer.valueOf(Aware.getSetting(getApplicationContext(), Settings.STATUS_ESM_END_HOUR));
    }
}
