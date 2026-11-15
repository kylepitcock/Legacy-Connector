package net.minecraftforge.fml.relauncher;

import java.util.Map;

/** FML's own loading plugin (it has no transformers here): the stand-in {@code CoreModManager.loadPlugins} starts with it, as FML's list starts with its own plugins. */
public class FMLCorePlugin implements IFMLLoadingPlugin {
    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
