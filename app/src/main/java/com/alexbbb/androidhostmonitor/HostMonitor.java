package com.alexbbb.androidhostmonitor;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Static class which periodically monitors configured hosts reachability.
 * @author alexbbb (Aleksandar Gotev)
 */
public class HostMonitor {

    private static final String LOG_TAG = "HostMonitor";

    /**
     * Name of the parameter passed in the broadcast intent.
     */
    public static final String PARAM_STATUS = "HostStatus";

    /**
     * Default check interval.
     */
    public static final int DEFAULT_CHEK_INTERVAL = 30;

    /**
     * Default socket connection timeout.
     */
    public static final int DEFAULT_TIMEOUT = 2;

    /**
     * Private constructor to avoid instantiation.
     */
    private HostMonitor() {}

    private static Context mContext;
    private static int mCheckInterval;
    private static int mConnectTimeout;
    private static String mBroadcastActionString;

    private static ScheduledExecutorService scheduler;
    private static ScheduledFuture<?> mScheduledTask = null;

    private static ConcurrentHashMap<Host, Boolean> mHosts = new ConcurrentHashMap<>();
    private static boolean mActive = false;
    private static int mConnectionType = -1;
    private static boolean mDebugEnabled = false;

    /**
     * Add a host to monitor. If the monitoring service is currently running, the check will be
     * performed at the next execution.
     * @param hostAddress host to monitor
     * @param port tcp port to monitor
     */
    public static void add(final String hostAddress, final int port) {
        Host newHost = new Host(hostAddress, port);

        if (!mHosts.containsKey(newHost)) {
            mHosts.put(newHost, false);
        }
    }

    /**
     * Remove a monitored host. The change will be applied starting from the next
     * reachability scan.
     * @param hostAddress host address to check
     * @param port tcp port to check
     */
    public static void remove(final String hostAddress, final int port) {
        mHosts.remove(new Host(hostAddress, port));
    }

    /**
     * Returns the last available host reachability status.
     * @param hostAddress host address to check
     * @param port tcp port to check
     * @return true if the host is reachable, false if it's not reachable or null
     * if the status could not be determined (this happens when you try to get the
     * status of a non-monitored host or the monitor task has not retured any result yet)
     */
    public static Boolean isReachable(final String hostAddress, int port) {
        return mHosts.get(new Host(hostAddress, port));
    }

    /**
     * Set the broadcast action string to use when broadcasting host status changes
     * @param broadcastAction (e.g.: com.example.yourapp.hoststatus)
     */
    public static synchronized void setBroadcastAction(String broadcastAction) {
        mBroadcastActionString = broadcastAction;
    }

    /**
     * Gets the currently configured broadcast action string.
     * @return
     */
    public static synchronized String getBroadcastActionString() {
        return mBroadcastActionString;
    }

    /**
     * Enable library debugging log.
     */
    public static synchronized void enableDebug() {
        mDebugEnabled = true;
    }

    /**
     * Starts the HostMonitor.
     * @param context application context
     * @param checkInterval how often (in seconds) to monitor the configured hosts
     * @param connectTimeout how many seconds to wait for the connection to the TCP socket to be
     *                       established
     */
    public static synchronized void start(Context context, int checkInterval, int connectTimeout) {
        if (context == null) {
            throw new IllegalArgumentException("Please provide a valid application context");
        }

        if (mBroadcastActionString == null || mBroadcastActionString.isEmpty()) {
            throw new IllegalArgumentException("Please call setBroadcastAction method before start");
        }

        if (checkInterval <= 0) {
            throw new IllegalArgumentException("Please provide a checkInterval in secs > 0");
        }

        if (connectTimeout <= 0) {
            throw new IllegalArgumentException("Please provide a connectTimeout in secs > 0");
        }

        if (mScheduledTask != null) {
            throw new RuntimeException("Please stop the current execution before starting a new one!");
        }
        mActive = true;

        mContext = context.getApplicationContext();
        mCheckInterval = checkInterval;
        mConnectTimeout = connectTimeout * 1000;

        start();
    }

    /**
     * Stops the HostMonitor and prevents automatic restarts on connectivity change.
     * @param sendDisconnectedStatus true to send a broadcast host status update notifying that
     *                               all the monitored hosts are unreachable. This is set to true
     *                               when HostMonitorConnectivityReceiver stops the execution
     *                               when there's no connectivity available, to preserve battery
     *                               life.
     */
    public static synchronized void shutdown(boolean sendDisconnectedStatus) {
        mActive = false;
        if (mScheduledTask == null) return;

        stop(sendDisconnectedStatus);
    }

    /**
     * Checks if the HostMonitor is currently running
     * @return true if the host monitor is currently running, false otherwise
     */
    public static synchronized boolean isRunning() {
        return (mScheduledTask != null);
    }

    protected static synchronized void setConnectionType(int type) {
        mConnectionType = type;
    }

    protected static synchronized void stop(boolean sendDisconnectedStatus) {
        if (mScheduledTask == null) return;

        log(LOG_TAG, "stopping");

        mScheduledTask.cancel(false);
        mScheduledTask = null;

        if (sendDisconnectedStatus) {
            for (Host host : mHosts.keySet()) {
                mHosts.put(host, false);
                notifyStatus(host, false);
            }
        }
    }

    protected static synchronized void start() {
        if (mScheduledTask != null || !mActive) return;

        log(LOG_TAG, "starting");

        if (scheduler == null) {
            log(LOG_TAG, "creating new thread pool");
            scheduler = Executors.newScheduledThreadPool(1);
        }

        initializeConnectionType();

        mScheduledTask = scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (mHosts.isEmpty()) {
                    log(LOG_TAG, "No hosts to check at this moment");
                    return;
                }

                log(LOG_TAG, "Starting reachability check");

                for (Host host : mHosts.keySet()) {
                    Boolean previousReachable = mHosts.get(host);
                    boolean currentReachable = isReachable(host);

                    if (previousReachable == null || previousReachable != currentReachable) {
                        log(LOG_TAG, "Host " + host.getHost() + " is currently " +
                                (currentReachable ? "reachable" : "unreachable") +
                                " on port " + host.getPort());
                        mHosts.put(host, currentReachable);
                        notifyStatus(host, currentReachable);
                    }
                }

                log(LOG_TAG, "Reachability check completed");
            }

            private boolean isReachable(Host host) {
                boolean currentReachable;
                Socket socket = null;

                try {
                    socket = new Socket();
                    socket.connect(host.resolve(), mConnectTimeout);
                    currentReachable = true;

                } catch (Exception exc) {
                    currentReachable = false;

                } finally {
                    if (socket != null) {
                        try {
                            socket.close();
                        } catch (Exception exc) {}
                    }
                }
                return currentReachable;
            }
        }, 0, mCheckInterval, TimeUnit.SECONDS);
    }

    private static void initializeConnectionType() {
        ConnectivityManager connMan =
                (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo info = connMan.getActiveNetworkInfo();
        if (info == null) {
            mConnectionType = -1;
        } else {
            mConnectionType = info.getType();
        }
    }

    private static synchronized void notifyStatus(Host host, boolean reachable) {
        HostStatus status = new HostStatus().setHost(host.getHost())
                                            .setPort(host.getPort())
                                            .setReachable(reachable)
                                            .setConnectionType(getConnectionType());

        Intent broadcastStatus = new Intent();
        broadcastStatus.setAction(mBroadcastActionString);
        broadcastStatus.putExtra(PARAM_STATUS, status);

        mContext.sendBroadcast(broadcastStatus);
    }

    private static ConnectionType getConnectionType() {
        if (mConnectionType < 0) return ConnectionType.NONE;
        if (mConnectionType == ConnectivityManager.TYPE_MOBILE) return ConnectionType.MOBILE;
        if (mConnectionType == ConnectivityManager.TYPE_WIFI) return ConnectionType.WIFI;

        Log.e(LOG_TAG, "Unimplemented connection type: " + mConnectionType + ", open a bug issue!");
        return ConnectionType.NONE;
    }

    protected static void log(String tag, String message) {
        if (mDebugEnabled) {
            Log.d(tag, message);
        }
    }
}
