package legacyconnector.vb1_7_3.net.minecraft.block.entity;

import java.util.HashMap;
import java.util.Map;

import legacyconnector.vb1_7_3.net.minecraft.block.Block;
import legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound;
import legacyconnector.vb1_7_3.net.minecraft.world.World;

/**
 * Beta 1.7.3 {@code TileEntity}. Hosted by one modern block entity type ({@code LegacyB173BlockEntity}): it saves this one's NBT,
 * ticks it on the server and hands it out through {@code World.getBlockTileEntity}. Classes are registered under a save id
 * ({@code ModLoader.RegisterTileEntity}), which {@link #writeNbt} writes as b1.7.3 did.
 */
public class BlockEntity {
    private static Map<String, Class<?>> idToClass = new HashMap<>();
    private static Map<Class<?>, String> classToId = new HashMap<>();

    public World world;
    public int x;
    public int y;
    public int z;
    protected boolean removed;

    /** The modern block entity hosting this one, or null. */
    public net.minecraft.world.level.block.entity.BlockEntity mcc$host;

    public BlockEntity() {}

    /** MCP {@code addMapping}. */
    public static void create(Class<?> type, String id) {
        if (classToId.containsKey(type)) throw new IllegalArgumentException("Duplicate id: " + id);
        idToClass.put(id, type);
        classToId.put(type, id);
    }

    public static synchronized Class<?> mcc$classOf(String id) {
        return idToClass.get(id);
    }

    public static synchronized String mcc$idOf(Class<?> type) {
        return classToId.get(type);
    }

    public void readNbt(NbtCompound nbt) {
        x = nbt.getInt("x");
        y = nbt.getInt("y");
        z = nbt.getInt("z");
    }

    public void writeNbt(NbtCompound nbt) {
        String id = classToId.get(getClass());
        if (id == null) throw new RuntimeException(getClass() + " is missing a mapping! This is a bug!");
        nbt.putString("id", id);
        nbt.putInt("x", x);
        nbt.putInt("y", y);
        nbt.putInt("z", z);
    }

    public void tick() {}

    /** MCP {@code createAndLoadEntity}: the tile entity of the id the NBT names, loaded from it, or null for an unknown id. */
    public static BlockEntity createFromNbt(NbtCompound nbt) {
        BlockEntity te = null;
        try {
            Class<?> type = idToClass.get(nbt.getString("id"));
            if (type != null) {
                var ctor = type.getDeclaredConstructor();
                ctor.setAccessible(true);
                te = (BlockEntity) ctor.newInstance();
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            te = null;
        }
        if (te != null) te.readNbt(nbt);
        else System.out.println("Skipping TileEntity with id " + nbt.getString("id"));
        return te;
    }

    public int getPushedBlockData() {
        return world == null ? 0 : world.getBlockMeta(x, y, z);
    }

    public void markDirty() {
        if (mcc$host != null) mcc$host.setChanged();
    }

    public double distanceFrom(double px, double py, double pz) {
        double dx = x + 0.5 - px, dy = y + 0.5 - py, dz = z + 0.5 - pz;
        return dx * dx + dy * dy + dz * dz;
    }

    public Block getBlock() {
        return world == null ? null : Block.BLOCKS[world.getBlockId(x, y, z)];
    }

    public boolean isRemoved() {
        return removed;
    }

    public void markRemoved() {
        removed = true;
    }

    public void cancelRemoval() {
        removed = false;
    }
}
