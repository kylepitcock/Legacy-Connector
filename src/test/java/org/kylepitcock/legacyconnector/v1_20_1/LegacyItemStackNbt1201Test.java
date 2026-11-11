package org.kylepitcock.legacyconnector.v1_20_1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

class LegacyItemStackNbt1201Test {
    @Test
    void absentCustomDataMatchesLegacyNullTagBehavior() {
        ItemStack stack = new ItemStack(Items.STONE);

        assertFalse(LegacyItemStackNbt1201.hasNBT(stack));
        assertNull(LegacyItemStackNbt1201.getNbt(stack));
    }

    @Test
    void setNbtStoresTheCustomDataComponentAndCopiesTheInput() {
        ItemStack stack = new ItemStack(Items.STONE);
        CompoundTag input = new CompoundTag();
        input.putString("storedGlowColor1", "4b73d4");

        LegacyItemStackNbt1201.setNbt(stack, input);
        input.putString("storedGlowColor1", "external-mutation");

        assertTrue(LegacyItemStackNbt1201.hasNBT(stack));
        assertNotNull(stack.get(DataComponents.CUSTOM_DATA));
        assertEquals("4b73d4", LegacyItemStackNbt1201.getNbt(stack).getString("storedGlowColor1"));
    }

    @Test
    void getNbtRetainsLegacyLiveCompoundMutationBehavior() {
        ItemStack stack = new ItemStack(Items.STONE);
        CompoundTag input = new CompoundTag();
        input.putString("storedCode", "000000");
        LegacyItemStackNbt1201.setNbt(stack, input);

        CompoundTag liveTag = LegacyItemStackNbt1201.getNbt(stack);
        liveTag.putString("storedCode", "123456");

        assertEquals("123456", LegacyItemStackNbt1201.getNbt(stack).getString("storedCode"));
    }

    @Test
    void assigningNullRemovesTheCustomDataComponent() {
        ItemStack stack = new ItemStack(Items.STONE);
        CompoundTag input = new CompoundTag();
        input.putInt("test", 1);
        LegacyItemStackNbt1201.setNbt(stack, input);
        assertTrue(LegacyItemStackNbt1201.hasNBT(stack));

        LegacyItemStackNbt1201.setNbt(stack, null);

        assertFalse(LegacyItemStackNbt1201.hasNBT(stack));
        assertNull(stack.get(DataComponents.CUSTOM_DATA));
        assertNull(LegacyItemStackNbt1201.getNbt(stack));
    }
}
