package legacyconnector.v1_12_2.net.minecraft.client.resources;

import com.google.common.base.Splitter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** A resource pack in a zip file. */
public class FileResourcePack extends AbstractResourcePack implements java.io.Closeable {
    public static final Splitter ENTRY_NAME_SPLITTER = Splitter.on('/').omitEmptyStrings().limit(3);
    private ZipFile resourcePackZipFile;

    public FileResourcePack(File file) {
        super(file);
    }

    private synchronized ZipFile zip() throws IOException {
        if (resourcePackZipFile == null) resourcePackZipFile = new ZipFile(resourcePackFile);
        return resourcePackZipFile;
    }

    @Override
    protected InputStream getInputStreamByName(String name) throws IOException {
        ZipFile zip = zip();
        ZipEntry entry = zip.getEntry(name);
        if (entry == null) throw new FileNotFoundException(resourcePackFile + ":" + name);
        return zip.getInputStream(entry);
    }

    @Override
    public boolean hasResourceName(String name) {
        try {
            return zip().getEntry(name) != null;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public Set<String> getResourceDomains() {
        Set<String> out = new HashSet<>();
        try {
            var entries = zip().entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (!name.startsWith("assets/")) continue;
                var parts = ENTRY_NAME_SPLITTER.splitToList(name);
                if (parts.size() > 1) {
                    String domain = parts.get(1);
                    if (domain.equals(domain.toLowerCase(java.util.Locale.ROOT))) out.add(domain);
                    else logNameNotLowercase(domain);
                }
            }
        } catch (IOException e) {
            // an unreadable pack has no domains
        }
        return out;
    }

    @Override
    public synchronized void close() throws IOException {
        if (resourcePackZipFile != null) {
            resourcePackZipFile.close();
            resourcePackZipFile = null;
        }
    }
}
