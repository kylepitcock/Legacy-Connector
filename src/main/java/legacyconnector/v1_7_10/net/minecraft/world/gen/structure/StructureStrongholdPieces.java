package legacyconnector.v1_7_10.net.minecraft.world.gen.structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.util.WeightedRandomChestContent;
import legacyconnector.v1_7_10.net.minecraft.world.World;

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

    /** Base of the stronghold pieces. Entries a mod appends here (reassigning the array, or replacing its content through reflection) are loot-table additions: {@code ChestLootBridge1710} reads the live field through {@link StructureChestFields} when the table loads. The array starts empty because the vanilla entries are in the modern loot table already. */
    public abstract static class Stronghold extends StructureComponent {
        public Stronghold() {
        }

        @Override
        protected void writeStructureToNBT(NBTTagCompound tag) {
        }

        @Override
        protected void readStructureFromNBT(NBTTagCompound tag) {
        }

        @Override
        public boolean addComponentParts(World world, Random rand, StructureBoundingBox box) {
            return false;
        }
    }

    public static class ChestCorridor extends Stronghold {
        /** 1.7.10 {@code field_75003_a}. */
        public static WeightedRandomChestContent[] strongholdChestContents = new WeightedRandomChestContent[0];

        public ChestCorridor() {
        }
    }

    public static class Library extends Stronghold {
        /** 1.7.10 {@code field_75007_b}. */
        public static WeightedRandomChestContent[] strongholdLibraryChestContents = new WeightedRandomChestContent[0];

        public Library() {
        }
    }

    public static class RoomCrossing extends Stronghold {
        /** 1.7.10 {@code field_75014_c}. */
        public static WeightedRandomChestContent[] strongholdRoomCrossingChestContents = new WeightedRandomChestContent[0];

        public RoomCrossing() {
        }
    }
}
