package org.kylepitcock.legacyconnector.core.report;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.kylepitcock.legacyconnector.core.report.BugReportBundle.Redactor;

class BugReportBundleTest {
    @TempDir Path tmp;

    private final Redactor redactor = new Redactor(List.of("C:\\Users\\Jane Doe", "/Users/jane"), "janed");

    @Test
    void redactsHomeAndUserInAllPathForms() {
        String in = "a C:\\Users\\Jane Doe\\AppData\\x b C:\\Users\\other\\y c C:/Users/bob/z d /Users/someone/Library e /home/linus/.minecraft f janed g notjaned"
                + " h C:\\\\Users\\\\esc\\\\p";
        String out = redactor.redact(in);
        assertFalse(out.contains("Jane Doe"), out);
        assertFalse(out.contains("other"), out);
        assertFalse(out.contains("bob"), out);
        assertFalse(out.contains("someone"), out);
        assertFalse(out.contains("linus"), out);
        assertFalse(out.contains("esc"), out);
        assertTrue(out.contains("<home>\\AppData\\x"), out);
        assertTrue(out.contains("f <user>"), out);
        assertTrue(out.contains("g notjaned"), "a longer word is not the user name: " + out);
    }

    @Test
    void stripsTokensFromLaunchArguments() {
        String out = redactor.redact("Launched with --username Steve --accessToken eyJhbGciOi.secret.value --uuid 1234-abcd-5678 --version 1.21.1"
                + "\nSetting user: Steve\nsession token:abcdef0123456789abcdef:1234abcd-0000\nAuthorization: Bearer abcdefghijklmnop12345");
        assertFalse(out.contains("eyJhbGciOi"), out);
        assertFalse(out.contains("1234-abcd-5678"), out);
        assertFalse(out.contains("abcdef0123456789abcdef"), out);
        assertFalse(out.contains("abcdefghijklmnop12345"), out);
        assertTrue(out.contains("--version 1.21.1"), out);
        assertTrue(out.contains("Setting user: <player>"), out);
    }

    @Test
    void largeFileKeepsHeadAndTail() throws IOException {
        Path f = tmp.resolve("big.log");
        StringBuilder sb = new StringBuilder("HEAD-MARK\n");
        for (int i = 0; i < 2000; i++) sb.append("line ").append(i).append('\n');
        sb.append("TAIL-MARK\n");
        Files.writeString(f, sb.toString());
        String capped = BugReportBundle.readCapped(f, 1000);
        assertTrue(capped.length() < 1500, "length " + capped.length());
        assertTrue(capped.startsWith("HEAD-MARK"));
        assertTrue(capped.trim().endsWith("TAIL-MARK"));
        assertTrue(capped.contains("bytes omitted"));
        assertEquals(sb.toString(), BugReportBundle.readCapped(f, 1 << 20), "a small file is kept whole");
    }

    @Test
    void zipHasSummaryAndRedactedExistingFilesOnly() throws IOException {
        Path log = tmp.resolve("latest.log");
        Files.writeString(log, "path C:\\Users\\Jane Doe\\.minecraft ok\n");
        Path missing = tmp.resolve("debug.log");
        Path report = tmp.resolve("legacyconnector-report.txt");
        Files.writeString(report, "report /Users/jane/mods");
        Path zip = BugReportBundle.write(tmp.resolve("out"), "20260101-120000", "Legacy Connector: 1.0 user janed", List.of(log, missing, report), redactor, 1 << 20);
        assertEquals("legacyconnector-bundle-20260101-120000.zip", zip.getFileName().toString());
        Map<String, String> entries = new HashMap<>();
        try (ZipInputStream in = new ZipInputStream(Files.newInputStream(zip))) {
            for (ZipEntry e; (e = in.getNextEntry()) != null; ) entries.put(e.getName(), new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
        assertEquals(java.util.Set.of("summary.txt", "latest.log", "legacyconnector-report.txt"), entries.keySet());
        assertEquals("Legacy Connector: 1.0 user <user>", entries.get("summary.txt"));
        assertEquals("path <home>\\.minecraft ok\n", entries.get("latest.log"));
        assertEquals("report <home>/mods", entries.get("legacyconnector-report.txt"));
    }
}
