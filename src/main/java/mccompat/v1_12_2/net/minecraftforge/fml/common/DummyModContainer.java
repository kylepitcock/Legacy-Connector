package mccompat.v1_12_2.net.minecraftforge.fml.common;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/** A mod container without a mod class (coremods register these to appear as mods). */
public class DummyModContainer implements ModContainer {
    private final ModMetadata md;
    private String label;
    /** The coremod's jar (FML's coremod containers reported it; mods map jars to mods with it). */
    public File mcc$source;

    public DummyModContainer(ModMetadata md) {
        this.md = md == null ? new ModMetadata() : md;
    }

    public DummyModContainer() {
        this(new ModMetadata());
    }

    public DummyModContainer(String label) {
        this();
        this.label = label;
    }

    @Override
    public String getModId() {
        return md.modId;
    }

    @Override
    public String getName() {
        return md.name;
    }

    @Override
    public String getVersion() {
        return md.version;
    }

    @Override
    public File getSource() {
        return mcc$source;
    }

    @Override
    public ModMetadata getMetadata() {
        return md;
    }

    @Override
    public Object getMod() {
        return null;
    }

    @Override
    public String getDisplayVersion() {
        return getVersion();
    }

    @Override
    public Map<String, String> getCustomModProperties() {
        return new HashMap<>();
    }

    @Override
    public String toString() {
        return md.modId + (label == null ? "" : ":" + label);
    }
}
