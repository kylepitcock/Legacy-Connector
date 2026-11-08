package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Blocks;
import org.kylepitcock.legacyconnector.core.recipe.OversizedCraftingRecipe;
import org.slf4j.LoggerFactory;

/** Actual registered recipes, vanilla menus/click dispatch, and constrained-inventory conservation. */
final class RecipeCountSelfTestB173 {
    private record Scenario(String name, ClickType click, int button, int free, boolean occupied, int copies, int crafts) {}
    private static final Scenario[] CASES = {
        new Scenario("pickup", ClickType.PICKUP, 0, 36, false, 1, 1),
        new Scenario("quick", ClickType.QUICK_MOVE, 0, 36, false, 1, 1),
        new Scenario("quick-partial", ClickType.QUICK_MOVE, 0, 2, false, 1, 1),
        new Scenario("quick-full", ClickType.QUICK_MOVE, 0, 0, false, 1, 0),
        new Scenario("hotbar", ClickType.SWAP, 0, 36, false, 1, 1),
        new Scenario("hotbar-partial", ClickType.SWAP, 0, 1, false, 1, 1),
        new Scenario("hotbar-occupied", ClickType.SWAP, 0, 36, true, 1, 0),
        new Scenario("offhand-full", ClickType.SWAP, 40, 0, false, 1, 1),
        new Scenario("offhand-occupied", ClickType.SWAP, 40, 36, true, 1, 0)
    };

    static void run(ServerLevel level, ServerPlayer player, BlockPos pos) {
        var log = LoggerFactory.getLogger("LegacyConnector/b1.7.3");
        var manager = level.getRecipeManager();
        var originals = new ArrayList<RecipeHolder<?>>(manager.getRecipes());
        var actual = manager.getAllRecipesFor(RecipeType.CRAFTING).stream().filter(h -> oversized(h.value())).toList();
        if (actual.isEmpty()) { log.info("B1.7.3 RECIPE COUNT TEST: 0 oversized recipes, 0 failures"); return; }
        var previous = player.containerMenu;
        var state = level.getBlockState(pos);
        int passed = 0, failed = 0;
        try {
            level.setBlock(pos, Blocks.CRAFTING_TABLE.defaultBlockState(), 3);
            player.setPos(pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5);
            var recipes = new ArrayList<RecipeHolder<CraftingRecipe>>(actual);
            var output = actual.getFirst().value().getResultItem(level.registryAccess()).copyWithCount(1);
            var milk = new ShapelessRecipe("", CraftingBookCategory.MISC, output, NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.MILK_BUCKET)));
            recipes.add(new RecipeHolder<>(ResourceLocation.fromNamespaceAndPath("legacyconnector", "test_oversized_remainder"), new OversizedCraftingRecipe.Shapeless(milk, 4)));
            recipes.add(new RecipeHolder<>(ResourceLocation.fromNamespaceAndPath("legacyconnector", "test_normal_output"), new ShapelessRecipe("", CraftingBookCategory.MISC, new ItemStack(Items.STICK, 4), NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.IRON_INGOT)))));
            for (var holder : recipes) {
                manager.replaceRecipes(List.of(holder));
                for (boolean inventory : new boolean[] {false, true}) {
                    if (inventory && !holder.value().canCraftInDimensions(2, 2)) continue;
                    var scenarios = new ArrayList<>(List.of(CASES));
                    if (holder.value().getIngredients().stream().allMatch(i -> i.isEmpty() || i.getItems()[0].getMaxStackSize() >= 2))
                        scenarios.add(new Scenario("quick-repeated", ClickType.QUICK_MOVE, 0, 36, false, 2, 2));
                    for (var scenario : scenarios) {
                        try {
                            exercise(level, player, pos, holder.value(), inventory, scenario);
                            passed++;
                        } catch (RuntimeException | LinkageError e) {
                            failed++;
                            log.error("B1.7.3 RECIPE COUNT TEST failed for {} {} {}", holder.id(), inventory ? "2x2" : "table", scenario.name(), e);
                        }
                    }
                }
            }
        } finally {
            manager.replaceRecipes(originals);
            player.getInventory().clearContent();
            player.containerMenu = previous;
            level.setBlock(pos, state, 3);
        }
        log.info("B1.7.3 RECIPE COUNT TEST: {} oversized recipes, {} transfer scenarios passed, {} failures", actual.size(), passed, failed);
    }

    private static boolean oversized(CraftingRecipe r) { return r instanceof OversizedCraftingRecipe.Shaped || r instanceof OversizedCraftingRecipe.Shapeless; }

    private static void exercise(ServerLevel level, ServerPlayer player, BlockPos pos, CraftingRecipe recipe, boolean inventory, Scenario scenario) {
        player.getInventory().clearContent();
        for (int i = scenario.free(); i < 36; i++) player.getInventory().setItem(i, new ItemStack(Items.STONE, 64));
        if (scenario.occupied()) player.getInventory().setItem(scenario.button(), new ItemStack(Items.STONE, 64));
        AbstractContainerMenu menu = inventory ? new InventoryMenu(player.getInventory(), true, player)
                : new CraftingMenu(97, player.getInventory(), ContainerLevelAccess.create(level, pos));
        player.containerMenu = menu;
        int side = inventory ? 2 : 3;
        var grid = new ArrayList<ItemStack>();
        for (int i = 0; i < side * side; i++) grid.add(ItemStack.EMPTY);
        int width = recipe instanceof ShapedRecipe s ? s.getWidth() : side;
        var ingredients = recipe.getIngredients();
        for (int i = 0; i < ingredients.size(); i++) {
            var choices = ingredients.get(i).getItems();
            if (choices.length > 0) grid.set(i / width * side + i % width, choices[0].copyWithCount(scenario.copies()));
        }
        var input = CraftingInput.of(side, side, grid);
        var expected = recipe.assemble(input, level.registryAccess());
        var remaining = recipe.getRemainingItems(input);
        for (int i = 0; i < grid.size(); i++) menu.getSlot(i + 1).set(grid.get(i).copy());
        menu.slotsChanged(menu.getSlot(1).container);
        require(ItemStack.matches(menu.getSlot(0).getItem(), expected), "output differs");
        var box = player.getBoundingBox().inflate(8);
        var oldDrops = new HashSet<>(level.getEntitiesOfClass(ItemEntity.class, box));
        try {
            menu.clicked(0, scenario.button(), scenario.click(), player);
            if (scenario.click() == ClickType.PICKUP) {
                require(ItemStack.matches(menu.getCarried(), expected), "pickup lost output");
                int start = inventory ? 9 : 10;
                int end = inventory ? 45 : 46;
                for (int i = start; i < end && !menu.getCarried().isEmpty(); i++) menu.clicked(i, 0, ClickType.PICKUP, player);
                require(menu.getCarried().isEmpty(), "cursor not emptied");
            }
            int count = 0;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                var stack = player.getInventory().getItem(i);
                require(stack.getCount() <= stack.getMaxStackSize(), "inventory slot overfilled: " + i);
                if (ItemStack.isSameItemSameComponents(stack, expected)) count += stack.getCount();
            }
            int dropped = 0;
            for (var entity : level.getEntitiesOfClass(ItemEntity.class, box)) if (!oldDrops.contains(entity) && ItemStack.isSameItemSameComponents(entity.getItem(), expected)) dropped += entity.getItem().getCount();
            require(count + dropped == expected.getCount() * scenario.crafts(), "output conservation: inventory=" + count + " dropped=" + dropped);
            if (scenario.free() == 36) require(dropped == 0, "dropped despite inventory capacity");
            for (int i = 0; i < grid.size(); i++) {
                ItemStack want = grid.get(i).copy();
                want.shrink(scenario.crafts());
                if (scenario.crafts() > 0 && i % side < input.width() && i / side < input.height()) {
                    var rest = remaining.get(i / side * input.width() + i % side);
                    if (want.isEmpty()) want = rest;
                }
                require(ItemStack.matches(menu.getSlot(i + 1).getItem(), want), "ingredient/remainder " + i);
            }
            if (scenario.crafts() > 0) require(menu.getSlot(0).getItem().isEmpty(), "ingredients not consumed");
            else require(ItemStack.matches(menu.getSlot(0).getItem(), expected), "blocked click changed result");
        } finally {
            for (var entity : level.getEntitiesOfClass(ItemEntity.class, box)) if (!oldDrops.contains(entity)) entity.discard();
        }
    }
    private static void require(boolean passed, String message) { if (!passed) throw new IllegalStateException(message); }
}
