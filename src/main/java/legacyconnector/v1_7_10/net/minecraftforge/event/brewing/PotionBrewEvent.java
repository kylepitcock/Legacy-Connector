package legacyconnector.v1_7_10.net.minecraftforge.event.brewing;

import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/** Brewing events of a brewing stand: {@link Pre} (cancel to stop the brew) and {@link Post} (the stacks were converted). */
public abstract class PotionBrewEvent extends legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event {
    private final ItemStack[] stacks;

    protected PotionBrewEvent(ItemStack[] stacks) {
        this.stacks = stacks;
    }

    public ItemStack getItem(int index) {
        return index >= 0 && index < stacks.length ? stacks[index] : null;
    }

    public void setItem(int index, ItemStack stack) {
        if (index >= 0 && index < stacks.length) stacks[index] = stack;
    }

    public int getLength() {
        return stacks.length;
    }

    /** Fired before a brew starts: cancelled, the stand does not brew. */
    @legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Cancelable
    public static class Pre extends PotionBrewEvent {
        public Pre(ItemStack[] stacks) {
            super(stacks);
        }
    }

    /** Fired after the bottles were converted. */
    public static class Post extends PotionBrewEvent {
        public Post(ItemStack[] stacks) {
            super(stacks);
        }
    }
}
