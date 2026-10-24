package legacyconnector.v1_7_10.net.minecraft.world;

/** World creation settings; legacy mods only use the game type enum. */
public class WorldSettings {
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
            capabilities.allowFlying = this == CREATIVE;
            capabilities.isCreativeMode = this == CREATIVE;
            capabilities.disableDamage = this == CREATIVE;
            if (this != CREATIVE) capabilities.isFlying = false;
            capabilities.allowEdit = this != ADVENTURE;
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
