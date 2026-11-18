package legacyconnector.v1_7_10.codechicken.nei.api;

import legacyconnector.v1_7_10.codechicken.nei.recipe.ICraftingHandler;
import legacyconnector.v1_7_10.codechicken.nei.recipe.IUsageHandler;
import legacyconnector.v1_7_10.net.minecraft.client.gui.inventory.GuiContainer;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import org.kylepitcock.legacyconnector.compat.nei.NeiBridge;

/** NEI's public registration API, recorded for the JEI bridge. GUI-overlay registrations are accepted and ignored. */
public class API {
    public static void registerRecipeHandler(ICraftingHandler handler) {
        NeiBridge.registerCraftingHandler(handler);
    }

    public static void registerUsageHandler(IUsageHandler handler) {
        NeiBridge.registerUsageHandler(handler);
    }

    public static void hideItem(ItemStack stack) {
        NeiBridge.hide(stack);
    }

    public static void hideItem(Item item) {
        if (item != null) NeiBridge.hide(new ItemStack(item, 1, 32767));
    }

    /** Groups items under a named subset of NEI's item panel; JEI has none, so the range is accepted and ignored. */
    public static void addSetRange(String name, legacyconnector.v1_7_10.codechicken.nei.MultiItemRange range) {
    }

    /** Adds a stack with NBT to NEI's item list; JEI lists the stack's item itself, so the entry is accepted and ignored. */
    public static void addNBTItem(ItemStack stack) {
    }

    /** Adds a range to a named NEI subset (accepted and ignored: JEI has no subsets). */
    public static void addToRange(String name, legacyconnector.v1_7_10.codechicken.nei.MultiItemRange range) {
    }

    /** NEI 1.6: the damage variants of an item by legacy id (JEI lists the subtypes itself). */
    public static void setItemDamageVariants(int id, java.util.Collection<ItemStack> variants) {
    }

    public static void addItemListEntry(ItemStack stack) {
    }

    public static void setItemListEntries(Item item, Iterable<ItemStack> entries) {
    }

    public static void setOverrideName(ItemStack stack, String name) {
    }

    public static void setMaxDamageException(int id, int maxDamage) {
    }

    public static void registerGuiOverlay(Class<? extends GuiContainer> gui, String identifier) {
    }

    public static void registerGuiOverlay(Class<? extends GuiContainer> gui, String identifier, int x, int y) {
    }

    public static void registerGuiOverlay(Class<? extends GuiContainer> gui, String identifier, IStackPositioner positioner) {
    }

    /** Names the block under the cursor in NEI's highlight tooltip (recorded for {@link ItemInfo#getIdentifierItems}). */
    public static void registerHighlightIdentifier(legacyconnector.v1_7_10.net.minecraft.block.Block block, IHighlightHandler handler) {
        ItemInfo.highlightIdentifiers.computeIfAbsent(block, b -> new java.util.ArrayList<>()).add(handler);
    }

    public static void registerHighlightHandler(IHighlightHandler handler, ItemInfo.Layout... layouts) {
        ItemInfo.highlightHandlers.add(handler);
    }

    /** A named item subset in NEI's list (the JEI bridge has no subset menu). */
    public static void addSubset(String name, ItemFilter filter) {
    }

    /** NEI 1.4.7 / 1.5.2: the root tag of a mod's subsets in the item panel's tree (kept, nothing draws it under JEI). */
    private static final legacyconnector.v1_7_10.codechicken.nei.SubSetRangeTag RANGE_ROOT = new legacyconnector.v1_7_10.codechicken.nei.SubSetRangeTag("root");

    public static legacyconnector.v1_7_10.codechicken.nei.SubSetRangeTag getRangeTag(String name) {
        return RANGE_ROOT.getTag(name);
    }

    /** NEI 1.4.7 / 1.5.2: hides a collection of item ids (JEI hides every damage value of the items too); stacks and items are accepted as well. */
    public static void hideItems(java.util.Collection<?> items) {
        if (items == null) return;
        for (Object o : items) {
            if (o instanceof Integer id) hideItem(legacyconnector.v1_6_4.net.minecraft.util.Ids.item(id));
            else if (o instanceof ItemStack stack) hideItem(stack);
            else if (o instanceof Item item) hideItem(item);
        }
    }

    /** NEI 1.4.7 / 1.5.2: a key binding by name, description and default key (JEI's input is its own; accepted and ignored). */
    public static void addKeyBind(String id, String description, int defaultKey) {
    }

    public static void addKeyBind(String name, int defaultKey) {
        legacyconnector.v1_7_10.codechicken.nei.NEIClientConfig.addKeyBinding(name, defaultKey);
    }

    public static void registerGuiOverlayHandler(Class<? extends GuiContainer> gui, IOverlayHandler handler, String identifier) {
    }

    public static void registerNEIGuiHandler(INEIGuiHandler handler) {
    }

    public static void setGuiOffset(Class<? extends GuiContainer> gui, int x, int y) {
    }

    /**
     * The machines that run a handler's recipes (NEI's later {@code RecipeCatalysts}, keyed by the handler's overlay id or class
     * name): the JEI category's icon and catalysts.
     */
    public static void addRecipeCatalyst(ItemStack stack, String handlerId) {
        NeiBridge.addCatalyst(handlerId, stack);
    }

    public static void addRecipeCatalyst(ItemStack stack, String handlerId, int priority) {
        NeiBridge.addCatalyst(handlerId, stack);
    }

    public static void addRecipeCatalyst(java.util.List<ItemStack> stacks, String handlerId) {
        if (stacks != null) for (ItemStack s : stacks) NeiBridge.addCatalyst(handlerId, s);
    }

    public static void addRecipeCatalyst(ItemStack stack, legacyconnector.v1_7_10.codechicken.nei.recipe.IRecipeHandler handler) {
        NeiBridge.addCatalyst(NeiBridge.handlerId(handler), stack);
    }

    public static void removeRecipeCatalyst(ItemStack stack, String handlerId) {
        NeiBridge.removeCatalyst(handlerId, stack);
    }
}
