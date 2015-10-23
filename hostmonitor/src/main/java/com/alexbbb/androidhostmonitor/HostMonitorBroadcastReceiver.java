package com.alexbbb.androidhostmonitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

/**
 * Reference implementation of the HostMonitor broadcast receiver.
 * @author alexbbb (Aleksandar Gotev)
 */
public class HostMonitorBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = HostMonitor.getBroadcastActionString();

        if (intent == null || action == null || !intent.getAction().equals(action)) return;

        HostStatus hostStatus = intent.getParcelableExtra(HostMonitor.PARAM_STATUS);

        onHostStatusChanged(hostStatus);
    }

    /**
     * Register this upload receiver.
     * It's recommended to register the receiver in Activity's onResume method.
     *
     * @param context context in which to register this receiver
     */
    public void register(final Context context) {
        String action = HostMonitor.getBroadcastActionString();

        if (action == null) {
            throw new RuntimeException("You have to start the HostMonitor first!");
        }

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(action);
        context.registerReceiver(this, intentFilter);
    }

    /**
     * Unregister this upload receiver.
     * It's recommended to unregister the receiver in Activity's onPause method.
     *
     * @param context context in which to unregister this receiver
     */
    public void unregister(final Context context) {
        context.unregisterReceiver(this);
    }

    /**
     * Method called when there's a host status change.
     * Override this in subclasses to implement your own business logic.
     * @param status new host status
     */
    public void onHostStatusChanged(HostStatus status) {

    }
}
