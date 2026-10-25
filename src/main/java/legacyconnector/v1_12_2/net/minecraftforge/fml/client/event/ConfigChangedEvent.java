package legacyconnector.v1_12_2.net.minecraftforge.fml.client.event;

import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/** Posted when a mod's config changed (its config GUI, or mods' own reload commands). */
public class ConfigChangedEvent extends Event {
    private final String modID;
    private final String configID;
    private final boolean isWorldRunning;
    private final boolean requiresMcRestart;

    public ConfigChangedEvent(String modID, String configID, boolean isWorldRunning, boolean requiresMcRestart) {
        this.modID = modID;
        this.configID = configID;
        this.isWorldRunning = isWorldRunning;
        this.requiresMcRestart = requiresMcRestart;
    }

    public String getModID() {
        return modID;
    }

    public String getConfigID() {
        return configID;
    }

    public boolean isWorldRunning() {
        return isWorldRunning;
    }

    public boolean isRequiresMcRestart() {
        return requiresMcRestart;
    }

    public static class OnConfigChangedEvent extends ConfigChangedEvent {
        public OnConfigChangedEvent(String modID, String configID, boolean isWorldRunning, boolean requiresMcRestart) {
            super(modID, configID, isWorldRunning, requiresMcRestart);
        }
    }

    public static class PostConfigChangedEvent extends ConfigChangedEvent {
        public PostConfigChangedEvent(String modID, String configID, boolean isWorldRunning, boolean requiresMcRestart) {
            super(modID, configID, isWorldRunning, requiresMcRestart);
        }
    }
}
