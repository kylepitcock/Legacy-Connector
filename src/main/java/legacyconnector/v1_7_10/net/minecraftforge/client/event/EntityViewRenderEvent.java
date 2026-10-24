package legacyconnector.v1_7_10.net.minecraftforge.client.event;

import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;

/** Events fired while the camera and fog of the frame are set up. */
public abstract class EntityViewRenderEvent extends Event {
    public final EntityLivingBase entity;
    public final double renderPartialTicks;

    public EntityViewRenderEvent(EntityLivingBase entity, double partialTicks) {
        this.entity = entity;
        this.renderPartialTicks = partialTicks;
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
