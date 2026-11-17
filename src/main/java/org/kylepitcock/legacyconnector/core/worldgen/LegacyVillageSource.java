package org.kylepitcock.legacyconnector.core.worldgen;

import java.util.List;
import java.util.Random;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;

/**
 * What {@link VillageAdditions} needs from one legacy version to add that version's village buildings (Forge village creation
 * handlers) to the vanilla villages: the version's handlers as buildings, each drawn by a pool element of its own type.
 */
public interface LegacyVillageSource {
    /** Whether the version has any village creation handler registered (cheap: asked for every jigsaw structure). */
    boolean active();

    /** The buildings of a village type ("plains", "desert", "savanna", "snowy", "taiga"), computed once per type. */
    List<Building> buildings(String type);

    /** Startup log: the buildings with their weight and limit for one sample village. */
    void describe();

    /** What a handler gave for one village: how often the building is picked and how many it may add. */
    record Weight(int weight, int limit) {}

    /** One legacy village building: its handler and the pool element that draws it. */
    interface Building {
        /** The handler's weight and limit for a village drawn with this random (1.x getVillagePieceWeight), or null to leave it out. */
        Weight weigh(Random rand);

        /** The footprint relative to the plot corner in a mode (0 to 3, rotations of the building), or null when it builds nothing there. */
        BoundingBox footprint(int mode);

        /** The element of this building as added to one village: it knows the village's streets and other pieces. */
        StructurePoolElement inVillage(List<BoundingBox> streets, List<BoundingBox> others);
    }
}
