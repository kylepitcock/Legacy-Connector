package legacyconnector.v1_7_10.net.minecraft.world.gen.structure;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.util.WeightedRandomChestContent;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** Desert and jungle temple pieces; only their chest lists are modelled. Entries a mod appends here (reassigning the array, or replacing its content through reflection) are loot-table additions: {@code ChestLootBridge1710} reads the live field through {@link StructureChestFields} when the table loads. The array starts empty because the vanilla entries are in the modern loot table already. */
public class ComponentScatteredFeaturePieces {
    public abstract static class Feature extends StructureComponent {
        public Feature() {
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

    public static class DesertPyramid extends Feature {
        /** 1.7.10 {@code field_74941_i}. */
        public static WeightedRandomChestContent[] itemsToGenerateInTemple = new WeightedRandomChestContent[0];

        public DesertPyramid() {
        }
    }

    public static class JunglePyramid extends Feature {
        /** 1.7.10 {@code field_74943_l}. */
        public static WeightedRandomChestContent[] junglePyramidsChestContents = new WeightedRandomChestContent[0];

        public JunglePyramid() {
        }
    }
}
