package legacyconnector.v1_12_2.net.minecraft.world.chunk.storage;

import java.io.File;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraft.world.chunk.Chunk;

/**
 * 1.12.2 Anvil chunk storage of a dimension folder. The modern game loads and saves chunks itself, so loading answers with a view
 * of the loaded chunk and saving marks it for the game's save; the static entity helpers create entities from saved NBT (with
 * their passengers) and add them to a world, as 1.12's structure and spawner code used them.
 */
public class AnvilChunkLoader implements IChunkLoader {
    public final File chunkSaveLocation;

    public AnvilChunkLoader(File chunkSaveLocationIn, legacyconnector.v1_12_2.net.minecraft.util.datafix.DataFixer dataFixerIn) {
        this.chunkSaveLocation = chunkSaveLocationIn;
    }

    @Override
    public Chunk loadChunk(World worldIn, int x, int z) {
        if (worldIn == null || worldIn.mcc$liveLevel() == null || !worldIn.isChunkGeneratedAt(x, z)) return null;
        return worldIn.getChunk(x, z);
    }

    /** Forge: loads the chunk if it is saved (the modern game loads it); index 0 the chunk, 1 its NBT (not exposed: null). */
    public Object[] loadChunk__Async(World worldIn, int x, int z) {
        Chunk c = loadChunk(worldIn, x, z);
        return c == null ? null : new Object[] {c, new NBTTagCompound()};
    }

    @Override
    public boolean isChunkGeneratedAt(int x, int z) {
        return RegionFileCache.chunkExists(chunkSaveLocation, x, z);
    }

    @Override
    public void saveChunk(World worldIn, Chunk chunkIn) {
        if (chunkIn != null) chunkIn.markDirty();
    }

    @Override
    public void saveExtraChunkData(World worldIn, Chunk chunkIn) {
    }

    @Override
    public void chunkTick() {
    }

    @Override
    public void flush() {
    }

    public int getPendingSaveCount() {
        return 0;
    }

    /** An entity (and its passengers) from saved NBT, positioned at x, y, z; spawned into the world when {@code attemptSpawn}. */
    public static Entity readWorldEntityPos(NBTTagCompound compound, World worldIn, double x, double y, double z, boolean attemptSpawn) {
        Entity entity = createEntityFromNBT(compound, worldIn);
        if (entity == null) return null;
        if (entity.mcc$entity != null) {
            entity.mcc$entity.moveTo(x, y, z, entity.mcc$entity.getYRot(), entity.mcc$entity.getXRot());
            for (var passenger : entity.mcc$entity.getIndirectPassengers()) passenger.moveTo(x, y, z, passenger.getYRot(), passenger.getXRot());
        }
        if (attemptSpawn) spawnEntity(entity, worldIn);
        return entity;
    }

    /** An entity (and its passengers) from saved NBT at its saved position; spawned into the world when {@code spawn}. */
    public static Entity readWorldEntity(NBTTagCompound compound, World worldIn, boolean spawn) {
        Entity entity = createEntityFromNBT(compound, worldIn);
        if (entity != null && spawn) spawnEntity(entity, worldIn);
        return entity;
    }

    protected static Entity createEntityFromNBT(NBTTagCompound compound, World worldIn) {
        var level = worldIn == null ? null : worldIn.mcc$level();
        if (level == null || compound == null || !(compound.mcc$modern() instanceof net.minecraft.nbt.CompoundTag tag)) return null;
        try {
            var created = net.minecraft.world.entity.EntityType.loadEntityRecursive(tag.copy(), level, e -> e);
            return org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(created);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Adds an entity and its passengers to the world (in a generation region, to the region). */
    public static void spawnEntity(Entity entityIn, World worldIn) {
        if (entityIn == null || entityIn.mcc$entity == null || worldIn == null) return;
        var modern = entityIn.mcc$entity;
        var access = worldIn.mcc$access();
        if (access instanceof net.minecraft.world.level.ServerLevelAccessor server) {
            server.addFreshEntityWithPassengers(modern);
        } else if (worldIn.spawnEntity(entityIn)) {
            for (var p : modern.getPassengers()) worldIn.spawnEntity(org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(p));
        }
    }
}
