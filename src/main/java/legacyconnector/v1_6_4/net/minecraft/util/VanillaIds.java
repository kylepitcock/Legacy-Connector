package legacyconnector.v1_6_4.net.minecraft.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Vanilla Minecraft 1.6.4 numeric id tables, read from the data files under {@code legacyconnector/v1_6_4/}
 * ({@code blocks.tsv}, {@code items.tsv}, {@code misc.tsv}). Pure data: no game classes, so it is testable offline.
 * The 1.7.10 name of an entry is the registry name the 1.7.10 shim (and so {@code BlockBridge}/{@code ItemBridge}) knows it by;
 * the 1.6.4 name is the static field of {@code Block}/{@code Item}/{@code BiomeGenBase}... in 1.6.4 (MCP names).
 */
public final class VanillaIds {
    /** One row. {@code name1710} is {@code null} for an id that is reserved in 1.6.4 but empty in 1.7.10 ("-" in the file). */
    public record Entry(int id, String name164, String name1710) {}

    public static final String BASE = "/legacyconnector/v1_6_4/";

    private static volatile VanillaIds instance;

    public final List<Entry> blocks;
    public final List<Entry> items;
    public final List<Entry> biomes;
    public final List<Entry> potions;
    public final List<Entry> enchantments;
    public final List<Entry> entities;

    private final Map<String, Entry> blockByField = new HashMap<>();
    private final Map<String, Entry> blockByName = new HashMap<>();
    private final Map<String, Entry> itemByField = new HashMap<>();
    private final Map<String, Entry> itemByName = new HashMap<>();

    private VanillaIds() {
        List<Entry> b = new ArrayList<>(), i = new ArrayList<>(), bi = new ArrayList<>(), p = new ArrayList<>(), e = new ArrayList<>(), en = new ArrayList<>();
        for (String[] r : read("blocks.tsv")) b.add(entry(r, 0));
        for (String[] r : read("items.tsv")) i.add(entry(r, 0));
        for (String[] r : read("misc.tsv")) {
            Entry x = entry(r, 1);
            switch (r[0]) {
                case "biome" -> bi.add(x);
                case "potion" -> p.add(x);
                case "enchantment" -> e.add(x);
                case "entity" -> en.add(x);
                default -> throw new IllegalStateException("misc.tsv: unknown kind " + r[0]);
            }
        }
        blocks = Collections.unmodifiableList(b);
        items = Collections.unmodifiableList(i);
        biomes = Collections.unmodifiableList(bi);
        potions = Collections.unmodifiableList(p);
        enchantments = Collections.unmodifiableList(e);
        entities = Collections.unmodifiableList(en);
        for (Entry x : b) {
            blockByField.put(x.name164, x);
            if (x.name1710 != null) blockByName.put(x.name1710, x);
        }
        for (Entry x : i) {
            itemByField.put(x.name164, x);
            itemByName.put(x.name1710, x);
        }
    }

    public static VanillaIds get() {
        VanillaIds v = instance;
        if (v == null) {
            synchronized (VanillaIds.class) {
                if ((v = instance) == null) instance = v = new VanillaIds();
            }
        }
        return v;
    }

    /** Row for {@code Block.<field>} of 1.6.4, or null. */
    public Entry blockByField(String field164) { return blockByField.get(field164); }

    /** Row for the 1.7.10 registry name (without "minecraft:"), or null. */
    public Entry blockByName(String name1710) { return blockByName.get(stripNs(name1710)); }

    /** Row for {@code Item.<field>} of 1.6.4, or null. */
    public Entry itemByField(String field164) { return itemByField.get(field164); }

    public Entry itemByName(String name1710) { return itemByName.get(stripNs(name1710)); }

    /** Vanilla block id of the 1.7.10 name, or -1 (e.g. stained_glass, which only exists in 1.7). */
    public int blockId(String name1710) {
        Entry e = blockByName(name1710);
        return e == null ? -1 : e.id;
    }

    public int itemId(String name1710) {
        Entry e = itemByName(name1710);
        if (e != null) return e.id;
        return blockId(name1710);
    }

    private static String stripNs(String n) {
        return n != null && n.startsWith("minecraft:") ? n.substring(10) : n;
    }

    private static Entry entry(String[] r, int off) {
        String n = r[off + 2].equals("-") ? null : r[off + 2];
        return new Entry(Integer.parseInt(r[off].trim()), r[off + 1], n);
    }

    private static List<String[]> read(String file) {
        List<String[]> rows = new ArrayList<>();
        try (InputStream in = VanillaIds.class.getResourceAsStream(BASE + file)) {
            if (in == null) throw new IllegalStateException("missing resource " + BASE + file);
            BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) continue;
                String[] c = line.split("\t");
                if (c.length < 3) throw new IllegalStateException(file + ": bad row '" + line + "'");
                rows.add(c);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return rows;
    }
}
