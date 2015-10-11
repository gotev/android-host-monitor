package com.alexbbb.androidhostmonitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

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
     * Method called when there's a host status change.
     * Override this in subclasses to implement your own business logic.
     * @param status new host status
     */
    public void onHostStatusChanged(HostStatus status) {

    }
}
