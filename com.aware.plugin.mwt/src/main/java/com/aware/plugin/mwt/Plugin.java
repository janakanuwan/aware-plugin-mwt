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
import com.aware.Barometer;
import com.aware.Locations;
import com.aware.ESM;
import com.aware.ui.esms.ESMFactory;
import com.aware.ui.esms.ESM_Checkbox;
import com.aware.ui.esms.ESM_Likert;
import com.aware.ui.esms.ESM_Number;
import com.aware.ui.esms.ESM_Radio;
import com.aware.utils.Aware_Plugin;
import com.aware.utils.Scheduler;

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
import java.util.concurrent.atomic.AtomicInteger;

public class Plugin extends Aware_Plugin {

    public static final String ACTION_AWARE_PLUGIN_DEVICE_MWT = "ACTION_AWARE_PLUGIN_DEVICE_MWT";
    public static final String ACTION_AWARE_MWT_TRIGGER = "ACTION_AWARE_MWT_TRIGGER";

    public static final String ACTION_AWARE_MWT_SCHDULE_CHECK = "ACTION_AWARE_MWT_SCHDULE_CHECK";

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
    private static final String ACTION_AWARE_ESM_EXPIRED = "ACTION_AWARE_ESM_EXPIRED";
    private static final String ACTION_AWARE_ESM_QUEUE_STARTED = "ACTION_AWARE_ESM_QUEUE_STARTED";

    private static final String ACTION_AWARE_ACTIVITY_ESCALATOR = "ACTION_AWARE_ACTIVITY_ESCALATOR";

    private static final String ACTION_AWARE_ACTIVITY_VEHICLE_SPEED = "ACTION_AWARE_ACTIVITY_VEHICLE_SPEED";

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

    private static final String MWT_TRIGGER_WALKING_START = "TRIGGER_WALKING_START";
    private static final String MWT_TRIGGER_WALKING_MIDDLE = "TRIGGER_WALKING_MIDDLE";
    private static final String MWT_TRIGGER_VEHICLE_START = "TRIGGER_VEHICLE_START";
    private static final String MWT_TRIGGER_VEHICLE_MIDDLE = "TRIGGER_VEHICLE_MIDDLE";
    private static final String MWT_TRIGGER_ESCALATOR = "TRIGGER_ESCALATOR";

    private static final long MINIMUM_ESM_GAP_IN_MILLIS = 10 * 60 * 1000L;
    private static final int ESM_NOTIFICATION_TIMEOUT_SECONDS = 300;

    private static final long MILLIS_IMMEDIATELY = 0;
    private static final long MILLIS_ASAP = 1;
    private static final long MILLIS_1_SECOND = 1000L;
    private static final long MILLIS_20_SECONDS = 20 * MILLIS_1_SECOND;
    private static final long MILLIS_30_SECONDS = 30 * MILLIS_1_SECOND;
    private static final long MILLIS_90_SECONDS = 90 * MILLIS_1_SECOND;
    private static final long MILLIS_1_MINUTE = 60 * MILLIS_1_SECOND;
    private static final long MILLIS_2_MINUTES = 2 * MILLIS_1_MINUTE;
    private static final long MILLIS_3_MINUTES = 3 * MILLIS_1_MINUTE;
    private static final long MILLIS_5_MINUTES = 5 * MILLIS_1_MINUTE;
    private static final long MILLIS_10_MINUTES = 10 * MILLIS_1_MINUTE;
    private static final long MILLIS_15_MINUTES = 15 * MILLIS_1_MINUTE;
    private static final long MILLIS_20_MINUTES = 20 * MILLIS_1_MINUTE;

    private static final int MAX_ESM_COUNT_PER_DAY = 10;

    public static final String MWT_DETECT_SCHEDULE_ID = "mwt_detect";
    public static final int MWT_DETECT_CHECK_INTERVAL_MINUTES = 5;

    public static String activityName = "";
    private static String triggerCause = "";
    private static String packageName = "";
    private static volatile long lastEsmMillis;
    private static volatile long lastEsmSeenMillis;
    private static AtomicInteger esmSeenCount = new AtomicInteger(0);

    private MwtListener eventListener;
    private BarometerListener barometerListener;
    private LocationsListener locationsListener;

    private void registerEventListener() {
        IntentFilter intentFilter = new IntentFilter();
//        intentFilter.addAction(ACTION_AWARE_CALL_ACCEPTED);
//        intentFilter.addAction(ACTION_AWARE_CALL_MADE);
        intentFilter.addAction(ACTION_AWARE_APPLICATIONS_FOREGROUND);
        intentFilter.addAction(ACTION_AWARE_GOOGLE_ACTIVITY_RECOGNITION);
        // trigger for MWT
        intentFilter.addAction(ACTION_AWARE_MWT_TRIGGER);
        // listen to ESM answers or dismiss
//        intentFilter.addAction(ACTION_AWARE_ESM_ANSWERED);
//        intentFilter.addAction(ACTION_AWARE_ESM_DISMISSED);
        intentFilter.addAction(ACTION_AWARE_ESM_QUEUE_STARTED);

        intentFilter.addAction(ACTION_AWARE_ACTIVITY_ESCALATOR);

        intentFilter.addAction(ACTION_AWARE_MWT_SCHDULE_CHECK);

        intentFilter.addAction(ACTION_AWARE_ACTIVITY_VEHICLE_SPEED);

        eventListener = new MwtListener(this);
        registerReceiver(eventListener, intentFilter);

        barometerListener = new BarometerListener(this);
        Barometer.setSensorObserver(barometerListener);

        locationsListener = new LocationsListener(this);
        Locations.setSensorObserver(locationsListener);

        addScheduler();
    }

    private void addScheduler() {
        try {
            Scheduler.Schedule schedule = new Scheduler.Schedule(MWT_DETECT_SCHEDULE_ID);
            schedule.setInterval(MWT_DETECT_CHECK_INTERVAL_MINUTES)
                    .setActionType(Scheduler.ACTION_TYPE_BROADCAST)
                    .setActionIntentAction(ACTION_AWARE_MWT_SCHDULE_CHECK);
            Scheduler.saveSchedule(getApplicationContext(), schedule);
        } catch (JSONException e) {
            Log.e(TAG, "Error in creating scheduler", e);
        }
    }

    private void scheduleMWTTrigger(final long millis, final String triggerCause) {
        new Handler().postDelayed(new Runnable() {
            public void run() {
                long now = System.currentTimeMillis();
                Log.d(TAG, "[ESM TRIGGER] " + now + ", cause: " + triggerCause + ", esmCount: " + esmSeenCount.get());

                if ((millis <= 0 || now - lastEsmMillis > MINIMUM_ESM_GAP_IN_MILLIS) && isCorrectDurationNow() && esmSeenCount.get() < MAX_ESM_COUNT_PER_DAY) {
                    Log.i(TAG, "[MWT ESM] Starting");
                    lastEsmMillis = now;
                    lastEsmSeenMillis = now;
                    CONTEXT_PRODUCER.onContext();
                    startESM(triggerCause);
                } else if (!isCorrectDurationNow()) {
                    esmSeenCount.set(0);
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

        Locations.setSensorObserver(null);
        this.locationsListener = null;

        removeScheduler();
    }

    private void removeScheduler() {
        Scheduler.removeSchedule(getApplicationContext(), MWT_DETECT_SCHEDULE_ID);
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

        private static int lastActivity = ACTIVITY_CODE_UNKNOWN;
        private static int currentActivity = ACTIVITY_CODE_UNKNOWN;

        private boolean activityChanged = false;

        private long lastAppChangeMillis = 0L;
        private static long lastActivityChangeMillis = 0L;
        private long lastEscalatorTime = 0L;
        private long lastVehicleMillis = 0L;

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

            if (ACTION_AWARE_ACTIVITY_ESCALATOR.equalsIgnoreCase(action)) {
//                Toast.makeText(plugin.getApplicationContext(), "Escalator Detected", Toast.LENGTH_SHORT).show();
                if (currentTimeMillis - lastEscalatorTime < MILLIS_20_SECONDS) {
                    return;
                }
                lastEscalatorTime = currentTimeMillis;
                Log.i(TAG_AWARE_MWT, "[MWT TRIGGER] Escalator");
                triggerCause = MWT_TRIGGER_ESCALATOR;
                plugin.scheduleMWTTrigger(MILLIS_ASAP, triggerCause);
            }

            if (ACTION_AWARE_ACTIVITY_VEHICLE_SPEED.equalsIgnoreCase(action)) {
//                Toast.makeText(plugin.getApplicationContext(), "Location Speed", Toast.LENGTH_SHORT).show();
                if (currentTimeMillis - lastVehicleMillis < MILLIS_5_MINUTES) {
                    return;
                }
                lastVehicleMillis = currentTimeMillis;
                Log.i(TAG_AWARE_MWT, "[MWT TRIGGER] Locations Speed");
                triggerCause = MWT_TRIGGER_VEHICLE_MIDDLE;
                plugin.scheduleMWTTrigger(MILLIS_ASAP, triggerCause);
            }

            if (ACTION_AWARE_ESM_QUEUE_STARTED.equalsIgnoreCase(action)) {
                lastEsmSeenMillis = currentTimeMillis;
                esmSeenCount.getAndIncrement();
            }

            if (ACTION_AWARE_GOOGLE_ACTIVITY_RECOGNITION.equalsIgnoreCase(action)) {
                int activity = intent.getIntExtra("activity", ACTIVITY_CODE_UNKNOWN);
                int confidence = intent.getIntExtra("confidence", 0);
                String newActivityName = Plugin.getActivityName(activity);
                Log.d(TAG_AWARE_MWT, "[MWT] Activity: " + newActivityName + ", " + confidence);

//                Toast.makeText(plugin.getApplicationContext(), newActivityName + ", " + confidence, Toast.LENGTH_LONG).show();

                if (!newActivityName.equalsIgnoreCase(activityName) && confidence > 55) {
                    lastActivity = getActivityCode(activityName);
                    currentActivity = activity;
                    lastActivityChangeMillis = currentTimeMillis;

                    activityName = newActivityName;
                    activityChanged = true;
                }
            }


            if (isStillOrWalkingOrVehicle(lastActivity) && isInVehicle(currentActivity)) {
                if (activityChanged) {
                    if (currentTimeMillis - lastActivityChangeMillis > MILLIS_1_MINUTE) {
                        Log.i(TAG_AWARE_MWT, "[MWT TRIGGER] First Time:" + activityName);
                        triggerCause = MWT_TRIGGER_VEHICLE_START;
                        plugin.scheduleMWTTrigger(MILLIS_ASAP, triggerCause);
                        activityChanged = false;
                    }
                } else {
                    if (lastEsmMillis - lastEsmSeenMillis > MILLIS_5_MINUTES) {
                        Log.i(TAG_AWARE_MWT, "[MWT TRIGGER] Did not answer:" + activityName);
                        lastEsmSeenMillis = currentTimeMillis;
                        triggerCause = MWT_TRIGGER_VEHICLE_MIDDLE;
                        plugin.scheduleMWTTrigger(MILLIS_IMMEDIATELY, triggerCause);
                    } else if (currentTimeMillis - lastEsmMillis > MILLIS_15_MINUTES + getRandomMillis(MILLIS_10_MINUTES)) {
                        Log.i(TAG_AWARE_MWT, "[MWT TRIGGER] Same activity more than 15 min:" + activityName);
                        triggerCause = MWT_TRIGGER_VEHICLE_MIDDLE;
                        plugin.scheduleMWTTrigger(MILLIS_ASAP, triggerCause);
                    }
                }
            }

            if (isStillOrWalking(lastActivity) && isWalking(currentActivity)) {
                if (activityChanged) {
                    if (currentTimeMillis - lastActivityChangeMillis > MILLIS_3_MINUTES) {
                        Log.i(TAG_AWARE_MWT, "[MWT TRIGGER] First Time:" + activityName);
                        triggerCause = MWT_TRIGGER_WALKING_START;
                        plugin.scheduleMWTTrigger(MILLIS_ASAP, triggerCause);
                        activityChanged = false;
                    }
                } else {
                    if (lastEsmMillis - lastEsmSeenMillis > MILLIS_5_MINUTES) {
                        Log.i(TAG_AWARE_MWT, "[MWT TRIGGER] Did not answer:" + activityName);
                        lastEsmSeenMillis = currentTimeMillis;
                        triggerCause = MWT_TRIGGER_WALKING_MIDDLE;
                        plugin.scheduleMWTTrigger(MILLIS_IMMEDIATELY, triggerCause);
                    } else if (currentTimeMillis - lastEsmMillis > MILLIS_15_MINUTES + getRandomMillis(MILLIS_10_MINUTES)) {
                        Log.i(TAG_AWARE_MWT, "[MWT TRIGGER] Same activity more than 15 min:" + activityName);
                        triggerCause = MWT_TRIGGER_WALKING_MIDDLE;
                        plugin.scheduleMWTTrigger(MILLIS_ASAP, triggerCause);
                    }
                }
            }

            if (ACTION_AWARE_APPLICATIONS_FOREGROUND.equalsIgnoreCase(action)) {
                ContentValues contentValues = intent.getParcelableExtra(EXTRA_DATA);
                Log.d(TAG_AWARE_MWT, "[MWT] App: " + contentValues.toString());

                String currentPackageName = contentValues.getAsString(PACKAGE_NAME);

                if (!packageName.equals(currentPackageName)) {
                    packageName = currentPackageName;
                    lastAppChangeMillis = currentTimeMillis;
                }
            }
        }

    }

    private static boolean isStillOrWalkingOrVehicle(int activityCode) {
        return isStillOrWalking(activityCode) || isInVehicle(activityCode);
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

        private static final String AMBIENT_PRESSURE = "double_values_0";

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

//                if (diff > DELTA || diff < -DELTA) {
//                    Log.d(TAG_AWARE_MWT, "Delta: " + diff);
//                }
            }

            diffTot += tot;
            diffTot /= 2;
//            Log.d(TAG_AWARE_MWT, "Diff Tot:" + tot);

            return diffTot >= THRESHOLD || diffTot <= -THRESHOLD;
        }
    }

    private static class LocationsListener implements Locations.AWARESensorObserver {
        private static final String TAG_AWARE_MWT = "AWARE::MWT";
        private static final String SPEED = "double_speed";

        private static final double SPEED_THRESHOLD = 5;

        private final Plugin plugin;

        private LocationsListener(Plugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public synchronized void onLocationChanged(ContentValues data) {
            double speed = data.getAsDouble(SPEED);

//            Log.d(TAG_AWARE_MWT, "[LOCATIONS] speed:" + speed);
            if (speed > SPEED_THRESHOLD) {
                plugin.sendBroadcast(new Intent(ACTION_AWARE_ACTIVITY_VEHICLE_SPEED));
            }
        }
    }


    private static String getQuestionnaire(String trigger, int emsExpirationThresholdInSeconds) throws JSONException {
        ESMFactory esmFactory = new ESMFactory();

        ESM_Likert languageReceptivityLikert = new ESM_Likert();
        languageReceptivityLikert
                .setLikertMax(7)
                .setLikertMaxLabel("Very High")
                .setLikertMinLabel("Not at all")
                .setLikertStep(1.0D)
                .setTitle("Receptivity to Learn")
                .setInstructions("How interested you are in learning a new language (vocabulary) now?")
                .setSubmitButton("Next")
                .setTrigger(trigger)
                .setExpirationThreshold(emsExpirationThresholdInSeconds)
                .setNotificationTimeout(ESM_NOTIFICATION_TIMEOUT_SECONDS);


        ESM_Likert languageReceptivityPhoneLikert = new ESM_Likert();
        languageReceptivityPhoneLikert
                .setLikertMax(7)
                .setLikertMaxLabel("Very High")
                .setLikertMinLabel("Not at all")
                .setLikertStep(1.0D)
                .setTitle("Receptivity to Learn: Phone")
                .setInstructions("How interested you are in learning a new language (vocabulary) now using PHONE?")
                .setSubmitButton("Next");

        ESM_Likert languageReceptivityGlassLikert = new ESM_Likert();
        languageReceptivityGlassLikert
                .setLikertMax(7)
                .setLikertMaxLabel("Very High")
                .setLikertMinLabel("Not at all")
                .setLikertStep(1.0D)
                .setTitle("Receptivity to Learn: Smart-Glass")
                .setInstructions("How interested you are in learning a new language (vocabulary) now using SMART-GLASS?")
                .setSubmitButton("Next");

        ESM_Radio reviewMethodRadio = new ESM_Radio();
        reviewMethodRadio
                .addRadio("Typing")
                .addRadio("Voice input")
                .addRadio("Multiple-choice question (MCQ)")
                .addRadio("Digital Flash cards")
                .addRadio("Not applicable")
                .addRadio("Other")
                .setTitle("Review Method")
                .setInstructions("What is your preferred method of reviewing words now?")
                .setSubmitButton("Next");

        esmFactory.addESM(languageReceptivityLikert);
        esmFactory.addESM(languageReceptivityPhoneLikert);
        esmFactory.addESM(languageReceptivityGlassLikert);
        esmFactory.addESM(reviewMethodRadio);

        ESM_Radio commutingMediumRadio = new ESM_Radio();
        commutingMediumRadio
                .addRadio("By Bus")
                .addRadio("By Taxi")
                .addRadio("By MRT/Train")
                .addRadio("Other")
                .setTitle("Commuting Medium")
                .setInstructions("What is your commuting medium now (within last 3 minutes)?")
                .setSubmitButton("Next");

        switch (trigger) {
            case MWT_TRIGGER_VEHICLE_MIDDLE:
            case MWT_TRIGGER_VEHICLE_START:
                ESM_Radio commutingRadio = new ESM_Radio();
                commutingRadio
                        .addRadio("By Bus/MRT/Taxi/Metro")
                        .addRadio("Not commuting")
                        .addRadio("Other")
                        .addFlow("By Bus/MRT/Taxi/Metro", commutingMediumRadio.build())
                        .setTitle("Commuting")
                        .setInstructions("Are you commuting now (within last 3 minutes)?")
                        .setSubmitButton("Next/Ok");

                addTransportationQuestions(commutingRadio, "By Bus/MRT/Taxi/Metro");
                addCommonQuestions(commutingRadio, "By Bus/MRT/Taxi/Metro");

                esmFactory.addESM(commutingRadio);

                if (MWT_TRIGGER_VEHICLE_START.equalsIgnoreCase(trigger)) {
                    ESM_Radio waitingRadio = new ESM_Radio();
                    waitingRadio
                            .addRadio("Yes")
                            .addRadio("No")
                            .setTitle("Waiting")
                            .setInstructions("Were you waiting for transportation about 5 minutes ago?")
                            .setSubmitButton("Next/Ok");
                    addWaitingQuestions(waitingRadio, "Yes");

                    esmFactory.addESM(waitingRadio);
                }

                break;
            case MWT_TRIGGER_WALKING_START:
            case MWT_TRIGGER_WALKING_MIDDLE:
                ESM_Radio walkingRadio = new ESM_Radio();
                walkingRadio
                        .addRadio("Yes")
                        .addRadio("No")
                        .addRadio("Other")
                        .setTitle("Walking")
                        .setInstructions("Are you walking now (within last 3 minutes) ?")
                        .setSubmitButton("Next/Ok");
                addWalkingQuestions(walkingRadio, "Yes");
                addCommonQuestions(walkingRadio, "Yes");

                esmFactory.addESM(walkingRadio);

                break;
            case MWT_TRIGGER_ESCALATOR:
                ESM_Radio escalatorRadio = new ESM_Radio();
                escalatorRadio
                        .addRadio("Yes")
                        .addRadio("No")
                        .addRadio("Other")
                        .setTitle("Escalator/Lift")
                        .setInstructions("Are you taking the lift / escalator now (within last 3 minutes)?")
                        .setSubmitButton("Next/Ok");

                addEscalatorQuestions(escalatorRadio, "Yes");
                addCommonQuestions(escalatorRadio, "Yes");

                esmFactory.addESM(escalatorRadio);

                break;
            case MWT_TRIGGER_MANUAL:
                ESM_Radio mainActivityRadio = new ESM_Radio();
                mainActivityRadio
                        .addRadio("By Bus/MRT/Taxi/Metro")
                        .addRadio("By Walking")
                        .addRadio("Taking Escalator/Lift")
                        .addRadio("Waiting for Commuting")
                        .addRadio("Not commuting")
                        .addRadio("Other")
                        .addFlow("By Bus/MRT/Taxi/Metro", commutingMediumRadio.build())
                        .setTitle("Commuting")
                        .setInstructions("Are you commuting or waiting for commuting now (within last 3 minutes)?")
                        .setSubmitButton("Next/Ok");

                addTransportationQuestions(mainActivityRadio, "By Bus/MRT/Taxi/Metro");
                addCommonQuestions(mainActivityRadio, "By Bus/MRT/Taxi/Metro");

                addWalkingQuestions(mainActivityRadio, "By Walking");
                addCommonQuestions(mainActivityRadio, "By Walking");

                addEscalatorQuestions(mainActivityRadio, "Taking Escalator/Lift");
                addCommonQuestions(mainActivityRadio, "Taking Escalator/Lift");

                addWaitingQuestions(mainActivityRadio, "Waiting for Commuting");

                esmFactory.addESM(mainActivityRadio);

                break;
            default:
                ESM_Radio otherActivityRadio = new ESM_Radio();
                otherActivityRadio
                        .addRadio("Nothing")
                        .addRadio("Other")
                        .setTitle("Other Activities")
                        .setInstructions("What are the activities you are doing now?")
                        .setSubmitButton("Next/Ok");

                addCommonQuestions(otherActivityRadio, "Other");
                break;
        }

        return esmFactory.build();
    }


    private static void addTransportationQuestions(ESM_Radio esmRadio, String selectedOption) throws JSONException {
        ESM_Number expectedTimeNumeric = new ESM_Number();
        expectedTimeNumeric.setTitle("Expected Time: Transportation")
                .setInstructions("How long (minutes) do you think it will take for you to get off?")
                .setSubmitButton("Next");

        esmRadio.addFlow(selectedOption, expectedTimeNumeric.build());
    }

    private static void addWaitingQuestions(ESM_Radio esmRadio, String selectedOption) throws JSONException {
        ESM_Number expectedTimeNumeric = new ESM_Number();
        expectedTimeNumeric.setTitle("Expected Time: Waiting")
                .setInstructions("How long (minutes) did you wait for transportation?")
                .setSubmitButton("Next");

        ESM_Likert languageReceptivityLikert = new ESM_Likert();
        languageReceptivityLikert
                .setLikertMax(7)
                .setLikertMaxLabel("Very High")
                .setLikertMinLabel("Not at all")
                .setLikertStep(1.0D)
                .setTitle("Receptivity to Learn: Waiting")
                .setInstructions("How interested were in learning a new language (vocabulary) during waiting?")
                .setSubmitButton("Next");


        ESM_Likert languageReceptivityPhoneLikert = new ESM_Likert();
        languageReceptivityPhoneLikert
                .setLikertMax(7)
                .setLikertMaxLabel("Very High")
                .setLikertMinLabel("Not at all")
                .setLikertStep(1.0D)
                .setTitle("Receptivity to Learn: Waiting : Phone")
                .setInstructions("How interested were in learning a new language (vocabulary) during waiting using PHONE?")
                .setSubmitButton("Next");

        ESM_Likert languageReceptivityGlassLikert = new ESM_Likert();
        languageReceptivityGlassLikert
                .setLikertMax(7)
                .setLikertMaxLabel("Very High")
                .setLikertMinLabel("Not at all")
                .setLikertStep(1.0D)
                .setTitle("Receptivity to Learn: Waiting : Smart-Glass")
                .setInstructions("How interested were in learning a new language (vocabulary) during waiting using SMART-GLASS?")
                .setSubmitButton("Next");

        ESM_Radio reviewMethodRadio = new ESM_Radio();
        reviewMethodRadio
                .addRadio("Typing")
                .addRadio("Voice input")
                .addRadio("Multiple-choice question (MCQ)")
                .addRadio("Digital Flash cards")
                .addRadio("Not applicable")
                .addRadio("Other")
                .setTitle("Review Method: Waiting")
                .setInstructions("What is your preferred method of reviewing words now?")
                .setSubmitButton("Next");

        esmRadio.addFlow(selectedOption, expectedTimeNumeric.build());
        esmRadio.addFlow(selectedOption, languageReceptivityLikert.build());
        esmRadio.addFlow(selectedOption, languageReceptivityPhoneLikert.build());
        esmRadio.addFlow(selectedOption, languageReceptivityGlassLikert.build());
        esmRadio.addFlow(selectedOption, reviewMethodRadio.build());

        addBaseQuestions(esmRadio, selectedOption, " : Waiting", "during waiting");
    }


    private static void addWalkingQuestions(ESM_Radio esmRadio, String selectedOption) throws JSONException {
        ESM_Likert urgencyLikert = new ESM_Likert();
        urgencyLikert
                .setLikertMax(5)
                .setLikertMaxLabel("Very Urgent")
                .setLikertMinLabel("Not Urgent")
                .setLikertStep(1.0D)
                .setTitle("Urgency")
                .setInstructions("How urgent it is for you to arrive your destination?")
                .setSubmitButton("Next");
        ESM_Number expectedTimeNumeric = new ESM_Number();
        expectedTimeNumeric.setTitle("Expected Time: Walking")
                .setInstructions("How long (minutes) do you think it will take for you to arrive at your destination?")
                .setSubmitButton("Next");
        ESM_Likert familiarityLikert = new ESM_Likert();
        familiarityLikert
                .setLikertMax(5)
                .setLikertMaxLabel("Very High")
                .setLikertMinLabel("Very Low")
                .setLikertStep(1.0D)
                .setTitle("Familiarity")
                .setInstructions("How familiar are you with the road?")
                .setSubmitButton("Next");

        esmRadio.addFlow(selectedOption, urgencyLikert.build());
        esmRadio.addFlow(selectedOption, expectedTimeNumeric.build());
        esmRadio.addFlow(selectedOption, familiarityLikert.build());
    }

    private static void addEscalatorQuestions(ESM_Radio esmRadio, String selectedOption) throws JSONException {
        ESM_Likert urgencyLikert = new ESM_Likert();
        urgencyLikert
                .setLikertMax(5)
                .setLikertMaxLabel("Very Urgent")
                .setLikertMinLabel("Not Urgent")
                .setLikertStep(1.0D)
                .setTitle("Urgency")
                .setInstructions("How urgent it is for you to arrive your destination?")
                .setSubmitButton("Next");
        ESM_Number expectedTimeNumeric = new ESM_Number();
        expectedTimeNumeric.setTitle("Expected Time: Escalator")
                .setInstructions("HHow long (minutes) do you think it will take for you to get off")
                .setSubmitButton("Next");

        esmRadio.addFlow(selectedOption, urgencyLikert.build());
        esmRadio.addFlow(selectedOption, expectedTimeNumeric.build());

    }

    private static void addCommonQuestions(ESM_Radio esmRadio, String selectedOption) throws JSONException {
        addBaseQuestions(esmRadio, selectedOption, "", "now");
    }

    private static void addBaseQuestions(ESM_Radio esmRadio, String selectedOption, String scenario, String timeInfo) throws JSONException {
        ESM_Radio postureRadio = new ESM_Radio();
        postureRadio
                .addRadio("Moving (e.g. walking/running)")
                .addRadio("Standing")
                .addRadio("Sitting")
                .addRadio("Lying")
                .addRadio("Other")
                .setTitle("Body Posture" + scenario)
                .setInstructions("What's your posture " + timeInfo + "?")
                .setSubmitButton("Next");
        ESM_Likert visualAttentionConditionLikert = new ESM_Likert();
        visualAttentionConditionLikert
                .setLikertMax(5)
                .setLikertMaxLabel("Very High")
                .setLikertMinLabel("Very Low")
                .setLikertStep(1.0D)
                .setTitle("Visual Attention" + scenario)
                .setInstructions("How much visual attention should you pay " + timeInfo + "?")
                .setSubmitButton("Next");
        ESM_Likert physicalConditionLikert = new ESM_Likert();
        physicalConditionLikert
                .setLikertMax(5)
                .setLikertMaxLabel("Very Active")
                .setLikertMinLabel("Very Tired")
                .setLikertStep(1.0D)
                .setTitle("Physical Condition" + scenario)
                .setInstructions("What is your physical condition " + timeInfo + "?")
                .setSubmitButton("Next");
        ESM_Likert frustationLikert = new ESM_Likert();
        frustationLikert
                .setLikertMax(5)
                .setLikertMaxLabel("Not Frustrated at all")
                .setLikertMinLabel("Very Frustrated")
                .setLikertStep(1.0D)
                .setTitle("Frustration" + scenario)
                .setInstructions("How are you feeling " + timeInfo + "?")
                .setSubmitButton("Next");
        ESM_Likert sadHappyLikert = new ESM_Likert();
        sadHappyLikert
                .setLikertMax(7)
                .setLikertMaxLabel("Very Happy")
                .setLikertMinLabel("Very Sad")
                .setLikertStep(1.0D)
                .setTitle("Happiness" + scenario)
                .setInstructions("How are you feeling " + timeInfo + "?")
                .setSubmitButton("Next");
        ESM_Likert boredExcitedLikert = new ESM_Likert();
        boredExcitedLikert
                .setLikertMax(7)
                .setLikertMaxLabel("Very Excited")
                .setLikertMinLabel("Very Bored")
                .setLikertStep(1.0D)
                .setTitle("Excitement" + scenario)
                .setInstructions("How are you feeling " + timeInfo + "?")
                .setSubmitButton("Next");
        ESM_Likert tensedRelaxedLikert = new ESM_Likert();
        tensedRelaxedLikert
                .setLikertMax(7)
                .setLikertMaxLabel("Very Relaxed")
                .setLikertMinLabel("Very Tense")
                .setLikertStep(1.0D)
                .setTitle("Relaxation" + scenario)
                .setInstructions("How are you feeling " + timeInfo + "?")
                .setSubmitButton("Next");
        ESM_Radio socialContextRadio = new ESM_Radio();
        socialContextRadio
                .addRadio("Alone")
                .addRadio("Family members")
                .addRadio("Friends")
                .addRadio("Strangers")
                .addRadio("Other")
                .setTitle("Social Context" + scenario)
                .setInstructions("Who are you with " + timeInfo + "?")
                .setSubmitButton("Next");
        ESM_Likert crowdLikert = new ESM_Likert();
        crowdLikert
                .setLikertMax(5)
                .setLikertMaxLabel("Very Crowded")
                .setLikertMinLabel("Not Crowded")
                .setLikertStep(1.0D)
                .setTitle("Crowdedness" + scenario)
                .setInstructions("How crowded is your surrounding " + timeInfo + "?")
                .setSubmitButton("Next");
        ESM_Likert noiseLikert = new ESM_Likert();
        noiseLikert
                .setLikertMax(5)
                .setLikertMaxLabel("Very Noisy")
                .setLikertMinLabel("Very Quiet")
                .setLikertStep(1.0D)
                .setTitle("Noisiness" + scenario)
                .setInstructions("How noisy is your surrounding " + timeInfo + "?")
                .setSubmitButton("Next");
        ESM_Likert temperatureLikert = new ESM_Likert();
        temperatureLikert
                .setLikertMax(5)
                .setLikertMaxLabel("Too Cold")
                .setLikertMinLabel("Too Hot")
                .setLikertStep(1.0D)
                .setTitle("Temperature" + scenario)
                .setInstructions("How cold/hot is your surrounding " + timeInfo + "?")
                .setSubmitButton("Next");

        // filling
        ESM_Checkbox fillingActivitiesCheckbox = new ESM_Checkbox();
        fillingActivitiesCheckbox
                .addCheck("Listening to music/radio")
                .addCheck("Watching videos")
                .addCheck("Chatting / Messaging")
                .addCheck("Conversing (face to face/call)")
                .addCheck("Browsing / Searching online")
                .addCheck("Reading books/articles/news")
                .addCheck("Observing / Exploring / Checking surrounding")
                .addCheck("Consuming food/drinks")
                .addCheck("Pondering / Contemplating")
                .addCheck("Working / Studying")
                .addCheck("Relaxing / Sleeping")
                .addCheck("Doing Nothing")
                .addCheck("Other")
                .setTitle("Filling activities" + scenario)
                .setInstructions("What are your filling activities " + timeInfo + "?")
                .setSubmitButton("Next");
        ESM_Likert physicalDemandLikert = new ESM_Likert();
        physicalDemandLikert
                .setLikertMax(5)
                .setLikertMaxLabel("Very High")
                .setLikertMinLabel("Very Low")
                .setLikertStep(1.0D)
                .setTitle("Physical Demand" + scenario)
                .setInstructions("How physically demanding are those filling activities?")
                .setSubmitButton("Next");
        ESM_Likert mentalDemandLikert = new ESM_Likert();
        mentalDemandLikert
                .setLikertMax(5)
                .setLikertMaxLabel("Very High")
                .setLikertMinLabel("Very Low")
                .setLikertStep(1.0D)
                .setTitle("Mental Demand" + scenario)
                .setInstructions("How mentally demanding are those filling activities?")
                .setSubmitButton("Next/Ok");
        ESM_Radio fillingActivityRadio = new ESM_Radio();
        fillingActivityRadio
                .addRadio("Yes")
                .addRadio("No")
                .addRadio("Other")
                .setTitle("Filling activity" + scenario)
                .setInstructions("Are you doing any filling activities (e.g. listen to music, mobile checking, reading, conversing, consuming, ...) " + timeInfo + "?")
                .setSubmitButton("Next")
                .addFlow("Yes", fillingActivitiesCheckbox.build())
                .addFlow("Yes", physicalDemandLikert.build())
                .addFlow("Yes", mentalDemandLikert.build());


        esmRadio.addFlow(selectedOption, postureRadio.build());
        esmRadio.addFlow(selectedOption, visualAttentionConditionLikert.build());
        esmRadio.addFlow(selectedOption, physicalConditionLikert.build());
        esmRadio.addFlow(selectedOption, frustationLikert.build());
        esmRadio.addFlow(selectedOption, sadHappyLikert.build());
        esmRadio.addFlow(selectedOption, boredExcitedLikert.build());
        esmRadio.addFlow(selectedOption, tensedRelaxedLikert.build());
        esmRadio.addFlow(selectedOption, socialContextRadio.build());
        esmRadio.addFlow(selectedOption, crowdLikert.build());
        esmRadio.addFlow(selectedOption, noiseLikert.build());
        esmRadio.addFlow(selectedOption, temperatureLikert.build());
        esmRadio.addFlow(selectedOption, fillingActivityRadio.build());
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
