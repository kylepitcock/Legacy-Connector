package mccompat.v1_7_10.cpw.mods.fml.common;

import java.io.File;
import java.util.Map;

/** Subset of the legacy ModContainer interface that mods actually call. */
public interface ModContainer {
    String getModId();

    String getName();

    String getVersion();

    File getSource();

    ModMetadata getMetadata();

    Object getMod();

    String getDisplayVersion();

    Map<String, String> getCustomModProperties();
}
