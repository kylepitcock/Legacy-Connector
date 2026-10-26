package legacyconnector.v1_7_10.cpw.mods.fml.common;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * FML's class loader of mods. Legacy mods are loaded by the game's class loader here, so this one delegates to it;
 * files added to it (libraries a mod downloads) are searched after the game's classes. Its "parent sources" (the
 * Minecraft jar and libraries mods scanned for classes, as CodeChickenCore's class discoverer did) are none: Minecraft
 * is not a legacy jar, and the mods' own jars are listed by their containers.
 */
public class ModClassLoader extends URLClassLoader {
    private static final List<String> STANDARD_LIBRARIES = List.of("jinput.jar", "lwjgl.jar", "lwjgl_util.jar", "rt.jar");
    private final List<File> mcc$added = new ArrayList<>();

    public ModClassLoader(ClassLoader parent) {
        super(new URL[0], parent);
    }

    public void addFile(File modFile) throws MalformedURLException {
        if (modFile == null) return;
        mcc$added.add(modFile);
        addURL(modFile.toURI().toURL());
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return super.loadClass(name);
    }

    public File[] getParentSources() {
        return new File[0];
    }

    public List<String> getDefaultLibraries() {
        return STANDARD_LIBRARIES;
    }

    public boolean isDefaultLibrary(File file) {
        return file != null && STANDARD_LIBRARIES.contains(file.getName());
    }

    public void clearNegativeCacheFor(java.util.Set<String> classList) {
    }

    public URL[] getParentURLs() {
        return new URL[0];
    }
}
