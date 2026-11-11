package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Modern items Beta never had enter original inventories intact instead of being lost. */
class NativeStackItemB173Test {
    @BeforeAll static void bootstrap() throws Exception {NativeStackComponentsB173Test.bootstrap();}

    @Test void modernOnlyItemRoundTripsWithComponentsCountAndPersistence() {
        var copper = new ItemStack(Items.COPPER_INGOT, 37);
        copper.set(DataComponents.CUSTOM_NAME, Component.literal("Modern copper"));
        var legacy = StacksB173.toLegacy(copper);
        assertNotNull(legacy);
        assertEquals(NativeStackItemB173.ID, legacy.itemId);
        assertNotNull(legacy.getItem());
        assertEquals(37, legacy.count);
        var back = StacksB173.toModern(legacy);
        assertTrue(ItemStack.isSameItemSameComponents(copper, back));
        assertEquals(37, back.getCount());
        var split = legacy.split(5);
        assertEquals(5, StacksB173.toModern(split).getCount());
        assertEquals("Modern copper", StacksB173.toModern(split).getHoverName().getString());
        var restored = new legacyconnector.vb1_7_3.net.minecraft.item.ItemStack(legacy.writeNbt(new NbtCompound()));
        assertEquals(NativeStackItemB173.ID, restored.itemId);
        assertTrue(ItemStack.isSameItemSameComponents(copper, StacksB173.toModern(restored)));
        assertEquals(32, StacksB173.toModern(restored).getCount());
        assertTrue(StacksB173.canRepresent(copper));
    }

    @Test void differentModernItemsNeverShareBetaSubtypeAndKeepModernStackLimits() {
        var pearls = StacksB173.toLegacy(new ItemStack(Items.ENDER_PEARL, 3));
        var sword = StacksB173.toLegacy(new ItemStack(Items.NETHERITE_SWORD));
        var copper = StacksB173.toLegacy(new ItemStack(Items.COPPER_INGOT));
        // Ender pearls existed only later; Beta's id+damage merge test must keep these apart.
        assertFalse(pearls.isItemEqual(copper));
        assertEquals(16, pearls.getMaxCount());
        assertEquals(1, sword.getMaxCount());
        assertFalse(sword.isStackable());
        assertEquals(64, copper.getMaxCount());
    }

    @Test void damagedModernToolKeepsDurabilityThroughBetaStorage() {
        var sword = new ItemStack(Items.NETHERITE_SWORD);sword.setDamageValue(123);
        var legacy = StacksB173.toLegacy(sword);
        var restored = new legacyconnector.vb1_7_3.net.minecraft.item.ItemStack(legacy.writeNbt(new NbtCompound()));
        assertEquals(123, StacksB173.toModern(restored).getDamageValue());
        assertSame(Items.NETHERITE_SWORD, StacksB173.toModern(restored).getItem());
    }

    @Test void betaItemsStillUseTheirOriginalIds() {
        assertEquals(264, StacksB173.toLegacy(new ItemStack(Items.DIAMOND)).itemId);
    }
}
