package legacyconnector.v1_12_2.net.minecraftforge.fml.common.gameevent;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/** FML's player events: logging in and out, respawning, changing dimension, crafting and smelting. */
public class PlayerEvent extends Event {
    public final EntityPlayer player;

    private PlayerEvent(EntityPlayer player) {
        this.player = player;
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

    public static class ItemCraftedEvent extends PlayerEvent {
        public final legacyconnector.v1_12_2.net.minecraft.item.ItemStack crafting;
        public final legacyconnector.v1_12_2.net.minecraft.inventory.IInventory craftMatrix;

        public ItemCraftedEvent(EntityPlayer player, legacyconnector.v1_12_2.net.minecraft.item.ItemStack crafting, legacyconnector.v1_12_2.net.minecraft.inventory.IInventory craftMatrix) {
            super(player);
            this.crafting = crafting;
            this.craftMatrix = craftMatrix;
        }
    }

    public static class ItemSmeltedEvent extends PlayerEvent {
        public final legacyconnector.v1_12_2.net.minecraft.item.ItemStack smelting;

        public ItemSmeltedEvent(EntityPlayer player, legacyconnector.v1_12_2.net.minecraft.item.ItemStack smelting) {
            super(player);
            this.smelting = smelting;
        }
    }
}
