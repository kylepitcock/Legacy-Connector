package legacyconnector.v1_12_2.net.minecraft.world;

/** 1.12.2 game modes (the modern {@link net.minecraft.world.level.GameType} without NOT_SET). */
public enum GameType {
    NOT_SET(-1, "", ""), SURVIVAL(0, "survival", "s"), CREATIVE(1, "creative", "c"), ADVENTURE(2, "adventure", "a"), SPECTATOR(3, "spectator", "sp");

    private final int id;
    private final String name;
    private final String shortName;

    GameType(int id, String name, String shortName) {
        this.id = id;
        this.name = name;
        this.shortName = shortName;
    }

    public int getID() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean hasLimitedInteractions() {
        return this == ADVENTURE || this == SPECTATOR;
    }

    public boolean isCreative() {
        return this == CREATIVE;
    }

    public boolean isSurvivalOrAdventure() {
        return this == SURVIVAL || this == ADVENTURE;
    }

    public static GameType getByID(int id) {
        return parseGameTypeWithDefault(id, SURVIVAL);
    }

    public static GameType parseGameTypeWithDefault(int id, GameType fallback) {
        for (GameType t : values()) if (t.id == id) return t;
        return fallback;
    }

    public static GameType getByName(String name) {
        return parseGameTypeWithDefault(name, SURVIVAL);
    }

    public static GameType parseGameTypeWithDefault(String name, GameType fallback) {
        for (GameType t : values()) if (t.name.equals(name) || t.shortName.equals(name)) return t;
        return fallback;
    }

    public net.minecraft.world.level.GameType mcc$modern() {
        return switch (this) {
            case CREATIVE -> net.minecraft.world.level.GameType.CREATIVE;
            case ADVENTURE -> net.minecraft.world.level.GameType.ADVENTURE;
            case SPECTATOR -> net.minecraft.world.level.GameType.SPECTATOR;
            default -> net.minecraft.world.level.GameType.SURVIVAL;
        };
    }

    public static GameType mcc$of(net.minecraft.world.level.GameType modern) {
        return modern == null ? NOT_SET : getByID(modern.getId());
    }
}
