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

        /** The plot of a vanilla building at a path (see VanillaVillageBuildings), or null where it does not fit. */
        protected static StructureBoundingBox mcc$plot(String kind, Start start, List list, int x, int y, int z, int facing) {
            int[] size = org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaVillageBuildings.size(kind, start != null && start.inDesert, facing);
            if (size == null) return null;
            StructureBoundingBox box = StructureBoundingBox.getComponentToAddBoundingBox(x, y, z, 0, 0, 0, size[0], size[1], size[2], facing);
            return canVillageGoDeeper(box) && StructureComponent.findIntersecting(list, box) == null ? box : null;
        }

        /** Builds the vanilla building of this plot inside {@code clip}. */
        protected boolean mcc$build(String kind, World world, Random rand, StructureBoundingBox clip) {
            org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaVillageBuildings.place(kind, this.field_143014_b, this.coordBaseMode, this.boundingBox, world, rand, clip);
            return true;
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
        /** Forge: the biome the village is in. */
        public legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase biome;
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

    // Vanilla's village buildings. Their factories (func_749xx_a) are what mods' own village layouts call to add a vanilla
    // house: each is the modern village building of the same use (see VanillaVillageBuildings), on a plot sized from it that
    // lies against the path; null, as in 1.7.10, when the plot is taken or too deep (the caller tries another piece).

    public static class House1 extends Village {
        public House1() {
        }

        public House1(Start start, int type, Random rand, StructureBoundingBox box, int facing) {
            super(start, type);
            this.coordBaseMode = facing;
            this.boundingBox = box;
        }

        @Override
        public boolean addComponentParts(World world, Random rand, StructureBoundingBox box) {
            return mcc$build("library", world, rand, box);
        }

        public static House1 func_74898_a(Start start, List list, Random rand, int x, int y, int z, int facing, int type) {
            StructureBoundingBox plot = mcc$plot("library", start, list, x, y, z, facing);
            return plot == null ? null : new House1(start, type, rand, plot, facing);
        }
    }

    public static class House2 extends Village {
        public House2() {
        }

        public House2(Start start, int type, Random rand, StructureBoundingBox box, int facing) {
            super(start, type);
            this.coordBaseMode = facing;
            this.boundingBox = box;
        }

        @Override
        public boolean addComponentParts(World world, Random rand, StructureBoundingBox box) {
            return mcc$build("smithy", world, rand, box);
        }

        public static House2 func_74915_a(Start start, List list, Random rand, int x, int y, int z, int facing, int type) {
            StructureBoundingBox plot = mcc$plot("smithy", start, list, x, y, z, facing);
            return plot == null ? null : new House2(start, type, rand, plot, facing);
        }
    }

    public static class House3 extends Village {
        public House3() {
        }

        public House3(Start start, int type, Random rand, StructureBoundingBox box, int facing) {
            super(start, type);
            this.coordBaseMode = facing;
            this.boundingBox = box;
        }

        @Override
        public boolean addComponentParts(World world, Random rand, StructureBoundingBox box) {
            return mcc$build("big_house", world, rand, box);
        }

        public static House3 func_74921_a(Start start, List list, Random rand, int x, int y, int z, int facing, int type) {
            StructureBoundingBox plot = mcc$plot("big_house", start, list, x, y, z, facing);
            return plot == null ? null : new House3(start, type, rand, plot, facing);
        }
    }

    public static class House4Garden extends Village {
        public House4Garden() {
        }

        public House4Garden(Start start, int type, Random rand, StructureBoundingBox box, int facing) {
            super(start, type);
            this.coordBaseMode = facing;
            this.boundingBox = box;
        }

        @Override
        public boolean addComponentParts(World world, Random rand, StructureBoundingBox box) {
            return mcc$build("garden_house", world, rand, box);
        }

        public static House4Garden func_74912_a(Start start, List list, Random rand, int x, int y, int z, int facing, int type) {
            StructureBoundingBox plot = mcc$plot("garden_house", start, list, x, y, z, facing);
            return plot == null ? null : new House4Garden(start, type, rand, plot, facing);
        }
    }

    public static class WoodHut extends Village {
        public WoodHut() {
        }

        public WoodHut(Start start, int type, Random rand, StructureBoundingBox box, int facing) {
            super(start, type);
            this.coordBaseMode = facing;
            this.boundingBox = box;
        }

        @Override
        public boolean addComponentParts(World world, Random rand, StructureBoundingBox box) {
            return mcc$build("hut", world, rand, box);
        }

        public static WoodHut func_74908_a(Start start, List list, Random rand, int x, int y, int z, int facing, int type) {
            StructureBoundingBox plot = mcc$plot("hut", start, list, x, y, z, facing);
            return plot == null ? null : new WoodHut(start, type, rand, plot, facing);
        }
    }

    public static class Hall extends Village {
        public Hall() {
        }

        public Hall(Start start, int type, Random rand, StructureBoundingBox box, int facing) {
            super(start, type);
            this.coordBaseMode = facing;
            this.boundingBox = box;
        }

        @Override
        public boolean addComponentParts(World world, Random rand, StructureBoundingBox box) {
            return mcc$build("hall", world, rand, box);
        }

        public static Hall func_74906_a(Start start, List list, Random rand, int x, int y, int z, int facing, int type) {
            StructureBoundingBox plot = mcc$plot("hall", start, list, x, y, z, facing);
            return plot == null ? null : new Hall(start, type, rand, plot, facing);
        }
    }

    public static class Field1 extends Village {
        public Field1() {
        }

        public Field1(Start start, int type, Random rand, StructureBoundingBox box, int facing) {
            super(start, type);
            this.coordBaseMode = facing;
            this.boundingBox = box;
        }

        @Override
        public boolean addComponentParts(World world, Random rand, StructureBoundingBox box) {
            return mcc$build("large_field", world, rand, box);
        }

        public static Field1 func_74900_a(Start start, List list, Random rand, int x, int y, int z, int facing, int type) {
            StructureBoundingBox plot = mcc$plot("large_field", start, list, x, y, z, facing);
            return plot == null ? null : new Field1(start, type, rand, plot, facing);
        }
    }

    public static class Field2 extends Village {
        public Field2() {
        }

        public Field2(Start start, int type, Random rand, StructureBoundingBox box, int facing) {
            super(start, type);
            this.coordBaseMode = facing;
            this.boundingBox = box;
        }

        @Override
        public boolean addComponentParts(World world, Random rand, StructureBoundingBox box) {
            return mcc$build("field", world, rand, box);
        }

        public static Field2 func_74902_a(Start start, List list, Random rand, int x, int y, int z, int facing, int type) {
            StructureBoundingBox plot = mcc$plot("field", start, list, x, y, z, facing);
            return plot == null ? null : new Field2(start, type, rand, plot, facing);
        }
    }

    public static class Church extends Village {
        public Church() {
        }

        public Church(Start start, int type, Random rand, StructureBoundingBox box, int facing) {
            super(start, type);
            this.coordBaseMode = facing;
            this.boundingBox = box;
        }

        @Override
        public boolean addComponentParts(World world, Random rand, StructureBoundingBox box) {
            return mcc$build("church", world, rand, box);
        }

        /** The church's villager is a priest. */
        protected int getVillagerType(int existing) {
            return 2;
        }

        public static Church func_74919_a(Start start, List list, Random rand, int x, int y, int z, int facing, int type) {
            StructureBoundingBox plot = mcc$plot("church", start, list, x, y, z, facing);
            return plot == null ? null : new Church(start, type, rand, plot, facing);
        }
    }

    /**
     * The vanilla buildings' weights for a layout a mod continues from its own path (1.7.10's base list; the modern village
     * already has its own houses, so each kind comes once or twice).
     */
    public static List<PieceWeight> mcc$vanillaWeights() {
        List<PieceWeight> out = new java.util.ArrayList<>();
        out.add(new PieceWeight(House4Garden.class, 4, 2));
        out.add(new PieceWeight(Church.class, 20, 1));
        out.add(new PieceWeight(House1.class, 20, 1));
        out.add(new PieceWeight(WoodHut.class, 3, 2));
        out.add(new PieceWeight(Hall.class, 15, 1));
        out.add(new PieceWeight(Field1.class, 3, 1));
        out.add(new PieceWeight(Field2.class, 3, 2));
        out.add(new PieceWeight(House2.class, 15, 1));
        out.add(new PieceWeight(House3.class, 8, 1));
        return out;
    }

    /** One building of a weight: a vanilla one through its factory, a mod's through its village creation handler. */
    private static StructureComponent mcc$create(PieceWeight w, Start start, List list, Random rand, int x, int y, int z, int facing, int type) {
        Class<?> c = w.villagePieceClass;
        if (c == House4Garden.class) return House4Garden.func_74912_a(start, list, rand, x, y, z, facing, type);
        if (c == Church.class) return Church.func_74919_a(start, list, rand, x, y, z, facing, type);
        if (c == House1.class) return House1.func_74898_a(start, list, rand, x, y, z, facing, type);
        if (c == WoodHut.class) return WoodHut.func_74908_a(start, list, rand, x, y, z, facing, type);
        if (c == Hall.class) return Hall.func_74906_a(start, list, rand, x, y, z, facing, type);
        if (c == Field1.class) return Field1.func_74900_a(start, list, rand, x, y, z, facing, type);
        if (c == Field2.class) return Field2.func_74902_a(start, list, rand, x, y, z, facing, type);
        if (c == House2.class) return House2.func_74915_a(start, list, rand, x, y, z, facing, type);
        if (c == House3.class) return House3.func_74921_a(start, list, rand, x, y, z, facing, type);
        for (var handler : org.kylepitcock.legacyconnector.v1_7_10.bridge.VillagerBridge.creationHandlers()) {
            if (handler.getComponentClass() != c) continue;
            Object built = handler.buildComponent(w, start, list, rand, x, y, z, facing, type);
            return built instanceof StructureComponent component ? component : null;
        }
        return null;
    }

    /**
     * The next building at a path of a legacy layout, as 1.7.10 chose it: weighted among the start's pieces that may still
     * spawn, five tries, within 112 blocks of the start. The building joins the pieces and the start's pending list, whose
     * entries the planner lets extend the layout in turn.
     */
    private static StructureComponent getNextVillageStructureComponent(Start start, List list, Random rand, int x, int y, int z, int facing, int type) {
        if (start == null || type > 50 || start.getBoundingBox() == null) return null;
        if (Math.abs(x - start.getBoundingBox().minX) > 112 || Math.abs(z - start.getBoundingBox().minZ) > 112) return null;
        List<?> weights = start.structureVillageWeightedPieceList;
        if (weights == null || weights.isEmpty()) return null;
        for (int attempt = 0; attempt < 5; attempt++) {
            int total = 0;
            for (Object o : weights) if (o instanceof PieceWeight w && w.canSpawnMoreVillagePieces()) total += w.villagePieceWeight;
            if (total <= 0) return null;
            int r = rand.nextInt(total);
            for (Object o : weights) {
                if (!(o instanceof PieceWeight w) || !w.canSpawnMoreVillagePieces()) continue;
                r -= w.villagePieceWeight;
                if (r >= 0) continue;
                StructureComponent piece = mcc$create(w, start, list, rand, x, y, z, facing, type + 1);
                if (piece != null) {
                    w.villagePiecesSpawned++;
                    list.add(piece);
                    start.field_74932_i.add(piece);
                    return piece;
                }
                break;
            }
        }
        return null;
    }
}
