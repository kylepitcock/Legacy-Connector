package legacyconnector.v1_20_1.net.minecraftforge.fml.javafmlmod;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;

/**
 * Forge 1.20.1's way for a mod constructor to reach its mod event bus ({@code FMLJavaModLoadingContext.get().getModEventBus()}). NeoForge 1.21.1 injects the
 * bus into the constructor instead; this reads it from the container FML is constructing the mod for.
 */
public class FMLJavaModLoadingContext {
    private static final FMLJavaModLoadingContext INSTANCE = new FMLJavaModLoadingContext();

    private FMLJavaModLoadingContext() {
    }

    public static FMLJavaModLoadingContext get() {
        return INSTANCE;
    }

    public IEventBus getModEventBus() {
        return ModLoadingContext.get().getActiveContainer().getEventBus();
    }

    public ModContainer getContainer() {
        return ModLoadingContext.get().getActiveContainer();
    }
}
