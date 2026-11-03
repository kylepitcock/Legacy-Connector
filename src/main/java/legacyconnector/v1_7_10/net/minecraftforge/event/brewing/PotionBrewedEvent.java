package legacyconnector.v1_7_10.net.minecraftforge.event.brewing;

import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;

/** A brewing stand finished brewing: listeners may change the potions in {@link #brewingStacks} (the three bottle slots, 0 to 2). */
public class PotionBrewedEvent extends legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event {
    public ItemStack[] brewingStacks;

    public PotionBrewedEvent(ItemStack[] brewingStacks) {
        this.brewingStacks = brewingStacks;
    }
}
