package legacyconnector.v1_12_2.net.minecraft.world;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EnumCreatureType;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.biome.Biome;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;

/** A server world: a view over a {@link ServerLevel} (or a generation region of one while legacy generators run). */
public class WorldServer extends World implements legacyconnector.v1_12_2.net.minecraft.util.IThreadListener {
    /** Forge: teleporters mods add for this world. */
    public final List<Teleporter> customTeleporters = new ArrayList<>();
    private final Teleporter worldTeleporter;

    public WorldServer(LevelAccessor access) {
        super(access);
        this.worldTeleporter = new Teleporter(this);
    }

    private ServerLevel server() {
        return mcc$level() instanceof ServerLevel s ? s : null;
    }

    @Override
    public com.google.common.util.concurrent.ListenableFuture<Object> addScheduledTask(Runnable task) {
        com.google.common.util.concurrent.SettableFuture<Object> future = com.google.common.util.concurrent.SettableFuture.create();
        ServerLevel s = server();
        Runnable run = () -> {
            try {
                task.run();
                future.set(null);
            } catch (Throwable t) {
                future.setException(t);
            }
        };
        if (s == null) run.run();
        else if (s.getServer().isSameThread()) run.run();
        else s.getServer().execute(run);
        return future;
    }

    @Override
    public boolean isCallingFromMinecraftThread() {
        ServerLevel s = server();
        return s == null || s.getServer().isSameThread();
    }

    public Entity getEntityFromUuid(java.util.UUID uuid) {
        ServerLevel s = server();
        if (s == null || uuid == null || mcc$liveLevel() == null) return null;
        var e = s.getEntity(uuid);
        return e == null ? null : org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(e);
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraft.world.gen.ChunkProviderServer getChunkProvider() {
        return mcc$serverChunkProvider();
    }

    public Teleporter getDefaultTeleporter() {
        return worldTeleporter;
    }

    public void resetUpdateEntityTick() {
    }

    public BlockPos getSpawnCoordinate() {
        return mcc$get$provider(this).getSpawnCoordinate();
    }

    public java.io.File getChunkSaveLocation() {
        ServerLevel s = server();
        if (s == null) return new java.io.File(".");
        return net.minecraft.world.level.dimension.DimensionType.getStorageFolder(s.dimension(),
                s.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)).toFile();
    }

    public legacyconnector.v1_12_2.net.minecraft.world.gen.structure.template.TemplateManager getStructureTemplateManager() {
        return getSaveHandler().getStructureTemplateManager();
    }

    /** What may spawn here: the biome's list for the creature type (the modern game does the spawning). */
    public List<Biome.SpawnListEntry> mcc$possibleCreatures(EnumCreatureType type, BlockPos pos) {
        return mcc$serverChunkProvider().getPossibleCreatures(type, pos);
    }

    public Biome.SpawnListEntry getSpawnListEntryForTypeAt(EnumCreatureType type, BlockPos pos) {
        List<Biome.SpawnListEntry> list = mcc$possibleCreatures(type, pos);
        if (list == null || list.isEmpty()) return null;
        int total = 0;
        for (Biome.SpawnListEntry e : list) total += e.itemWeight;
        if (total <= 0) return null;
        int pick = rand.nextInt(total);
        for (Biome.SpawnListEntry e : list) {
            pick -= e.itemWeight;
            if (pick < 0) return e;
        }
        return null;
    }

    public boolean canCreatureTypeSpawnHere(EnumCreatureType type, Biome.SpawnListEntry entry, BlockPos pos) {
        List<Biome.SpawnListEntry> list = mcc$possibleCreatures(type, pos);
        return list != null && list.contains(entry);
    }

    public void saveAllChunks(boolean all, Object progress) {
    }

    public void flush() {
    }

    public boolean areAllPlayersAsleep() {
        ServerLevel s = server();
        if (s == null || s.players().isEmpty()) return false;
        for (var p : s.players()) if (!p.isSpectator() && !p.isSleepingLongEnough()) return false;
        return true;
    }
}
