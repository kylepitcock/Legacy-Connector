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
}
