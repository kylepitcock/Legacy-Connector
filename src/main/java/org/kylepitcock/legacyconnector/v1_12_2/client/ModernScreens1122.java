package org.kylepitcock.legacyconnector.v1_12_2.client;

import legacyconnector.v1_12_2.net.minecraft.client.Minecraft;
import legacyconnector.v1_12_2.net.minecraft.client.gui.GuiMainMenu;
import legacyconnector.v1_12_2.net.minecraft.client.gui.GuiScreen;
import legacyconnector.v1_12_2.net.minecraft.client.gui.inventory.GuiContainer;
import legacyconnector.v1_12_2.net.minecraft.client.gui.inventory.GuiContainerCreative;
import legacyconnector.v1_12_2.net.minecraft.client.gui.inventory.GuiInventory;
import legacyconnector.v1_12_2.net.minecraft.client.gui.recipebook.GuiRecipeBook;
import legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.inventory.Slot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122;
import org.kylepitcock.legacyconnector.v1_12_2.gui.ModernContainer1122;

/**
 * Legacy stand-ins for the game's own screens, typed as the 1.12.2 class each one replaces, so mods' screen event handlers that test
 * {@code instanceof GuiInventory}, {@code GuiContainerCreative}, {@code GuiContainer} or {@code GuiMainMenu} find them and read the
 * position, size, slots and recipe book of the modern screen. Other screens get the plain {@link ScreenEvents1122.View}.
 */
public final class ModernScreens1122 {
    private ModernScreens1122() {}

    static GuiScreen viewOf(Screen screen) {
        try {
            if (screen instanceof TitleScreen) return new MainMenuView(screen);
            if (screen instanceof net.minecraft.client.gui.screens.options.controls.ControlsScreen cs) return new ControlsView(cs);
            if (screen instanceof CreativeModeInventoryScreen cs) return new CreativeView(cs);
            if (screen instanceof InventoryScreen is) return new InventoryView(is);
            if (screen instanceof net.minecraft.client.gui.screens.inventory.CraftingScreen cs) return new CraftingView(cs);
            if (screen instanceof net.minecraft.client.gui.screens.inventory.HopperScreen hs) return new HopperView(hs);
            if (screen instanceof net.minecraft.client.gui.screens.inventory.BrewingStandScreen bs) return new BrewingView(bs);
            if (screen instanceof net.minecraft.client.gui.screens.inventory.BeaconScreen bs) return new BeaconView(bs);
            if (screen instanceof AbstractContainerScreen<?> acs) return new ContainerView(acs);
        } catch (RuntimeException | LinkageError e) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/Gui").debug("No typed legacy view of {}: {}", screen.getClass().getName(), e.toString());
        }
        return new ScreenEvents1122.View(screen);
    }

    private static EntityPlayer player() {
        var p = net.minecraft.client.Minecraft.getInstance().player;
        return p != null && EntityBridge1122.wrap(p) instanceof EntityPlayer ep ? ep : null;
    }

    private static void init(GuiScreen view, Screen modern) {
        view.mcc$initView(Minecraft.getMinecraft(), modern.width, modern.height);
    }

    /** What the container stand-ins share: the modern screen's position and size, its slots and hovered slot. */
    private static final class Link implements GuiContainer.Host {
        private final java.lang.ref.WeakReference<AbstractContainerScreen<?>> modern;

        Link(AbstractContainerScreen<?> modern) {
            this.modern = new java.lang.ref.WeakReference<>(modern);
        }

        AbstractContainerScreen<?> get() {
            return modern.get();
        }

        @Override
        public void mcc$renderContainer(int mouseX, int mouseY, float partialTicks) {
        }

        @Override
        public void mcc$renderTooltip(int mouseX, int mouseY) {
        }

        @Override
        public Slot mcc$hoveredSlot() {
            var s = modern.get();
            if (s == null || s.getSlotUnderMouse() == null) return null;
            var view = ModernContainer1122.of(s.getMenu());
            int i = s.getSlotUnderMouse().index;
            return i >= 0 && i < view.inventorySlots.size() ? view.inventorySlots.get(i) : null;
        }

        /** Copies the modern screen's state into the stand-in (called whenever the stand-in is handed to legacy code). */
        void sync(GuiContainer view) {
            var s = modern.get();
            if (s == null) return;
            view.setGuiSize(s.width, s.height);
            view.inventorySlots = ModernContainer1122.of(s.getMenu());
            view.mcc$setSize(s.getXSize(), s.getYSize());
            view.mcc$setOrigin(s.getGuiLeft(), s.getGuiTop());
        }
    }

    /** The modern controls screen as GuiControls; its parent is the stand-in of the screen it returns to. */
    public static final class ControlsView extends legacyconnector.v1_12_2.net.minecraft.client.gui.GuiControls implements ScreenEvents1122.Modern {
        private final java.lang.ref.WeakReference<Screen> modern;

        ControlsView(Screen modern) {
            super(null, Minecraft.mcc$get$gameSettings(Minecraft.getMinecraft()));
            this.modern = new java.lang.ref.WeakReference<>(modern);
            init(this, modern);
        }

        @Override
        public Screen mcc$modern() {
            return modern.get();
        }

        @Override
        public void mcc$sync() {
            Screen s = modern.get();
            if (s == null) return;
            setGuiSize(s.width, s.height);
            if (parentScreen == null) {
                Object last = Fields1122.get(net.minecraft.client.gui.screens.options.OptionsSubScreen.class, s, "lastScreen");
                if (last instanceof Screen ls) parentScreen = ScreenEvents1122.legacyOf(ls);
            }
        }
    }

    public static final class MainMenuView extends GuiMainMenu implements ScreenEvents1122.Modern {
        private final java.lang.ref.WeakReference<Screen> modern;

        MainMenuView(Screen modern) {
            this.modern = new java.lang.ref.WeakReference<>(modern);
            init(this, modern);
        }

        @Override
        public Screen mcc$modern() {
            return modern.get();
        }

        @Override
        public void mcc$sync() {
            Screen s = modern.get();
            if (s != null) setGuiSize(s.width, s.height);
        }
    }

    public static class ContainerView extends GuiContainer implements ScreenEvents1122.Modern {
        private final Link link;

        ContainerView(AbstractContainerScreen<?> modern) {
            super(ModernContainer1122.of(modern.getMenu()));
            this.link = new Link(modern);
            init(this, modern);
            mcc$attach(link);
            link.sync(this);
        }

        @Override
        protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        }

        @Override
        public Screen mcc$modern() {
            return link.get();
        }

        @Override
        public void mcc$sync() {
            link.sync(this);
        }

        @Override
        public boolean doesGuiPauseGame() {
            Screen s = link.get();
            return s != null && s.isPauseScreen();
        }
    }

    public static final class InventoryView extends GuiInventory implements ScreenEvents1122.Modern {
        private final Link link;

        InventoryView(InventoryScreen modern) {
            super(player());
            this.link = new Link(modern);
            init(this, modern);
            mcc$attach(link);
            mcc$setRecipeBook(GuiRecipeBook.mcc$of(modern.getRecipeBookComponent()));
            link.sync(this);
        }

        @Override
        public Screen mcc$modern() {
            return link.get();
        }

        @Override
        public void mcc$sync() {
            link.sync(this);
        }

        @Override
        public boolean doesGuiPauseGame() {
            return false;
        }
    }

    public static final class CreativeView extends GuiContainerCreative implements ScreenEvents1122.Modern {
        private final Link link;

        CreativeView(CreativeModeInventoryScreen modern) {
            super(player());
            this.link = new Link(modern);
            init(this, modern);
            mcc$attach(link);
            link.sync(this);
        }

        /** The legacy index of the modern screen's selected tab (-1 for a tab no legacy tab stands for, such as the survival inventory). */
        @Override
        public int getSelectedTabIndex() {
            return legacyIndex(selectedTab());
        }

        @Override
        public Screen mcc$modern() {
            return link.get();
        }

        @Override
        public void mcc$sync() {
            link.sync(this);
        }

        @Override
        public boolean doesGuiPauseGame() {
            return false;
        }
    }

    public static final class CraftingView extends legacyconnector.v1_12_2.net.minecraft.client.gui.inventory.GuiCrafting implements ScreenEvents1122.Modern {
        private final Link link;

        CraftingView(AbstractContainerScreen<?> modern) {
            super(null, null);
            this.link = new Link(modern);
            init(this, modern);
            mcc$attach(link);
            if (modern instanceof net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener r) mcc$setRecipeBook(GuiRecipeBook.mcc$of(r.getRecipeBookComponent()));
            link.sync(this);
        }

        @Override
        public Screen mcc$modern() {
            return link.get();
        }

        @Override
        public void mcc$sync() {
            link.sync(this);
        }

        @Override
        public boolean doesGuiPauseGame() {
            return false;
        }
    }

    public static final class HopperView extends legacyconnector.v1_12_2.net.minecraft.client.gui.GuiHopper implements ScreenEvents1122.Modern {
        private final Link link;

        HopperView(AbstractContainerScreen<?> modern) {
            super(null, null);
            this.link = new Link(modern);
            init(this, modern);
            mcc$attach(link);
            link.sync(this);
        }

        @Override
        public Screen mcc$modern() {
            return link.get();
        }

        @Override
        public void mcc$sync() {
            link.sync(this);
        }

        @Override
        public boolean doesGuiPauseGame() {
            return false;
        }
    }

    public static final class BrewingView extends legacyconnector.v1_12_2.net.minecraft.client.gui.inventory.GuiBrewingStand implements ScreenEvents1122.Modern {
        private final Link link;

        BrewingView(AbstractContainerScreen<?> modern) {
            super(null, null);
            this.link = new Link(modern);
            init(this, modern);
            mcc$attach(link);
            link.sync(this);
        }

        @Override
        public Screen mcc$modern() {
            return link.get();
        }

        @Override
        public void mcc$sync() {
            link.sync(this);
        }

        @Override
        public boolean doesGuiPauseGame() {
            return false;
        }
    }

    public static final class BeaconView extends legacyconnector.v1_12_2.net.minecraft.client.gui.inventory.GuiBeacon implements ScreenEvents1122.Modern {
        private final Link link;

        BeaconView(AbstractContainerScreen<?> modern) {
            super(null, null);
            this.link = new Link(modern);
            init(this, modern);
            mcc$attach(link);
            link.sync(this);
        }

        @Override
        public Screen mcc$modern() {
            return link.get();
        }

        @Override
        public void mcc$sync() {
            link.sync(this);
        }

        @Override
        public boolean doesGuiPauseGame() {
            return false;
        }
    }

    private static java.lang.reflect.Field selectedTabField;

    private static net.minecraft.world.item.CreativeModeTab selectedTab() {
        try {
            if (selectedTabField == null) {
                var f = CreativeModeInventoryScreen.class.getDeclaredField("selectedTab");
                f.setAccessible(true);
                selectedTabField = f;
            }
            return (net.minecraft.world.item.CreativeModeTab) selectedTabField.get(null);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    /** The legacy tab a modern tab stands for: the vanilla groups by their 1.12 names, the mods' tabs by the id the bridge gave them. */
    static int legacyIndex(net.minecraft.world.item.CreativeModeTab tab) {
        if (tab == null) return -1;
        var key = net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
        if (key == null) return -1;
        String label = switch (key.toString()) {
            case "minecraft:building_blocks" -> "buildingBlocks";
            case "minecraft:functional_blocks", "minecraft:colored_blocks", "minecraft:natural_blocks" -> "decorations";
            case "minecraft:redstone_blocks" -> "redstone";
            case "minecraft:food_and_drinks" -> "food";
            case "minecraft:tools_and_utilities" -> "tools";
            case "minecraft:combat" -> "combat";
            case "minecraft:ingredients", "minecraft:spawn_eggs" -> "misc";
            case "minecraft:search" -> "search";
            default -> null;
        };
        java.util.List<CreativeTabs> tabs;
        synchronized (CreativeTabs.CREATIVE_TAB_ARRAY) {
            tabs = new java.util.ArrayList<>(CreativeTabs.mcc$TABS);
        }
        for (CreativeTabs t : tabs) {
            if (t == null || t.getTabLabel() == null) continue;
            if (label != null) {
                if (label.equals(t.getTabLabel())) return t.getTabIndex();
            } else if (key.getNamespace().equals("legacyconnector")
                    && key.getPath().equals("legacy12_" + org.kylepitcock.legacyconnector.core.Ids.normalizePath(t.getTabLabel()).replace('/', '_').replace('.', '_'))) {
                return t.getTabIndex();
            }
        }
        return -1;
    }
}
