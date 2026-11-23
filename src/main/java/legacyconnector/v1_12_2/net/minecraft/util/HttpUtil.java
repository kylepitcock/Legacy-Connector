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

    // ---- gap batch (kept together at the end)

    /**
     * Downloads a resource pack to {@code saveFile} on the downloader pool (1.12's server resource pack download). Fails the future when the
     * response is larger than {@code maxSize} bytes or the request fails; the progress callback shows the loading string and the percentage.
     */
    public static com.google.common.util.concurrent.ListenableFuture<Object> downloadResourcePack(java.io.File saveFile, String packUrl,
            java.util.Map<String, String> requestProperties, int maxSize, IProgressUpdate progress, java.net.Proxy proxy) {
        return DOWNLOADER_EXECUTOR.submit(() -> {
            java.net.HttpURLConnection connection = null;
            try {
                if (progress != null) {
                    progress.resetProgressAndMessage("Downloading Resource Pack");
                    progress.displayLoadingString("Making request...");
                }
                connection = (java.net.HttpURLConnection) (proxy == null ? new java.net.URL(packUrl).openConnection() : new java.net.URL(packUrl).openConnection(proxy));
                if (requestProperties != null) requestProperties.forEach(connection::setRequestProperty);
                long length = connection.getContentLengthLong();
                if (maxSize > 0 && length > maxSize) throw new java.io.IOException("Filesize is bigger than maximum allowed (file is " + length + ", limit is " + maxSize + ")");
                if (connection.getResponseCode() >= 400) throw new java.io.IOException("Server returned HTTP " + connection.getResponseCode());
                if (saveFile.getParentFile() != null) saveFile.getParentFile().mkdirs();
                long total = 0;
                try (var in = connection.getInputStream(); var out = new java.io.FileOutputStream(saveFile)) {
                    byte[] buf = new byte[4096];
                    int n;
                    while ((n = in.read(buf)) != -1) {
                        total += n;
                        if (maxSize > 0 && total > maxSize) throw new java.io.IOException("Filesize was bigger than maximum allowed (got >= " + total + ", limit was " + maxSize + ")");
                        out.write(buf, 0, n);
                        if (progress != null && length > 0) progress.setLoadingProgress((int) (total * 100L / length));
                    }
                }
                if (progress != null) progress.setDoneWorking();
                return null;
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }
}
