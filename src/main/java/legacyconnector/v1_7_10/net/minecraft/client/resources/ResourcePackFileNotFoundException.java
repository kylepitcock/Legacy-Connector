package legacyconnector.v1_7_10.net.minecraft.client.resources;

import java.io.File;
import java.io.FileNotFoundException;

/** A file missing from a resource pack. */
public class ResourcePackFileNotFoundException extends FileNotFoundException {
    public ResourcePackFileNotFoundException(File pack, String name) {
        super(String.format("'%s' in ResourcePack '%s'", name, pack));
    }
}
