package legacyconnector.v1_12_2.net.minecraftforge.event.brewing;

import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/** A brewing stand finishes a brew: {@link Pre} before the potions are made (cancelable), {@link Post} after. */
public class PotionBrewEvent extends Event {
    private final NonNullList<ItemStack> stacks;

    protected PotionBrewEvent(NonNullList<ItemStack> stacks) {
        this.stacks = stacks;
    }

    public ItemStack getItem(int index) {
        return index >= 0 && index < stacks.size() ? stacks.get(index) : ItemStack.EMPTY;
    }

    public void setItem(int index, ItemStack stack) {
        if (index >= 0 && index < stacks.size()) stacks.set(index, stack);
    }

    public int getLength() {
        return stacks.size();
    }

    @Cancelable
    public static class Pre extends PotionBrewEvent {
        public Pre(NonNullList<ItemStack> stacks) {
            super(stacks);
        }
    }

    public static class Post extends PotionBrewEvent {
        public Post(NonNullList<ItemStack> stacks) {
            super(stacks);
        }
    }
}
