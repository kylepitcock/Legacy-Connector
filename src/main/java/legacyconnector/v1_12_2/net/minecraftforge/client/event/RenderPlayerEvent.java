package legacyconnector.v1_12_2.net.minecraftforge.client.event;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.RenderPlayer;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable;

/** A player is drawn (x/y/z relative to the camera); cancelling Pre skips drawing them. */
public abstract class RenderPlayerEvent extends legacyconnector.v1_12_2.net.minecraftforge.event.entity.player.PlayerEvent {
    private final RenderPlayer renderer;
    private final float partialRenderTick;
    private final double x;
    private final double y;
    private final double z;

    public RenderPlayerEvent(EntityPlayer player, RenderPlayer renderer, float partialRenderTick, double x, double y, double z) {
        super(player);
        this.renderer = renderer;
        this.partialRenderTick = partialRenderTick;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public RenderPlayer getRenderer() {
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
    public static class Pre extends RenderPlayerEvent {
        public Pre(EntityPlayer player, RenderPlayer renderer, float tick, double x, double y, double z) {
            super(player, renderer, tick, x, y, z);
        }
    }

    public static class Post extends RenderPlayerEvent {
        public Post(EntityPlayer player, RenderPlayer renderer, float tick, double x, double y, double z) {
            super(player, renderer, tick, x, y, z);
        }
    }

    /** Deprecated in 1.12: the held item, helmet and cape of the player. */
    @Deprecated
    public abstract static class Specials extends RenderPlayerEvent {
        public Specials(EntityPlayer player, RenderPlayer renderer, float partialTicks) {
            super(player, renderer, partialTicks, 0.0D, 0.0D, 0.0D);
        }

        @Cancelable
        public static class Pre extends Specials {
            private boolean renderHelmet = true;
            private boolean renderCape = true;
            private boolean renderItem = true;

            public Pre(EntityPlayer player, RenderPlayer renderer, float partialTicks) {
                super(player, renderer, partialTicks);
            }

            public boolean shouldRenderHelmet() {
                return renderHelmet;
            }

            public void setRenderHelmet(boolean value) {
                renderHelmet = value;
            }

            public boolean shouldRenderCape() {
                return renderCape;
            }

            public void setRenderCape(boolean value) {
                renderCape = value;
            }

            public boolean shouldRenderItem() {
                return renderItem;
            }

            public void setRenderItem(boolean value) {
                renderItem = value;
            }
        }

        public static class Post extends Specials {
            public Post(EntityPlayer player, RenderPlayer renderer, float partialTicks) {
                super(player, renderer, partialTicks);
            }
        }
    }
}
