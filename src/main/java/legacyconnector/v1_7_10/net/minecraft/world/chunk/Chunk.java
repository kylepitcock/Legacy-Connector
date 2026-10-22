package legacyconnector.v1_7_10.net.minecraft.world.chunk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_7_10.net.minecraft.world.ChunkCoordIntPair;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * 1.7.10 chunk: a view of a modern chunk through its world (block, metadata and tile entity access by local
 * coordinates, biome ids per column, the modified flag).
 */
public class Chunk {
    public final int xPosition;
    public final int zPosition;
    public World worldObj;
    public boolean isChunkLoaded = true;
    public boolean isTerrainPopulated = true;
    public boolean isLightPopulated = true;
    public boolean isModified;
    public boolean hasEntities;
    public Map<Object, TileEntity> chunkTileEntityMap = new HashMap<>();
    @SuppressWarnings("unchecked")
    public List<Object>[] entityLists = new List[16];
    public int[] heightMap = new int[256];
    private final ChunkAccess modern;
    /** Array mode (a chunk a mod's generator builds): blocks as x << 12 | z << 8 | y, metadata by the same index. */
    private Block[] mcc$blocks;
    private byte[] mcc$meta;
    private byte[] mcc$biomes;

    /** While set on a thread, {@code new Chunk(world, x, z)} builds a detached array chunk (a generator's new chunk). */
    private static final ThreadLocal<Boolean> DETACHED = new ThreadLocal<>();

    public static void mcc$beginDetached() {
        DETACHED.set(Boolean.TRUE);
    }

    public static void mcc$endDetached() {
        DETACHED.remove();
    }

    public Chunk(World world, int x, int z) {
        this(world, x, z, null);
        if (DETACHED.get() != null) {
            // 1.7.10's fresh chunk: empty storage the generator fills (directly or through ExtendedBlockStorage).
            this.mcc$blocks = new Block[65536];
            this.mcc$meta = new byte[65536];
            this.mcc$height = 256;
            this.isTerrainPopulated = false;
            this.isLightPopulated = false;
        }
    }

    public Chunk(World world, ChunkAccess modern) {
        this(world, modern.getPos().x, modern.getPos().z, modern);
    }

    /** A chunk of 256-high column arrays, as legacy terrain generators build them. */
    public Chunk(World world, Block[] blocks, byte[] meta, int x, int z) {
        this(world, x, z, null);
        this.mcc$blocks = blocks;
        this.mcc$height = Math.max(1, blocks.length / 256);
        this.mcc$meta = meta == null ? new byte[blocks.length] : meta;
        this.isTerrainPopulated = false;
        this.isLightPopulated = false;
    }

    public boolean mcc$hasArrays() {
        return mcc$blocks != null;
    }

    public Block[] mcc$blocks() {
        return mcc$blocks;
    }

    public byte[] mcc$meta() {
        return mcc$meta;
    }

    /** Column height of an array chunk: the arrays hold (x * 16 + z) * height + y (vanilla 256, some mods' 128). */
    private int mcc$height = 256;

    public int mcc$arrayHeight() {
        return mcc$height;
    }

    private int index(int x, int y, int z) {
        return ((x & 15) * 16 + (z & 15)) * mcc$height + y;
    }

    private Chunk(World world, int x, int z, ChunkAccess modern) {
        this.worldObj = world;
        this.xPosition = x;
        this.zPosition = z;
        this.modern = modern;
        for (int i = 0; i < entityLists.length; i++) entityLists[i] = new ArrayList<>();
    }

    public ChunkAccess mcc$modern() {
        return modern;
    }

    private int wx(int x) {
        return (xPosition << 4) + (x & 15);
    }

    private int wz(int z) {
        return (zPosition << 4) + (z & 15);
    }

    public boolean isAtLocation(int x, int z) {
        return x == xPosition && z == zPosition;
    }

    public Block getBlock(int x, int y, int z) {
        if (mcc$blocks != null) {
            if (y < 0 || y >= mcc$height) return legacyconnector.v1_7_10.net.minecraft.init.Blocks.air;
            Block b = mcc$blocks[index(x, y, z)];
            return b == null ? legacyconnector.v1_7_10.net.minecraft.init.Blocks.air : b;
        }
        return worldObj.getBlock(wx(x), y, wz(z));
    }

    public int getBlockMetadata(int x, int y, int z) {
        if (mcc$blocks != null) return y < 0 || y >= mcc$height ? 0 : mcc$meta[index(x, y, z)] & 15;
        return worldObj.getBlockMetadata(wx(x), y, wz(z));
    }

    public boolean setBlockMetadata(int x, int y, int z, int meta) {
        if (mcc$blocks != null) {
            if (y < 0 || y >= mcc$height) return false;
            mcc$meta[index(x, y, z)] = (byte) meta;
            return true;
        }
        return worldObj.setBlockMetadataWithNotify(wx(x), y, wz(z), meta, 2);
    }

    public boolean func_150807_a(int x, int y, int z, Block block, int meta) {
        if (mcc$blocks != null) {
            if (y < 0 || y >= mcc$height) return false;
            int i = index(x, y, z);
            mcc$blocks[i] = block;
            mcc$meta[i] = (byte) meta;
            return true;
        }
        return worldObj.setBlock(wx(x), y, wz(z), block, meta, 2);
    }

    public int getHeightValue(int x, int z) {
        if (mcc$blocks != null) {
            for (int y = mcc$height - 1; y > 0; y--) {
                Block b = mcc$blocks[index(x, y, z)];
                if (b != null && b != legacyconnector.v1_7_10.net.minecraft.init.Blocks.air) return y + 1;
            }
            return 0;
        }
        return worldObj.getHeightValue(wx(x), wz(z));
    }

    public int getTopFilledSegment() {
        if (modern == null) return 0;
        int top = modern.getHighestFilledSectionIndex();
        return top < 0 ? 0 : modern.getSectionYFromSectionIndex(top) << 4;
    }

    /** Legacy biome id per column (z << 4 | x), from the modern biomes at the surface. */
    public byte[] getBiomeArray() {
        if (mcc$biomes != null) return mcc$biomes;
        if (mcc$blocks != null) return mcc$biomes = new byte[256];
        byte[] ids = new byte[256];
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int wx = wx(x), wz = wz(z);
                var biome = worldObj.getBiomeGenForCoords(wx, wz);
                ids[z << 4 | x] = (byte) (biome == null ? 1 : biome.biomeID);
            }
        }
        return ids;
    }

    public void setBiomeArray(byte[] biomes) {
        this.mcc$biomes = biomes;
    }

    public legacyconnector.v1_7_10.net.minecraft.world.biome.BiomeGenBase getBiomeGenForWorldCoords(int x, int z, Object chunkManager) {
        return worldObj.getBiomeGenForCoords(wx(x), wz(z));
    }

    public TileEntity func_150806_e(int x, int y, int z) {
        return worldObj.getTileEntity(wx(x), y, wz(z));
    }

    public TileEntity getTileEntityUnsafe(int x, int y, int z) {
        return func_150806_e(x, y, z);
    }

    public void removeTileEntity(int x, int y, int z) {
        worldObj.removeTileEntity(wx(x), y, wz(z));
    }

    public void setChunkModified() {
        isModified = true;
        if (modern != null) modern.setUnsaved(true);
    }

    public boolean needsSaving(boolean all) {
        return modern != null && modern.isUnsaved();
    }

    private legacyconnector.v1_7_10.net.minecraft.world.chunk.storage.ExtendedBlockStorage[] mcc$storage;

    /** Sixteen 16-block sections covering legacy heights 0-255, as views onto this chunk. */
    public legacyconnector.v1_7_10.net.minecraft.world.chunk.storage.ExtendedBlockStorage[] getBlockStorageArray() {
        if (mcc$storage == null) {
            mcc$storage = new legacyconnector.v1_7_10.net.minecraft.world.chunk.storage.ExtendedBlockStorage[16];
            for (int i = 0; i < 16; i++) mcc$storage[i] = new legacyconnector.v1_7_10.net.minecraft.world.chunk.storage.ExtendedBlockStorage(this, i << 4);
        }
        return mcc$storage;
    }

    public void setStorageArrays(legacyconnector.v1_7_10.net.minecraft.world.chunk.storage.ExtendedBlockStorage[] storage) {
    }

    public ChunkCoordIntPair getChunkCoordIntPair() {
        return new ChunkCoordIntPair(xPosition, zPosition);
    }

    public Random getRandomWithSeed(long seed) {
        return new Random(worldObj.getSeed() + (long) xPosition * xPosition * 4987142 + xPosition * 5947611L
                + (long) zPosition * zPosition * 4392871L + zPosition * 389711L ^ seed);
    }

    public boolean getAreLevelsEmpty(int minY, int maxY) {
        if (modern == null) return true;
        for (int y = minY; y <= maxY; y += 16) {
            if (y < modern.getMinBuildHeight() || y >= modern.getMaxBuildHeight()) continue;
            if (!modern.getSection(modern.getSectionIndex(y)).hasOnlyAir()) return false;
        }
        return true;
    }

    public boolean canBlockSeeTheSky(int x, int y, int z) {
        return worldObj.canBlockSeeTheSky(wx(x), y, wz(z));
    }

    public void generateSkylightMap() {
    }

    public void generateHeightMap() {
    }

    public void resetRelightChecks() {
    }

    public void enqueueRelightChecks() {
    }

    public void onChunkLoad() {
    }

    public void onChunkUnload() {
    }

    public boolean isEmpty() {
        return modern == null;
    }

    public BlockPos mcc$pos(int x, int y, int z) {
        return new BlockPos(wx(x), y, wz(z));
    }
}
