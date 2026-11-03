package legacyconnector.v1_7_10.net.minecraft.world.gen.structure;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.util.WeightedRandomChestContent;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** Nether fortress pieces; only the chest list shared by the pieces is modelled. Entries a mod appends here (reassigning the array, or replacing its content through reflection) are loot-table additions: {@code ChestLootBridge1710} reads the live field through {@link StructureChestFields} when the table loads. The array starts empty because the vanilla entries are in the modern loot table already. */
public class StructureNetherBridgePieces {
    public abstract static class Piece extends StructureComponent {
        /** 1.7.10 {@code field_111019_a}: the chests of the fortress pieces. */
        public static WeightedRandomChestContent[] field_111019_a = new WeightedRandomChestContent[0];

        public Piece() {
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
}
