package legacyconnector.v1_12_2.net.minecraftforge.fml.client;

import java.util.Set;
import legacyconnector.v1_12_2.net.minecraft.client.Minecraft;
import legacyconnector.v1_12_2.net.minecraft.client.gui.GuiScreen;
import legacyconnector.v1_12_2.net.minecraftforge.fml.client.config.GuiConfig;

/** The config screen of a mod's {@code @Config} classes, for mods without a factory of their own. */
public class DefaultGuiFactory implements IModGuiFactory {
    protected String modid;
    protected String title;
    protected Minecraft minecraft;

    protected DefaultGuiFactory(String modid, String title) {
        this.modid = modid;
        this.title = title;
    }

    @Override
    public boolean hasConfigGui() {
        return true;
    }

    @Override
    public void initialize(Minecraft minecraftInstance) {
        this.minecraft = minecraftInstance;
    }

    @Override
    public GuiScreen createConfigGui(GuiScreen parentScreen) {
        return new GuiConfig(parentScreen, modid, title);
    }

    @Override
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
        return null;
    }

    public static IModGuiFactory forMod(legacyconnector.v1_12_2.net.minecraftforge.fml.common.ModContainer mod) {
        return new DefaultGuiFactory(mod.getModId(), mod.getName());
    }
}
