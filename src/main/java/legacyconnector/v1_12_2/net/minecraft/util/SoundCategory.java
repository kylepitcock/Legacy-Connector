package legacyconnector.v1_12_2.net.minecraft.util;

public enum SoundCategory {
    MASTER("master"), MUSIC("music"), RECORDS("record"), WEATHER("weather"), BLOCKS("block"), HOSTILE("hostile"),
    NEUTRAL("neutral"), PLAYERS("player"), AMBIENT("ambient"), VOICE("voice");

    private final String name;

    SoundCategory(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    private static final java.util.Map<String, SoundCategory> BY_NAME = new java.util.HashMap<>();

    static {
        for (SoundCategory c : values()) BY_NAME.put(c.name, c);
    }

    public static SoundCategory getByName(String name) {
        return BY_NAME.get(name);
    }

    public static java.util.Set<String> getSoundCategoryNames() {
        return BY_NAME.keySet();
    }

    /** The modern sound source (same constants under the same names). */
    public net.minecraft.sounds.SoundSource mcc$modern() {
        return net.minecraft.sounds.SoundSource.valueOf(name());
    }

    public static SoundCategory mcc$of(net.minecraft.sounds.SoundSource source) {
        return source == null ? MASTER : valueOf(source.name());
    }
}
