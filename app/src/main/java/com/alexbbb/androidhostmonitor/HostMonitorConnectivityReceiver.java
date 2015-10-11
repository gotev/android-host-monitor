package com.alexbbb.androidhostmonitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

/**
 * Monitors connectivity changes and starts or stops the HostMonitor accordingly.
 * @author alexbbb (Aleksandar Gotev)
 */
public class HostMonitorConnectivityReceiver extends BroadcastReceiver {

    private static final String LOG_TAG = "HostMonitorCR";

    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) {
            Log.d(LOG_TAG, "connection available :)");
            HostMonitor.setConnectionType(networkInfo.getType());
            HostMonitor.start();

        } else {
            Log.d(LOG_TAG, "connection unavailable :(");
            HostMonitor.setConnectionType(-1);
            HostMonitor.stop(true);
        }
    }
}
