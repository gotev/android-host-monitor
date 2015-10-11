package com.alexbbb.androidhostmonitor;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * @author alexbbb (Aleksandar Gotev)
 */
public class HostStatus implements Parcelable {

    private String host;
    private int port;
    private boolean reachable;
    private int connectionType;

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

    public boolean isReachable() {
        return reachable;
    }

    public HostStatus setReachable(boolean reachable) {
        this.reachable = reachable;
        return this;
    }

    public int getConnectionType() {
        return connectionType;
    }

    public HostStatus setConnectionType(int connectionType) {
        this.connectionType = connectionType;
        return this;
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
        dest.writeInt(reachable ? 1 : 0);
        dest.writeInt(connectionType);
    }

    private HostStatus(Parcel in) {
        host = in.readString();
        port = in.readInt();
        reachable = (in.readInt() == 1);
        connectionType = in.readInt();
    }

    @Override
    public String toString() {
        return "{ \"host\": \"" + host + "\", \"port\": " + port +
                ", \"reachable\": " + reachable + ", \"connection_type\": " + connectionType + "}";
    }
}
