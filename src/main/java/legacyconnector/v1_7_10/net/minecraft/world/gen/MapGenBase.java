package legacyconnector.v1_7_10.net.minecraft.world.gen;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraft.world.chunk.IChunkProvider;

/** Base of chunk-spanning generators (caves, ravines, structures): called once per chunk for the chunks around it. */
public class MapGenBase {
    protected int range = 8;
    protected Random rand = new Random();
    protected World worldObj;

    /**
     * Block id tables of the legacy versions that keep their blocks as numeric ids (1.6.4 and older); the forward layer installs them with its id
     * registry. Without them the byte[] shaped members below are inert.
     */
    public static volatile java.util.function.IntFunction<Block> mcc$blockById;
    public static volatile java.util.function.ToIntFunction<Block> mcc$idOfBlock;

    private static final ClassValue<Boolean> LEGACY_RECURSIVE = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            for (Class<?> c = type; c != null && c != MapGenBase.class; c = c.getSuperclass()) {
                try {
                    c.getDeclaredMethod("recursiveGenerate", World.class, int.class, int.class, int.class, int.class, byte[].class);
                    return Boolean.TRUE;
                } catch (NoSuchMethodException e) {
                    // not declared here
                }
            }
            return Boolean.FALSE;
        }
    };

    public void generate(IChunkProvider provider, World world, int chunkX, int chunkZ, Block[] blocks) {
        int k = this.range;
        this.worldObj = world;
        this.rand.setSeed(world.getSeed());
        long l = this.rand.nextLong();
        long i1 = this.rand.nextLong();
        // a legacy generator overriding recursiveGenerate(..., byte[]) works on block ids: convert once for the whole neighbourhood
        java.util.function.ToIntFunction<Block> idOf = mcc$idOfBlock;
        byte[] ids = idOf != null && blocks != null && LEGACY_RECURSIVE.get(getClass()) ? toIds(blocks, idOf) : null;
        byte[] before = ids == null ? null : ids.clone();
        for (int j1 = chunkX - k; j1 <= chunkX + k; ++j1) {
            for (int k1 = chunkZ - k; k1 <= chunkZ + k; ++k1) {
                long l1 = j1 * l;
                long i2 = k1 * i1;
                this.rand.setSeed(l1 ^ i2 ^ world.getSeed());
                if (ids != null) this.recursiveGenerate(world, j1, k1, chunkX, chunkZ, ids);
                else this.func_151538_a(world, j1, k1, chunkX, chunkZ, blocks);
            }
        }
        if (ids != null) copyBack(before, ids, blocks, mcc$blockById);
    }

    /** The 1.6.4 shape of {@code generate}: the chunk is an array of block ids (index {@code x << 12 | z << 8 | y}, as the {@code Block[]} of 1.7.10 is laid out). */
    public void generate(IChunkProvider provider, World world, int chunkX, int chunkZ, byte[] ids) {
        java.util.function.IntFunction<Block> byId = mcc$blockById;
        java.util.function.ToIntFunction<Block> idOf = mcc$idOfBlock;
        if (byId == null || idOf == null || ids == null) return;
        Block[] blocks = new Block[ids.length];
        for (int i = 0; i < ids.length; i++) blocks[i] = byId.apply(ids[i] & 255);
        byte[] before = ids.clone();
        generate(provider, world, chunkX, chunkZ, blocks);
        copyBack(before, toIds(blocks, idOf), ids);
    }

    /** Legacy {@code recursiveGenerate} on block ids: what 1.6.4 generators override instead of {@code func_151538_a}. */
    protected void recursiveGenerate(World world, int x, int z, int chunkX, int chunkZ, byte[] ids) {
    }

    /** The ids of a block array as bytes (ids above 255 cannot be represented and read as 0). */
    public static <T> byte[] toIds(T[] blocks, java.util.function.ToIntFunction<T> idOf) {
        byte[] out = new byte[blocks.length];
        for (int i = 0; i < blocks.length; i++) {
            int id = blocks[i] == null ? 0 : idOf.applyAsInt(blocks[i]);
            out[i] = id > 0 && id < 256 ? (byte) id : 0;
        }
        return out;
    }

    /** Writes the entries a legacy generator changed (after != before) of {@code ids} back into the block array; untouched entries keep their block. */
    public static <T> void copyBack(byte[] before, byte[] after, T[] blocks, java.util.function.IntFunction<T> byId) {
        for (int i = 0; i < blocks.length && i < after.length; i++) {
            if (after[i] != before[i]) blocks[i] = byId.apply(after[i] & 255);
        }
    }

    /** Writes the entries that differ from {@code before} of {@code after} into {@code ids}. */
    private static void copyBack(byte[] before, byte[] after, byte[] ids) {
        for (int i = 0; i < ids.length && i < after.length; i++) {
            if (after[i] != before[i]) ids[i] = after[i];
        }
    }

    protected void func_151538_a(World world, int x, int z, int chunkX, int chunkZ, Block[] blocks) {
    }
}
