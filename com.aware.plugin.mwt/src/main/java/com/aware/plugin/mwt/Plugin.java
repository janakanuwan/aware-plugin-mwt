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
import android.widget.Toast;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.Barometer;
import com.aware.ESM;
import com.aware.ui.esms.ESMFactory;
import com.aware.ui.esms.ESM_Checkbox;
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
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Plugin extends Aware_Plugin {

    public static final String ACTION_AWARE_PLUGIN_DEVICE_MWT = "ACTION_AWARE_PLUGIN_DEVICE_MWT";
    public static final String ACTION_AWARE_MWT_TRIGGER = "ACTION_AWARE_MWT_TRIGGER";

    private static final String ACTION_AWARE_MWT_TRIGGER_CAUSE = "ACTION_AWARE_MWT_TRIGGER_CAUSE";
    private static final String ACTION_AWARE_MWT_TRIGGER_INIT_DELAY_MILLIS = "ACTION_AWARE_MWT_TRIGGER_INIT_DELAY_MILLIS";

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

    private static final String ACTION_AWARE_ESM_ANSWERED = "ACTION_AWARE_ESM_ANSWERED";
    private static final String ACTION_AWARE_ESM_DISMISSED = "ACTION_AWARE_ESM_DISMISSED";

    private static final String ACTION_AWARE_ACTIVITY_ESCALATOR = "ACTION_AWARE_ACTIVITY_ESCALATOR";
    private static final String AMBIENT_PRESSURE = "double_values_0";

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
    private static final String MWT_TRIGGER_RANDOM = "TRIGGER_RANDOM";

    private static final String MWT_TRIGGER_WALKING = "TRIGGER_WALKING";
    private static final String MWT_TRIGGER_VEHICLE = "TRIGGER_VEHICLE";

    private static final long MINIMUM_ESM_GAP_IN_MILLIS = 15 * 60 * 1000L;
    private static final int ESM_NOTIFICATION_TIMEOUT_SECONDS = 300;

    private static final long MILLIS_IMMEDIATELY = 0;
    private static final long MILLIS_ASAP = 1;
    private static final long MILLIS_1_SECOND = 1000L;
    private static final long MILLIS_30_SECONDS = 30 * MILLIS_1_SECOND;
    private static final long MILLIS_90_SECONDS = 90 * MILLIS_1_SECOND;
    private static final long MILLIS_1_MINUTE = 60 * MILLIS_1_SECOND;
    private static final long MILLIS_2_MINUTES = 2 * MILLIS_1_MINUTE;
    private static final long MILLIS_3_MINUTES = 3 * MILLIS_1_MINUTE;
    private static final long MILLIS_5_MINUTES = 5 * MILLIS_1_MINUTE;
    private static final long MILLIS_10_MINUTES = 10 * MILLIS_1_MINUTE;
    private static final long MILLIS_20_MINUTES = 20 * MILLIS_1_MINUTE;

    public static String activityName = "";
    private static String triggerCause = "";
    private static String packageName = "";
    private static long lastEsmMillis;
    private static long lastEsmAnsweredOrDismissedMillis;

    private MwtListener eventListener;
    private BarometerListener barometerListener;

    private void registerEventListener() {
        IntentFilter intentFilter = new IntentFilter();
//        intentFilter.addAction(ACTION_AWARE_CALL_ACCEPTED);
//        intentFilter.addAction(ACTION_AWARE_CALL_MADE);
        intentFilter.addAction(ACTION_AWARE_APPLICATIONS_FOREGROUND);
        intentFilter.addAction(ACTION_AWARE_GOOGLE_ACTIVITY_RECOGNITION);
        // trigger for MWT
        intentFilter.addAction(ACTION_AWARE_MWT_TRIGGER);
        // listen to ESM answers or dismiss
        intentFilter.addAction(ACTION_AWARE_ESM_ANSWERED);
        intentFilter.addAction(ACTION_AWARE_ESM_DISMISSED);
        intentFilter.addAction(ACTION_AWARE_ACTIVITY_ESCALATOR);

        eventListener = new MwtListener(this);
        registerReceiver(eventListener, intentFilter);

        barometerListener = new BarometerListener(this);
        Barometer.setSensorObserver(barometerListener);
    }

    private void scheduleMWTTrigger(final long millis, final String triggerCause) {
        new Handler().postDelayed(new Runnable() {
            public void run() {
                long now = System.currentTimeMillis();
                if ((millis <= 0 || now - lastEsmMillis > MINIMUM_ESM_GAP_IN_MILLIS) && isCorrectDurationNow()) {
                    Log.i(TAG, "[MWT ESM] Start: " + now + ", cause: " + triggerCause);
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
            ESM.queueESM(this, getQuestionnaire(trigger, getEsmExpirationThresholdSeconds()));
        } catch (JSONException jSONException) {
            Log.e(this.TAG, "[MWT_ESM] Error", jSONException);
        }
    }

    private void unregisterEventListener() {
        unregisterReceiver(this.eventListener);
        this.eventListener = null;

        Barometer.setSensorObserver(null);
        this.barometerListener = null;
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
            if (Aware.getSetting(this, Settings.STATUS_PLUGIN_PING_SERVER).length() == 0) {
                Aware.setSetting(this, Settings.STATUS_PLUGIN_PING_SERVER, false);
            }
            if (Aware.getSetting(this, Settings.STATUS_ESM_EXPIRATION_THRESHOLD_SECONDS).length() == 0) {
                Aware.setSetting(this, Settings.STATUS_ESM_EXPIRATION_THRESHOLD_SECONDS, Settings.DEFAULT_ESM_EXPIRATION_THRESHOLD_SECONDS);
            }
            if (Aware.getSetting(this, Settings.STATUS_ESM_START_HOUR).length() == 0) {
                Aware.setSetting(this, Settings.STATUS_ESM_START_HOUR, Settings.DEFAULT_ESM_START_HOUR);
            }
            if (Aware.getSetting(this, Settings.STATUS_ESM_END_HOUR).length() == 0) {
                Aware.setSetting(this, Settings.STATUS_ESM_END_HOUR, Settings.DEFAULT_ESM_END_HOUR);
            }
            if (Aware.getSetting(this, Settings.STATUS_MWT_DETECTION).length() == 0) {
                Aware.setSetting(this, Settings.STATUS_MWT_DETECTION, false);
            }
            if (Aware.getSetting(this, Settings.STATUS_RANDOM_ESM).length() == 0) {
                Aware.setSetting(this, Settings.STATUS_RANDOM_ESM, false);
            }
            if (Aware.getSetting(this, Settings.STATUS_RANDOM_ESM_GAP_MINUTES).length() == 0) {
                Aware.setSetting(this, Settings.STATUS_RANDOM_ESM_GAP_MINUTES, Settings.DEFAULT_RANDOM_ESM_GAP_MINUTES);
            }

            if (shouldPingServer()) {
                startServerTriggers();
            } else {
                stopServerTrigger();
            }
            if (shouldEnableRandomEsm()) {
                startRandomEsmScheduler();
            } else {
                stopRandomEsmScheduler();
            }

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
        stopRandomEsmScheduler();
        unregisterEventListener();

        ContentResolver.setSyncAutomatically(Aware.getAWAREAccount(this), Provider.getAuthority(this), false);
        ContentResolver.removePeriodicSync(
                Aware.getAWAREAccount(this),
                Provider.getAuthority(this),
                Bundle.EMPTY
        );

        Aware.setSetting(this, Settings.STATUS_PLUGIN_MWT, false);
    }

    private static final long SERVER_PING_DELAY_MILLIS = 5 * 1000L;
    private static final long SERVER_TRIGGER_GAP_MILLIS = 60 * 1000L;

    private Timer timer = null;
    private static long lastServerTriggerMillis = 0;

    public void startServerTriggers() {
        stopServerTrigger();

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                long now = System.currentTimeMillis();
                if (now - lastServerTriggerMillis > SERVER_TRIGGER_GAP_MILLIS
                        && isCorrectDurationNow()
                        && isServerTriggerAvailable()) {
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

    private ScheduledExecutorService esmScheduler = null;

    private void startRandomEsmScheduler() {
        stopRandomEsmScheduler();

        final int randomEsmGapInMinutes = getRandomEsmGapInMinutes();

        esmScheduler = Executors.newSingleThreadScheduledExecutor();
        esmScheduler.scheduleAtFixedRate(new Runnable() {
            public void run() {
                if (isCorrectDurationNow()) {
                    Log.d(Aware.TAG, "[ESM TRIGGER] Random ESM");
                    Intent esmTriggerIntent = new Intent(ACTION_AWARE_MWT_TRIGGER);
                    esmTriggerIntent.putExtra(ACTION_AWARE_MWT_TRIGGER_CAUSE, MWT_TRIGGER_RANDOM);
                    esmTriggerIntent.putExtra(ACTION_AWARE_MWT_TRIGGER_INIT_DELAY_MILLIS, (new Random().nextInt(randomEsmGapInMinutes) * 60 * 1000L));
                    sendBroadcast(esmTriggerIntent);
                }
            }
        }, 0, randomEsmGapInMinutes, TimeUnit.MINUTES);
    }

    private void stopRandomEsmScheduler() {
        if (esmScheduler != null) {
            esmScheduler.shutdownNow();
            esmScheduler = null;
        }
    }


    private static class MwtListener extends BroadcastReceiver {
        private static final String TAG_AWARE_MWT = "AWARE::MWT";
        private final Plugin plugin;

        private int lastActivity = ACTIVITY_CODE_STILL;
        private int currentActivity = ACTIVITY_CODE_STILL;

        private boolean activityChanged = false;

        private long lastAppChangeMillis = 0L;
        private long lastActivityChangeMillis = 0L;

        private MwtListener(Plugin plugin) {
            this.plugin = plugin;
        }

        public void onReceive(Context param1Context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG_AWARE_MWT, "[ACTION]: " + action);

            if (plugin.shouldDetectMwt()) {
                detectMwt(intent);
            }

            if (ACTION_AWARE_MWT_TRIGGER.equalsIgnoreCase(action)) {
                String trigger = intent.getStringExtra(ACTION_AWARE_MWT_TRIGGER_CAUSE);
                if (trigger == null) {
                    trigger = MWT_TRIGGER_MANUAL;
                }
                Log.i(TAG_AWARE_MWT, "[MWT TRIGGER] " + trigger);

                long initDelayMillis = intent.getLongExtra(ACTION_AWARE_MWT_TRIGGER_INIT_DELAY_MILLIS, 0);
                triggerCause = trigger;
                plugin.scheduleMWTTrigger(initDelayMillis, triggerCause);
            }
        }

        private void detectMwt(Intent intent) {
            String action = intent.getAction();

            long currentTimeMillis = System.currentTimeMillis();

            if (ACTION_AWARE_ESM_ANSWERED.equalsIgnoreCase(action) || ACTION_AWARE_ESM_DISMISSED.equalsIgnoreCase(action)) {
                lastEsmAnsweredOrDismissedMillis = currentTimeMillis;
            }

            if (ACTION_AWARE_GOOGLE_ACTIVITY_RECOGNITION.equalsIgnoreCase(action)) {
                int activity = intent.getIntExtra("activity", ACTIVITY_CODE_UNKNOWN);
                int confidence = intent.getIntExtra("confidence", 0);
                String newActivityName = Plugin.getActivityName(activity);
                Log.d(TAG_AWARE_MWT, "[MWT] Activity: " + newActivityName + ", " + confidence);

                Toast.makeText(plugin.getApplicationContext(), newActivityName + ", " + confidence, Toast.LENGTH_SHORT).show();

                if (!newActivityName.equalsIgnoreCase(activityName) && confidence > 60) {
                    lastActivity = getActivityCode(activityName);
                    currentActivity = activity;
                    lastActivityChangeMillis = currentTimeMillis;

                    activityName = newActivityName;
                    activityChanged = true;
                }
            }

            if (isStillOrWalking(lastActivity) && isInVehicle(currentActivity)) {
                if (activityChanged) {
                    if (currentTimeMillis - lastActivityChangeMillis > MILLIS_30_SECONDS + getRandomMillis(MILLIS_90_SECONDS)) {
                        Log.i(TAG_AWARE_MWT, "[MWT TRIGGER] First Time:" + activityName);
                        triggerCause = MWT_TRIGGER_VEHICLE;
                        plugin.scheduleMWTTrigger(MILLIS_ASAP, triggerCause);

                        activityChanged = false;
                    }
                } else {
                    if (lastEsmMillis - lastEsmAnsweredOrDismissedMillis > MILLIS_5_MINUTES + getRandomMillis(MILLIS_1_MINUTE)) {
                        Log.i(TAG_AWARE_MWT, "[MWT TRIGGER] Did not answer:" + activityName);
                        triggerCause = MWT_TRIGGER_VEHICLE;
                        plugin.scheduleMWTTrigger(MILLIS_IMMEDIATELY, triggerCause);
                    } else {
                        if (currentTimeMillis - lastEsmMillis > MILLIS_20_MINUTES + getRandomMillis(MILLIS_10_MINUTES)) {
                            Log.i(TAG_AWARE_MWT, "[MWT TRIGGER] Same activity more than 20 min:" + activityName);
                            triggerCause = MWT_TRIGGER_VEHICLE;
                            plugin.scheduleMWTTrigger(MILLIS_ASAP, triggerCause);
                        }
                    }
                }
            }

            if (isStill(lastActivity) && isWalking(currentActivity)) {
                if (activityChanged) {
                    if (currentTimeMillis - lastActivityChangeMillis > MILLIS_3_MINUTES + getRandomMillis(MILLIS_2_MINUTES)) {
                        Log.i(TAG_AWARE_MWT, "[MWT TRIGGER] First Time:" + activityName);
                        triggerCause = MWT_TRIGGER_WALKING;
                        plugin.scheduleMWTTrigger(MILLIS_ASAP, triggerCause);

                        activityChanged = false;
                    }
                } else {
                    if (lastEsmMillis - lastEsmAnsweredOrDismissedMillis > MILLIS_5_MINUTES + getRandomMillis(MILLIS_1_MINUTE)) {
                        Log.i(TAG_AWARE_MWT, "[MWT TRIGGER] Did not answer:" + activityName);
                        triggerCause = MWT_TRIGGER_WALKING;
                        plugin.scheduleMWTTrigger(MILLIS_IMMEDIATELY, triggerCause);
                    } else {
                        if (currentTimeMillis - lastEsmMillis > MILLIS_20_MINUTES + getRandomMillis(MILLIS_10_MINUTES)) {
                            Log.i(TAG_AWARE_MWT, "[MWT TRIGGER] Same activity more than 20 min:" + activityName);
                            triggerCause = MWT_TRIGGER_WALKING;
                            plugin.scheduleMWTTrigger(MILLIS_ASAP, triggerCause);
                        }
                    }
                }
            }

            if (ACTION_AWARE_APPLICATIONS_FOREGROUND.equalsIgnoreCase(action)) {
                ContentValues contentValues = intent.getParcelableExtra(EXTRA_DATA);
                Log.d(TAG_AWARE_MWT, "[MWT] App: " + contentValues.toString());

                String currentPackageName = contentValues.getAsString(PACKAGE_NAME);

                if (!packageName.equals(currentPackageName)) {
                    packageName = currentPackageName;
                    lastAppChangeMillis = System.currentTimeMillis();
                }
            }


//            boolean expectedActivity = false;
//
//            if (ACTION_AWARE_GOOGLE_ACTIVITY_RECOGNITION.equalsIgnoreCase(action)) {
//                int activity = intent.getIntExtra("activity", ACTIVITY_CODE_UNKNOWN);
//                int confidence = intent.getIntExtra("confidence", 0);
//                String newActivityName = Plugin.getActivityName(activity);
//                Log.d(TAG_AWARE_MWT, "[MWT] Activity: " + newActivityName + ", " + confidence);
//                if (!newActivityName.equals(activityName) && confidence > 60) {
//                    expectedActivity = activity == ACTIVITY_CODE_IN_VEHICLE || activity == ACTIVITY_CODE_STILL || activity == ACTIVITY_CODE_WALKING;
//                    activityName = newActivityName;
//                    lastActivityChangeMillis = System.currentTimeMillis();
//                }
//            }
//            if (expectedActivity && System.currentTimeMillis() - lastActivityChangeMillis > 20000L) {
//                Log.i(TAG_AWARE_MWT, "[MWT TRIGGER] Activity:" + activityName);
//                triggerCause = MWT_TRIGGER_ACTIVITY_CHANGE;
//                plugin.scheduleMWTTrigger(5000L);
//            }
//
//            boolean expectedApp = false;
//
//            if (ACTION_AWARE_APPLICATIONS_FOREGROUND.equalsIgnoreCase(action)) {
//                ContentValues contentValues = intent.getParcelableExtra(EXTRA_DATA);
//                Log.d(TAG_AWARE_MWT, "[MWT] App: " + contentValues.toString());
//
//                String currentPackageName = contentValues.getAsString(PACKAGE_NAME);
//                expectedApp = isExpectedSocialMediaPackage(currentPackageName);
//
//                if (!packageName.equals(currentPackageName)) {
//                    packageName = currentPackageName;
//                    lastAppChangeMillis = System.currentTimeMillis();
//                }
//            }
//            if (expectedApp && System.currentTimeMillis() - lastAppChangeMillis > 60000L) {
//                Log.i(TAG_AWARE_MWT, "[MWT TRIGGER] App");
//                triggerCause = MWT_TRIGGER_SOCIAL_MEDIA;
//                plugin.scheduleMWTTrigger(10000L);
//            }
//
//            if (ACTION_AWARE_CALL_ACCEPTED.equalsIgnoreCase(action) || ACTION_AWARE_CALL_MADE.equalsIgnoreCase(action)) {
//                Log.i(TAG_AWARE_MWT, "[MWT TRIGGER] Call");
//                triggerCause = MWT_TRIGGER_AFTER_CALL;
//                plugin.scheduleMWTTrigger(5000L);
//            }

        }

    }

    private static boolean isStillOrWalking(int activityCode) {
        return isStill(activityCode) || isWalking(activityCode);
    }

    private static boolean isInVehicle(int activityCode) {
        return activityCode == ACTIVITY_CODE_IN_VEHICLE;
    }

    private static boolean isStill(int activityCode) {
        return activityCode == ACTIVITY_CODE_STILL
                || activityCode == ACTIVITY_CODE_TILTING;
    }

    private static boolean isWalking(int activityCode) {
        return activityCode == ACTIVITY_CODE_WALKING
                || activityCode == ACTIVITY_CODE_ON_FOOT;
    }

    private static long getRandomMillis(long duration) {
        if (duration <= 0) {
            return 0;
        }

        if (duration > Integer.MAX_VALUE) {
            return 1 + (new Random().nextLong() % duration);
        } else {
            return 1 + new Random().nextInt((int) duration);
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

    private static int getActivityCode(String name) {
        switch (name) {
            case "still":
                return ACTIVITY_CODE_STILL;
            case "walking":
                return ACTIVITY_CODE_WALKING;
            case "on_foot":
                return ACTIVITY_CODE_ON_FOOT;
            case "on_vehicle":
                return ACTIVITY_CODE_IN_VEHICLE;
            case "tilting":
                return ACTIVITY_CODE_TILTING;
            case "running":
                return ACTIVITY_CODE_RUNNING;
            case "on_bicycle":
                return ACTIVITY_CODE_ON_BICYCLE;
            default:
                return ACTIVITY_CODE_UNKNOWN;
        }
    }

    private static class BarometerListener implements Barometer.AWARESensorObserver {
        private static final String TAG_AWARE_MWT = "AWARE::MWT";

        private static final int MAX_PRESSURE_BUFFER_SIZE = 256;

        private static final float DELTA = 1.0f;
        private static final int THRESHOLD = 1;
        private static final int MAX_PRESSURE_DIFF = 500;

        private final float[] pressureValues = new float[MAX_PRESSURE_BUFFER_SIZE];
        private int index = 0;
        private final Plugin plugin;

        private int diffTot = 0;

        private BarometerListener(Plugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public synchronized void onBarometerChanged(ContentValues data) {
            if (index >= MAX_PRESSURE_BUFFER_SIZE) {
                index = 0;
            }
            pressureValues[index] = data.getAsFloat(AMBIENT_PRESSURE);

            if (isEscalator(index)) {
                plugin.sendBroadcast(new Intent(ACTION_AWARE_ACTIVITY_ESCALATOR));
            }
            index++;
        }

        private boolean isEscalator(int index) {
            int current = index;

            int tot = 0;

            int iteration = 0;
            while (iteration < MAX_PRESSURE_BUFFER_SIZE) {
                float diff;
                if (current > 1) {
                    diff = pressureValues[current] - pressureValues[current - 1];
                } else if (current == 0) {
                    diff = pressureValues[0] - pressureValues[MAX_PRESSURE_BUFFER_SIZE - 1];
                } else {
                    current = MAX_PRESSURE_BUFFER_SIZE - 1;
                    diff = pressureValues[current] - pressureValues[current - 1];
                }
                current--;


                if (diff > DELTA && diff < MAX_PRESSURE_DIFF) {
                    tot++;
                } else if (diff < -DELTA && diff > -MAX_PRESSURE_DIFF) {
                    tot--;
                }
                iteration++;

                if (diff > DELTA || diff < -DELTA) {
                    Log.d(TAG_AWARE_MWT, "Delta: " + diff);
                }
            }

            diffTot += tot;
            diffTot /= 2;
            Log.d(TAG_AWARE_MWT, "Diff Tot:" + tot);

            return diffTot >= THRESHOLD || diffTot <= -THRESHOLD;
        }
    }

    private static String getQuestionnaire(String trigger, int emsExpirationThresholdInSeconds) throws JSONException {
        ESMFactory eSMFactory = new ESMFactory();

        ESM_Likert languageReceptivityLikert = new ESM_Likert();
        languageReceptivityLikert
                .setLikertMax(7)
                .setLikertMaxLabel("Very High")
                .setLikertMinLabel("Very Low")
                .setLikertStep(1.0D)
                .setTitle("Receptivity to Learn")
                .setInstructions("How interested you are in learning a new language (vocabulary) now?")
                .setSubmitButton("Next")
                .setTrigger(trigger)
                .setExpirationThreshold(emsExpirationThresholdInSeconds)
                .setNotificationTimeout(ESM_NOTIFICATION_TIMEOUT_SECONDS);

        // commuting
        ESM_Radio commutingMediumRadio = new ESM_Radio();
        commutingMediumRadio
                .addRadio("Walking")
                .addRadio("Taking the bus")
                .addRadio("Taking the MRT")
                .addRadio("Riding a bike / driving")
                .addRadio("Taking the lift / elevator")
                .addRadio("Other")
                .setTitle("Commuting Medium")
                .setInstructions("What's the medium of commuting?")
                .setSubmitButton("OK");

        // waiting
        ESM_Radio waitingCategoryRadio = new ESM_Radio();
        waitingCategoryRadio
                .addRadio("Waiting for a working machine/system (e.g. wait for the printer, wait for loading, etc)")
                .addRadio("Waiting for someone (e.g. hold on the phone, wait to go, etc)")
                .addRadio("Waiting in turn (e.g. bus, queue, etc)")
                .addRadio("Other")
                .setTitle("Waiting Category")
                .setInstructions("What's your purpose of waiting?")
                .setSubmitButton("Next");
        ESM_Radio waitingTimeRadio = new ESM_Radio();
        waitingTimeRadio
                .addRadio("Cannot be estimated")
                .addRadio("Less than 1 min")
                .addRadio("Between 1 min - 10 min")
                .addRadio("Between 10 min - 20 min")
                .addRadio("More than 20min")
                .addRadio("Other")
                .setTitle("Waiting Time")
                .setInstructions("How long do you estimate you should wait?")
                .setSubmitButton("OK");

        // relaxing
        ESM_Radio relaxingCategoryRadio = new ESM_Radio();
        relaxingCategoryRadio
                .addRadio("Watching videos")
                .addRadio("Listening to music")
                .addRadio("Taking a break")
                .addRadio("Reading")
                .addRadio("Using a smart phone for leisure")
                .addRadio("Other")
                .setTitle("Relaxing Reason")
                .setInstructions("What's your way of relaxing?")
                .setSubmitButton("OK");

        // conversing
        ESM_Radio conversingMediumRadio = new ESM_Radio();
        conversingMediumRadio
                .addRadio("Face to face")
                .addRadio("By audio/video call")
                .addRadio("By text/multimedia messages")
                .addRadio("Other")
                .setTitle("Conversing Method")
                .setInstructions("What's your way of conversing?")
                .setSubmitButton("OK");

        // primary activities
        ESM_Radio primaryActivityRadio = new ESM_Radio();
        primaryActivityRadio
                .addRadio("Commuting (e.g. walking/bus/lift)")
                .addRadio("Waiting")
                .addRadio("Relaxing")
                .addRadio("Conversing (e.g. talk/call/chat)")
                .addRadio("Consuming")
                .addRadio("Working/Studying")
                .addRadio("Exercising")
                .addRadio("Observing/Exploring")
                .addRadio("Other")
                .addFlow("Commuting (e.g. walking/bus/lift)", commutingMediumRadio.build())
                .addFlow("Waiting", waitingCategoryRadio.build())
                .addFlow("Waiting", waitingTimeRadio.build())
                .addFlow("Relaxing", relaxingCategoryRadio.build())
                .addFlow("Conversing (e.g. talk/call/chat)", conversingMediumRadio.build())
                .setTitle("Primary Activity")
                .setInstructions("What is the MAIN activity you are doing now?")
                .setSubmitButton("Next");

        // common
        ESM_Likert familiarityLikert = new ESM_Likert();
        familiarityLikert
                .setLikertMax(5)
                .setLikertMaxLabel("Very High")
                .setLikertMinLabel("Very Low")
                .setLikertStep(1.0D)
                .setTitle("Familiarity")
                .setInstructions("How familiar are you with the MAIN activity?")
                .setSubmitButton("Next");
        ESM_Radio postureRadio = new ESM_Radio();
        postureRadio
                .addRadio("Moving (e.g. walking/running)")
                .addRadio("Standing")
                .addRadio("Sitting")
                .addRadio("Lying")
                .addRadio("Other")
                .setTitle("Body Posture")
                .setInstructions("What's your posture?")
                .setSubmitButton("Next");
        ESM_Likert physicalDemandLikert = new ESM_Likert();
        physicalDemandLikert
                .setLikertMax(5)
                .setLikertMaxLabel("Very High")
                .setLikertMinLabel("Very Low")
                .setLikertStep(1.0D)
                .setTitle("Physical Demand")
                .setInstructions("How physically demanding is the task you are doing now?")
                .setSubmitButton("Next");
        ESM_Likert mentalDemandLikert = new ESM_Likert();
        mentalDemandLikert
                .setLikertMax(5)
                .setLikertMaxLabel("Very High")
                .setLikertMinLabel("Very Low")
                .setLikertStep(1.0D)
                .setTitle("Mental Demand")
                .setInstructions("How mentally demanding is the task you are doing now?")
                .setSubmitButton("Next");
        ESM_Likert urgencyLikert = new ESM_Likert();
        urgencyLikert
                .setLikertMax(5)
                .setLikertMaxLabel("Very Urgent")
                .setLikertMinLabel("Not Urgent")
                .setLikertStep(1.0D)
                .setTitle("Urgency")
                .setInstructions("How urgent is the task you are doing now?")
                .setSubmitButton("Next");
        ESM_Likert physicalConditionLikert = new ESM_Likert();
        physicalConditionLikert
                .setLikertMax(5)
                .setLikertMaxLabel("Very Active")
                .setLikertMinLabel("Very Tired")
                .setLikertStep(1.0D)
                .setTitle("Physical Condition")
                .setInstructions("What is your physical condition now?")
                .setSubmitButton("Next");
        ESM_PAM moodGrid = new ESM_PAM();
        moodGrid
                .setTitle("Mood")
                .setInstructions("What is your mood right now? Choose the most appropriate image.")
                .setSubmitButton("Next");
        ESM_Radio socialContextRadio = new ESM_Radio();
        socialContextRadio
                .addRadio("Alone")
                .addRadio("Family members")
                .addRadio("Friends")
                .addRadio("Strangers")
                .addRadio("Other")
                .setTitle("Social Context")
                .setInstructions("Who are you with now?")
                .setSubmitButton("Next");
        ESM_Likert crowdLikert = new ESM_Likert();
        crowdLikert
                .setLikertMax(5)
                .setLikertMaxLabel("Very Crowded")
                .setLikertMinLabel("Not Crowded")
                .setLikertStep(1.0D)
                .setTitle("Crowdedness")
                .setInstructions("How crowded is your environment now?")
                .setSubmitButton("Next");
        ESM_Checkbox environmentConditionCheckbox = new ESM_Checkbox();
        environmentConditionCheckbox
                .addCheck("Quiet")
                .addCheck("Noisy")
                .addCheck("Hot")
                .addCheck("Cool")
                .addCheck("Other")
                .setTitle("Environmental Conditions")
                .setInstructions("What are the environmental conditions?")
                .setSubmitButton("Next");
        ESM_Radio secondaryActivityRadio = new ESM_Radio();
        secondaryActivityRadio
                .addRadio("Commuting")
                .addRadio("Waiting")
                .addRadio("Relaxing")
                .addRadio("Conversing")
                .addRadio("Consuming")
                .addRadio("Working/Studying")
                .addRadio("Exercising")
                .addRadio("Observing/Exploring")
                .addRadio("Doing Nothing")
                .addRadio("Other")
                .addFlow("Commuting", commutingMediumRadio.build())
                .addFlow("Waiting", waitingCategoryRadio.build())
                .addFlow("Waiting", waitingTimeRadio.build())
                .addFlow("Relaxing", relaxingCategoryRadio.build())
                .addFlow("Conversing", conversingMediumRadio.build())
                .setTitle("Secondary Activity")
                .setInstructions("What is the secondary activities you are doing now?")
                .setSubmitButton("Next");
        ESM_Radio otherLearningActivityRadio = new ESM_Radio();
        otherLearningActivityRadio
                .addRadio("None")
                .addRadio("Other")
                .setTitle("Learning Activity Preferences")
                .setInstructions("Any other desired things you like to learn now? If not why?")
                .setSubmitButton("OK/Next");

        eSMFactory.addESM(languageReceptivityLikert);
        eSMFactory.addESM(primaryActivityRadio);
        eSMFactory.addESM(familiarityLikert);
        eSMFactory.addESM(postureRadio);
        eSMFactory.addESM(physicalDemandLikert);
        eSMFactory.addESM(mentalDemandLikert);
        eSMFactory.addESM(urgencyLikert);
        eSMFactory.addESM(physicalConditionLikert);
        eSMFactory.addESM(moodGrid);
        eSMFactory.addESM(socialContextRadio);
        eSMFactory.addESM(crowdLikert);
        eSMFactory.addESM(environmentConditionCheckbox);
        eSMFactory.addESM(secondaryActivityRadio);
        eSMFactory.addESM(otherLearningActivityRadio);

        return eSMFactory.build();
    }

    private boolean shouldPingServer() {
        return Aware.getSetting(getApplicationContext(), Settings.STATUS_PLUGIN_PING_SERVER).equals("true");
    }

    private int getEsmExpirationThresholdSeconds() {
        return Integer.valueOf(Aware.getSetting(getApplicationContext(), Settings.STATUS_ESM_EXPIRATION_THRESHOLD_SECONDS));
    }

    private int getEsmStartHour() {
        return Integer.valueOf(Aware.getSetting(getApplicationContext(), Settings.STATUS_ESM_START_HOUR));
    }

    private int getEsmStopHour() {
        return Integer.valueOf(Aware.getSetting(getApplicationContext(), Settings.STATUS_ESM_END_HOUR));
    }

    private boolean shouldDetectMwt() {
        return Aware.getSetting(getApplicationContext(), Settings.STATUS_MWT_DETECTION).equals("true");
    }

    private boolean shouldEnableRandomEsm() {
        return Aware.getSetting(getApplicationContext(), Settings.STATUS_RANDOM_ESM).equals("true");
    }

    private int getRandomEsmGapInMinutes() {
        return Integer.valueOf(Aware.getSetting(getApplicationContext(), Settings.STATUS_RANDOM_ESM_GAP_MINUTES));
    }
}
