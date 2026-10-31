package org.kylepitcock.legacyconnector.loader;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HexFormat;
import java.util.jar.Attributes;

/**
 * The provenance watermark of a translated jar: which original it was made from, by which translator, and that it is a local derivative
 * that must not be redistributed. Written into the manifest and into {@value #RESOURCE}; neither is read by the game.
 */
public final class Provenance {
    public static final String RESOURCE = "META-INF/legacyconnector/provenance.json";
    public static final String PROJECT_URL = "https://kylepitcock.github.io/Legacy-Connector/";
    public static final String NOTICE = "This file is a machine-translated derivative of the original mod named below. Legacy Connector made it "
            + "locally from the user's own copy of that mod, for that user's own use. It needs Legacy Connector to run and is not the original "
            + "authors' work in this form. It is not meant to be redistributed: share the original mod instead.";

    private Provenance() {}

    /** Version of the translator (Legacy Connector loader), "dev" outside a built jar. */
    public static String translatorVersion() {
        String v = Provenance.class.getPackage() == null ? null : Provenance.class.getPackage().getImplementationVersion();
        return v == null ? "dev" : v;
    }

    public static String sha256Hex(Path file) throws IOException {
        java.security.MessageDigest md = SplitPackageResolver.sha256();
        try (InputStream in = Files.newInputStream(file)) {
            byte[] buf = new byte[1 << 16];
            int n;
            while ((n = in.read(buf)) > 0) md.update(buf, 0, n);
        }
        return HexFormat.of().formatHex(md.digest());
    }

    /** The content of {@link #RESOURCE}. */
    public static byte[] json(LegacyJar jar, String originalSha256, int transformerVersion) {
        JsonObject o = new JsonObject();
        o.addProperty("notice", NOTICE);
        o.addProperty("generator", "Legacy Connector");
        o.addProperty("project", PROJECT_URL);
        o.addProperty("translatorVersion", translatorVersion());
        o.addProperty("transformerVersion", transformerVersion);
        o.addProperty("translatedAt", Instant.now().toString());
        o.addProperty("legacyVersion", jar.version.id());
        o.addProperty("originalFile", jar.fileName());
        o.addProperty("originalSha256", originalSha256);
        JsonArray mods = new JsonArray();
        for (LegacyModDecl m : jar.mods) {
            JsonObject mo = new JsonObject();
            mo.addProperty("id", m.legacyId);
            mo.addProperty("version", m.version);
            mods.add(mo);
        }
        o.add("mods", mods);
        return new GsonBuilder().setPrettyPrinting().create().toJson(o).getBytes(StandardCharsets.UTF_8);
    }

    /** Manifest attributes (single-line, ASCII). */
    public static void addManifest(Attributes attrs, LegacyJar jar, String originalSha256) {
        attrs.putValue("LegacyConnector-Translator-Version", translatorVersion());
        attrs.putValue("LegacyConnector-Original-Sha256", originalSha256);
        attrs.putValue("LegacyConnector-Notice", "Machine-translated derivative made locally for personal use; requires Legacy Connector ("
                + PROJECT_URL + "); not for redistribution.");
    }
}
