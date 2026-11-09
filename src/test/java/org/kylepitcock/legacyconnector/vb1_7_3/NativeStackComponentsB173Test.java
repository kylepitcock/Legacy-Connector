package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class NativeStackComponentsB173Test {
    @BeforeAll static void bootstrap() throws Exception {
        var dist = net.neoforged.fml.loading.FMLLoader.class.getDeclaredField("dist");
        dist.setAccessible(true);dist.set(null, net.neoforged.api.distmarker.Dist.CLIENT);
        net.neoforged.fml.loading.LoadingModList.of(java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.Map.of());
        net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();
        VanillaB173.init();
    }
    private static ItemStack named(net.minecraft.world.item.Item item, int count) {
        var stack = new ItemStack(item, count);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Native fixture"));
        var data = new CompoundTag();
        data.putIntArray("ints", new int[] {1, 2, 3});
        data.putLongArray("longs", new long[] {Long.MAX_VALUE, 4});
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        return stack;
    }
    private static void assertNamed(ItemStack stack) {
        assertEquals("Native fixture", stack.getHoverName().getString());
        var data = stack.get(DataComponents.CUSTOM_DATA).copyTag();
        assertArrayEquals(new int[] {1, 2, 3}, data.getIntArray("ints"));
        assertArrayEquals(new long[] {Long.MAX_VALUE, 4}, data.getLongArray("longs"));
    }

    @Test void nativePatchSurvivesRoundtripCopySplitAndOriginalBetaNbt() {
        var original = named(Items.DIAMOND, 7);
        var legacy = StacksB173.toLegacy(original);
        assertNotNull(legacy);
        original.set(DataComponents.CUSTOM_NAME, Component.literal("Mutated source"));
        assertNamed(StacksB173.toModern(legacy));
        assertNamed(StacksB173.toModern(legacy.copy()));
        var split = legacy.split(2);
        assertEquals(5, legacy.count);assertEquals(2, split.count);
        assertNamed(StacksB173.toModern(split));
        var tag = split.writeNbt(new NbtCompound());
        assertTrue(tag.contains("legacyconnector:native_stack"));
        var restored = new legacyconnector.vb1_7_3.net.minecraft.item.ItemStack(tag);
        assertEquals(2, restored.count);assertNamed(StacksB173.toModern(restored));
    }

    @Test void originalDamageAndFlattenedSubtypeEditsWinOverTheNativeSnapshot() {
        var sword = named(Items.DIAMOND_SWORD, 1);sword.setDamageValue(17);
        var legacy = StacksB173.toLegacy(sword);
        legacy.setDamage(0);
        assertEquals(0, StacksB173.toModern(legacy).getDamageValue());
        legacy.setDamage(29);
        var restored = new legacyconnector.vb1_7_3.net.minecraft.item.ItemStack(legacy.writeNbt(new NbtCompound()));
        assertEquals(29, StacksB173.toModern(restored).getDamageValue());assertNamed(StacksB173.toModern(restored));
        var wool = StacksB173.toLegacy(named(Items.WHITE_WOOL, 3));
        wool.setDamage(14);
        assertEquals(Items.RED_WOOL, StacksB173.toModern(wool).getItem());assertNamed(StacksB173.toModern(wool));
        wool.itemId = 266;
        var gold = StacksB173.toModern(wool);
        assertEquals(Items.GOLD_INGOT, gold.getItem());assertFalse(gold.has(DataComponents.CUSTOM_NAME));
    }

    @Test void plainBetaStacksKeepOriginalNbtAndComponentComparisonDetectsNativeChanges() {
        var legacy = new legacyconnector.vb1_7_3.net.minecraft.item.ItemStack(264, 2, 0);
        var tag = legacy.writeNbt(new NbtCompound());
        assertEquals(3, tag.values().size());
        var a = StacksB173.toLegacy(named(Items.DIAMOND, 2));
        var b = a.copy();assertTrue(StacksB173.sameComponents(a, b));
        var changed = named(Items.DIAMOND, 2);changed.set(DataComponents.CUSTOM_NAME, Component.literal("Changed"));
        assertFalse(StacksB173.sameComponents(a, StacksB173.toLegacy(changed)));
    }

    @Test void originalInventoryMergesKeepDifferentNativeNamesAndCustomDataSeparate() {
        var a = named(Items.DIAMOND, 2);
        var b = named(Items.DIAMOND, 3);
        b.set(DataComponents.CUSTOM_NAME, Component.literal("Different incoming name"));
        var otherData = new CompoundTag();otherData.putString("other", "incoming data");
        b.set(DataComponents.CUSTOM_DATA, CustomData.of(otherData));
        var player = new legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity(null) {};
        player.inventory.setStack(0, StacksB173.toLegacy(a));
        assertTrue(player.inventory.addStack(StacksB173.toLegacy(b)));
        assertTrue(ItemStack.matches(a, StacksB173.toModern(player.inventory.getStack(0))));
        assertTrue(ItemStack.matches(b, StacksB173.toModern(player.inventory.getStack(1))));

        player.inventory.setStack(1, null);
        player.inventory.setStack(9, StacksB173.toLegacy(b));
        var menu = new legacyconnector.vb1_7_3.net.minecraft.screen.PlayerScreenHandler(player.inventory);
        assertNotNull(menu.quickMove(9));
        assertNull(player.inventory.getStack(9));
        assertTrue(ItemStack.matches(a, StacksB173.toModern(player.inventory.getStack(0))));
        assertTrue(ItemStack.matches(b, StacksB173.toModern(player.inventory.getStack(1))));
    }

    @Test void subtypeMigrationAfterOriginalNbtWriteRetainsNativeComponents() {
        var wool = StacksB173.toLegacy(named(Items.WHITE_WOOL, 3));
        var tag = wool.writeNbt(new NbtCompound());
        tag.putShort("Damage", (short) 14);
        var migrated = new legacyconnector.vb1_7_3.net.minecraft.item.ItemStack(tag);
        var modern = StacksB173.toModern(migrated);
        assertEquals(Items.RED_WOOL, modern.getItem());
        assertEquals(3, modern.getCount());assertNamed(modern);
    }

    @Test void adapterFlushesSameCountNativeComponentAndDamageEdits() {
        var inventory = new legacyconnector.vb1_7_3.net.minecraft.inventory.CraftingResultInventory();
        var adapter = LegacyInventoryB173.of(inventory);
        adapter.setItem(0, named(Items.DIAMOND_SWORD, 1));
        adapter.getItem(0).set(DataComponents.CUSTOM_NAME, Component.literal("Edited in place"));
        adapter.setChanged();
        assertEquals("Edited in place", StacksB173.toModern(inventory.getStack(0)).getHoverName().getString());
        adapter.getItem(0).setDamageValue(31);
        adapter.setChanged();
        assertEquals(31, inventory.getStack(0).getDamage());
        assertEquals(31, StacksB173.toModern(inventory.getStack(0)).getDamageValue());
        assertEquals(1, inventory.getStack(0).count);
    }
}
