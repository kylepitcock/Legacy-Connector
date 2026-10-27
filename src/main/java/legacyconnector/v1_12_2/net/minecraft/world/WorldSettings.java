package legacyconnector.v1_12_2.net.minecraft.world;

/** The settings a 1.12.2 world is created with. */
public final class WorldSettings {
    private final long seed;
    private final GameType gameType;
    private final boolean mapFeaturesEnabled;
    private final boolean hardcoreEnabled;
    private final WorldType terrainType;
    private boolean commandsAllowed;
    private boolean bonusChestEnabled;
    private String generatorOptions = "";

    public WorldSettings(long seed, GameType gameType, boolean mapFeaturesEnabled, boolean hardcoreEnabled, WorldType terrainType) {
        this.seed = seed;
        this.gameType = gameType;
        this.mapFeaturesEnabled = mapFeaturesEnabled;
        this.hardcoreEnabled = hardcoreEnabled;
        this.terrainType = terrainType;
    }

    public WorldSettings(legacyconnector.v1_12_2.net.minecraft.world.storage.WorldInfo info) {
        this(info.getSeed(), info.getGameType(), info.isMapFeaturesEnabled(), info.isHardcoreModeEnabled(), info.getTerrainType());
    }

    public WorldSettings enableBonusChest() {
        bonusChestEnabled = true;
        return this;
    }

    public WorldSettings enableCommands() {
        commandsAllowed = true;
        return this;
    }

    public WorldSettings setGeneratorOptions(String options) {
        generatorOptions = options;
        return this;
    }

    public boolean isBonusChestEnabled() {
        return bonusChestEnabled;
    }

    public long getSeed() {
        return seed;
    }

    public GameType getGameType() {
        return gameType;
    }

    public boolean getHardcoreEnabled() {
        return hardcoreEnabled;
    }

    public boolean isMapFeaturesEnabled() {
        return mapFeaturesEnabled;
    }

    public WorldType getTerrainType() {
        return terrainType;
    }

    public boolean areCommandsAllowed() {
        return commandsAllowed;
    }

    public static GameType getGameTypeById(int id) {
        return GameType.getByID(id);
    }

    public String getGeneratorOptions() {
        return generatorOptions;
    }
}
