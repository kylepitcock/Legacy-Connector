package legacyconnector.v1_12_2.net.minecraftforge.client.event;

import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.EntityRenderer;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/**
 * The camera's view: fog (density, colour, distance), the camera angles and the field of view, posted from the game's viewport events.
 * {@code getState()} is the block at the camera.
 */
public abstract class EntityViewRenderEvent extends Event {
    private final EntityRenderer renderer;
    private final Entity entity;
    private final IBlockState state;
    private final double renderPartialTicks;

    public EntityViewRenderEvent(EntityRenderer renderer, Entity entity, IBlockState state, double renderPartialTicks) {
        this.renderer = renderer;
        this.entity = entity;
        this.state = state;
        this.renderPartialTicks = renderPartialTicks;
    }

    public EntityRenderer getRenderer() {
        return renderer;
    }

    public Entity getEntity() {
        return entity;
    }

    public IBlockState getState() {
        return state;
    }

    public double getRenderPartialTicks() {
        return renderPartialTicks;
    }

    /** Cancelled with a density: exponential fog of that density. */
    @Cancelable
    public static class FogDensity extends EntityViewRenderEvent {
        private float density;

        public FogDensity(EntityRenderer renderer, Entity entity, IBlockState state, double partial, float density) {
            super(renderer, entity, state, partial);
            this.density = density;
        }

        public float getDensity() {
            return density;
        }

        public void setDensity(float density) {
            this.density = density;
        }
    }

    /** The fog is set up: handlers set it with GL fog calls (captured and applied to the game's fog). */
    public static class RenderFogEvent extends EntityViewRenderEvent {
        private final int fogMode;
        private final float farPlaneDistance;

        public RenderFogEvent(EntityRenderer renderer, Entity entity, IBlockState state, double partial, int fogMode, float farPlaneDistance) {
            super(renderer, entity, state, partial);
            this.fogMode = fogMode;
            this.farPlaneDistance = farPlaneDistance;
        }

        public int getFogMode() {
            return fogMode;
        }

        public float getFarPlaneDistance() {
            return farPlaneDistance;
        }
    }

    public static class FogColors extends EntityViewRenderEvent {
        private float red;
        private float green;
        private float blue;

        public FogColors(EntityRenderer renderer, Entity entity, IBlockState state, double partial, float red, float green, float blue) {
            super(renderer, entity, state, partial);
            this.red = red;
            this.green = green;
            this.blue = blue;
        }

        public float getRed() {
            return red;
        }

        public void setRed(float red) {
            this.red = red;
        }

        public float getGreen() {
            return green;
        }

        public void setGreen(float green) {
            this.green = green;
        }

        public float getBlue() {
            return blue;
        }

        public void setBlue(float blue) {
            this.blue = blue;
        }
    }

    /** The camera's yaw, pitch and roll (handlers shake or tilt it). */
    @Cancelable
    public static class CameraSetup extends EntityViewRenderEvent {
        private float yaw;
        private float pitch;
        private float roll;

        public CameraSetup(EntityRenderer renderer, Entity entity, IBlockState state, double partial, float yaw, float pitch, float roll) {
            super(renderer, entity, state, partial);
            this.yaw = yaw;
            this.pitch = pitch;
            this.roll = roll;
        }

        public float getYaw() {
            return yaw;
        }

        public void setYaw(float yaw) {
            this.yaw = yaw;
        }

        public float getPitch() {
            return pitch;
        }

        public void setPitch(float pitch) {
            this.pitch = pitch;
        }

        public float getRoll() {
            return roll;
        }

        public void setRoll(float roll) {
            this.roll = roll;
        }
    }

    /** The field of view in degrees. */
    public static class FOVModifier extends EntityViewRenderEvent {
        private float fov;

        public FOVModifier(EntityRenderer renderer, Entity entity, IBlockState state, double partial, float fov) {
            super(renderer, entity, state, partial);
            this.fov = fov;
        }

        public float getFOV() {
            return fov;
        }

        public void setFOV(float fov) {
            this.fov = fov;
        }
    }
}
