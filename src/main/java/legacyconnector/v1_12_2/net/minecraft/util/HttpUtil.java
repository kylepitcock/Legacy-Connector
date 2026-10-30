package legacyconnector.v1_12_2.net.minecraft.util;

/** 1.12's HTTP helpers: the pool resource packs and other downloads run on. */
public class HttpUtil {
    public static final com.google.common.util.concurrent.ListeningExecutorService DOWNLOADER_EXECUTOR = com.google.common.util.concurrent.MoreExecutors
            .listeningDecorator(java.util.concurrent.Executors.newCachedThreadPool(new com.google.common.util.concurrent.ThreadFactoryBuilder()
                    .setDaemon(true).setNameFormat("Downloader %d").build()));

    /** A free local port for LAN games (a socket bound to port 0 gets one), 25564 when that fails. */
    public static int getSuitableLanPort() {
        try (var socket = new java.net.ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (java.io.IOException e) {
            return 25564;
        }
    }
}
