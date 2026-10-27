package legacyconnector.v1_12_2.net.minecraft.world.storage.loot.functions;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.world.storage.loot.LootContext;
import legacyconnector.v1_12_2.net.minecraft.world.storage.loot.conditions.LootCondition;

/** 1.12.2 loot function: merges a compound into the stack's tag. */
public class SetNBT extends LootFunction {
    private final NBTTagCompound tag;

    public SetNBT(LootCondition[] conditions, NBTTagCompound tag) {
        super(conditions);
        this.tag = tag;
    }

    @Override
    public ItemStack apply(ItemStack stack, Random rand, LootContext context) {
        NBTTagCompound current = stack.getTagCompound();
        if (current == null) current = tag.copy();
        else current.merge(tag);
        stack.setTagCompound(current);
        return stack;
    }
}
