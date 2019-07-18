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
import com.aware.ui.esms.ESM_Freetext;
import com.aware.ui.esms.ESM_Likert;
import com.aware.ui.esms.ESM_PAM;
import com.aware.ui.esms.ESM_Radio;
import com.aware.utils.Aware_Plugin;

import org.json.JSONException;

import java.util.Calendar;

public class Plugin extends Aware_Plugin {

    public static final String ACTION_AWARE_PLUGIN_DEVICE_MWT = "ACTION_AWARE_PLUGIN_DEVICE_MWT";
    public static final String ACTION_AWARE_MWT_DETECT = "ACTION_AWARE_MWT_DETECT";
    public static final String ACTION_AWARE_MWT_TRIGGER = "ACTION_AWARE_MWT_TRIGGER";

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

    private static final long THIRTY_MINUTES_IN_MILLIS = 1800000L;

    public static String activityName = "";
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
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(now);
                Log.d(TAG, "Now " + calendar);
                int i = calendar.get(Calendar.HOUR_OF_DAY);
                if (millis <= 0 || now - lastEsmMillis > THIRTY_MINUTES_IN_MILLIS && i >= 9 && i <= 18) {
                    Log.i(TAG, "[MWT ESM] Start");
                    Plugin.lastEsmMillis = now;
                    CONTEXT_PRODUCER.onContext();
                    sendBroadcast(new Intent(ACTION_AWARE_MWT_DETECT));
                    startESM();
                }
            }
        }, millis);
    }

    private void startESM() {
        try {
            ESM.queueESM(this, getQuestionnaire());
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
                contentValues.put(Provider.MWT_Data.NAME, Plugin.activityName);
                contentValues.put(Provider.MWT_Data.BIG_NUMBER, 0);
                if (DEBUG) {
                    Log.d(TAG, contentValues.toString());
                }

                getContentResolver().insert(Provider.MWT_Data.CONTENT_URI, contentValues);
                Intent intent = new Intent(ACTION_AWARE_PLUGIN_DEVICE_MWT);
                intent.putExtra(Provider.MWT_Data.NAME, Plugin.activityName);
                intent.putExtra(Provider.MWT_Data.BIG_NUMBER, 0);
                sendBroadcast(intent);
            }
        };
        registerEventListener();
        Log.i(TAG, "[MWT] OnCreate");
    }


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

        Log.i(this.TAG, "[MWT] onStartCommand");
        if (PERMISSIONS_OK) {
            DEBUG = Aware.getSetting(this, Aware_Preferences.DEBUG_FLAG).equals("true");
            Aware.setSetting(this, Settings.STATUS_PLUGIN_MWT, true);

            if ((Aware.getSetting(this, Aware_Preferences.FREQUENCY_WEBSERVICE).length() >= 0 && !Aware.isSyncEnabled(this, Provider.getAuthority(this)) && Aware.isStudy(this) && getApplicationContext().getPackageName().equalsIgnoreCase("com.aware.phone")) || getApplicationContext().getResources().getBoolean(R.bool.standalone)) {
                ContentResolver.setIsSyncable(Aware.getAWAREAccount(this), Provider.getAuthority(this), 1);
                ContentResolver.setSyncAutomatically(Aware.getAWAREAccount(this), Provider.getAuthority(this), true);
                ContentResolver.addPeriodicSync(
                        Aware.getAWAREAccount(this),
                        Provider.getAuthority(this),
                        Bundle.EMPTY,
                        Long.parseLong(Aware.getSetting(this, Aware_Preferences.FREQUENCY_WEBSERVICE)) * 60L
                );
            }
        }
        return START_STICKY;
    }

    public void onDestroy() {
        super.onDestroy();
        unregisterEventListener();
        ContentResolver.setSyncAutomatically(Aware.getAWAREAccount(this), Provider.getAuthority(this), false);
        ContentResolver.removePeriodicSync(Aware.getAWAREAccount(this), Provider.getAuthority(this), Bundle.EMPTY);
        Aware.setSetting(this, Settings.STATUS_PLUGIN_MWT, false);
    }


    private static class MwtListener extends BroadcastReceiver {
        private static final String TAG_AWARE_MWT = "AWARE::MWT";
        private final Plugin plugin;

        private long lastAppChangeMillis = 0L;
        private String packageName = "";
        private long lastActivityChangeMillis = 0L;

        private MwtListener(Plugin plugin) {
            this.plugin = plugin;
        }

        public void onReceive(Context param1Context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG_AWARE_MWT, action);

            boolean expectedActivity = false;

            if (ACTION_AWARE_GOOGLE_ACTIVITY_RECOGNITION.equals(action)) {
                int activity = intent.getIntExtra("activity", ACTIVITY_CODE_UNKNOWN);
                int confidence = intent.getIntExtra("confidence", ACTIVITY_CODE_UNKNOWN);
                String activityName = Plugin.getActivityName(activity);
                Log.d(TAG_AWARE_MWT, "[MWT] Activity: " + activityName + ", " + confidence);
                if (!activityName.equals(Plugin.activityName) && confidence > 60) {
                    expectedActivity = activity == ACTIVITY_CODE_IN_VEHICLE || activity == ACTIVITY_CODE_STILL || activity == ACTIVITY_CODE_WALKING;
                    Plugin.activityName = activityName;
                    lastActivityChangeMillis = System.currentTimeMillis();
                }
            }
            if (expectedActivity && System.currentTimeMillis() - lastActivityChangeMillis > 20000L) {
                Log.i(TAG_AWARE_MWT, "[MWT TRIGGER] Activity:" + Plugin.activityName);
                plugin.scheduleMWTTrigger(5000L);
            }

            boolean expectedApp = false;

            if (ACTION_AWARE_APPLICATIONS_FOREGROUND.equals(action)) {
                ContentValues contentValues = intent.getParcelableExtra(EXTRA_DATA);
                Log.d(TAG_AWARE_MWT, "[MWT] App: " + contentValues.toString());

                String currentPackageName = contentValues.getAsString(PACKAGE_NAME);
                switch (currentPackageName) {
                    case "com.google.android.talk":
                    case "com.facebook.katana":
                    case "com.android.chrome":
                    case "com.google.android.gm":
                    case "com.tencent.mm":
                    case "com.whatsapp":
                    case "com.google.android.youtube":
                        expectedApp = true;
                        break;
                    default:
                        expectedApp = false;
                }

                if (!packageName.equals(currentPackageName)) {
                    packageName = currentPackageName;
                    lastAppChangeMillis = System.currentTimeMillis();
                }
            }
            if (expectedApp && System.currentTimeMillis() - lastAppChangeMillis > 60000L) {
                Log.i(TAG_AWARE_MWT, "[MWT TRIGGER] App");
                plugin.scheduleMWTTrigger(10000L);
            }

            if (ACTION_AWARE_CALL_ACCEPTED.equals(action) || ACTION_AWARE_CALL_MADE.equals(action)) {
                Log.i(TAG_AWARE_MWT, "[MWT TRIGGER] Call");
                plugin.scheduleMWTTrigger(5000L);
            }

            if (ACTION_AWARE_MWT_TRIGGER.equals(action)) {
                Log.i(TAG_AWARE_MWT, "[MWT TRIGGER] Manual");
                plugin.scheduleMWTTrigger(0);
            }
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

    private static String getQuestionnaire() throws JSONException {
        ESMFactory eSMFactory = new ESMFactory();

        ESM_Likert eSM_Likert = new ESM_Likert();
        eSM_Likert.setLikertMax(7)
                .setLikertMaxLabel("Great")
                .setLikertMinLabel("Not at all")
                .setLikertStep(1.0D)
                .setTitle("Receptivity to learn")
                .setInstructions("How do you feel about learning now?")
                .setSubmitButton("Next")
                .setTrigger("mwt")
                .setExpirationThreshold(120)
                .setNotificationTimeout(180);
        ESM_Freetext eSM_Freetext = new ESM_Freetext();
        eSM_Freetext.setTitle("Learning activity preference")
                .setInstructions("Which activities you like to learn now? If not why?")
                .setSubmitButton("Next");
        ESM_Radio eSM_Radio1 = new ESM_Radio();
        eSM_Radio1.addRadio("Not familiar")
                .addRadio("Little familiar")
                .addRadio("Quite familiar")
                .setTitle("Familiarity")
                .setInstructions("How familiar are you on the road?")
                .setSubmitButton("Next");
        ESM_Radio eSM_Radio2 = new ESM_Radio();
        eSM_Radio2.addRadio("For food to be ready")
                .addRadio("For bus")
                .addRadio("For lift")
                .addRadio("For a friend")
                .addRadio("For a turn")
                .addRadio("Other")
                .setTitle("Reason for waiting")
                .setInstructions("Why are you waiting for?")
                .setSubmitButton("Next");
        ESM_Radio eSM_Radio3 = new ESM_Radio();
        eSM_Radio3.addRadio("Walking")
                .addRadio("Consuming")
                .addRadio("Waiting")
                .addRadio("Commuting")
                .addRadio("Exercising")
                .addRadio("Working/Studying")
                .addRadio("Other")
                .addFlow("Walking", eSM_Radio1.build())
                .addFlow("Commuting", eSM_Radio1.build())
                .addFlow("Waiting", eSM_Radio2.build())
                .setTitle("Primary Action")
                .setInstructions("What is the primary task you are doing now?")
                .setSubmitButton("Next");
        eSM_Radio1 = new ESM_Radio();
        eSM_Radio1.addRadio("Mobile Checking")
                .addRadio("Resting")
                .addRadio("Conversing")
                .addRadio("Observing")
                .addRadio("Exploring")
                .addRadio("Pondering")
                .addRadio("Other")
                .setTitle("Secondary Action")
                .setInstructions("What is the secondary task you are doing now?")
                .setSubmitButton("Next");
        ESM_Radio eSM_Radio4 = new ESM_Radio();
        eSM_Radio4.addRadio("Alone")
                .addRadio("With 1 person")
                .addRadio("With 2 people")
                .addRadio("With 3 people")
                .addRadio("More than 3 people")
                .setTitle("Social Context")
                .setInstructions("How many people are there with you now?")
                .setSubmitButton("Next");
        ESM_Radio eSM_Radio5 = new ESM_Radio();
        eSM_Radio5.addRadio("Less crowded")
                .addRadio("Medium crowded")
                .addRadio("Highly crowded")
                .setTitle("Surrounding crowd")
                .setInstructions("What is the crowd in surrounding?")
                .setSubmitButton("Next");
        ESM_Radio eSM_Radio6 = new ESM_Radio();
        eSM_Radio6.addRadio("Active")
                .addRadio("Tired")
                .addRadio("Normal")
                .addRadio("Other")
                .setTitle("Physical condition")
                .setInstructions("How do you feel physically?")
                .setSubmitButton("Next");
        ESM_PAM eSM_PAM = new ESM_PAM();
        eSM_PAM.setTitle("Mood").
                setInstructions("What is your mood now?")
                .setSubmitButton("OK");

        eSMFactory.addESM(eSM_Likert);
        eSMFactory.addESM(eSM_Freetext);
        eSMFactory.addESM(eSM_Radio3);
        eSMFactory.addESM(eSM_Radio1);
        eSMFactory.addESM(eSM_Radio4);
        eSMFactory.addESM(eSM_Radio5);
        eSMFactory.addESM(eSM_Radio6);
        eSMFactory.addESM(eSM_PAM);

        return eSMFactory.build();
    }
}
