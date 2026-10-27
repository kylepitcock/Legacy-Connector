package legacyconnector.v1_12_2.net.minecraftforge.client.event;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.RenderLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/**
 * A living entity is drawn; x/y/z are its position relative to the camera. Cancelling Pre skips drawing it. The renderer is the legacy
 * renderer when the entity has one (the layer's posts pass null for modern renderers).
 */
public abstract class RenderLivingEvent<T extends EntityLivingBase> extends Event {
    private final EntityLivingBase entity;
    private final RenderLivingBase<T> renderer;
    private final float partialRenderTick;
    private final double x;
    private final double y;
    private final double z;

    @Deprecated
    public RenderLivingEvent(EntityLivingBase entity, RenderLivingBase<T> renderer, double x, double y, double z) {
        this(entity, renderer, 1.0F, x, y, z);
    }

    public RenderLivingEvent(EntityLivingBase entity, RenderLivingBase<T> renderer, float partialRenderTick, double x, double y, double z) {
        this.entity = entity;
        this.renderer = renderer;
        this.partialRenderTick = partialRenderTick;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public EntityLivingBase getEntity() {
        return entity;
    }

    public RenderLivingBase<T> getRenderer() {
        return renderer;
    }

    public float getPartialRenderTick() {
        return partialRenderTick;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    @Cancelable
    public static class Pre<T extends EntityLivingBase> extends RenderLivingEvent<T> {
        @Deprecated
        public Pre(EntityLivingBase entity, RenderLivingBase<T> renderer, double x, double y, double z) {
            super(entity, renderer, x, y, z);
        }

        public Pre(EntityLivingBase entity, RenderLivingBase<T> renderer, float partialRenderTick, double x, double y, double z) {
            super(entity, renderer, partialRenderTick, x, y, z);
        }
    }

    public static class Post<T extends EntityLivingBase> extends RenderLivingEvent<T> {
        @Deprecated
        public Post(EntityLivingBase entity, RenderLivingBase<T> renderer, double x, double y, double z) {
            super(entity, renderer, x, y, z);
        }

        public Post(EntityLivingBase entity, RenderLivingBase<T> renderer, float partialRenderTick, double x, double y, double z) {
            super(entity, renderer, partialRenderTick, x, y, z);
        }
    }

    /** The name tag and other specials above the entity. */
    public abstract static class Specials<T extends EntityLivingBase> extends RenderLivingEvent<T> {
        public Specials(EntityLivingBase entity, RenderLivingBase<T> renderer, double x, double y, double z) {
            super(entity, renderer, 0.0F, x, y, z);
        }

        @Cancelable
        public static class Pre<T extends EntityLivingBase> extends Specials<T> {
            public Pre(EntityLivingBase entity, RenderLivingBase<T> renderer, double x, double y, double z) {
                super(entity, renderer, x, y, z);
            }
        }

        public static class Post<T extends EntityLivingBase> extends Specials<T> {
            public Post(EntityLivingBase entity, RenderLivingBase<T> renderer, double x, double y, double z) {
                super(entity, renderer, x, y, z);
            }
        }
    }
}
