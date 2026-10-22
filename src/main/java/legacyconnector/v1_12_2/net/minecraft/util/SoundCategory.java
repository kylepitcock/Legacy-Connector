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
}
