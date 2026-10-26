package legacyconnector.v1_7_10.net.minecraft.world;

/** World creation settings (mods build them for their own fake or proxy worlds, and for the game type enum). */
public class WorldSettings {
    public long seed;
    public GameType theGameType;
    public boolean mapFeaturesEnabled;
    public boolean hardcoreEnabled;
    public WorldType terrainType;
    public boolean commandsAllowed;
    public boolean bonusChestEnabled;
    /** The generator options (flat preset, customized settings). */
    public String field_82751_h = "";

    public WorldSettings() {
        this(0L, GameType.SURVIVAL, true, false, WorldType.DEFAULT);
    }

    public WorldSettings(long seed, GameType gameType, boolean mapFeatures, boolean hardcore, WorldType type) {
        this.seed = seed;
        this.theGameType = gameType;
        this.mapFeaturesEnabled = mapFeatures;
        this.hardcoreEnabled = hardcore;
        this.terrainType = type;
    }

    /** The settings a saved world was created with. */
    public WorldSettings(legacyconnector.v1_7_10.net.minecraft.world.storage.WorldInfo info) {
        this(info.getSeed(), info.getGameType(), info.isMapFeaturesEnabled(), info.isHardcoreModeEnabled(), info.getTerrainType());
    }

    public WorldSettings enableBonusChest() {
        this.bonusChestEnabled = true;
        return this;
    }

    public WorldSettings enableCommands() {
        this.commandsAllowed = true;
        return this;
    }

    /** Sets the generator options (1.7.10 named it after the world name field it once was). */
    public WorldSettings setWorldName(String options) {
        this.field_82751_h = options;
        return this;
    }

    public boolean isBonusChestEnabled() {
        return bonusChestEnabled;
    }

    public long getSeed() {
        return seed;
    }

    public GameType getGameType() {
        return theGameType;
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

    public String func_82749_j() {
        return field_82751_h;
    }

    public enum GameType {
        NOT_SET(-1, ""),
        SURVIVAL(0, "survival"),
        CREATIVE(1, "creative"),
        ADVENTURE(2, "adventure");

        private final int id;
        private final String name;

        GameType(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getID() {
            return id;
        }

        public String getName() {
            return name;
        }

        public void configurePlayerCapabilities(legacyconnector.v1_7_10.net.minecraft.entity.player.PlayerCapabilities capabilities) {
            legacyconnector.v1_7_10.net.minecraft.entity.player.PlayerCapabilities.mcc$set$allowFlying(capabilities, this == CREATIVE);
            legacyconnector.v1_7_10.net.minecraft.entity.player.PlayerCapabilities.mcc$set$isCreativeMode(capabilities, this == CREATIVE);
            legacyconnector.v1_7_10.net.minecraft.entity.player.PlayerCapabilities.mcc$set$disableDamage(capabilities, this == CREATIVE);
            if (this != CREATIVE) legacyconnector.v1_7_10.net.minecraft.entity.player.PlayerCapabilities.mcc$set$isFlying(capabilities, false);
            legacyconnector.v1_7_10.net.minecraft.entity.player.PlayerCapabilities.mcc$set$allowEdit(capabilities, this != ADVENTURE);
        }

        public boolean isAdventure() {
            return this == ADVENTURE;
        }

        public boolean isCreative() {
            return this == CREATIVE;
        }

        public boolean isSurvivalOrAdventure() {
            return this == SURVIVAL || this == ADVENTURE;
        }

        public static GameType getByID(int id) {
            for (GameType t : values()) if (t.id == id) return t;
            return SURVIVAL;
        }

        public static GameType getByName(String name) {
            for (GameType t : values()) if (t.name.equals(name)) return t;
            return SURVIVAL;
        }
    }
}
