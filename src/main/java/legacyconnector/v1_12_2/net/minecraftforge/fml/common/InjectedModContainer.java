package legacyconnector.v1_12_2.net.minecraftforge.fml.common;

import java.io.File;
import java.util.Map;

/** A coremod's mod container as FML listed it: the container the coremod made, and the jar it came from. */
public class InjectedModContainer implements ModContainer {
    private final File source;
    public final ModContainer wrappedContainer;

    public InjectedModContainer(ModContainer mc, File source) {
        this.source = source != null ? source : mc.getSource();
        this.wrappedContainer = mc;
    }

    @Override
    public String getModId() {
        return wrappedContainer.getModId();
    }

    @Override
    public String getName() {
        return wrappedContainer.getName();
    }

    @Override
    public String getVersion() {
        return wrappedContainer.getVersion();
    }

    @Override
    public File getSource() {
        return source;
    }

    @Override
    public ModMetadata getMetadata() {
        return wrappedContainer.getMetadata();
    }

    @Override
    public Object getMod() {
        return wrappedContainer.getMod();
    }

    @Override
    public String getDisplayVersion() {
        return wrappedContainer.getDisplayVersion();
    }

    @Override
    public Map<String, String> getCustomModProperties() {
        return wrappedContainer.getCustomModProperties();
    }

    @Override
    public String getGuiClassName() {
        return wrappedContainer.getGuiClassName();
    }

    @Override
    public java.util.List<String> getOwnedPackages() {
        return wrappedContainer.getOwnedPackages();
    }
}
