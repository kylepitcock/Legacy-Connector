package legacyconnector.v1_7_10.cpw.mods.fml.relauncher;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/** FML's file-list helper: files with the same content (by MD5) are kept once, and the list is sorted. */
public class FileListHelper {
    public static File[] sortFileList(File dir, FilenameFilter filter) {
        return dir == null ? null : sortFileList(dir.listFiles(filter));
    }

    public static File[] sortFileList(File[] files) {
        if (files == null) return null;
        Map<String, File> byHash = new LinkedHashMap<>();
        for (File file : files) {
            try {
                String hash = md5(file);
                File first = byHash.putIfAbsent(hash, file);
                if (first != null) {
                    org.slf4j.LoggerFactory.getLogger("LegacyConnector/FML").debug("{} duplicates {}; using {}", file.getName(), first.getName(), first.getName());
                }
            } catch (IOException e) {
                // unreadable: left out, as FML did
            }
        }
        File[] result = byHash.values().toArray(new File[0]);
        Arrays.sort(result);
        return result;
    }

    private static String md5(File file) throws IOException {
        try (InputStream in = Files.newInputStream(file.toPath())) {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) > 0) digest.update(buffer, 0, n);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest.digest()) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
    }
}
