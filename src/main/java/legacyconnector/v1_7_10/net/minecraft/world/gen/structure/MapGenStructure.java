package legacyconnector.v1_7_10.net.minecraft.world.gen.structure;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.world.ChunkCoordIntPair;
import legacyconnector.v1_7_10.net.minecraft.world.ChunkPosition;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraft.world.gen.MapGenBase;

/**
 * Structure generator: decides per chunk whether a structure starts there ({@link #canSpawnStructureAtCoords}), lays
 * its pieces out, and places the pieces that intersect each chunk as it is populated. Structure starts are not
 * persisted (the seed decides them again identically), so the map is rebuilt as chunks are visited.
 */
public abstract class MapGenStructure extends MapGenBase {
    protected Map structureMap = new HashMap();

    {
        org.kylepitcock.legacyconnector.v1_7_10.bridge.LegacyStructures.track(this);
    }

    public abstract String getStructureName();

    @Override
    protected final void func_151538_a(World world, int cx, int cz, int p4, int p5, Block[] blocks) {
        func_143027_a(world);
        if (!this.structureMap.containsKey(ChunkCoordIntPair.chunkXZ2Int(cx, cz))) {
            this.rand.nextInt();
            if (this.canSpawnStructureAtCoords(cx, cz)) {
                StructureStart start = this.getStructureStart(cx, cz);
                this.structureMap.put(ChunkCoordIntPair.chunkXZ2Int(cx, cz), start);
            }
        }
    }

    public boolean generateStructuresInChunk(World world, Random rand, int cx, int cz) {
        int k = (cx << 4) + 8;
        int l = (cz << 4) + 8;
        boolean flag = false;
        for (Object o : this.structureMap.values().toArray()) {
            StructureStart start = (StructureStart) o;
            if (start.isSizeableStructure() && start.getBoundingBox().intersectsWith(k, l, k + 15, l + 15)) {
                start.generateStructure(world, rand, new StructureBoundingBox(k, l, k + 15, l + 15));
                flag = true;
            }
        }
        return flag;
    }

    public boolean hasStructureAt(int x, int y, int z) {
        return this.func_143028_c(x, y, z) != null;
    }

    protected StructureStart func_143028_c(int x, int y, int z) {
        for (Object o : this.structureMap.values()) {
            StructureStart start = (StructureStart) o;
            if (start.isSizeableStructure() && start.getBoundingBox().intersectsWith(x, z, x, z)) {
                for (Object c : start.getComponents()) {
                    if (((StructureComponent) c).getBoundingBox().isVecInside(x, y, z)) return start;
                }
            }
        }
        return null;
    }

    public boolean func_142038_b(int x, int y, int z) {
        for (Object o : this.structureMap.values()) {
            StructureStart start = (StructureStart) o;
            if (start.isSizeableStructure() && start.getBoundingBox().intersectsWith(x, z, x, z)) return true;
        }
        return false;
    }

    public ChunkPosition getNearestInstance(World world, int x, int y, int z) {
        this.worldObj = world;
        this.rand.setSeed(world.getSeed());
        long l = this.rand.nextLong();
        long i1 = this.rand.nextLong();
        long j1 = (long) (x >> 4) * l;
        long k1 = (long) (z >> 4) * i1;
        this.rand.setSeed(j1 ^ k1 ^ world.getSeed());
        this.func_151538_a(world, x >> 4, z >> 4, 0, 0, null);
        double best = Double.MAX_VALUE;
        ChunkPosition found = null;
        for (Object o : this.structureMap.values()) {
            StructureStart start = (StructureStart) o;
            if (start.isSizeableStructure() && !start.getComponents().isEmpty()) {
                ChunkPosition p = ((StructureComponent) start.getComponents().get(0)).func_151553_a();
                int dx = p.chunkPosX - x;
                int dy = p.chunkPosY - y;
                int dz = p.chunkPosZ - z;
                double d = dx * dx + dy * dy + dz * dz;
                if (d < best) {
                    best = d;
                    found = p;
                }
            }
        }
        if (found != null) return found;
        List list = this.getCoordList();
        if (list != null) {
            ChunkPosition nearest = null;
            for (Object o : list) {
                ChunkPosition p = (ChunkPosition) o;
                int dx = p.chunkPosX - x;
                int dy = p.chunkPosY - y;
                int dz = p.chunkPosZ - z;
                double d = dx * dx + dy * dy + dz * dz;
                if (d < best) {
                    best = d;
                    nearest = p;
                }
            }
            return nearest;
        }
        return null;
    }

    protected List getCoordList() {
        return null;
    }

    /**
     * The saved starts of this generator (mods read it by reflection to update a start, Twilight Forest marks conquered
     * strongholds in it). It lives in memory with the generator: starts are decided again from the seed, see the class doc.
     */
    private volatile MapGenStructureData field_143029_e;

    /** Prepares the structure data (1.7.10 loaded it from the world's saved data here). */
    protected void func_143027_a(World world) {
        if (field_143029_e == null) {
            synchronized (this) {
                if (field_143029_e == null) field_143029_e = new MapGenStructureData(getStructureName());
            }
        }
    }

    /** Records a start's current state (its pieces and the mod's own NBT) in the structure data. */
    protected void func_143026_a(int x, int z, StructureStart start) {
        func_143027_a(worldObj);
        try {
            field_143029_e.func_143043_a(start.func_143021_a(x, z), x, z);
            field_143029_e.markDirty();
        } catch (RuntimeException e) {
            // a start that cannot write itself is just not recorded
        }
    }

    protected abstract boolean canSpawnStructureAtCoords(int x, int z);

    protected abstract StructureStart getStructureStart(int x, int z);
}
