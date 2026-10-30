package legacyconnector.v1_12_2.net.minecraft.world.gen.structure;

import java.util.List;
import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.ModernPieces1122;

/**
 * 1.12.2 temple pieces (desert and jungle pyramids, swamp huts, igloos) as mod structure starts create them. Each is built by the
 * modern game's piece of that temple (its successor), created at the legacy piece's position; the legacy piece takes its box and
 * facing and saves it with its own data.
 */
public class ComponentScatteredFeaturePieces {
    public static void registerScatteredFeaturePieces() {
        MapGenStructureIO.registerStructureComponent(DesertPyramid.class, "TeDP");
        MapGenStructureIO.registerStructureComponent(JunglePyramid.class, "TeJP");
        MapGenStructureIO.registerStructureComponent(SwampHut.class, "TeSH");
        MapGenStructureIO.registerStructureComponent(Igloo.class, "Iglu");
    }

    /** A temple piece: its size, and the ground height it settled on (-1 before it did). */
    public abstract static class Feature extends StructureComponent {
        protected int width;
        protected int height;
        protected int depth;
        protected int horizontalPos = -1;
        /** The modern pieces that build this temple. */
        protected List<net.minecraft.world.level.levelgen.structure.StructurePiece> mcc$modern = new java.util.ArrayList<>();

        public Feature() {
        }

        protected Feature(Random rand, int x, int y, int z, int sizeX, int sizeY, int sizeZ) {
            super(0);
            this.width = sizeX;
            this.height = sizeY;
            this.depth = sizeZ;
            this.setCoordBaseMode(EnumFacing.Plane.HORIZONTAL.random(rand));
            if (this.getCoordBaseMode().getAxis() == EnumFacing.Axis.Z) {
                this.boundingBox = new StructureBoundingBox(x, y, z, x + sizeX - 1, y + sizeY - 1, z + sizeZ - 1);
            } else {
                this.boundingBox = new StructureBoundingBox(x, y, z, x + sizeZ - 1, y + sizeY - 1, z + sizeX - 1);
            }
        }

        /** Takes the box and facing of the modern pieces built for this temple. */
        protected void mcc$adopt(List<net.minecraft.world.level.levelgen.structure.StructurePiece> pieces) {
            this.mcc$modern = pieces;
            if (pieces.isEmpty()) return;
            this.boundingBox = ModernPieces1122.box(pieces);
            EnumFacing facing = ModernPieces1122.facing(pieces);
            if (facing != null) this.setCoordBaseMode(facing);
            this.width = this.boundingBox.getXSize();
            this.height = this.boundingBox.getYSize();
            this.depth = this.boundingBox.getZSize();
        }

        @Override
        protected void writeStructureToNBT(NBTTagCompound tagCompound) {
            tagCompound.setInteger("Width", this.width);
            tagCompound.setInteger("Height", this.height);
            tagCompound.setInteger("Depth", this.depth);
            tagCompound.setInteger("HPos", this.horizontalPos);
            ModernPieces1122.save(mcc$modern, tagCompound);
        }

        @Override
        protected void readStructureFromNBT(NBTTagCompound tagCompound, legacyconnector.v1_12_2.net.minecraft.world.gen.structure.template.TemplateManager manager) {
            this.width = tagCompound.getInteger("Width");
            this.height = tagCompound.getInteger("Height");
            this.depth = tagCompound.getInteger("Depth");
            this.horizontalPos = tagCompound.getInteger("HPos");
            this.mcc$modern = ModernPieces1122.load(tagCompound);
        }

        /** Moves the piece onto the average ground height of its footprint (once); false while none of it is in the box. */
        protected boolean offsetToAverageGroundLevel(World worldIn, StructureBoundingBox structurebb, int yOffset) {
            if (this.horizontalPos >= 0) return true;
            int sum = 0;
            int count = 0;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int z = this.boundingBox.minZ; z <= this.boundingBox.maxZ; ++z) {
                for (int x = this.boundingBox.minX; x <= this.boundingBox.maxX; ++x) {
                    pos.setPos(x, 64, z);
                    if (structurebb.isVecInside(pos)) {
                        var provider = World.mcc$get$provider(worldIn);
                        sum += Math.max(worldIn.getTopSolidOrLiquidBlock(pos).getY(), provider == null ? worldIn.getSeaLevel() + 1 : provider.getAverageGroundLevel());
                        ++count;
                    }
                }
            }
            if (count == 0) return false;
            this.horizontalPos = sum / count;
            this.boundingBox.offset(0, this.horizontalPos - this.boundingBox.minY + yOffset, 0);
            return true;
        }

        @Override
        public void offset(int x, int y, int z) {
            super.offset(x, y, z);
            ModernPieces1122.offset(mcc$modern, x, y, z);
        }

        /** Builds the modern pieces (which settle on the ground themselves). */
        @Override
        public boolean addComponentParts(World worldIn, Random randomIn, StructureBoundingBox structureBoundingBoxIn) {
            return ModernPieces1122.place(mcc$modern, worldIn, randomIn, structureBoundingBoxIn);
        }
    }

    public static class DesertPyramid extends Feature {
        public DesertPyramid() {
        }

        public DesertPyramid(Random rand, int x, int z) {
            super(rand, x, 64, z, 21, 15, 21);
            mcc$adopt(new java.util.ArrayList<>(List.of(new net.minecraft.world.level.levelgen.structure.structures.DesertPyramidPiece(ModernPieces1122.random(rand), x, z))));
        }
    }

    public static class JunglePyramid extends Feature {
        public JunglePyramid() {
        }

        public JunglePyramid(Random rand, int x, int z) {
            super(rand, x, 64, z, 12, 10, 15);
            mcc$adopt(new java.util.ArrayList<>(List.of(new net.minecraft.world.level.levelgen.structure.structures.JungleTemplePiece(ModernPieces1122.random(rand), x, z))));
        }
    }

    public static class SwampHut extends Feature {
        public SwampHut() {
        }

        public SwampHut(Random rand, int x, int z) {
            super(rand, x, 64, z, 7, 7, 9);
            mcc$adopt(new java.util.ArrayList<>(List.of(new net.minecraft.world.level.levelgen.structure.structures.SwampHutPiece(ModernPieces1122.random(rand), x, z))));
        }
    }

    /** An igloo (with its basement and ladder shaft one time in two): the modern igloo pieces, from the server's templates. */
    public static class Igloo extends Feature {
        public Igloo() {
        }

        public Igloo(Random rand, int x, int z) {
            super(rand, x, 64, z, 7, 5, 8);
            var templates = ModernPieces1122.templates();
            if (templates == null) return;
            var random = ModernPieces1122.random(rand);
            var collector = new ModernPieces1122.Collector();
            var rotation = net.minecraft.world.level.block.Rotation.getRandom(random);
            net.minecraft.world.level.levelgen.structure.structures.IglooPieces.addPieces(templates, new net.minecraft.core.BlockPos(x, 90, z), rotation, collector, random);
            mcc$adopt(collector.pieces);
        }
    }
}
