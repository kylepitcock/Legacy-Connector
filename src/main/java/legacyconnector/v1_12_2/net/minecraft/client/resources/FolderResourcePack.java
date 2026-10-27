package legacyconnector.v1_12_2.net.minecraft.client.resources;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

/** A resource pack in a folder. */
public class FolderResourcePack extends AbstractResourcePack {
    public FolderResourcePack(File folder) {
        super(folder);
    }

    @Override
    protected InputStream getInputStreamByName(String name) throws IOException {
        File file = new File(resourcePackFile, name);
        if (!file.isFile()) throw new FileNotFoundException(file.getPath());
        return new java.io.BufferedInputStream(new java.io.FileInputStream(file));
    }

    @Override
    protected boolean hasResourceName(String name) {
        return new File(resourcePackFile, name).isFile();
    }

    @Override
    public Set<String> getResourceDomains() {
        Set<String> out = new HashSet<>();
        File[] dirs = new File(resourcePackFile, "assets/").listFiles(File::isDirectory);
        if (dirs != null) {
            for (File dir : dirs) {
                String name = getRelativeName(new File(resourcePackFile, "assets/"), dir);
                if (name.endsWith("/")) name = name.substring(0, name.length() - 1);
                if (name.equals(name.toLowerCase(java.util.Locale.ROOT))) out.add(name);
                else logNameNotLowercase(name);
            }
        }
        return out;
    }
}
