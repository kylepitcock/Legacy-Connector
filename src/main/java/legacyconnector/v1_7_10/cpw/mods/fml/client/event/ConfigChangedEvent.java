package legacyconnector.v1_7_10.cpw.mods.fml.client.event;

import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event;

/** A mod's config was changed (by its config screen); listeners reload what they cached. */
public class ConfigChangedEvent extends Event {
    public final String modID;
    public final String configID;
    public final boolean isWorldRunning;
    public final boolean requiresMcRestart;

    public ConfigChangedEvent(String modID, String configID, boolean isWorldRunning, boolean requiresMcRestart) {
        this.modID = modID;
        this.configID = configID;
        this.isWorldRunning = isWorldRunning;
        this.requiresMcRestart = requiresMcRestart;
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
