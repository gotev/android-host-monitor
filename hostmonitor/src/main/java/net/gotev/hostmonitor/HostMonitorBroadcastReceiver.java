package net.gotev.hostmonitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

/**
 * Reference implementation of the HostMonitor broadcast receiver.
 * @author gotev (Aleksandar Gotev)
 */
public class HostMonitorBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = new HostMonitorConfig(context).getBroadcastAction();

        if (intent == null || action == null || !intent.getAction().equals(action)) {
            return;
        }

        HostStatus hostStatus = intent.getParcelableExtra(HostStatus.PARAM_STATUS);
        onHostStatusChanged(hostStatus);
    }

    /**
     * Register this host monitor receiver.
     * If you use this receiver in an {@link android.app.Activity}, you have to call this method inside
     * {@link android.app.Activity#onResume()}, after {@code super.onResume();}.<br>
     * If you use it in a {@link android.app.Service}, you have to
     * call this method inside {@link android.app.Service#onCreate()}, after {@code super.onCreate();}.
     *
     * @param context context in which to register this receiver
     */
    public void register(final Context context) {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(new HostMonitorConfig(context).getBroadcastAction());
        context.registerReceiver(this, intentFilter);
    }

    /**
     * Unregister this host monitor receiver.
     * If you use this receiver in an {@link android.app.Activity}, you have to call this method inside
     * {@link android.app.Activity#onPause()}, after {@code super.onPause();}.<br>
     * If you use it in a {@link android.app.Service}, you have to
     * call this method inside {@link android.app.Service#onDestroy()}.
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
        //do nothing here
    }
}
