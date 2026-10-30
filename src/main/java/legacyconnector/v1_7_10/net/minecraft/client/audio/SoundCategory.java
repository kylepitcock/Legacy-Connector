package legacyconnector.v1_7_10.net.minecraft.client.audio;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.sounds.SoundSource;

/** 1.7.10's volume sliders; each is one of the modern sound sources. */
public enum SoundCategory {
    MASTER("master", 0, SoundSource.MASTER),
    MUSIC("music", 1, SoundSource.MUSIC),
    RECORDS("record", 2, SoundSource.RECORDS),
    WEATHER("weather", 3, SoundSource.WEATHER),
    BLOCKS("block", 4, SoundSource.BLOCKS),
    MOBS("hostile", 5, SoundSource.HOSTILE),
    ANIMALS("neutral", 6, SoundSource.NEUTRAL),
    PLAYERS("player", 7, SoundSource.PLAYERS),
    AMBIENT("ambient", 8, SoundSource.AMBIENT);

    private static final Map<String, SoundCategory> BY_NAME = new HashMap<>();
    private static final Map<Integer, SoundCategory> BY_ID = new HashMap<>();
    private final String categoryName;
    private final int categoryId;
    private final SoundSource source;

    static {
        for (SoundCategory c : values()) {
            BY_NAME.put(c.categoryName, c);
            BY_ID.put(c.categoryId, c);
        }
    }

    /** 1.7.10's constructor, which mods call through EnumHelper to add a category (DragonAPI: ChromatiCraft's music); it plays as master. */
    SoundCategory(String name, int id) {
        this(name, id, SoundSource.MASTER);
    }

    SoundCategory(String name, int id, SoundSource source) {
        this.categoryName = name;
        this.categoryId = id;
        this.source = source;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public int getCategoryId() {
        return categoryId;
    }

    /** The category by its sounds.json name ("master", "block", ...). */
    public static SoundCategory func_147154_a(String name) {
        SoundCategory c = BY_NAME.get(name);
        if (c != null) return c;
        // categories mods added at runtime (EnumHelper) are only in values()
        for (SoundCategory v : values()) if (v.categoryName.equals(name)) return v;
        return null;
    }

    public SoundSource mcc$source() {
        return source;
    }

    public static SoundCategory mcc$of(SoundSource source) {
        for (SoundCategory c : values()) if (c.source == source) return c;
        return MASTER;
    }
}
