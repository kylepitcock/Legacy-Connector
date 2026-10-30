package legacyconnector.v1_12_2.net.minecraftforge.client.model.animation;

import legacyconnector.v1_12_2.net.minecraft.world.World;

/** Forge 1.12.2 animation clock: world time in seconds, with the partial tick of the frame being drawn. */
public enum Animation {
    INSTANCE;

    private float clientPartialTickTime;

    public static float getWorldTime(World world) {
        return getWorldTime(world, 0.0F);
    }

    public static float getWorldTime(World world, float tickProgress) {
        return world == null ? tickProgress / 20.0F : (world.getTotalWorldTime() + tickProgress) / 20.0F;
    }

    public static float getPartialTickTime() {
        try {
            return net.minecraft.client.Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        } catch (RuntimeException | LinkageError e) {
            return INSTANCE.clientPartialTickTime;
        }
    }
}
