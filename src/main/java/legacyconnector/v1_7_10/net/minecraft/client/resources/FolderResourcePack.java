package legacyconnector.v1_7_10.net.minecraft.client.resources;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

/** A pack in a folder. */
public class FolderResourcePack extends AbstractResourcePack {
    public FolderResourcePack(File folder) {
        super(folder);
    }

    @Override
    protected InputStream getInputStreamByName(String name) throws IOException {
        return new BufferedInputStream(new FileInputStream(new File(resourcePackFile, name)));
    }

    @Override
    protected boolean hasResourceName(String name) {
        return new File(resourcePackFile, name).isFile();
    }

    public Set getResourceDomains() {
        Set<String> domains = new HashSet<>();
        File[] dirs = new File(resourcePackFile, "assets").listFiles(File::isDirectory);
        if (dirs != null) for (File dir : dirs) {
            String name = dir.getName();
            if (name.equals(name.toLowerCase(java.util.Locale.ROOT))) domains.add(name);
        }
        return domains;
    }
}
