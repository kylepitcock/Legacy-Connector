package org.kylepitcock.legacyconnector.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Issue 113: a stack with an enchantment holder of another registry set must still save (two lookups stand for the client's and the server's registries). */
class StackHoldersTest {
    @BeforeAll
    static void bootstrapMinecraft() throws Exception {
        var dist = net.neoforged.fml.loading.FMLLoader.class.getDeclaredField("dist");
        dist.setAccessible(true);
        dist.set(null, net.neoforged.api.distmarker.Dist.CLIENT);
        net.neoforged.fml.loading.LoadingModList.of(
                java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.Map.of());
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    private static ItemStack enchantedFrom(HolderLookup.Provider source) {
        var stack = new ItemStack(Items.DIAMOND_PICKAXE);
        var m = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        m.set(source.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE), 3);
        stack.set(DataComponents.ENCHANTMENTS, m.toImmutable());
        return stack;
    }

    @Test
    void foreignHolderFailsToSaveUntilRepaired() {
        HolderLookup.Provider client = VanillaRegistries.createLookup();
        HolderLookup.Provider server = VanillaRegistries.createLookup();
        var stack = enchantedFrom(client);
        assertThrows(RuntimeException.class, () -> stack.save(server));
        assertTrue(StackHolders.repair(stack, server));
        var saved = stack.save(server); // would throw "not valid in current registry set"
        assertEquals(3, ItemStack.parse(server, saved).orElseThrow().getEnchantments().getLevel(
                server.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE)));
    }

    @Test
    void ownHolderIsLeftAlone() {
        HolderLookup.Provider server = VanillaRegistries.createLookup();
        var stack = enchantedFrom(server);
        assertFalse(StackHolders.repair(stack, server));
        stack.save(server);
    }

    @Test
    void storedEnchantmentsOfABookAreRepairedToo() {
        HolderLookup.Provider client = VanillaRegistries.createLookup();
        HolderLookup.Provider server = VanillaRegistries.createLookup();
        var book = new ItemStack(Items.ENCHANTED_BOOK);
        var m = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        m.set(client.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
        book.set(DataComponents.STORED_ENCHANTMENTS, m.toImmutable());
        assertTrue(StackHolders.repair(book, server));
        book.save(server);
    }

    @Test
    void failedSaveIsRetriedAndFinallySavedWithoutEnchantments() {
        HolderLookup.Provider server = VanillaRegistries.createLookup();
        var stack = enchantedFrom(server);
        var failure = new IllegalStateException("Element x is not valid in current registry set");
        // a save that keeps failing as long as the stack carries enchantments: the copy without them is written
        var tag = StackHolders.recover(stack, server, failure, s -> {
            if (!s.getEnchantments().isEmpty()) throw new IllegalStateException("Element y is not valid in current registry set");
            return s.save(server);
        });
        assertTrue(tag.toString().contains("netherite_hoe") || tag.toString().contains("diamond_pickaxe"));
        // a save that works after the holders were looked up again keeps them
        var again = StackHolders.recover(stack, server, failure, s -> s.save(server));
        assertTrue(again.toString().contains("fortune"));
    }
}
