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

    /** The modern source (same constants in the same order). */
    public net.minecraft.sounds.SoundSource mcc$modern() {
        return net.minecraft.sounds.SoundSource.values()[ordinal()];
    }

    public static SoundCategory mcc$of(net.minecraft.sounds.SoundSource source) {
        return values()[source.ordinal()];
    }

    public static SoundCategory getByName(String name) {
        for (SoundCategory c : values()) if (c.name.equals(name)) return c;
        return null;
    }


    public static java.util.Set<String> getSoundCategoryNames() {
        java.util.Set<String> names = new java.util.LinkedHashSet<>();
        for (SoundCategory c : values()) names.add(c.getName());
        return names;
    }
}
