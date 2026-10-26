package legacyconnector.v1_7_10.net.minecraft.client.resources;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipFile;

/** A pack in a zip or jar file. */
public class FileResourcePack extends AbstractResourcePack implements Closeable {
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
        var entry = zip.getEntry(name);
        if (entry == null) throw new FileNotFoundException(name);
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

    public Set getResourceDomains() {
        Set<String> domains = new HashSet<>();
        try {
            var entries = zip().entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (!name.startsWith("assets/")) continue;
                String[] parts = name.split("/");
                if (parts.length > 2 && !parts[1].isEmpty() && parts[1].equals(parts[1].toLowerCase(java.util.Locale.ROOT))) domains.add(parts[1]);
            }
        } catch (IOException ignored) {
            // an unreadable file has no domains
        }
        return domains;
    }

    @Override
    public synchronized void close() throws IOException {
        if (resourcePackZipFile != null) {
            resourcePackZipFile.close();
            resourcePackZipFile = null;
        }
    }
}
