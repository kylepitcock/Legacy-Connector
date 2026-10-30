package legacyconnector.v1_12_2.net.minecraft.world.gen.structure;

import java.util.List;
import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.ModernPieces1122;

/**
 * 1.12.2 ocean monument pieces. The monument a mod's start creates ({@link MonumentBuilding}) is built by the modern monument
 * building (its successor, with all its rooms) at the same position and facing, saved with the legacy piece.
 */
public class StructureOceanMonumentPieces {
    public static void registerOceanMonumentPieces() {
        MapGenStructureIO.registerStructureComponent(MonumentBuilding.class, "OMB");
    }

    /** A monument piece. */
    public abstract static class Piece extends StructureComponent {
        public Piece() {
        }

        protected Piece(int type) {
            super(type);
        }
    }

    /** The whole monument: the modern monument building at (x, 39, z) facing the given way. */
    public static class MonumentBuilding extends Piece {
        private List<net.minecraft.world.level.levelgen.structure.StructurePiece> mcc$modern = new java.util.ArrayList<>();

        public MonumentBuilding() {
        }

        public MonumentBuilding(Random rand, int x, int z, EnumFacing facing) {
            super(0);
            var direction = facing == null || !facing.mcc$modern().getAxis().isHorizontal() ? net.minecraft.core.Direction.NORTH : facing.mcc$modern();
            this.mcc$modern = new java.util.ArrayList<>(List.of(
                    new net.minecraft.world.level.levelgen.structure.structures.OceanMonumentPieces.MonumentBuilding(ModernPieces1122.random(rand), x, z, direction)));
            this.setCoordBaseMode(facing);
            this.boundingBox = ModernPieces1122.box(this.mcc$modern);
        }

        @Override
        public boolean addComponentParts(World worldIn, Random randomIn, StructureBoundingBox structureBoundingBoxIn) {
            return ModernPieces1122.place(mcc$modern, worldIn, randomIn, structureBoundingBoxIn);
        }

        @Override
        public void offset(int x, int y, int z) {
            super.offset(x, y, z);
            ModernPieces1122.offset(mcc$modern, x, y, z);
        }

        @Override
        protected void writeStructureToNBT(NBTTagCompound tagCompound) {
            ModernPieces1122.save(mcc$modern, tagCompound);
        }

        @Override
        protected void readStructureFromNBT(NBTTagCompound tagCompound, legacyconnector.v1_12_2.net.minecraft.world.gen.structure.template.TemplateManager manager) {
            this.mcc$modern = ModernPieces1122.load(tagCompound);
        }
    }
}
