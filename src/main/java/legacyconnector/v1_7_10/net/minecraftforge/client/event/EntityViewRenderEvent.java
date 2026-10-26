package legacyconnector.v1_7_10.net.minecraftforge.client.event;

import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;

/** Events fired while the camera and fog of the frame are set up. */
public abstract class EntityViewRenderEvent extends Event {
    public final EntityLivingBase entity;
    public final double renderPartialTicks;
    /** The block at the camera (water, lava, a mod fluid): mods colour the fog by it. */
    public final legacyconnector.v1_7_10.net.minecraft.block.Block block;

    private static legacyconnector.v1_7_10.net.minecraft.block.Block mcc$blockAtCamera(EntityLivingBase entity) {
        try {
            var camera = net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera();
            var level = net.minecraft.client.Minecraft.getInstance().level;
            if (level == null) return legacyconnector.v1_7_10.net.minecraft.init.Blocks.air;
            var state = level.getBlockState(camera.getBlockPosition());
            var fluid = camera.getFluidInCamera();
            if (fluid == net.minecraft.world.level.material.FogType.WATER) state = net.minecraft.world.level.block.Blocks.WATER.defaultBlockState();
            else if (fluid == net.minecraft.world.level.material.FogType.LAVA) state = net.minecraft.world.level.block.Blocks.LAVA.defaultBlockState();
            var legacy = org.kylepitcock.legacyconnector.v1_7_10.bridge.BlockBridge.legacyBlock(state);
            return legacy != null ? legacy : legacyconnector.v1_7_10.net.minecraft.init.Blocks.air;
        } catch (RuntimeException | LinkageError e) {
            return legacyconnector.v1_7_10.net.minecraft.init.Blocks.air;
        }
    }

    public EntityViewRenderEvent(EntityLivingBase entity, double partialTicks) {
        this.entity = entity;
        this.renderPartialTicks = partialTicks;
        this.block = mcc$blockAtCamera(entity);
    }

    /** Handlers change the colour the sky fades to at the horizon (the fog colour). */
    public static class FogColors extends EntityViewRenderEvent {
        public float red;
        public float green;
        public float blue;

        public FogColors(EntityLivingBase entity, double partialTicks, float red, float green, float blue) {
            super(entity, partialTicks);
            this.red = red;
            this.green = green;
            this.blue = blue;
        }
    }

    @Cancelable
    public static class FogDensity extends EntityViewRenderEvent {
        public float density;

        public FogDensity(EntityLivingBase entity, double partialTicks, float density) {
            super(entity, partialTicks);
            this.density = density;
        }
    }

    /**
     * Fired after the fog of the frame is set up (outside fluids and blindness); handlers change it with
     * {@code GL11.glFogf(GL_FOG_START / GL_FOG_END, ...)}. {@code fogMode} is -1 for the sky's fog and 0 for the terrain's,
     * {@code farPlaneDistance} the render distance in blocks.
     */
    public static class RenderFogEvent extends EntityViewRenderEvent {
        public final int fogMode;
        public final float farPlaneDistance;

        public RenderFogEvent(EntityLivingBase entity, double partialTicks, int fogMode, float farPlaneDistance) {
            super(entity, partialTicks);
            this.fogMode = fogMode;
            this.farPlaneDistance = farPlaneDistance;
        }
    }

    public static class CameraSetup extends EntityViewRenderEvent {
        public float yaw;
        public float pitch;
        public float roll;

        public CameraSetup(EntityLivingBase entity, double partialTicks, float yaw, float pitch, float roll) {
            super(entity, partialTicks);
            this.yaw = yaw;
            this.pitch = pitch;
            this.roll = roll;
        }
    }
}
