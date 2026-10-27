package legacyconnector.v1_12_2.net.minecraftforge.fml.client;

import java.util.Set;
import legacyconnector.v1_12_2.net.minecraft.client.Minecraft;
import legacyconnector.v1_12_2.net.minecraft.client.gui.GuiScreen;

/** A mod's config screen factory ({@code @Mod(guiFactory = ...)}): the mod list's Config button opens what it creates. */
public interface IModGuiFactory {
    void initialize(Minecraft minecraftInstance);

    default boolean hasConfigGui() {
        return mainConfigGuiClass() != null;
    }

    /** 1.12.2's way; factories written for 1.12.0 only name the screen class ({@link #mainConfigGuiClass}). */
    default GuiScreen createConfigGui(GuiScreen parentScreen) {
        Class<? extends GuiScreen> cls = mainConfigGuiClass();
        if (cls == null) return null;
        try {
            var ctor = cls.getDeclaredConstructor(GuiScreen.class);
            ctor.setAccessible(true);
            return ctor.newInstance(parentScreen);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not create the config screen " + cls.getName(), e);
        }
    }

    @Deprecated
    default Class<? extends GuiScreen> mainConfigGuiClass() {
        return null;
    }

    Set<RuntimeOptionCategoryElement> runtimeGuiCategories();

    /** An in-game options category (FML never showed these). */
    class RuntimeOptionCategoryElement {
        public final String parent;
        public final String child;

        public RuntimeOptionCategoryElement(String parent, String child) {
            this.parent = parent;
            this.child = child;
        }
    }
}
