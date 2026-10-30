package legacyconnector.v1_12_2.net.minecraftforge.event.entity.player;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.inventory.Container;

/** A player opened or closed a container (from the modern container events). */
public class PlayerContainerEvent extends PlayerEvent {
    private final Container container;

    public PlayerContainerEvent(EntityPlayer player, Container container) {
        super(player);
        this.container = container;
    }

    public Container getContainer() {
        return container;
    }

    public static class Open extends PlayerContainerEvent {
        public Open(EntityPlayer player, Container container) {
            super(player, container);
        }
    }

    public static class Close extends PlayerContainerEvent {
        public Close(EntityPlayer player, Container container) {
            super(player, container);
        }
    }
}
