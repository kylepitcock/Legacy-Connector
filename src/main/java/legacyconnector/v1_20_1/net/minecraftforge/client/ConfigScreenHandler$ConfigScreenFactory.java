package legacyconnector.v1_20_1.net.minecraftforge.client;

import java.util.function.BiFunction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.fml.IExtensionPoint;

/**
 * Forge 1.20.1's config screen extension point ({@code ConfigScreenHandler.ConfigScreenFactory}). The client half of the runtime re-registers it as
 * NeoForge's {@code IConfigScreenFactory} on the mod's container, so the "Config" button of the mod list opens it.
 */
public class ConfigScreenHandler$ConfigScreenFactory implements IExtensionPoint {
    private final BiFunction<Minecraft, Screen, Screen> screenFunction;

    public ConfigScreenHandler$ConfigScreenFactory(BiFunction<Minecraft, Screen, Screen> screenFunction) {
        this.screenFunction = screenFunction;
    }

    public BiFunction<Minecraft, Screen, Screen> screenFunction() {
        return screenFunction;
    }
}
