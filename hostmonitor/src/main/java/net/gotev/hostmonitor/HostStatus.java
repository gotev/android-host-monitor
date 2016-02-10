package net.gotev.hostmonitor;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.Gson;

/**
 * Contains the current and previous reachability status of a host and port.
 * @author gotev (Aleksandar Gotev)
 */
public class HostStatus implements Parcelable {

    /**
     * Name of the parameter passed in the broadcast intent.
     */
    public static final String PARAM_STATUS = "HostStatus";

    private String host;
    private int port;
    private boolean previousReachable;
    private boolean reachable;
    private ConnectionType previousConnectionType;
    private ConnectionType connectionType;

    public HostStatus() { }

    public String getHost() {
        return host;
    }

    public HostStatus setHost(String host) {
        this.host = host;
        return this;
    }

    public int getPort() {
        return port;
    }

    public HostStatus setPort(int port) {
        this.port = port;
        return this;
    }

    public boolean isPreviousReachable() {
        return previousReachable;
    }

    public HostStatus setPreviousReachable(boolean previousReachable) {
        this.previousReachable = previousReachable;
        return this;
    }

    public boolean isReachable() {
        return reachable;
    }

    public HostStatus setReachable(boolean reachable) {
        this.reachable = reachable;
        return this;
    }

    public ConnectionType getConnectionType() {
        return connectionType;
    }

    public HostStatus setConnectionType(ConnectionType connectionType) {
        this.connectionType = connectionType;
        return this;
    }

    public ConnectionType getPreviousConnectionType() {
        return previousConnectionType;
    }

    public HostStatus setPreviousConnectionType(ConnectionType connectionType) {
        this.previousConnectionType = connectionType;
        return this;
    }

    public boolean connectionTypeChanged() {
        return previousConnectionType != connectionType;
    }

    public boolean reachabilityChanged() {
        return previousReachable != reachable;
    }

    // This is used to regenerate the object.
    // All Parcelables must have a CREATOR that implements these two methods
    public static final Parcelable.Creator<HostStatus> CREATOR = new Parcelable.Creator<HostStatus>() {
        @Override
        public HostStatus createFromParcel(final Parcel in) {
            return new HostStatus(in);
        }

        @Override
        public HostStatus[] newArray(final int size) {
            return new HostStatus[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(host);
        dest.writeInt(port);
        dest.writeInt(previousReachable ? 1 : 0);
        dest.writeInt(reachable ? 1 : 0);
        dest.writeInt(connectionType.ordinal());
        dest.writeInt(previousConnectionType.ordinal());
    }

    private HostStatus(Parcel in) {
        host = in.readString();
        port = in.readInt();
        previousReachable = (in.readInt() == 1);
        reachable = (in.readInt() == 1);
        connectionType = ConnectionType.values()[in.readInt()];
        previousConnectionType = ConnectionType.values()[in.readInt()];
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
