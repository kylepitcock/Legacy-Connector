package org.kylepitcock.legacyconnector.v1_12_2.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import legacyconnector.v1_12_2.net.minecraft.client.Minecraft;
import legacyconnector.v1_12_2.net.minecraft.client.gui.GuiScreen;
import legacyconnector.v1_12_2.net.minecraftforge.fml.client.IModGuiFactory;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.Mod;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.kylepitcock.legacyconnector.core.LegacyModRecord;
import org.slf4j.LoggerFactory;

/**
 * FML's mod list opened a 1.12 mod's config screen through its {@code @Mod(guiFactory)} (or, for mods with {@code @Config} classes and no
 * factory, the default config screen); here NeoForge's mod list does, through the {@link IConfigScreenFactory} registered on the mod's
 * container. The legacy screen's parent stands for the mod list, so its Done button goes back there.
 */
public final class ConfigScreens1122 {
    private static final Map<String, IModGuiFactory> FACTORIES = new ConcurrentHashMap<>();

    private ConfigScreens1122() {}

    /** Called once per constructed 1.12.2 mod on the client. */
    public static void register(LegacyModRecord mod) {
        if (mod.mainClass == null || mod.container == null) return;
        Mod annotation = mod.mainClass.getAnnotation(Mod.class);
        boolean hasFactory = annotation != null && !annotation.guiFactory().isEmpty();
        boolean hasConfig = legacyconnector.v1_12_2.net.minecraftforge.common.config.ConfigManager.getModConfigClasses(mod.legacyId).length > 0;
        if (!hasFactory && !hasConfig) return;
        try {
            mod.container.registerExtensionPoint(IConfigScreenFactory.class, (IConfigScreenFactory) (container, parent) -> open(mod, parent));
        } catch (RuntimeException e) {
            LoggerFactory.getLogger("LegacyConnector/Gui").warn("{}: could not register its config screen: {}", mod.legacyId, e.toString());
        }
    }

    /** The mod's gui factory, created and initialized on first use as FML did after loading; null if it failed. */
    private static IModGuiFactory factory(LegacyModRecord mod) {
        IModGuiFactory existing = FACTORIES.get(mod.legacyId);
        if (existing != null) return existing;
        Mod annotation = mod.mainClass.getAnnotation(Mod.class);
        String factoryClass = annotation == null ? "" : annotation.guiFactory();
        try {
            IModGuiFactory factory;
            if (factoryClass.isEmpty()) {
                factory = new DefaultFactory(mod.legacyId, annotation == null || annotation.name().isEmpty() ? mod.legacyId : annotation.name());
            } else {
                Class<?> cls = Class.forName(factoryClass, true, mod.mainClass.getClassLoader());
                var ctor = cls.getDeclaredConstructor();
                ctor.setAccessible(true);
                factory = (IModGuiFactory) ctor.newInstance();
            }
            factory.initialize(Minecraft.getMinecraft());
            FACTORIES.put(mod.legacyId, factory);
            return factory;
        } catch (Throwable t) {
            LoggerFactory.getLogger("LegacyConnector/Gui").error("{}: could not create its gui factory {}", mod.legacyId, factoryClass, t);
            return null;
        }
    }

    /** FMLClientHandler.getGuiFactoryFor: the gui factory of a legacy mod (null when it has none or it failed). */
    public static IModGuiFactory factoryFor(String legacyId) {
        if (legacyId == null) return null;
        LegacyModRecord mod = org.kylepitcock.legacyconnector.core.LegacyMods.byLegacyId(legacyId);
        if (mod == null || mod.mainClass == null) return null;
        Mod annotation = mod.mainClass.getAnnotation(Mod.class);
        boolean hasFactory = annotation != null && !annotation.guiFactory().isEmpty();
        boolean hasConfig = legacyconnector.v1_12_2.net.minecraftforge.common.config.ConfigManager.getModConfigClasses(mod.legacyId).length > 0;
        return hasFactory || hasConfig ? factory(mod) : null;
    }

    private static Screen open(LegacyModRecord mod, Screen parent) {
        try {
            IModGuiFactory factory = factory(mod);
            if (factory == null || !factory.hasConfigGui()) return parent;
            GuiScreen screen = factory.createConfigGui(ScreenEvents1122.legacyOf(parent));
            return screen == null ? parent : ClientGui1122.host(screen);
        } catch (Throwable t) {
            LoggerFactory.getLogger("LegacyConnector/Gui").error("{}: could not open its config screen", mod.legacyId, t);
            return parent;
        }
    }

    private static final class DefaultFactory extends legacyconnector.v1_12_2.net.minecraftforge.fml.client.DefaultGuiFactory {
        DefaultFactory(String modid, String title) {
            super(modid, title);
        }
    }
}
