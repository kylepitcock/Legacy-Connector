package org.kylepitcock.legacyconnector.compat.jei;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import legacyconnector.v1_12_2.mezz.jei.api.recipe.IRecipeCategory;
import legacyconnector.v1_12_2.mezz.jei.api.recipe.transfer.IRecipeTransferError;
import legacyconnector.v1_12_2.mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import legacyconnector.v1_12_2.mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import legacyconnector.v1_12_2.mezz.jei.api.recipe.transfer.IRecipeTransferInfo;
import legacyconnector.v1_12_2.mezz.jei.api.recipe.transfer.IRecipeTransferRegistry;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge.Recipe;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122;
import org.kylepitcock.legacyconnector.v1_12_2.gui.LegacyMenu1122;

/**
 * Recipe transfer for 1.12.2 JEI plugins: the plus button of a bridged category moves the recipe's ingredients into the open legacy
 * container. A basic registration (slot ranges) and a legacy {@code IRecipeTransferInfo} are handed to the modern JEI's own basic
 * transfer; a plugin's own {@code IRecipeTransferHandler} runs against a legacy recipe layout rebuilt from the shown recipe.
 */
final class Jei1122Transfer {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("LegacyConnector/JEI1122");

    private Jei1122Transfer() {}

    /** What a legacy plugin registered. */
    sealed interface Entry permits Basic, Handler, Info {
        String uid();
    }

    record Basic(Class<?> containerClass, String uid, int recipeStart, int recipeCount, int inventoryStart, int inventoryCount) implements Entry {}

    record Handler(IRecipeTransferHandler<?> handler, String uid) implements Entry {}

    record Info(IRecipeTransferInfo<?> info) implements Entry {
        @Override
        public String uid() {
            return info.getRecipeCategoryUid();
        }
    }

    static IRecipeTransferRegistry registry(List<Entry> into) {
        return new IRecipeTransferRegistry() {
            @Override
            public void addRecipeTransferHandler(Class<? extends legacyconnector.v1_12_2.net.minecraft.inventory.Container> containerClass, String uid,
                    int recipeSlotStart, int recipeSlotCount, int inventorySlotStart, int inventorySlotCount) {
                if (containerClass != null && uid != null) into.add(new Basic(containerClass, uid, recipeSlotStart, recipeSlotCount, inventorySlotStart, inventorySlotCount));
            }

            @Override
            public void addRecipeTransferHandler(IRecipeTransferHandler<?> handler, String uid) {
                if (handler != null && uid != null) into.add(new Handler(handler, uid));
            }

            @SuppressWarnings("deprecation")
            @Override
            public void addRecipeTransferHandler(IRecipeTransferHandler<?> handler) {
                if (handler != null && handler.getRecipeCategoryUid() != null) into.add(new Handler(handler, handler.getRecipeCategoryUid()));
            }

            @Override
            public void addRecipeTransferHandler(IRecipeTransferInfo<?> info) {
                if (info != null && info.getRecipeCategoryUid() != null) into.add(new Info(info));
            }
        };
    }

    // ------------------------------------------------------------------ errors

    /** JEI 4's transfer errors, kept as data until the modern ones are built. */
    record Error(IRecipeTransferError.Type type, String message, Collection<Integer> slots) implements IRecipeTransferError {
        @Override
        public Type getType() {
            return type;
        }
    }

    static final IRecipeTransferHandlerHelper HELPER = new IRecipeTransferHandlerHelper() {
        @Override
        public IRecipeTransferError createInternalError() {
            return new Error(IRecipeTransferError.Type.INTERNAL, null, List.of());
        }

        @Override
        public IRecipeTransferError createUserErrorWithTooltip(String message) {
            return new Error(IRecipeTransferError.Type.USER_FACING, message, List.of());
        }

        @Override
        public IRecipeTransferError createUserErrorForSlots(String message, Collection<Integer> slots) {
            return new Error(IRecipeTransferError.Type.USER_FACING, message, slots == null ? List.of() : List.copyOf(slots));
        }
    };

    /** The modern error for a legacy one (what the modern JEI shows on the plus button). */
    static mezz.jei.api.recipe.transfer.IRecipeTransferError modern(mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper helper, IRecipeTransferError legacy) {
        if (legacy == null) return null;
        if (legacy.getType() == IRecipeTransferError.Type.INTERNAL) return helper.createInternalError();
        String message = legacy instanceof Error e && e.message() != null ? e.message() : "";
        return helper.createUserErrorWithTooltip(Component.literal(message));
    }

    // ------------------------------------------------------------------ registration with the modern JEI

    static void register(IRecipeTransferRegistration registration, List<Entry> entries, Function<String, Jei1122Bridge.Category> categoryOf) {
        if (entries.isEmpty()) return;
        var helper = registration.getTransferHelper();
        java.util.Set<String> done = new java.util.HashSet<>();
        for (Entry first : entries) {
            String uid = first.uid();
            if (!done.add(uid)) continue;
            var category = categoryOf.apply(uid);
            if (category == null) {
                LOG.debug("1.12.2 JEI transfer for {}: not a bridged category (the vanilla ones keep their own transfer)", uid);
                continue;
            }
            List<Entry> mine = new ArrayList<>();
            for (Entry e : entries) if (e.uid().equals(uid)) mine.add(e);
            try {
                registration.addRecipeTransferHandler(new Dispatcher(helper, category, mine), category.type);
            } catch (RuntimeException e) {
                LOG.debug("JEI refused the 1.12.2 transfer handler of {}: {}", uid, e.toString());
            }
        }
    }

    /** The legacy handler a container and category have, for a plugin that asks the recipe registry. */
    static IRecipeTransferHandler<?> handlerFor(legacyconnector.v1_12_2.net.minecraft.inventory.Container container, IRecipeCategory<?> category) {
        var bridge = Jei1122Bridge.current;
        if (bridge == null || container == null || category == null) return null;
        for (Entry e : bridge.transfers()) {
            if (e instanceof Handler h && h.uid().equals(category.getUid()) && h.handler().getContainerClass().isInstance(container)) return h.handler();
        }
        return null;
    }

    /** One modern handler per bridged category: the first legacy registration that fits the open container does the work. */
    private static final class Dispatcher implements mezz.jei.api.recipe.transfer.IRecipeTransferHandler<LegacyMenu1122, Recipe> {
        private final mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper helper;
        private final Jei1122Bridge.Category category;
        private final List<Entry> entries;
        private final java.util.Map<Entry, mezz.jei.api.recipe.transfer.IRecipeTransferHandler<LegacyMenu1122, Recipe>> basic = new java.util.IdentityHashMap<>();

        Dispatcher(mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper helper, Jei1122Bridge.Category category, List<Entry> entries) {
            this.helper = helper;
            this.category = category;
            this.entries = entries;
            for (Entry e : entries) {
                if (e instanceof Basic b) basic.put(e, helper.createUnregisteredRecipeTransferHandler(new BasicInfo(category, b)));
                else if (e instanceof Info i) basic.put(e, helper.createUnregisteredRecipeTransferHandler(adapt(category, i.info())));
                else if (e instanceof Handler h && h.handler() instanceof legacyconnector.v1_12_2.mezz.jei.transfer.BasicRecipeTransferHandler<?> bh) {
                    basic.put(e, helper.createUnregisteredRecipeTransferHandler(adapt(category, bh.info())));
                }
            }
        }

        @Override
        public Class<? extends LegacyMenu1122> getContainerClass() {
            return LegacyMenu1122.class;
        }

        @Override
        public Optional<MenuType<LegacyMenu1122>> getMenuType() {
            return Optional.empty();
        }

        @Override
        public RecipeType<Recipe> getRecipeType() {
            return category.type;
        }

        @Override
        public mezz.jei.api.recipe.transfer.IRecipeTransferError transferRecipe(LegacyMenu1122 menu, Recipe recipe, IRecipeSlotsView slots, Player player,
                boolean maxTransfer, boolean doTransfer) {
            var container = menu.legacy();
            for (Entry e : entries) {
                try {
                    if (!accepts(e, container)) continue;
                    var modern = basic.get(e);
                    if (modern != null) return modern.transferRecipe(menu, recipe, slots, player, maxTransfer, doTransfer);
                    return runLegacy(((Handler) e).handler(), container, recipe, player, maxTransfer, doTransfer);
                } catch (RuntimeException | LinkageError ex) {
                    LOG.debug("1.12.2 JEI transfer of {} failed: {}", e.uid(), ex.toString());
                    return helper.createInternalError();
                }
            }
            return helper.createInternalError();
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private mezz.jei.api.recipe.transfer.IRecipeTransferError runLegacy(IRecipeTransferHandler handler, legacyconnector.v1_12_2.net.minecraft.inventory.Container container,
                Recipe recipe, Player player, boolean maxTransfer, boolean doTransfer) {
            var layout = category.layoutOf(recipe);
            var legacyPlayer = EntityBridge1122.wrap(player);
            if (!(legacyPlayer instanceof legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer ep)) return helper.createInternalError();
            return modern(helper, handler.transferRecipe(container, layout, ep, maxTransfer, doTransfer));
        }

        private static boolean accepts(Entry e, legacyconnector.v1_12_2.net.minecraft.inventory.Container container) {
            if (e instanceof Basic b) return b.containerClass().isInstance(container);
            if (e instanceof Info i) return i.info().getContainerClass().isInstance(container);
            if (e instanceof Handler h) return h.handler().getContainerClass().isInstance(container);
            return false;
        }
    }

    /** A slot-range registration as the modern JEI's recipe transfer info. */
    private record BasicInfo(Jei1122Bridge.Category category, Basic basic) implements mezz.jei.api.recipe.transfer.IRecipeTransferInfo<LegacyMenu1122, Recipe> {
        @Override
        public Class<? extends LegacyMenu1122> getContainerClass() {
            return LegacyMenu1122.class;
        }

        @Override
        public Optional<MenuType<LegacyMenu1122>> getMenuType() {
            return Optional.empty();
        }

        @Override
        public RecipeType<Recipe> getRecipeType() {
            return category.type;
        }

        @Override
        public boolean canHandle(LegacyMenu1122 menu, Recipe recipe) {
            return basic.containerClass().isInstance(menu.legacy());
        }

        @Override
        public List<Slot> getRecipeSlots(LegacyMenu1122 menu, Recipe recipe) {
            return range(menu, basic.recipeStart(), basic.recipeCount());
        }

        @Override
        public List<Slot> getInventorySlots(LegacyMenu1122 menu, Recipe recipe) {
            return range(menu, basic.inventoryStart(), basic.inventoryCount());
        }
    }

    private static List<Slot> range(LegacyMenu1122 menu, int start, int count) {
        List<Slot> out = new ArrayList<>();
        for (int i = Math.max(0, start); i < start + count && i < menu.slots.size(); i++) out.add(menu.getSlot(i));
        return out;
    }

    /** A legacy IRecipeTransferInfo (slot lists the mod computes from its container) as the modern one. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static mezz.jei.api.recipe.transfer.IRecipeTransferInfo<LegacyMenu1122, Recipe> adapt(Jei1122Bridge.Category category, IRecipeTransferInfo info) {
        return new mezz.jei.api.recipe.transfer.IRecipeTransferInfo<>() {
            @Override
            public Class<? extends LegacyMenu1122> getContainerClass() {
                return LegacyMenu1122.class;
            }

            @Override
            public Optional<MenuType<LegacyMenu1122>> getMenuType() {
                return Optional.empty();
            }

            @Override
            public RecipeType<Recipe> getRecipeType() {
                return category.type;
            }

            @Override
            public boolean canHandle(LegacyMenu1122 menu, Recipe recipe) {
                return info.getContainerClass().isInstance(menu.legacy()) && info.canHandle(menu.legacy());
            }

            @Override
            public List<Slot> getRecipeSlots(LegacyMenu1122 menu, Recipe recipe) {
                return slots(menu, info.getRecipeSlots(menu.legacy()));
            }

            @Override
            public List<Slot> getInventorySlots(LegacyMenu1122 menu, Recipe recipe) {
                return slots(menu, info.getInventorySlots(menu.legacy()));
            }

            @Override
            public boolean requireCompleteSets(LegacyMenu1122 menu, Recipe recipe) {
                return info.requireCompleteSets();
            }

            private List<Slot> slots(LegacyMenu1122 menu, List<legacyconnector.v1_12_2.net.minecraft.inventory.Slot> legacy) {
                List<Slot> out = new ArrayList<>();
                if (legacy != null) for (var s : legacy) if (s != null && s.slotNumber >= 0 && s.slotNumber < menu.slots.size()) out.add(menu.getSlot(s.slotNumber));
                return out;
            }
        };
    }
}
