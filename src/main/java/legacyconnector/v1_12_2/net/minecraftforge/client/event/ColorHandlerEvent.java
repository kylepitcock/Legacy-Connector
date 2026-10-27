package legacyconnector.v1_12_2.net.minecraftforge.client.event;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.color.BlockColors;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.color.ItemColors;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/** Fired when the game collects colour handlers: mods register their block and item tints here. */
public class ColorHandlerEvent extends Event {
    public static class Block extends ColorHandlerEvent {
        private final BlockColors blockColors;

        public Block(BlockColors colors) {
            blockColors = colors;
        }

        public BlockColors getBlockColors() {
            return blockColors;
        }
    }

    public static class Item extends ColorHandlerEvent {
        private final ItemColors itemColors;
        private final BlockColors blockColors;

        public Item(ItemColors itemColors, BlockColors blockColors) {
            this.itemColors = itemColors;
            this.blockColors = blockColors;
        }

        public ItemColors getItemColors() {
            return itemColors;
        }

        public BlockColors getBlockColors() {
            return blockColors;
        }
    }
}
