package legacyconnector.v1_7_10.net.minecraftforge.client.event;

import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.entity.RenderPlayer;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.PlayerEvent;

/**
 * Fired around drawing a player; handlers add their own model parts or cancel the vanilla drawing. {@code x}, {@code y},
 * {@code z} (the player relative to the camera) were added after 1.7.10: events made with 1.7.10's constructors compute them.
 */
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

    public RenderPlayerEvent(EntityPlayer player, RenderPlayer renderer, float partialRenderTick) {
        this(player, renderer, partialRenderTick, mcc$relative(player, partialRenderTick, 0), mcc$relative(player, partialRenderTick, 1),
                mcc$relative(player, partialRenderTick, 2));
    }

    /** The player's interpolated position relative to the camera along one axis (0 x, 1 y, 2 z). */
    private static double mcc$relative(EntityPlayer player, float partial, int axis) {
        try {
            var modern = player.mcc$entity();
            var camera = net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            if (modern == null) return 0.0D;
            var at = modern.getPosition(partial);
            return axis == 0 ? at.x - camera.x : axis == 1 ? at.y - camera.y : at.z - camera.z;
        } catch (RuntimeException | LinkageError e) {
            return 0.0D;
        }
    }

    @Cancelable
    public static class Pre extends RenderPlayerEvent {
        public Pre(EntityPlayer player, RenderPlayer renderer, float partialRenderTick, double x, double y, double z) {
            super(player, renderer, partialRenderTick, x, y, z);
        }

        public Pre(EntityPlayer player, RenderPlayer renderer, float partialRenderTick) {
            super(player, renderer, partialRenderTick);
        }
    }

    public static class Post extends RenderPlayerEvent {
        public Post(EntityPlayer player, RenderPlayer renderer, float partialRenderTick, double x, double y, double z) {
            super(player, renderer, partialRenderTick, x, y, z);
        }

        public Post(EntityPlayer player, RenderPlayer renderer, float partialRenderTick) {
            super(player, renderer, partialRenderTick);
        }
    }

    public abstract static class Specials extends RenderPlayerEvent {
        public Specials(EntityPlayer player, RenderPlayer renderer, float partialRenderTick, double x, double y, double z) {
            super(player, renderer, partialRenderTick, x, y, z);
        }

        public Specials(EntityPlayer player, RenderPlayer renderer, float partialRenderTick) {
            super(player, renderer, partialRenderTick);
        }

        @Cancelable
        public static class Pre extends Specials {
            public boolean renderHelmet = true;
            public boolean renderCape = true;
            public boolean renderItem = true;

            public Pre(EntityPlayer player, RenderPlayer renderer, float partialRenderTick, double x, double y, double z) {
                super(player, renderer, partialRenderTick, x, y, z);
            }

            public Pre(EntityPlayer player, RenderPlayer renderer, float partialRenderTick) {
                super(player, renderer, partialRenderTick);
            }
        }

        public static class Post extends Specials {
            public Post(EntityPlayer player, RenderPlayer renderer, float partialRenderTick, double x, double y, double z) {
                super(player, renderer, partialRenderTick, x, y, z);
            }

            public Post(EntityPlayer player, RenderPlayer renderer, float partialRenderTick) {
                super(player, renderer, partialRenderTick);
            }
        }
    }

    /**
     * Fired before a piece of armor is drawn on a player. {@code slot} is the armor slot (0 boots, 1 leggings, 2 chestplate,
     * 3 helmet). A {@code result} other than -1 replaces the vanilla decision: 0 or less draws nothing for the piece.
     */
    public static class SetArmorModel extends RenderPlayerEvent {
        public int result = -1;
        public final int slot;
        public final ItemStack stack;

        public SetArmorModel(EntityPlayer player, RenderPlayer renderer, int slot, float partialTick, ItemStack stack) {
            super(player, renderer, partialTick);
            this.slot = slot;
            this.stack = stack;
        }
    }
}
