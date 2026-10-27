package legacyconnector.v1_12_2.net.minecraftforge.event.terraingen;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.math.ChunkPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event.HasResult;

/** Forge 1.12.2: a legacy chunk generator made one of its map generators (caves, structures); handlers may replace it. */
public class InitMapGenEvent extends Event {
    private final EventType type;
    private final legacyconnector.v1_12_2.net.minecraft.world.gen.MapGenBase originalGen;
    private legacyconnector.v1_12_2.net.minecraft.world.gen.MapGenBase newGen;

    public InitMapGenEvent(EventType type, legacyconnector.v1_12_2.net.minecraft.world.gen.MapGenBase original) {
        this.type = type;
        this.originalGen = original;
        this.newGen = original;
    }

    public EventType getType() {
        return type;
    }

    public legacyconnector.v1_12_2.net.minecraft.world.gen.MapGenBase getOriginalGen() {
        return originalGen;
    }

    public legacyconnector.v1_12_2.net.minecraft.world.gen.MapGenBase getNewGen() {
        return newGen;
    }

    public void setNewGen(legacyconnector.v1_12_2.net.minecraft.world.gen.MapGenBase newGen) {
        this.newGen = newGen;
    }

    public enum EventType {
        CAVE, MINESHAFT, NETHER_BRIDGE, NETHER_CAVE, RAVINE, SCATTERED_FEATURE, STRONGHOLD, VILLAGE, OCEAN_MONUMENT, WOODLAND_MANSION, END_CITY, CUSTOM
    }
}
