package net.gotev.hostmonitor;

import java.net.InetSocketAddress;

/**
 * Represents a host to monitor.
 * @author gotev (Aleksandar Gotev)
 */
class Host {
    private final String host;
    private final int port;

    public Host(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public InetSocketAddress resolve() {
        return new InetSocketAddress(host, port);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Host host1 = (Host) o;

        return port == host1.port && host.equals(host1.host);
    }

    @Override
    public int hashCode() {
        int result = host.hashCode();
        result = 31 * result + port;
        return result;
    }
}
