package legacyconnector.v1_7_10.net.minecraft.world.gen.structure;

import java.util.List;
import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraft.world.biome.WorldChunkManager;

/**
 * The base classes mods' village components extend (Forge's village creation handlers build them). The vanilla village
 * layout itself is the modern jigsaw one; see {@code LegacyVillages} for how legacy components are placed in it.
 */
public class StructureVillagePieces {
    public static class PieceWeight {
        public Class villagePieceClass;
        public final int villagePieceWeight;
        public int villagePiecesSpawned;
        public int villagePiecesLimit;

        public PieceWeight(Class pieceClass, int weight, int limit) {
            this.villagePieceClass = pieceClass;
            this.villagePieceWeight = weight;
            this.villagePiecesLimit = limit;
        }

        public boolean canSpawnMoreVillagePiecesOfType(int size) {
            return this.villagePiecesLimit == 0 || this.villagePiecesSpawned < this.villagePiecesLimit;
        }

        public boolean canSpawnMoreVillagePieces() {
            return this.villagePiecesLimit == 0 || this.villagePiecesSpawned < this.villagePiecesLimit;
        }
    }

    public abstract static class Village extends StructureComponent {
        protected int field_143015_k = -1;
        private int villagersSpawned;
        private boolean field_143014_b;

        public Village() {
        }

        protected Village(Start start, int type) {
            super(type);
            if (start != null) this.field_143014_b = start.inDesert;
        }

        @Override
        protected void writeStructureToNBT(NBTTagCompound tag) {
            tag.setInteger("HPos", this.field_143015_k);
            tag.setInteger("VCount", this.villagersSpawned);
            tag.setByte("Desert", (byte) (this.field_143014_b ? 1 : 0));
        }

        @Override
        protected void readStructureFromNBT(NBTTagCompound tag) {
            this.field_143015_k = tag.getInteger("HPos");
            this.villagersSpawned = tag.getInteger("VCount");
            this.field_143014_b = tag.getByte("Desert") == 1;
        }

        protected StructureComponent getNextComponentNN(Start start, List list, Random rand, int y, int offset) {
            switch (this.coordBaseMode) {
                case 0:
                    return getNextVillageStructureComponent(start, list, rand, this.boundingBox.minX - 1, this.boundingBox.minY + y, this.boundingBox.minZ + offset, 1, this.getComponentType());
                case 1:
                    return getNextVillageStructureComponent(start, list, rand, this.boundingBox.minX + offset, this.boundingBox.minY + y, this.boundingBox.minZ - 1, 2, this.getComponentType());
                case 2:
                    return getNextVillageStructureComponent(start, list, rand, this.boundingBox.minX - 1, this.boundingBox.minY + y, this.boundingBox.minZ + offset, 1, this.getComponentType());
                case 3:
                    return getNextVillageStructureComponent(start, list, rand, this.boundingBox.minX + offset, this.boundingBox.minY + y, this.boundingBox.minZ - 1, 2, this.getComponentType());
                default:
                    return null;
            }
        }

        protected StructureComponent getNextComponentPP(Start start, List list, Random rand, int y, int offset) {
            switch (this.coordBaseMode) {
                case 0:
                    return getNextVillageStructureComponent(start, list, rand, this.boundingBox.maxX + 1, this.boundingBox.minY + y, this.boundingBox.minZ + offset, 3, this.getComponentType());
                case 1:
                    return getNextVillageStructureComponent(start, list, rand, this.boundingBox.minX + offset, this.boundingBox.minY + y, this.boundingBox.maxZ + 1, 0, this.getComponentType());
                case 2:
                    return getNextVillageStructureComponent(start, list, rand, this.boundingBox.maxX + 1, this.boundingBox.minY + y, this.boundingBox.minZ + offset, 3, this.getComponentType());
                case 3:
                    return getNextVillageStructureComponent(start, list, rand, this.boundingBox.minX + offset, this.boundingBox.minY + y, this.boundingBox.maxZ + 1, 0, this.getComponentType());
                default:
                    return null;
            }
        }

        protected int getAverageGroundLevel(World world, StructureBoundingBox box) {
            int total = 0;
            int count = 0;
            for (int z = this.boundingBox.minZ; z <= this.boundingBox.maxZ; ++z) {
                for (int x = this.boundingBox.minX; x <= this.boundingBox.maxX; ++x) {
                    if (box.isVecInside(x, 64, z)) {
                        total += Math.max(world.getTopSolidOrLiquidBlock(x, z), World.mcc$get$provider(world).getAverageGroundLevel());
                        ++count;
                    }
                }
            }
            return count == 0 ? -1 : total / count;
        }

        protected static boolean canVillageGoDeeper(StructureBoundingBox box) {
            return box != null && box.minY > 10;
        }

        protected void spawnVillagers(World world, StructureBoundingBox box, int x, int y, int z, int count) {
            if (this.villagersSpawned < count) {
                for (int i = this.villagersSpawned; i < count; ++i) {
                    int vx = this.getXWithOffset(x + i, z);
                    int vy = this.getYWithOffset(y);
                    int vz = this.getZWithOffset(x + i, z);
                    if (!box.isVecInside(vx, vy, vz)) break;
                    ++this.villagersSpawned;
                    org.kylepitcock.legacyconnector.v1_7_10.bridge.VillagerBridge.spawn(world, this.getVillagerType(i), vx + 0.5D, vy, vz + 0.5D);
                }
            }
        }

        protected int getVillagerType(int index) {
            return 0;
        }

        protected Block func_151558_b(Block block, int meta) {
            if (this.field_143014_b) {
                if (block == Blocks.log || block == Blocks.log2) return Blocks.sandstone;
                if (block == Blocks.cobblestone) return Blocks.sandstone;
                if (block == Blocks.planks) return Blocks.sandstone;
                if (block == Blocks.oak_stairs) return Blocks.sandstone_stairs;
                if (block == Blocks.stone_stairs) return Blocks.sandstone_stairs;
                if (block == Blocks.gravel) return Blocks.sandstone;
            }
            return block;
        }

        protected int func_151557_c(Block block, int meta) {
            if (this.field_143014_b) {
                if (block == Blocks.log || block == Blocks.log2) return 0;
                if (block == Blocks.cobblestone) return 0;
                if (block == Blocks.planks) return 2;
            }
            return meta;
        }

        @Override
        protected void placeBlockAtCurrentPosition(World world, Block block, int meta, int x, int y, int z, StructureBoundingBox box) {
            Block b = this.func_151558_b(block, meta);
            int m = this.func_151557_c(block, meta);
            super.placeBlockAtCurrentPosition(world, b, m, x, y, z, box);
        }

        @Override
        protected void fillWithBlocks(World world, StructureBoundingBox box, int x1, int y1, int z1, int x2, int y2, int z2, Block outer, Block inner, boolean onlyReplaceSolid) {
            Block o = this.func_151558_b(outer, 0);
            Block i = this.func_151558_b(inner, 0);
            super.fillWithBlocks(world, box, x1, y1, z1, x2, y2, z2, o, i, onlyReplaceSolid);
        }

        @Override
        protected void func_151554_b(World world, Block block, int meta, int x, int y, int z, StructureBoundingBox box) {
            Block b = this.func_151558_b(block, meta);
            int m = this.func_151557_c(block, meta);
            super.func_151554_b(world, b, m, x, y, z, box);
        }
    }

    public abstract static class Road extends Village {
        public Road() {
        }

        protected Road(Start start, int type) {
            super(start, type);
        }
    }

    public static class Well extends Village {
        public Well() {
        }

        public Well(Start start, int type, Random rand, int x, int z) {
            super(start, type);
            this.coordBaseMode = rand.nextInt(4);
            this.boundingBox = new StructureBoundingBox(x, 64, z, x + 6 - 1, 78, z + 6 - 1);
        }

        @Override
        public boolean addComponentParts(World world, Random rand, StructureBoundingBox box) {
            return true;
        }
    }

    public static class Path extends Road {
        public Path() {
        }

        public Path(Start start, int type, Random rand, StructureBoundingBox box, int facing) {
            super(start, type);
            this.coordBaseMode = facing;
            this.boundingBox = box;
        }

        @Override
        public boolean addComponentParts(World world, Random rand, StructureBoundingBox box) {
            return true;
        }
    }

    public static class Start extends Well {
        public WorldChunkManager worldChunkMngr;
        public boolean inDesert;
        public int terrainType;
        public PieceWeight structVillagePieceWeight;
        public List structureVillageWeightedPieceList;
        public List field_74932_i = new java.util.ArrayList();
        public List field_74930_j = new java.util.ArrayList();

        public Start() {
        }

        public Start(WorldChunkManager manager, int type, Random rand, int x, int z, List pieces, int terrainType) {
            super(null, 0, rand, x, z);
            this.worldChunkMngr = manager;
            this.structureVillageWeightedPieceList = pieces;
            this.terrainType = terrainType;
        }

        public WorldChunkManager getWorldChunkManager() {
            return this.worldChunkMngr;
        }
    }

    /** Layout of vanilla villages is the modern jigsaw one: nothing continues from a legacy component. */
    private static StructureComponent getNextVillageStructureComponent(Start start, List list, Random rand, int x, int y, int z, int facing, int type) {
        return null;
    }
}
