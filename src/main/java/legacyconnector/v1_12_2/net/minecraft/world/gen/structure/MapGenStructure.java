package legacyconnector.v1_12_2.net.minecraft.world.gen.structure;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.math.ChunkPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraft.world.chunk.ChunkPrimer;
import legacyconnector.v1_12_2.net.minecraft.world.gen.MapGenBase;

/**
 * 1.12.2 structure generator of a legacy chunk generator: decides per chunk whether a structure starts there
 * ({@link #canSpawnStructureAtCoords}), keeps the starts by chunk, and builds the parts of each start that fall into a chunk when the
 * chunk is populated ({@link #generateStructure}). Starts are saved with the world's per-dimension data under the structure name.
 */
public abstract class MapGenStructure extends MapGenBase {
    private MapGenStructureData structureData;
    protected it.unimi.dsi.fastutil.longs.Long2ObjectMap<StructureStart> structureMap = it.unimi.dsi.fastutil.longs.Long2ObjectMaps.synchronize(
            new it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<>(1024));

    public MapGenStructure() {
    }

    public abstract String getStructureName();

    @Override
    protected final synchronized void recursiveGenerate(World worldIn, int chunkX, int chunkZ, int originalX, int originalZ, ChunkPrimer chunkPrimerIn) {
        this.initializeStructureData(worldIn);
        long key = ChunkPos.asLong(chunkX, chunkZ);
        if (!this.structureMap.containsKey(key)) {
            this.rand.nextInt();
            try {
                if (this.canSpawnStructureAtCoords(chunkX, chunkZ)) {
                    StructureStart start = this.getStructureStart(chunkX, chunkZ);
                    this.structureMap.put(key, start);
                    if (start.isSizeableStructure()) this.setStructureStart(chunkX, chunkZ, start);
                }
            } catch (RuntimeException e) {
                org.slf4j.LoggerFactory.getLogger("LegacyConnector/WorldGen").warn("1.12.2 structure {} failed to start at chunk {},{}: {}", getStructureName(),
                        chunkX, chunkZ, e.toString());
            }
        }
    }

    public synchronized boolean generateStructure(World worldIn, Random randomIn, ChunkPos chunkCoord) {
        this.initializeStructureData(worldIn);
        int i = (chunkCoord.x << 4) + 8;
        int j = (chunkCoord.z << 4) + 8;
        boolean flag = false;
        for (StructureStart start : new java.util.ArrayList<>(this.structureMap.values())) {
            if (start.isSizeableStructure() && start.isValidForPostProcess(chunkCoord) && start.getBoundingBox().intersectsWith(i, j, i + 15, j + 15)) {
                start.generateStructure(worldIn, randomIn, new StructureBoundingBox(i, j, i + 15, j + 15));
                start.notifyPostProcessAt(chunkCoord);
                flag = true;
                this.setStructureStart(start.getChunkPosX(), start.getChunkPosZ(), start);
            }
        }
        return flag;
    }

    public boolean isInsideStructure(BlockPos pos) {
        if (this.world == null) return false;
        this.initializeStructureData(this.world);
        return this.getStructureAt(pos) != null;
    }

    protected StructureStart getStructureAt(BlockPos pos) {
        for (StructureStart start : new java.util.ArrayList<>(this.structureMap.values())) {
            if (start.isSizeableStructure() && start.getBoundingBox().isVecInside(pos)) {
                for (StructureComponent c : start.getComponents()) {
                    if (c.getBoundingBox().isVecInside(pos)) return start;
                }
            }
        }
        return null;
    }

    public boolean isPositionInStructure(World worldIn, BlockPos pos) {
        this.initializeStructureData(worldIn);
        for (StructureStart start : new java.util.ArrayList<>(this.structureMap.values())) {
            if (start.isSizeableStructure() && start.getBoundingBox().isVecInside(pos)) return true;
        }
        return false;
    }

    public abstract BlockPos getNearestStructurePos(World worldIn, BlockPos pos, boolean findUnexplored);

    protected void initializeStructureData(World worldIn) {
        if (this.structureData != null || worldIn == null) return;
        this.world = worldIn;
        try {
            var storage = worldIn.getPerWorldStorage();
            this.structureData = (MapGenStructureData) storage.getOrLoadData(MapGenStructureData.class, this.getStructureName());
            if (this.structureData == null) {
                this.structureData = new MapGenStructureData(this.getStructureName());
                storage.setData(this.getStructureName(), this.structureData);
            } else {
                var tag = this.structureData.getTagCompound();
                for (String key : tag.getKeySet()) {
                    var nbt = tag.getTag(key);
                    if (nbt instanceof legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound c && c.hasKey("ChunkX") && c.hasKey("ChunkZ")) {
                        StructureStart start = MapGenStructureIO.getStructureStart(c, worldIn);
                        if (start != null) this.structureMap.put(ChunkPos.asLong(c.getInteger("ChunkX"), c.getInteger("ChunkZ")), start);
                    }
                }
            }
        } catch (RuntimeException | LinkageError e) {
            this.structureData = new MapGenStructureData(this.getStructureName());
        }
    }

    private void setStructureStart(int chunkX, int chunkZ, StructureStart start) {
        if (this.structureData == null) return;
        this.structureData.writeInstance(start.writeStructureComponentsToNBT(chunkX, chunkZ), chunkX, chunkZ);
        this.structureData.markDirty();
    }

    protected abstract boolean canSpawnStructureAtCoords(int chunkX, int chunkZ);

    protected abstract StructureStart getStructureStart(int chunkX, int chunkZ);

    /** Vanilla's spiral search for the nearest chunk the generator would start a structure in (spacing/separation grid). */
    protected static BlockPos findNearestStructurePosBySpacing(World worldIn, MapGenStructure gen, BlockPos pos, int distance, int separation, int seed,
            boolean addExtraRandomness, int maxAttempts, boolean findUnexplored) {
        int i = pos.getX() >> 4;
        int j = pos.getZ() >> 4;
        int k = 0;
        for (Random random = new Random(); k <= maxAttempts; ++k) {
            for (int l = -k; l <= k; ++l) {
                boolean flag = l == -k || l == k;
                for (int i1 = -k; i1 <= k; ++i1) {
                    boolean flag1 = i1 == -k || i1 == k;
                    if (flag || flag1) {
                        int j1 = i + distance * l;
                        int k1 = j + distance * i1;
                        if (j1 < 0) j1 -= distance - 1;
                        if (k1 < 0) k1 -= distance - 1;
                        int l1 = j1 / distance;
                        int i2 = k1 / distance;
                        Random random1 = worldIn.setRandomSeed(l1, i2, seed);
                        l1 = l1 * distance;
                        i2 = i2 * distance;
                        if (addExtraRandomness) {
                            l1 = l1 + (random1.nextInt(distance - separation) + random1.nextInt(distance - separation)) / 2;
                            i2 = i2 + (random1.nextInt(distance - separation) + random1.nextInt(distance - separation)) / 2;
                        } else {
                            l1 = l1 + random1.nextInt(distance - separation);
                            i2 = i2 + random1.nextInt(distance - separation);
                        }
                        MapGenBase.setupChunkSeed(worldIn.getSeed(), random, l1, i2);
                        random.nextInt();
                        if (gen.canSpawnStructureAtCoords(l1, i2)) {
                            if (!findUnexplored || !worldIn.isChunkGeneratedAt(l1, i2)) return new BlockPos((l1 << 4) + 8, 64, (i2 << 4) + 8);
                        } else if (k == 0) {
                            break;
                        }
                    }
                }
                if (k == 0) break;
            }
        }
        return null;
    }
}
