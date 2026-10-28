package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import java.util.AbstractList;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;

/**
 * The slots of a modern {@link net.minecraft.world.Container} as the {@code NonNullList<ItemStack>} a 1.12.2 tile entity keeps its
 * items in ({@code chestContents}, {@code furnaceItemStacks}): reads are live views of the modern stacks, writes replace them. Legacy
 * views of the game's block entities use it, so the inventory code of the legacy classes works on the real contents.
 */
public final class ModernStackList1122 extends NonNullList<ItemStack> {
    public ModernStackList1122(net.minecraft.world.Container modern) {
        super(new Slots(modern), ItemStack.EMPTY);
    }

    private static final class Slots extends AbstractList<ItemStack> {
        private final net.minecraft.world.Container modern;

        Slots(net.minecraft.world.Container modern) {
            this.modern = modern;
        }

        @Override
        public ItemStack get(int index) {
            return Stacks1122.view(modern.getItem(index));
        }

        @Override
        public ItemStack set(int index, ItemStack element) {
            ItemStack before = get(index);
            modern.setItem(index, Stacks1122.toModernLive(element));
            modern.setChanged();
            return before;
        }

        @Override
        public int size() {
            return modern.getContainerSize();
        }
    }
}
