package legacyconnector.v1_7_10.net.minecraft.client.multiplayer;

import legacyconnector.v1_7_10.net.minecraft.world.World;
import net.minecraft.world.level.LevelAccessor;

/** Client world view; {@code World.mcc$wrap} returns this for client levels. */
public class WorldClient extends World {
    public WorldClient(LevelAccessor level) {
        super(level);
    }

    public legacyconnector.v1_7_10.net.minecraft.util.Vec3 getSkyColor(legacyconnector.v1_7_10.net.minecraft.entity.Entity entity, float partial) {
        var level = net.minecraft.client.Minecraft.getInstance().level;
        if (level == null) return legacyconnector.v1_7_10.net.minecraft.util.Vec3.createVectorHelper(0.5D, 0.7D, 1.0D);
        var color = level.getSkyColor(net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera().getPosition(), partial);
        return legacyconnector.v1_7_10.net.minecraft.util.Vec3.createVectorHelper(color.x, color.y, color.z);
    }

    public double getHorizon() {
        return 63.0D;
    }

    /** How bright the stars are: full at night, none by day. */
    public float getStarBrightness(float partialTicks) {
        float angle = getCelestialAngle(partialTicks);
        float brightness = 1.0F - (net.minecraft.util.Mth.cos(angle * (float) Math.PI * 2.0F) * 2.0F + 0.25F);
        brightness = Math.max(0.0F, Math.min(1.0F, brightness));
        return brightness * brightness * 0.5F;
    }

    /** Sets how hard it rains on the client (0 clear, 1 full). */
    public void setRainStrength(float strength) {
        var level = mcc$fullLevel();
        if (level != null) level.setRainLevel(strength);
    }
}
