package legacyconnector.v1_7_10.net.minecraftforge.client.event;

import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.entity.RenderPlayer;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.PlayerEvent;

/** Fired around drawing a player; handlers add their own model parts or cancel the vanilla drawing. */
public abstract class RenderPlayerEvent extends PlayerEvent {
    public final RenderPlayer renderer;
    public final float partialRenderTick;
    public final double x;
    public final double y;
    public final double z;

    public RenderPlayerEvent(EntityPlayer player, RenderPlayer renderer, float partialRenderTick, double x, double y, double z) {
        super(player);
        this.renderer = renderer;
        this.partialRenderTick = partialRenderTick;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Cancelable
    public static class Pre extends RenderPlayerEvent {
        public Pre(EntityPlayer player, RenderPlayer renderer, float partialRenderTick, double x, double y, double z) {
            super(player, renderer, partialRenderTick, x, y, z);
        }
    }

    public static class Post extends RenderPlayerEvent {
        public Post(EntityPlayer player, RenderPlayer renderer, float partialRenderTick, double x, double y, double z) {
            super(player, renderer, partialRenderTick, x, y, z);
        }
    }

    public abstract static class Specials extends RenderPlayerEvent {
        public Specials(EntityPlayer player, RenderPlayer renderer, float partialRenderTick, double x, double y, double z) {
            super(player, renderer, partialRenderTick, x, y, z);
        }

        @Cancelable
        public static class Pre extends Specials {
            public boolean renderHelmet = true;
            public boolean renderCape = true;
            public boolean renderItem = true;

            public Pre(EntityPlayer player, RenderPlayer renderer, float partialRenderTick, double x, double y, double z) {
                super(player, renderer, partialRenderTick, x, y, z);
            }
        }

        public static class Post extends Specials {
            public Post(EntityPlayer player, RenderPlayer renderer, float partialRenderTick, double x, double y, double z) {
                super(player, renderer, partialRenderTick, x, y, z);
            }
        }
    }
}
