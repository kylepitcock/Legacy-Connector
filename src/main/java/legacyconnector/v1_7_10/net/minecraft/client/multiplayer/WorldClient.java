package legacyconnector.v1_7_10.net.minecraft.client.multiplayer;

import legacyconnector.v1_7_10.net.minecraft.world.World;
import net.minecraft.world.level.LevelAccessor;

/** Client world view; {@code World.mcc$wrap} returns this for client levels. */
public class WorldClient extends World {
    public WorldClient(LevelAccessor level) {
        super(level);
    }

    /**
     * The modern sky colour where the dimension's provider keeps the default (1.7.10's client asked the provider first:
     * Galacticraft's planets colour their own sky; this override had skipped them and Mars had the overworld's blue).
     */
    @Override
    public legacyconnector.v1_7_10.net.minecraft.util.Vec3 getSkyColorBody(legacyconnector.v1_7_10.net.minecraft.entity.Entity entity, float partial) {
        var level = net.minecraft.client.Minecraft.getInstance().level;
        if (level == null) return legacyconnector.v1_7_10.net.minecraft.util.Vec3.createVectorHelper(0.5D, 0.7D, 1.0D);
        var color = level.getSkyColor(net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera().getPosition(), partial);
        return legacyconnector.v1_7_10.net.minecraft.util.Vec3.createVectorHelper(color.x, color.y, color.z);
    }

    /** How bright the stars are where the provider keeps the default: full at night, none by day. */
    @Override
    public float getStarBrightnessBody(float partialTicks) {
        float angle = getCelestialAngle(partialTicks);
        float brightness = 1.0F - (net.minecraft.util.Mth.cos(angle * (float) Math.PI * 2.0F) * 2.0F + 0.25F);
        brightness = Math.max(0.0F, Math.min(1.0F, brightness));
        return brightness * brightness * 0.5F;
    }

    private net.minecraft.client.multiplayer.ClientLevel mcc$client() {
        return mcc$fullLevel() instanceof net.minecraft.client.multiplayer.ClientLevel c ? c : null;
    }

    /** Puts an entity into the client world under the given network id (client-only entities, entities a mod spawns itself). */
    public void addEntityToWorld(int id, legacyconnector.v1_7_10.net.minecraft.entity.Entity entity) {
        var level = mcc$client();
        if (level == null || entity == null || entity.mcc$entity() == null) return;
        var modern = entity.mcc$entity();
        var old = level.getEntity(id);
        if (old != null && old != modern) level.removeEntity(id, net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
        modern.setId(id);
        level.addEntity(modern);
    }

    /** Takes the entity with the network id out of the client world; returns it (null when there was none). */
    public legacyconnector.v1_7_10.net.minecraft.entity.Entity removeEntityFromWorld(int id) {
        var level = mcc$client();
        if (level == null) return null;
        var modern = level.getEntity(id);
        if (modern == null) return null;
        var legacy = org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge.wrap(modern);
        level.removeEntity(id, net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
        return legacy;
    }

    /** The random ambient effects around a point (void fog, blocks' display ticks), as the client runs them each tick. */
    public void doVoidFogParticles(int x, int y, int z) {
        var level = mcc$client();
        if (level != null) level.animateTick(x, y, z);
    }

    /** Closes the connection to the server (leaving the world follows with {@code Minecraft.loadWorld(null)}). */
    public void sendQuittingDisconnectingPacket() {
        var level = mcc$client();
        if (level != null) level.disconnect();
    }

    /** Sets how hard it rains on the client (0 clear, 1 full). */
    public void setRainStrength(float strength) {
        var level = mcc$fullLevel();
        if (level != null) level.setRainLevel(strength);
    }
}
