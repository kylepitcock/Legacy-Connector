package org.kylepitcock.legacyconnector.loader.tools;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** A truncated archive in the jar set is reported as skipped and the other jars are still checked. */
class CorruptArchiveTest {
    @TempDir Path temp;

    @Test
    void truncatedZipIsSkippedWithAReasonAndTheRestContinues() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream z = new ZipOutputStream(bytes)) {
            z.putNextEntry(new ZipEntry("readme.txt"));
            z.write("not a mod".repeat(200).getBytes());
            z.closeEntry();
        }
        byte[] whole = bytes.toByteArray();
        Path good = temp.resolve("plain.jar");
        Files.write(good, whole);
        Path truncated = temp.resolve("truncated.jar");
        Files.write(truncated, java.util.Arrays.copyOf(whole, whole.length / 2));   // the central directory (END header) is gone
        Path index = temp.resolve("shim-index.txt");
        Files.writeString(index, "");
        Path json = temp.resolve("out.json");
        System.setProperty("legacyconnector.check.json", json.toString());
        try {
            CheckJar.main(new String[] {index.toString(), truncated.toString(), good.toString()});
        } finally {
            System.clearProperty("legacyconnector.check.json");
        }
        JsonArray records = JsonParser.parseString(Files.readString(json)).getAsJsonArray();
        assertEquals(2, records.size());
        var first = records.get(0).getAsJsonObject();
        assertEquals("truncated.jar", first.get("jar").getAsString());
        assertEquals("skipped", first.get("result").getAsString());
        assertTrue(first.get("reason").getAsString().contains("unreadable archive"), first.get("reason").getAsString());
        assertEquals("not-legacy", records.get(1).getAsJsonObject().get("result").getAsString());
    }
}
