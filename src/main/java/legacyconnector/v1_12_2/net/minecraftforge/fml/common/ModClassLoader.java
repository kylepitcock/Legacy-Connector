package legacyconnector.v1_12_2.net.minecraftforge.fml.common;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * FML's class loader for mod jars. Translated mods all live in the game layer, so this loader delegates to it; jars added at runtime
 * ({@link #addFile}) are searched after it, untranslated.
 */
public class ModClassLoader extends URLClassLoader {
    static {
        ClassLoader.registerAsParallelCapable();
    }

    public ModClassLoader(ClassLoader parent) {
        super(new URL[0], parent);
    }

    public void addFile(File modFile) throws MalformedURLException {
        addURL(modFile.toURI().toURL());
    }

    public File[] getParentSources() {
        return new File[0];
    }

    public void clearNegativeCacheFor(java.util.Set<String> classList) {
    }

    public boolean isDefaultLibrary(File file) {
        return false;
    }

    public Class<?> loadBaseModClass(String modClazzName) throws ClassNotFoundException {
        return Class.forName(modClazzName, false, this);
    }
}
