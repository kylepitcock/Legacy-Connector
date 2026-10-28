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
    /** Forge's field (by delegate), read by reflection like BlockColors.blockColorMap: a mod handler, else the game's colour. */
    @SuppressWarnings("unused")
    private final Map<legacyconnector.v1_12_2.net.minecraftforge.registries.IRegistryDelegate<Item>, IItemColor> itemColorMap = new java.util.AbstractMap<>() {
        @Override
        public IItemColor get(Object key) {
            if (!(key instanceof legacyconnector.v1_12_2.net.minecraftforge.registries.IRegistryDelegate<?> d) || !(d.get() instanceof Item item)) return null;
            IItemColor handler = handlers.get(item);
            return handler != null ? handler : (stack, tint) -> org.kylepitcock.legacyconnector.v1_12_2.client.Colors1122.modernItemColor(stack, tint);
        }

        @Override
        public IItemColor put(legacyconnector.v1_12_2.net.minecraftforge.registries.IRegistryDelegate<Item> key, IItemColor value) {
            IItemColor old = key == null ? null : handlers.get(key.get());
            if (key != null && key.get() != null) registerItemColorHandler(value, key.get());
            return old;
        }

        @Override
        public java.util.Set<Entry<legacyconnector.v1_12_2.net.minecraftforge.registries.IRegistryDelegate<Item>, IItemColor>> entrySet() {
            java.util.Set<Entry<legacyconnector.v1_12_2.net.minecraftforge.registries.IRegistryDelegate<Item>, IItemColor>> out = new java.util.LinkedHashSet<>();
            for (var e : handlers.entrySet()) out.add(new java.util.AbstractMap.SimpleEntry<>(e.getKey().delegate, e.getValue()));
            return out;
        }
    };
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
