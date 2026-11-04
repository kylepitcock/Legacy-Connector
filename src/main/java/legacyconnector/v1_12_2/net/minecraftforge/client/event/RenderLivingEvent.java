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

    private RenderLivingBase<T> resolved;

    /** The renderer drawing the entity; the modern pipeline has no legacy one, so it is looked up (or a stand-in built) on first use. */
    @SuppressWarnings("unchecked")
    public RenderLivingBase<T> getRenderer() {
        if (renderer != null) return renderer;
        if (resolved != null) return resolved;
        var manager = legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.RenderManager.mcc$instance();
        try {
            var found = manager.getEntityRenderObject(entity);
            if (found instanceof RenderLivingBase<?> living) return resolved = (RenderLivingBase<T>) living;
        } catch (RuntimeException | LinkageError ignored) {
            // falls through to the stand-in
        }
        return resolved = (RenderLivingBase<T>) new RenderLivingBase<EntityLivingBase>(manager, new legacyconnector.v1_12_2.net.minecraft.client.model.ModelBase() {
        }, 0.0F) {
            @Override
            protected legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation getEntityTexture(EntityLivingBase e) {
                return null;
            }
        };
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
