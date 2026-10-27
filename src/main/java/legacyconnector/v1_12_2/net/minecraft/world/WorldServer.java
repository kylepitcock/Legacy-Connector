package legacyconnector.v1_12_2.net.minecraft.world;

/** 1.12.2 server world: a view over a modern {@link net.minecraft.server.level.ServerLevel}. */
public class WorldServer extends World {
    protected WorldServer(net.minecraft.server.level.ServerLevel level) {
        super(level);
    }

    public net.minecraft.server.level.ServerLevel mcc$serverLevel() {
        return (net.minecraft.server.level.ServerLevel) mcc$level();
    }

    public legacyconnector.v1_12_2.net.minecraft.entity.Entity getEntityFromUuid(java.util.UUID uuid) {
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(mcc$serverLevel().getEntity(uuid));
    }

    public boolean addScheduledTask(Runnable task) {
        mcc$serverLevel().getServer().execute(task);
        return true;
    }

    public boolean isCallingFromMinecraftThread() {
        return mcc$serverLevel().getServer().isSameThread();
    }

    public void saveAllChunks(boolean all, Object progress) {
        mcc$serverLevel().save(null, true, false);
    }
}
