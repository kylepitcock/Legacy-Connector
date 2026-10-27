package legacyconnector.v1_12_2.net.minecraftforge.fml.common.gameevent;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.inventory.IInventory;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/**
 * FML's player events, posted on {@code MinecraftForge.EVENT_BUS} (see {@code EventBridge1122}). The item pickup event is
 * left to the synthesized API until the legacy item entity exists.
 */
public class PlayerEvent extends Event {
    public final EntityPlayer player;

    private PlayerEvent(EntityPlayer player) {
        this.player = player;
    }

    public static class ItemCraftedEvent extends PlayerEvent {
        public final ItemStack crafting;
        public final IInventory craftMatrix;

        public ItemCraftedEvent(EntityPlayer player, ItemStack crafting, IInventory craftMatrix) {
            super(player);
            this.crafting = crafting;
            this.craftMatrix = craftMatrix;
        }
    }

    public static class ItemSmeltedEvent extends PlayerEvent {
        public final ItemStack smelting;

        public ItemSmeltedEvent(EntityPlayer player, ItemStack crafting) {
            super(player);
            this.smelting = crafting;
        }
    }

    public static class PlayerLoggedInEvent extends PlayerEvent {
        public PlayerLoggedInEvent(EntityPlayer player) {
            super(player);
        }
    }

    public static class PlayerLoggedOutEvent extends PlayerEvent {
        public PlayerLoggedOutEvent(EntityPlayer player) {
            super(player);
        }
    }

    public static class PlayerRespawnEvent extends PlayerEvent {
        private final boolean endConquered;

        public PlayerRespawnEvent(EntityPlayer player, boolean endConquered) {
            super(player);
            this.endConquered = endConquered;
        }

        /** True when the player left the End through its exit portal (the old player was not dead). */
        public boolean isEndConquered() {
            return endConquered;
        }
    }

    public static class PlayerChangedDimensionEvent extends PlayerEvent {
        public final int fromDim;
        public final int toDim;

        public PlayerChangedDimensionEvent(EntityPlayer player, int fromDim, int toDim) {
            super(player);
            this.fromDim = fromDim;
            this.toDim = toDim;
        }
    }

    /** A player picked up an item entity (posted after the pickup, with what was picked up). */
    public static class ItemPickupEvent extends PlayerEvent {
        public final ItemStack stack;
        private final legacyconnector.v1_12_2.net.minecraft.entity.item.EntityItem originalEntity;

        public ItemPickupEvent(EntityPlayer player, legacyconnector.v1_12_2.net.minecraft.entity.item.EntityItem entPickedUp, ItemStack stack) {
            super(player);
            this.originalEntity = entPickedUp;
            this.stack = stack;
        }

        public ItemStack getStack() {
            return stack;
        }

        public legacyconnector.v1_12_2.net.minecraft.entity.item.EntityItem getOriginalEntity() {
            return originalEntity;
        }
    }
}
