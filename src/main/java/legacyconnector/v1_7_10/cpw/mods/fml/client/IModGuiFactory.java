package legacyconnector.v1_7_10.cpw.mods.fml.client;

import java.util.List;
import java.util.Set;
import legacyconnector.v1_7_10.net.minecraft.client.Minecraft;
import legacyconnector.v1_7_10.net.minecraft.client.gui.Gui;
import legacyconnector.v1_7_10.net.minecraft.client.gui.GuiScreen;

/**
 * A mod's {@code @Mod(guiFactory = ...)}: its config screen class, opened from the mod list's Config button (NeoForge's mod list here,
 * see {@code LegacyConfigScreens}). The runtime option categories were never shown by FML 1.7.10.
 */
public interface IModGuiFactory {
    void initialize(Minecraft minecraftInstance);

    /** The config screen; it has a constructor taking the parent {@link GuiScreen}. */
    Class<? extends GuiScreen> mainConfigGuiClass();

    Set<RuntimeOptionCategoryElement> runtimeGuiCategories();

    RuntimeOptionGuiHandler getHandlerFor(RuntimeOptionCategoryElement element);

    class RuntimeOptionCategoryElement {
        public final String parent;
        public final String child;

        public RuntimeOptionCategoryElement(String parent, String child) {
            this.parent = parent;
            this.child = child;
        }
    }

    interface RuntimeOptionGuiHandler {
        void addWidgets(List<Gui> widgetList, int x, int y, int w, int h);

        void paint(int x, int y, int w, int h);

        void actionCallback(int actionId);

        void close();
    }
}
