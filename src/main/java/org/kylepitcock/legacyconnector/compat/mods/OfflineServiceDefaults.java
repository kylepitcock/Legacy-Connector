package org.kylepitcock.legacyconnector.compat.mods;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * MOD-SPECIFIC (Crafting Dead / Crafting Dead Origins 1.2.8 to 1.3.5, mod id {@code craftingdead}): the mod downloads its service
 * settings ({@code craftingdead/craftingdead.json}: master server, resource packs, website links) from a host that no longer exists
 * and its client preInit then dies reading them. The builds read different keys ({@code master-server-host/port} in 1.3.5,
 * {@code network-address}, {@code remote} and a second pack {@code team} in 1.2.8 and 1.3.0), so the seed is the union of every key
 * any build asks for, each with a value of the type the build reads it as. Keys missing from an existing file are added too (a file
 * seeded for another build), keys present are never touched. As in Forge the download is still tried first. The mod compares the
 * SHA-1 of its local pack files with the settings' hashes and only downloads (opening a Swing progress window a headless-AWT game
 * cannot show) when they differ, so empty placeholder packs ({@code resourcepacks/craftingdead.zip}, {@code craftingdead/team.json})
 * and their hashes are seeded. Remove this class and its one call in {@code Platform1710.preInit} to drop the fix.
 */
public final class OfflineServiceDefaults {
    static final String MOD = "craftingdead";

    private OfflineServiceDefaults() {}

    /** Called before the mod's preInit; {@code gameDir} is the directory the mod resolves {@code user.dir} to. */
    public static void beforePreInit(String modId, Path gameDir) {
        if (!MOD.equals(modId)) return;
        try {
            Path dir = gameDir.resolve("craftingdead");
            Path file = dir.resolve("craftingdead.json");
            JsonObject have = new JsonObject();
            if (Files.exists(file)) {
                try {
                    have = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
                } catch (RuntimeException e) {
                    return; // not ours to repair
                }
            }
            Path pack = gameDir.resolve("resourcepacks").resolve("craftingdead.zip");
            Path team = dir.resolve("team.json");
            if (!Files.exists(pack)) {
                java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
                try (java.util.zip.ZipOutputStream zip = new java.util.zip.ZipOutputStream(bytes)) {
                    zip.putNextEntry(new java.util.zip.ZipEntry("pack.mcmeta"));
                    zip.write("{\"pack\":{\"pack_format\":1,\"description\":\"Crafting Dead offline placeholder\"}}".getBytes(StandardCharsets.UTF_8));
                    zip.closeEntry();
                }
                Files.createDirectories(pack.getParent());
                Files.write(pack, bytes.toByteArray());
            }
            if (!Files.exists(team)) {
                Files.createDirectories(dir);
                Files.writeString(team, "[]", StandardCharsets.UTF_8); // 1.2.8 parses its team list as a JSON array
            }
            String url = "http://127.0.0.1:9/";
            JsonObject defaults = new JsonObject();
            defaults.addProperty("master-server-host", "localhost");
            defaults.addProperty("master-server-port", 1);
            defaults.addProperty("network-address", "localhost:1");
            defaults.addProperty("remote", url + "craftingdead.json");
            defaults.addProperty("display-presents", false);
            defaults.add("resource-pack", packEntry(url + "craftingdead.zip", pack));
            defaults.add("team", packEntry(url + "team.json", team));
            defaults.add("servers", new JsonObject());
            defaults.add("capes", new JsonObject());
            for (String k : new String[] {"website", "store", "maps", "discord"}) defaults.addProperty(k, "http://localhost/");
            boolean changed = !Files.exists(file);
            for (var e : defaults.entrySet()) {
                if (!have.has(e.getKey())) {
                    have.add(e.getKey(), e.getValue());
                    changed = true;
                }
            }
            if (changed) {
                Files.createDirectories(dir);
                Files.writeString(file, new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(have), StandardCharsets.UTF_8);
            }
        } catch (IOException | java.security.NoSuchAlgorithmException e) {
            // the mod then fails as it did before
        }
    }

    private static JsonObject packEntry(String url, Path local) throws IOException, java.security.NoSuchAlgorithmException {
        StringBuilder hash = new StringBuilder();
        for (byte b : java.security.MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(local))) hash.append(String.format("%02x", b));
        JsonObject o = new JsonObject();
        o.addProperty("url", url);
        o.addProperty("hash", hash.toString());
        return o;
    }
}
