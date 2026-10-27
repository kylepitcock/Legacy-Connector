package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.inventory.InventoryCrafting;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.NonNullList;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * Legacy crafting grids against the modern recipe book: vanilla's (and modern mods') recipes are data pack recipes that the legacy
 * {@code CraftingManager} registry does not hold, so a legacy container's grid also matches them here.
 */
public final class Crafting1122 {
    private Crafting1122() {}

    private record Match(RecipeHolder<CraftingRecipe> recipe, CraftingInput.Positioned input) {}

    private static Optional<Match> find(InventoryCrafting inv, World world) {
        var level = Inventories1122.level(world);
        if (level == null || inv == null) return Optional.empty();
        List<net.minecraft.world.item.ItemStack> items = new ArrayList<>(inv.getSizeInventory());
        for (int i = 0; i < inv.getSizeInventory(); i++) items.add(Stacks1122.toModern(inv.getStackInSlot(i)));
        int w = inv.getWidth(), h = inv.getHeight();
        if (w * h != items.size()) return Optional.empty();
        var positioned = CraftingInput.ofPositioned(w, h, items);
        return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, positioned.input(), level).map(r -> new Match(r, positioned));
    }

    /** The result of the grid by the modern recipes, or empty. */
    public static ItemStack modernResult(InventoryCrafting inv, World world) {
        try {
            var level = Inventories1122.level(world);
            return find(inv, world).map(m -> Stacks1122.fromModern(m.recipe().value().assemble(m.input().input(), level.registryAccess())))
                    .orElse(ItemStack.EMPTY);
        } catch (RuntimeException | LinkageError e) {
            return ItemStack.EMPTY;
        }
    }

    /** What the modern recipe leaves in the grid (buckets, ...), laid out over the whole legacy grid; null when no modern recipe matches. */
    public static NonNullList<ItemStack> modernRemaining(InventoryCrafting inv, World world) {
        try {
            var match = find(inv, world);
            if (match.isEmpty()) return null;
            var m = match.get();
            var rest = m.recipe().value().getRemainingItems(m.input().input());
            NonNullList<ItemStack> out = NonNullList.withSize(inv.getSizeInventory(), ItemStack.EMPTY);
            int iw = m.input().input().width();
            for (int k = 0; k < rest.size(); k++) {
                int x = m.input().left() + k % iw, y = m.input().top() + k / iw;
                int index = x + y * inv.getWidth();
                if (index >= 0 && index < out.size()) out.set(index, Stacks1122.fromModern(rest.get(k)));
            }
            return out;
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    /** Forge's ItemCraftedEvent for a legacy result slot, through the modern event (legacy handlers hear it from the event bridge). */
    public static void firePlayerCrafted(EntityPlayer player, ItemStack stack, InventoryCrafting matrix) {
        var p = Inventories1122.player(player);
        if (p == null) return;
        var grid = new net.minecraft.world.SimpleContainer(matrix.getSizeInventory());
        for (int i = 0; i < matrix.getSizeInventory(); i++) grid.setItem(i, Stacks1122.toModern(matrix.getStackInSlot(i)));
        net.neoforged.neoforge.event.EventHooks.firePlayerCraftingEvent(p, Stacks1122.toModern(stack), grid);
    }
}
