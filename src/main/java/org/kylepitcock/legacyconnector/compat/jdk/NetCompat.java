package org.kylepitcock.legacyconnector.compat.jdk;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Legacy mods open URL connections (update checks, name generators, wiki downloads) without timeouts, which
 * stalls the loading thread for as long as a dead, firewalled or moved host takes to give up (Ars Magica 2's
 * postInit waited 20 s on such hosts). Connections opened by translated mods get bounded timeouts unless the
 * mod set its own; the mod's usual error path then handles the failure.
 */
public final class NetCompat {
    /** Milliseconds to establish a connection. */
    public static final int CONNECT_TIMEOUT = 3000;
    /** Milliseconds a read may stall. */
    public static final int READ_TIMEOUT = 5000;

    private NetCompat() {}

    /** Milliseconds a host name may take to resolve (the timeouts above do not cover the lookup, which can take 10 s for a dead domain). */
    public static final int RESOLVE_TIMEOUT = 3000;

    /** Hosts that did not resolve in time: later connections fail at once instead of waiting again. */
    private static final Map<String, Boolean> UNRESOLVED = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> RESOLVED = new ConcurrentHashMap<>();

    public static URLConnection openConnection(URL url) throws IOException {
        resolve(url.getHost());
        URLConnection connection = url.openConnection();
        bound(connection);
        return connection;
    }

    public static InputStream openStream(URL url) throws IOException {
        return openConnection(url).getInputStream();
    }

    private static void resolve(String host) throws IOException {
        if (host == null || host.isEmpty() || RESOLVED.containsKey(host)) return;
        if (UNRESOLVED.containsKey(host)) throw new UnknownHostException(host);
        var failure = new AtomicReference<Throwable>();
        Thread lookup = new Thread(() -> {
            try {
                InetAddress.getByName(host);
            } catch (Throwable e) {
                failure.set(e);
            }
        }, "Legacy Connector host lookup " + host);
        lookup.setDaemon(true);
        lookup.start();
        try {
            lookup.join(RESOLVE_TIMEOUT);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        if (lookup.isAlive() || failure.get() != null) {
            UNRESOLVED.put(host, Boolean.TRUE);
            throw new UnknownHostException(host);
        }
        RESOLVED.put(host, Boolean.TRUE);
    }

    private static void bound(URLConnection connection) {
        if (!connection.getURL().getProtocol().startsWith("http")) return;
        if (connection.getConnectTimeout() == 0) connection.setConnectTimeout(CONNECT_TIMEOUT);
        if (connection.getReadTimeout() == 0) connection.setReadTimeout(READ_TIMEOUT);
    }
}
