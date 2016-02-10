package net.gotev.hostmonitor;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.PowerManager;

import java.net.Socket;

/**
 * Service which performs reachability checks of the configured hosts and ports.
 * @author gotev (Aleksandar Gotev)
 */
public class HostMonitor extends IntentService {

    private static final String LOG_TAG = HostMonitor.class.getSimpleName();
    private static final String ACTION_CHECK = "net.gotev.hostmonitor.check";

    private static final String PARAM_CONNECTION_TYPE = "net.gotev.hostmonitor.connection_type";

    /**
     * Name of the parameter passed in the broadcast intent.
     */
    public static final String PARAM_STATUS = "HostStatus";

    public HostMonitor() {
        super(LOG_TAG);
    }

    /**
     * Starts the host monitor check
     * @param context application context
     */
    public static void start(Context context) {
        Intent intent = new Intent(context, HostMonitor.class);
        intent.setAction(ACTION_CHECK);
        context.startService(intent);
    }

    /**
     * Starts the host monitor check.
     * @param context application context
     * @param connectionType current connection type
     */
    protected static void start(Context context, ConnectionType connectionType) {
        Intent intent = new Intent(context, HostMonitor.class);
        intent.setAction(ACTION_CHECK);
        intent.putExtra(PARAM_CONNECTION_TYPE, connectionType.ordinal());
        context.startService(intent);
    }

    /**
     * Stops the host monitor check.
     * @param context application context
     */
    public static void stop(Context context) {
        context.stopService(new Intent(context, HostMonitor.class));
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null || !ACTION_CHECK.equals(intent.getAction())) return;

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                                                                  getClass().getSimpleName());

        wakeLock.acquire();

        HostMonitorConfig config = new HostMonitorConfig(this);

        if (config.getHostsMap().isEmpty()) {
            Logger.debug(LOG_TAG, "No hosts to check at this moment");

        } else {
            ConnectionType connectionType = getConnectionType(intent);

            if (connectionType == ConnectionType.NONE) {
                notifyThatAllTheHostsAreUnreachable(connectionType, config);
            } else {
                checkReachability(connectionType, config);
            }
        }

        wakeLock.release();
    }

    private void notifyThatAllTheHostsAreUnreachable(ConnectionType connectionType,
                                                     HostMonitorConfig config) {
        Logger.debug(LOG_TAG, "No active connection. Notifying that all the hosts are unreachable");

        for (Host host : config.getHostsMap().keySet()) {
            Status previousStatus = config.getHostsMap().get(host);
            Status newStatus = new Status(false, connectionType);

            Logger.debug(LOG_TAG, "Host " + host.getHost() + " is currently unreachable on port "
                    + host.getPort());

            config.getHostsMap().put(host, newStatus);
            notifyStatus(config.getBroadcastAction(), host, previousStatus, newStatus);
        }

        config.saveHostsMap();
    }

    private void checkReachability(ConnectionType connectionType, HostMonitorConfig config) {
        Logger.debug(LOG_TAG, "Starting reachability check");

        for (Host host : config.getHostsMap().keySet()) {
            Status previousStatus = config.getHostsMap().get(host);
            boolean currentReachable = isReachable(host, config.getSocketTimeout(), config.getMaxAttempts());
            Status newStatus = new Status(currentReachable, connectionType);

            Logger.debug(LOG_TAG, "Host " + host.getHost() + " is currently " +
                    (currentReachable ? "reachable" : "unreachable") +
                    " on port " + host.getPort() + " via " + connectionType);

            config.getHostsMap().put(host, newStatus);
            notifyStatus(config.getBroadcastAction(), host, previousStatus, newStatus);
        }

        config.saveHostsMap();
        Logger.debug(LOG_TAG, "Reachability check finished!");
    }

    private ConnectionType getConnectionType(Intent intent) {
        int connTypeInt = intent.getIntExtra(PARAM_CONNECTION_TYPE, -1);

        ConnectionType type;

        if (connTypeInt < 0) {
            type = getCurrentConnectionType(this);
        } else {
            type = ConnectionType.values()[connTypeInt];
        }

        return type;
    }

    protected static ConnectionType getCurrentConnectionType(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (networkInfo == null || !networkInfo.isConnected()) {
            return ConnectionType.NONE;
        }

        int type = networkInfo.getType();

        if (type == ConnectivityManager.TYPE_MOBILE) return ConnectionType.MOBILE;
        if (type == ConnectivityManager.TYPE_WIFI) return ConnectionType.WIFI;

        Logger.error(LOG_TAG, "Unsupported connection type: " + type + ". Returning NONE");
        return ConnectionType.NONE;
    }

    private boolean isReachable(Host host, int connectTimeout, int maxAttempts) {
        int attempts = 0;
        boolean reachable = false;

        while (attempts < maxAttempts) {
            reachable = isReachable(host, connectTimeout);
            if (reachable) break;
            attempts++;
        }

        return reachable;
    }

    private boolean isReachable(Host host, int connectTimeout) {
        boolean reachable;
        Socket socket = null;

        try {
            socket = new Socket();
            socket.connect(host.resolve(), connectTimeout);
            reachable = true;

        } catch (Exception exc) {
            reachable = false;

        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (Exception exc) {
                    Logger.debug(LOG_TAG, "Error while closing socket.");
                }
            }
        }

        return reachable;
    }

    private void notifyStatus(String broadcastAction, Host host,
                              Status previousStatus, Status currentStatus) {
        HostStatus status = new HostStatus()
                .setHost(host.getHost())
                .setPort(host.getPort())
                .setPreviousReachable(previousStatus.isReachable())
                .setPreviousConnectionType(previousStatus.getConnectionType())
                .setReachable(currentStatus.isReachable())
                .setConnectionType(currentStatus.getConnectionType());

        Intent broadcastStatus = new Intent(broadcastAction);
        broadcastStatus.putExtra(PARAM_STATUS, status);

        sendBroadcast(broadcastStatus);
    }
}
