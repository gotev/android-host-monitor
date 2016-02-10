package net.gotev.hostmonitor;

/**
 * Represents the status of a monitored host.
 * @author gotev (Aleksandar Gotev)
 */
class Status {
    private boolean reachable;
    private ConnectionType connectionType;

    public Status() {
        reachable = true;
        connectionType = ConnectionType.NONE;
    }

    public Status(boolean reachable, ConnectionType connectionType) {
        this.reachable = reachable;
        this.connectionType = connectionType;
    }

    public boolean isReachable() {
        return reachable;
    }

    public void setReachable(boolean reachable) {
        this.reachable = reachable;
    }

    public ConnectionType getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(ConnectionType connectionType) {
        this.connectionType = connectionType;
    }
}
