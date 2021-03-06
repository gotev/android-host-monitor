package net.gotev.hostmonitor;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Host Monitor configuration manager.
 * @author gotev (Aleksandar Gotev)
 */
public class HostMonitorConfig {

    // shared preferences file name
    private static final String PREFS_FILE_NAME = "host_monitor_config";

    // shared preferences keys
    private static final String KEY_HOSTS = "hosts";
    private static final String KEY_BROADCAST_ACTION = "broadcastAction";
    private static final String KEY_SOCKET_TIMEOUT = "socketTimeout";
    private static final String KEY_CHECK_INTERVAL = "checkInterval";
    private static final String KEY_MAX_ATTEMPTS = "maxAttempts";

    // default values
    private static final String DEFAULT_BROADCAST_ACTION = "net.gotev.hostmonitor.status";
    private static final int DEFAULT_SOCKET_TIMEOUT = 2000; //in milliseconds
    private static final int DEFAULT_CHECK_INTERVAL = 0; //in milliseconds
    private static final int DEFAULT_MAX_ATTEMPTS = 3;
    private static final int UNDEFINED = -1;
    private static final int PERIODIC_CHECK_ID = 0;

    private final Context mContext;
    private SharedPreferences mSharedPreferences;

    private Map<Host, Status> mHostsMap;
    private String mBroadcastAction;
    private int mSocketTimeout = UNDEFINED;
    private int mCheckInterval = UNDEFINED;
    private int mMaxAttempts = UNDEFINED;

    /**
     * Creates a new Host Monitor configuration instance
     * @param context application context
     */
    public HostMonitorConfig(Context context) {
        mContext = context.getApplicationContext();
    }

    private SharedPreferences getPrefs() {
        if (mSharedPreferences == null) {
            mSharedPreferences = mContext.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE);
        }

        return mSharedPreferences;
    }

    Map<Host, Status> getHostsMap() {
        if (mHostsMap == null) {
            String json = getPrefs().getString(KEY_HOSTS, "");

            if (json.isEmpty()) {
                mHostsMap = new HashMap<>();
            } else {
                Type typeOfMap = new TypeToken<HashMap<Host, Status>>(){}.getType();
                try {
                    mHostsMap = new Gson().fromJson(json, typeOfMap);
                } catch (Exception exc) {
                    Logger.error(getClass().getSimpleName(),
                                 "Error while deserializing hosts map: " + json
                                 + ". Ignoring values.", exc);
                    mHostsMap = new HashMap<>();
                }
            }
        }

        return mHostsMap;
    }

    /**
     * Set the broadcast action string to use when broadcasting host status changes
     * @param broadcastAction (e.g.: com.example.yourapp.hoststatus)
     * @return {@link HostMonitorConfig}
     */
    public HostMonitorConfig setBroadcastAction(String broadcastAction) {
        if (broadcastAction == null || broadcastAction.isEmpty())
            throw new IllegalArgumentException("Broadcast action MUST not be null or empty!");

        mBroadcastAction = broadcastAction;
        return this;
    }

    /**
     * Gets the broadcast action used for host status changes.
     * @return the configured broadcast action string
     */
    public String getBroadcastAction() {
        if (mBroadcastAction == null) {
            mBroadcastAction = getPrefs().getString(KEY_BROADCAST_ACTION, DEFAULT_BROADCAST_ACTION);
        }

        return mBroadcastAction;
    }

    /**
     * Adds a new host to be monitored. The change will be applied starting from the next
     * reachability scan.
     * @param host host IP address or FQDN
     * @param port TCP port to check
     * @return {@link HostMonitorConfig}
     */
    public HostMonitorConfig add(final String host, final int port) {
        Host newHost = new Host(host, port);

        if (getHostsMap().keySet().contains(newHost)) return this;

        mHostsMap.put(newHost, new Status());

        return this;
    }

    /**
     * Remove a monitored host. The change will be applied starting from the next
     * reachability scan.
     * @param host host address to check
     * @param port tcp port to check
     * @return {@link HostMonitorConfig}
     */
    public HostMonitorConfig remove(final String host, final int port) {
        Host toRemove = new Host(host, port);

        if (!getHostsMap().keySet().contains(toRemove)) return this;

        mHostsMap.remove(toRemove);

        return this;
    }

    /**
     * Remove all the monitored hosts.
     * @return {@link HostMonitorConfig}
     */
    public HostMonitorConfig removeAll() {
        if (mHostsMap != null) {
            mHostsMap.clear();
        }

        return this;
    }

    /**
     * Set socket connection timeout in seconds.
     * @param seconds maximum number of seconds to wait for a socket connection to be
     *                established
     * @return {@link HostMonitorConfig}
     */
    public HostMonitorConfig setSocketTimeoutInSeconds(int seconds) {
        if (seconds < 1)
            throw new IllegalArgumentException("Specify at least one second timeout!");

        mSocketTimeout = seconds * 1000;
        return this;
    }

    /**
     * Set socket connection timeout in milliseconds.
     * @param millisecs maximum number of seconds to wait for a socket connection to be
     *                  established
     * @return {@link HostMonitorConfig}
     */
    public HostMonitorConfig setSocketTimeoutInMilliseconds(int millisecs) {
        if (millisecs < 1)
            throw new IllegalArgumentException("Specify at least one millisecond timeout!");

        mSocketTimeout = millisecs;
        return this;
    }

    /**
     * Get socket timeout in milliseconds. By default is 2000.
     * @return the configured socket timeout
     */
    public int getSocketTimeout() {
        if (mSocketTimeout <= 0) {
            mSocketTimeout = getPrefs().getInt(KEY_SOCKET_TIMEOUT, DEFAULT_SOCKET_TIMEOUT);
        }

        return mSocketTimeout;
    }

    /**
     * Set check interval in seconds.
     * 0 means that check interval is disabled (it's the default value).
     * @param seconds how often to check for hosts reachability
     * @return {@link HostMonitorConfig}
     */
    public HostMonitorConfig setCheckIntervalInSeconds(int seconds) {
        if (seconds < 0)
            throw new IllegalArgumentException("Specify a zero or positive check interval!");

        mCheckInterval = seconds * 1000;
        return this;
    }

    /**
     * Set check interval in minutes.
     * 0 means that check interval is disabled (it's the default value).
     * @param minutes how often to check for hosts reachability
     * @return {@link HostMonitorConfig}
     */
    public HostMonitorConfig setCheckIntervalInMinutes(int minutes) {
        if (minutes < 0)
            throw new IllegalArgumentException("Specify a zero or positive check interval!");

        mCheckInterval = minutes * 60 * 1000;
        return this;
    }

    /**
     * Get check interval in milliseconds. By default is zero, so no periodic check is
     * performed until you set it.
     * @return the configured check interval in milliseconds
     */
    public int getCheckInterval() {
        if (mCheckInterval <= 0) {
            mCheckInterval = getPrefs().getInt(KEY_CHECK_INTERVAL, DEFAULT_CHECK_INTERVAL);
        }

        return mCheckInterval;
    }

    /**
     * Sets the maximum number of socket connections to perform before notifying that the port
     * is unreachable.
     * @param maxAttempts maximum number of connections to try (must be at least 1)
     * @return {@link HostMonitorConfig}
     */
    public HostMonitorConfig setMaxAttempts(int maxAttempts) {
        if (maxAttempts < 1)
            throw new IllegalArgumentException("Set at least one attempt!");

        mMaxAttempts = maxAttempts;
        return this;
    }

    /**
     * Gets the maximum number of socket connections to perform before notifying that the port
     * is unreachable. Default value is 3.
     * @return number of attempts
     */
    public int getMaxAttempts() {
        if (mMaxAttempts <= 0) {
            mMaxAttempts = getPrefs().getInt(KEY_MAX_ATTEMPTS, DEFAULT_MAX_ATTEMPTS);
        }

        return mMaxAttempts;
    }

    void saveHostsMap() {
        Logger.debug(getClass().getSimpleName(), "saving hosts status map");
        Gson gson = new GsonBuilder().enableComplexMapKeySerialization().create();
        getPrefs().edit().putString(KEY_HOSTS, gson.toJson(mHostsMap)).apply();
    }

    /**
     * Resets the currently persisted configuration.
     * Disables the connectivity receiver and cancels all scheduled periodic checks (if any).
     * @param context application context
     */
    public static void reset(Context context) {
        Logger.debug(HostMonitor.class.getSimpleName(), "reset configuration");
        context.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE).edit().clear().apply();

        Util.setBroadcastReceiverEnabled(context, ConnectivityReceiver.class, false);

        Logger.debug(HostMonitor.class.getSimpleName(), "cancelling scheduled checks");
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(getPeriodicCheckIntent(context));
    }

    /**
     * Saves and applies the configuration changes.
     * If there aren't configured hosts, it disables the {@link ConnectivityReceiver} and cancels
     * scheduled period checks. If there is at least one configured host, it enables the
     * {@link ConnectivityReceiver} and re-schedules periodic checks with new settings. If periodic
     * check interval is set to zero, host reachability checks will be triggered only when the
     * connectivity status of the device changes.
     */
    public void save() {
        Logger.debug(getClass().getSimpleName(), "saving configuration");

        SharedPreferences.Editor prefs = getPrefs().edit();

        if (mHostsMap != null && !mHostsMap.isEmpty()) {
            Gson gson = new GsonBuilder().enableComplexMapKeySerialization().create();
            prefs.putString(KEY_HOSTS, gson.toJson(mHostsMap));
        }

        if (mBroadcastAction != null && !mBroadcastAction.isEmpty()) {
            prefs.putString(KEY_BROADCAST_ACTION, mBroadcastAction);
        }

        if (mSocketTimeout > 0) {
            prefs.putInt(KEY_SOCKET_TIMEOUT, mSocketTimeout);
        }

        if (mCheckInterval >= 0) {
            prefs.putInt(KEY_CHECK_INTERVAL, mCheckInterval);
        }

        if (mMaxAttempts > 0) {
            prefs.putInt(KEY_MAX_ATTEMPTS, mMaxAttempts);
        }

        prefs.apply();

        boolean thereIsAtLeastOneHost = !getHostsMap().isEmpty();
        Util.setBroadcastReceiverEnabled(mContext, ConnectivityReceiver.class, thereIsAtLeastOneHost);

        AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        PendingIntent intent = getPeriodicCheckIntent(mContext);

        Logger.debug(HostMonitor.class.getSimpleName(), "cancelling scheduled checks");
        alarmManager.cancel(intent);

        if (thereIsAtLeastOneHost) {
            if (getCheckInterval() > 0) {
                Logger.debug(getClass().getSimpleName(), "scheduling periodic checks every " +
                                                         (getCheckInterval() / 1000) + " seconds");
                alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
                                          System.currentTimeMillis() + getCheckInterval(),
                                          getCheckInterval(), intent);
            }

            Logger.debug(getClass().getSimpleName(), "triggering reachability check");
            HostMonitor.start(mContext);
        }
    }

    private static PendingIntent getPeriodicCheckIntent(Context context) {
        return PendingIntent.getBroadcast(context, PERIODIC_CHECK_ID,
                                          HostMonitor.getCheckIntent(context), 0);
    }
}
