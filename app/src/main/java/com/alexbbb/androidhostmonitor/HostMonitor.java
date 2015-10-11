package com.alexbbb.androidhostmonitor;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.net.InetSocketAddress;
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
    private static ScheduledFuture<Runnable> mScheduledTask = null;

    private static ConcurrentHashMap<InetSocketAddress, Boolean> mHosts = new ConcurrentHashMap<>();
    private static boolean mActive = false;
    private static int mConnectionType = -1;

    /**
     * Add a host to monitor. If the monitoring service is currently running, the check will be
     * performed at the next execution.
     * @param hostAddress host to monitor
     * @param port tcp port to monitor
     */
    public static void addHostToMonitor(final String hostAddress, final int port) {
        InetSocketAddress newHost = InetSocketAddress.createUnresolved(hostAddress, port);

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
    public static void removeHostToMonitor(final String hostAddress, final int port) {
        mHosts.remove(InetSocketAddress.createUnresolved(hostAddress, port));
    }

    /**
     * Returns the last available host reachability status.
     * @param hostAddress host address to check
     * @param port tcp port to check
     * @return null if the status could not be determined (this happens when you try to get the
     * status of a non-monitored host or the monitor scanner has not retured any result yet)
     */
    public static Boolean isHostReachable(final String hostAddress, int port) {
        return mHosts.get(InetSocketAddress.createUnresolved(hostAddress, port));
    }

        /**
         * Gets the currently configured broadcast action string.
         * @return
         */
    public static synchronized String getBroadcastActionString() {
        return mBroadcastActionString;
    }

    /**
     * Starts the HostMonitor.
     * @param context application context
     * @param broadcastActionString string used as an action when broadcasting reachability status
     * @param checkIntervalSecs how often (in seconds) to monitor the configured hosts
     * @param connectTimeout how many seconds to wait for the connection to the TCP socket to be
     *                       established
     */
    public static synchronized void start(Context context, String broadcastActionString,
                                          int checkIntervalSecs, int connectTimeout) {
        if (context == null) {
            throw new IllegalArgumentException("Please provide a valid application context");
        }

        if (broadcastActionString == null || broadcastActionString.isEmpty()) {
            throw new IllegalArgumentException("Please provide a valid broadcast action (e.g. com.yourapp.hostchange)");
        }

        if (checkIntervalSecs <= 0) {
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
        mBroadcastActionString = broadcastActionString;
        mCheckInterval = checkIntervalSecs;
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
    public static synchronized void stopAndDeactivate(boolean sendDisconnectedStatus) {
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

        Log.d(LOG_TAG, "stopping");

        mScheduledTask.cancel(false);
        mScheduledTask = null;

        if (sendDisconnectedStatus) {
            for (InetSocketAddress host : mHosts.keySet()) {
                mHosts.put(host, false);
                notifyStatus(host, false);
            }
        }
    }

    protected static synchronized void start() {
        if (mScheduledTask != null || !mActive) return;

        Log.d(LOG_TAG, "starting");

        if (scheduler == null) {
            Log.d(LOG_TAG, "creating new thread pool");
            scheduler = Executors.newScheduledThreadPool(1);
        }

        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (mHosts.isEmpty()) {
                    Log.d(LOG_TAG, "No hosts to check at this moment");
                    return;
                }

                for (InetSocketAddress host : mHosts.keySet()) {
                    Boolean previousReachable = mHosts.get(host);
                    boolean currentReachable = isCurrentReachable(host);

                    if (previousReachable == null || previousReachable != currentReachable) {
                        mHosts.put(host, currentReachable);
                        notifyStatus(host, currentReachable);
                    }
                }
            }

            private boolean isCurrentReachable(InetSocketAddress host) {
                boolean currentReachable;
                Socket socket = null;

                try {
                    socket = new Socket();

                    socket.connect(host, mConnectTimeout);
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

    private static void notifyStatus(InetSocketAddress host, boolean reachable) {
        HostStatus status = new HostStatus().setHost(host.getHostString())
                                            .setPort(host.getPort())
                                            .setReachable(reachable)
                                            .setConnectionType(mConnectionType);

        Intent broadcastStatus = new Intent();
        broadcastStatus.setAction(mBroadcastActionString);
        broadcastStatus.putExtra(PARAM_STATUS, status);

        mContext.sendBroadcast(broadcastStatus);
    }
}
