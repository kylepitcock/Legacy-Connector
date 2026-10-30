package legacyconnector.v1_12_2.net.minecraft.client.multiplayer;

import legacyconnector.v1_12_2.net.minecraft.world.World;

/** The client's world ({@code Minecraft.world}): a view over the modern client level. */
public class WorldClient extends World {
    private ChunkProviderClient clientChunkProvider;

    public WorldClient(net.minecraft.world.level.Level level) {
        super(level);
    }

    // ------------------------------------------------------------------ area R2b: the client world's own methods

    @Override
    public synchronized ChunkProviderClient getChunkProvider() {
        if (clientChunkProvider == null) clientChunkProvider = new ChunkProviderClient(this);
        return clientChunkProvider;
    }

    /** Whether the server sent the chunk. */
    protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) {
        return allowEmpty || mcc$level().hasChunk(x, z);
    }

    /** Plays a sound at the block's centre on this client only (delayed by distance when asked, as thunder is). */
    public void playSound(legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos, legacyconnector.v1_12_2.net.minecraft.util.SoundEvent sound,
            legacyconnector.v1_12_2.net.minecraft.util.SoundCategory category, float volume, float pitch, boolean distanceDelay) {
        playSound(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, sound, category, volume, pitch, distanceDelay);
    }

    /** Spawns a vanilla particle on this client. */
    public void spawnParticle(legacyconnector.v1_12_2.net.minecraft.util.EnumParticleTypes type, double x, double y, double z, double xSpeed, double ySpeed,
            double zSpeed, int... parameters) {
        spawnParticle(type, type.getShouldIgnoreRange(), x, y, z, xSpeed, ySpeed, zSpeed, parameters);
    }

    public void spawnParticle(legacyconnector.v1_12_2.net.minecraft.util.EnumParticleTypes type, boolean ignoreRange, double x, double y, double z,
            double xSpeed, double ySpeed, double zSpeed, int... parameters) {
        if (type == null) return;
        var options = type.mcc$modern(xSpeed, ySpeed, zSpeed, parameters);
        if (options != null) mcc$level().addParticle(options, ignoreRange, x, y, z, xSpeed, ySpeed, zSpeed);
    }

    /** Sends a packet to the server over the game's connection (added by area W: {@code World.sendPacketToServer}). */
    @Override
    public void sendPacketToServer(legacyconnector.v1_12_2.net.minecraft.network.Packet<?> packet) {
        legacyconnector.v1_12_2.net.minecraft.client.network.NetHandlerPlayClient.mcc$INSTANCE.sendPacket(packet);
    }

    /** Adds an entity to this client world under the network id (fake entities of client-side effects). */
    public void addEntityToWorld(int entityId, legacyconnector.v1_12_2.net.minecraft.entity.Entity entity) {
        org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldClient1122.addEntity(this, entityId, entity);
    }

    /** Removes the entity with the network id from this client world; returns it (null when there is none). */
    public legacyconnector.v1_12_2.net.minecraft.entity.Entity removeEntityFromWorld(int entityId) {
        return org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldClient1122.removeEntity(this, entityId);
    }
}
