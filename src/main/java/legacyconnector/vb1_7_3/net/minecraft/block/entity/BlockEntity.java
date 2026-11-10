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
    private static final Map<String,String> VANILLA_TYPES=Map.of(
            "Furnace","FurnaceBlockEntity","Chest","ChestBlockEntity","RecordPlayer","JukeboxBlockEntity",
            "Trap","DispenserBlockEntity","Sign","SignBlockEntity","MobSpawner","MobSpawnerBlockEntity",
            "Music","NoteBlockBlockEntity","Piston","PistonBlockEntity");
    static {
        create(ChestBlockEntity.class,"Chest");
        create(SignBlockEntity.class,"Sign");
        create(MobSpawnerBlockEntity.class,"MobSpawner");
    }

    public World world;
    public int x;
    public int y;
    public int z;
    protected boolean removed;

    /** The modern block entity hosting this one, or null. */
    public net.minecraft.world.level.block.entity.BlockEntity mcc$host;

    public BlockEntity() {}

    /** MCP {@code addMapping}. */
    public static synchronized void create(Class<?> type, String id) {
        if (classToId.containsKey(type)) throw new IllegalArgumentException("Duplicate id: " + id);
        idToClass.put(id, type);
        classToId.put(type, id);
    }

    public static synchronized Class<?> mcc$classOf(String id) {
        Class<?> registered=idToClass.get(id);
        if(registered!=null)return registered;
        String vanilla=VANILLA_TYPES.get(id);if(vanilla==null)return null;
        // Other vanilla classes may be supplied by the generated API layer rather than this shim.
        for(String prefix:new String[]{"legacyconnector.vb1_7_3.","legacyconnector_synth.vb1_7_3."})try{
            Class<?> type=Class.forName(prefix+"net.minecraft.block.entity."+vanilla,false,BlockEntity.class.getClassLoader());
            if(BlockEntity.class.isAssignableFrom(type)){idToClass.putIfAbsent(id,type);classToId.putIfAbsent(type,id);return idToClass.get(id);}
        }catch(ClassNotFoundException ignored){}
        return null;
    }

    public static synchronized String mcc$idOf(Class<?> type) {
        String registered=classToId.get(type);if(registered!=null)return registered;
        for(var entry:VANILLA_TYPES.entrySet())for(String prefix:new String[]{"legacyconnector.vb1_7_3.","legacyconnector_synth.vb1_7_3."})
            if(type.getName().equals(prefix+"net.minecraft.block.entity."+entry.getValue())){
                idToClass.putIfAbsent(entry.getKey(),type);classToId.put(type,entry.getKey());return entry.getKey();
            }
        return null;
    }

    public void readNbt(NbtCompound nbt) {
        x = nbt.getInt("x");
        y = nbt.getInt("y");
        z = nbt.getInt("z");
    }

    public void writeNbt(NbtCompound nbt) {
        String id = mcc$idOf(getClass());
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
            Class<?> type = mcc$classOf(nbt.getString("id"));
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
