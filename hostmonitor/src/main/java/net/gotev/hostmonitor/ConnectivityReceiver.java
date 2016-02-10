package net.gotev.hostmonitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

/**
 * Monitors connectivity changes.
 * @author gotev (Aleksandar Gotev)
 */
public class ConnectivityReceiver extends BroadcastReceiver {

    private static final String LOG_TAG = "HostMonitorCR";

    private static volatile PowerManager.WakeLock wakeLock;

    @Override
    public void onReceive(Context context, Intent intent) {
        Logger.debug(LOG_TAG, "onReceive");

        manageWakeLock(context);

        ConnectionType connectionType = HostMonitor.getCurrentConnectionType(context);

        Logger.debug(LOG_TAG, (connectionType == ConnectionType.NONE) ?
                              "connection unavailable" :
                              "connection available via " + connectionType);

        HostMonitor.start(context, connectionType);
    }

    private synchronized void manageWakeLock(Context context) {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }

        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, LOG_TAG);
        wakeLock.setReferenceCounted(false);
        wakeLock.acquire(10 * 1000);
    }
}
