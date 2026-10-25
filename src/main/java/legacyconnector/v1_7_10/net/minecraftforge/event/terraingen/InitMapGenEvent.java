package legacyconnector.v1_7_10.net.minecraftforge.event.terraingen;

import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event;
import legacyconnector.v1_7_10.net.minecraft.world.gen.MapGenBase;

/** A world generator is about to create one of its structure or carver generators: handlers may substitute {@link #newGen}. */
public class InitMapGenEvent extends Event {
    public enum EventType {
        CAVE, MINESHAFT, NETHER_BRIDGE, NETHER_CAVE, RAVINE, SCATTERED_FEATURE, STRONGHOLD, VILLAGE, CUSTOM
    }

    public final EventType type;
    public final MapGenBase originalGen;
    public MapGenBase newGen;

    public InitMapGenEvent(EventType type, MapGenBase original) {
        this.type = type;
        this.originalGen = original;
        this.newGen = original;
    }
}
