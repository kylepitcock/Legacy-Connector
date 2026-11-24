package org.kylepitcock.legacyconnector.core.report;

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * The bug report bundle behind {@code /legacyconnector report}: a zip of the logs and the Legacy Connector reports with the user's home
 * directory, user name and session tokens removed. No Minecraft types here, so it is unit-testable; {@link BugReport} gathers the
 * game-side facts (summary text, file locations).
 */
public final class BugReportBundle {
    /** Largest size of a text file kept whole; a bigger file keeps its head and its tail with a marker between. */
    public static final long DEFAULT_CAP = 8L * 1024 * 1024;

    private BugReportBundle() {}

    // ------------------------------------------------------------------ redaction

    private static final String BAD = "[^\\\\/\\s\"'<>|:*?]+";
    private static final Pattern WIN_USER = Pattern.compile("(?i)[A-Za-z]:(?:\\\\+|/+)Users(?:\\\\+|/+)" + BAD);
    private static final Pattern MAC_USER = Pattern.compile("/Users/" + BAD);
    private static final Pattern LINUX_USER = Pattern.compile("/home/" + BAD);
    private static final Pattern LAUNCH_ARG = Pattern.compile(
            "(?i)(--(?:accessToken|session|sessionId|uuid|xuid|clientId|userProperties|profileProperties)[ =]+)\\S+");
    private static final Pattern TOKEN_FIELD = Pattern.compile(
            "(?i)((?:access[_-]?token|session[_-]?id|session[_-]?token|auth[_-]?token|refresh[_-]?token|client[_-]?secret)[\"']?\\s*[:=]\\s*[\"']?)[A-Za-z0-9._~+/=:-]{8,}");
    private static final Pattern SESSION_TOKEN = Pattern.compile("(?i)token:[A-Za-z0-9._-]{16,}(?::[0-9a-f-]{8,})?");
    private static final Pattern BEARER = Pattern.compile("(?i)(bearer\\s+)[A-Za-z0-9._~+/=-]{16,}");
    private static final Pattern SETTING_USER = Pattern.compile("(?i)(Setting user:\\s*)\\S+");

    /** Removes personal data from log and report text. */
    public static final class Redactor {
        private final List<String> homes = new ArrayList<>();
        private final Pattern userPattern;

        /** @param homes home directories to replace literally (any slash style); @param user the OS user name, or null */
        public Redactor(List<String> homes, String user) {
            Set<String> seen = new LinkedHashSet<>();
            for (String h : homes) {
                if (h == null || h.isBlank() || h.length() < 4) continue;
                String t = h.trim();
                seen.add(t);
                seen.add(t.replace('\\', '/'));
                seen.add(t.replace('/', '\\'));
                seen.add(t.replace("\\", "\\\\"));
            }
            this.homes.addAll(seen);
            this.homes.sort((a, b) -> b.length() - a.length());
            this.userPattern = user == null || user.length() < 3 ? null
                    : Pattern.compile("(?<![A-Za-z0-9_])" + Pattern.quote(user) + "(?![A-Za-z0-9_])");
        }

        public String redact(String text) {
            String s = text;
            for (String h : homes) s = s.replace(h, "<home>");
            s = WIN_USER.matcher(s).replaceAll("<home>");
            s = MAC_USER.matcher(s).replaceAll("<home>");
            s = LINUX_USER.matcher(s).replaceAll("<home>");
            if (userPattern != null) s = userPattern.matcher(s).replaceAll("<user>");
            s = LAUNCH_ARG.matcher(s).replaceAll("$1<redacted>");
            s = TOKEN_FIELD.matcher(s).replaceAll("$1<redacted>");
            s = SESSION_TOKEN.matcher(s).replaceAll("token:<redacted>");
            s = BEARER.matcher(s).replaceAll("$1<redacted>");
            s = SETTING_USER.matcher(s).replaceAll("$1<player>");
            return s;
        }

        /** The default redactor: this machine's home directory and user name. */
        public static Redactor ofSystem() {
            return new Redactor(List.of(System.getProperty("user.home", ""), System.getenv().getOrDefault("USERPROFILE", ""),
                    System.getenv().getOrDefault("HOME", "")), System.getProperty("user.name"));
        }
    }

    // ------------------------------------------------------------------ size cap

    /** The file's text, or its first and last {@code cap/2} bytes with a marker line when it is larger than {@code cap}. */
    public static String readCapped(Path file, long cap) throws IOException {
        long size = Files.size(file);
        if (size <= cap) return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        long half = Math.max(1, cap / 2);
        byte[] head = new byte[(int) half];
        byte[] tail = new byte[(int) half];
        try (RandomAccessFile f = new RandomAccessFile(file.toFile(), "r")) {
            f.readFully(head);
            f.seek(size - half);
            f.readFully(tail);
        }
        long cut = size - 2 * half;
        String nl = System.lineSeparator();
        return new String(head, StandardCharsets.UTF_8) + nl + "... [Legacy Connector bundle: " + cut
                + " bytes omitted from the middle of this " + size + " byte file] ..." + nl
                + new String(tail, StandardCharsets.UTF_8);
    }

    // ------------------------------------------------------------------ zip

    /**
     * Writes {@code <outDir>/legacyconnector-bundle-<stamp>.zip} with {@code summary.txt} and each existing file (entry name = file name),
     * every text capped and redacted.
     *
     * @return the zip path
     */
    public static Path write(Path outDir, String stamp, String summary, List<Path> files, Redactor redactor, long cap) throws IOException {
        Files.createDirectories(outDir);
        Path zip = outDir.resolve("legacyconnector-bundle-" + stamp + ".zip");
        Set<String> names = new LinkedHashSet<>();
        try (OutputStream os = Files.newOutputStream(zip); ZipOutputStream out = new ZipOutputStream(os, StandardCharsets.UTF_8)) {
            put(out, "summary.txt", redactor.redact(summary));
            names.add("summary.txt");
            for (Path f : files) {
                if (f == null || !Files.isRegularFile(f)) continue;
                String name = f.getFileName().toString();
                if (!names.add(name)) continue;
                String text;
                try {
                    text = readCapped(f, cap);
                } catch (IOException e) {
                    text = "Could not read " + name + ": " + e;
                }
                put(out, name, redactor.redact(text));
            }
        }
        return zip;
    }

    private static void put(ZipOutputStream out, String name, String text) throws IOException {
        out.putNextEntry(new ZipEntry(name));
        out.write(text.getBytes(StandardCharsets.UTF_8));
        out.closeEntry();
    }
}
