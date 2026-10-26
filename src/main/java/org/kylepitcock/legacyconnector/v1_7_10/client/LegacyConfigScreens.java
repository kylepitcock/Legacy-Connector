package org.kylepitcock.legacyconnector.v1_7_10.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import legacyconnector.v1_7_10.cpw.mods.fml.client.IModGuiFactory;
import legacyconnector.v1_7_10.cpw.mods.fml.common.Mod;
import legacyconnector.v1_7_10.net.minecraft.client.Minecraft;
import legacyconnector.v1_7_10.net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.kylepitcock.legacyconnector.core.LegacyModRecord;
import org.slf4j.LoggerFactory;

/**
 * FML's mod list opened a mod's config screen through its {@code @Mod(guiFactory)}; here NeoForge's mod list does, through the
 * {@link IConfigScreenFactory} registered on the mod's container. The legacy screen's parent is {@link ModernParent}, so its Done
 * button goes back to the mod list.
 */
public final class LegacyConfigScreens {
    private static final Map<String, IModGuiFactory> FACTORIES = new ConcurrentHashMap<>();

    private LegacyConfigScreens() {}

    /** Called once per constructed 1.7.10 mod on the client. */
    public static void register(LegacyModRecord mod) {
        String factoryClass = factoryClass(mod);
        if (factoryClass == null) return;
        try {
            mod.container.registerExtensionPoint(IConfigScreenFactory.class, (IConfigScreenFactory) (container, parent) -> open(mod, parent));
        } catch (RuntimeException e) {
            LoggerFactory.getLogger("LegacyConnector/Gui").warn("{}: could not register its config screen: {}", mod.legacyId, e.toString());
        }
    }

    private static String factoryClass(LegacyModRecord mod) {
        if (mod.mainClass == null) return null;
        Mod annotation = mod.mainClass.getAnnotation(Mod.class);
        if (annotation == null || annotation.guiFactory().isEmpty()) return null;
        return annotation.guiFactory();
    }

    /** The mod's gui factory, created and initialized on first use as FML did after loading; null if it has none or it failed. */
    public static IModGuiFactory factory(LegacyModRecord mod) {
        String factoryClass = factoryClass(mod);
        if (factoryClass == null) return null;
        IModGuiFactory existing = FACTORIES.get(mod.legacyId);
        if (existing != null) return existing;
        try {
            Class<?> cls = Class.forName(factoryClass, true, mod.mainClass.getClassLoader());
            var ctor = cls.getDeclaredConstructor();
            ctor.setAccessible(true);
            IModGuiFactory factory = (IModGuiFactory) ctor.newInstance();
            factory.initialize(Minecraft.getMinecraft());
            FACTORIES.put(mod.legacyId, factory);
            return factory;
        } catch (Throwable t) {
            LoggerFactory.getLogger("LegacyConnector/Gui").error("{}: could not create its gui factory {}", mod.legacyId, factoryClass, t);
            return null;
        }
    }

    private static Screen open(LegacyModRecord mod, Screen parent) {
        try {
            IModGuiFactory factory = factory(mod);
            Class<? extends GuiScreen> screenClass = factory == null ? null : factory.mainConfigGuiClass();
            if (screenClass == null) return parent;
            var ctor = screenClass.getDeclaredConstructor(GuiScreen.class);
            ctor.setAccessible(true);
            return new LegacyScreen(ctor.newInstance(new ModernParent(parent)));
        } catch (Throwable t) {
            LoggerFactory.getLogger("LegacyConnector/Gui").error("{}: could not open its config screen", mod.legacyId, t);
            return parent;
        }
    }

    /** A legacy stand-in for a modern screen: displaying it shows the modern screen again. */
    public static final class ModernParent extends GuiScreen {
        private final Screen modern;

        public ModernParent(Screen modern) {
            this.modern = modern;
        }

        public Screen modern() {
            return modern;
        }
    }
}
