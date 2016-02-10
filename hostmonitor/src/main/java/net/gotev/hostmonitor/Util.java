package net.gotev.hostmonitor;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

/**
 * Utility methods.
 * @author Aleksandar Gotev
 */
class Util {

    private static final String LOG_TAG = Util.class.getSimpleName();

    /**
     * Private constructor to avoid instantiation.
     */
    private Util() { }

    /**
     * Enables or disables a {@link BroadcastReceiver}.
     * Note: be aware that enabling or disabling a component with DONT_KILL_APP on API 14 or 15
     * will wipe out any ongoing notifications your app has created.
     * http://stackoverflow.com/questions/5624470/enable-and-disable-a-broadcast-receiver
     * @param context application context
     * @param receiver broadcast receiver class to enable or disable
     * @param enabled new status
     */
    public static void setBroadcastReceiverEnabled(Context context,
                                                   Class<? extends BroadcastReceiver> receiver,
                                                   boolean enabled) {
        int newState = enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                               : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;

        Logger.debug(LOG_TAG, (enabled ? "enabling" : "disabling") + " connectivity receiver");

        context.getPackageManager()
               .setComponentEnabledSetting(new ComponentName(context, receiver),
                                           newState, PackageManager.DONT_KILL_APP);
    }
}
