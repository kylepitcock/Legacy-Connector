package legacyconnector.v1_7_10.net.minecraft.world.gen.structure;

import java.util.ArrayList;
import java.util.List;

/**
 * Vanilla's stronghold layout state. Strongholds are the modern ones (see {@link MapGenStronghold}), so there are no legacy
 * piece weights to draw from: {@link #prepareStructurePieces} leaves the piece list empty, which the 1.7.10 layout code
 * read as "no more rooms" (a stronghold a mod lays out itself ends after its start piece).
 */
public class StructureStrongholdPieces {
    public static List structurePieceList = new ArrayList();
    public static Class strongComponentType;
    public static int totalWeight;

    /** Resets the per-stronghold piece counters before a new stronghold is laid out. */
    public static void prepareStructurePieces() {
        structurePieceList = new ArrayList();
        totalWeight = 0;
        strongComponentType = null;
    }
}
