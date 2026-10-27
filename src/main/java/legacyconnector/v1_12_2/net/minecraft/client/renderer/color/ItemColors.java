package legacyconnector.v1_12_2.net.minecraft.client.renderer.color;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import org.kylepitcock.legacyconnector.v1_12_2.client.Colors1122;

/** Item tints: a handler registered for a mod item tints each of its modern subtype items (see {@link Colors1122}). */
public class ItemColors {
    private final Map<Item, IItemColor> handlers = new ConcurrentHashMap<>();
    private final BlockColors blockColors;

    public ItemColors(BlockColors colors) {
        blockColors = colors;
    }

    public static ItemColors mcc$instance() {
        return Colors1122.items();
    }

    public static ItemColors init(BlockColors colors) {
        return new ItemColors(colors);
    }

    public void registerItemColorHandler(IItemColor itemColor, Block... blocksIn) {
        if (itemColor == null || blocksIn == null) return;
        for (Block block : blocksIn) {
            Item item = block == null ? null : Item.getItemFromBlock(block);
            if (item != null) registerItemColorHandler(itemColor, item);
        }
    }

    public void registerItemColorHandler(IItemColor itemColor, Item... itemsIn) {
        if (itemColor == null || itemsIn == null) return;
        for (Item item : itemsIn) {
            if (item == null) continue;
            handlers.put(item, itemColor);
            if (this == mcc$instance()) Colors1122.registerItem(item, itemColor);
        }
    }

    public Map<Item, IItemColor> mcc$handlers() {
        return handlers;
    }

    public int colorMultiplier(ItemStack stack, int tintIndex) {
        if (stack == null || stack.isEmpty()) return -1;
        IItemColor handler = handlers.get(stack.getItem());
        if (handler != null) {
            try {
                return handler.colorMultiplier(stack, tintIndex);
            } catch (RuntimeException e) {
                return -1;
            }
        }
        return Colors1122.modernItemColor(stack, tintIndex);
    }
}
