package net.minecraft.launchwrapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * Retrofit's stand-in for LaunchWrapper's class loader while legacy coremods are asked for their class transformers
 * (before translation). It loads coremod classes from the legacy jars; Minecraft itself is not present.
 */
public class LaunchClassLoader extends URLClassLoader {
    public static final int BUFFER_SIZE = 1 << 12;
    /** Applied to every class this loader defines (renamed ASM 5 API). Set by Retrofit. */
    public static UnaryOperator<byte[]> mcc$definer = UnaryOperator.identity();

    private final List<IClassTransformer> transformers = new ArrayList<>();
    private final Set<String> classLoaderExceptions = new HashSet<>();
    private final Set<String> transformerExceptions = new HashSet<>();
    private final Map<String, Class<?>> cachedClasses = new HashMap<>();
    private IClassNameTransformer renameTransformer;

    public LaunchClassLoader(URL[] sources) {
        this(sources, LaunchClassLoader.class.getClassLoader());
    }

    public LaunchClassLoader(URL[] sources, ClassLoader parent) {
        super(sources, parent);
        for (String p : new String[] {"java.", "sun.", "org.lwjgl.", "org.apache.logging.", "net.minecraft.launchwrapper."}) addClassLoaderExclusion(p);
        for (String p : new String[] {"javax.", "argo.", "org.objectweb.asm.", "com.google.common.", "org.bouncycastle.",
                "net.minecraft.launchwrapper.injector.", "cpw.mods.fml.repackage.", "cpw.mods.fml.relauncher.",
                "cpw.mods.fml.common.asm.transformers."}) addTransformerExclusion(p);
    }

    public void registerTransformer(String transformerClassName) {
        try {
            IClassTransformer t = (IClassTransformer) loadClass(transformerClassName).getDeclaredConstructor().newInstance();
            transformers.add(t);
            if (t instanceof IClassNameTransformer n && renameTransformer == null) renameTransformer = n;
        } catch (Exception e) {
            throw new RuntimeException("Could not create class transformer " + transformerClassName, e);
        }
    }

    public List<IClassTransformer> getTransformers() {
        return Collections.unmodifiableList(transformers);
    }

    public void addClassLoaderExclusion(String toExclude) {
        classLoaderExceptions.add(toExclude);
    }

    public void addTransformerExclusion(String toExclude) {
        transformerExceptions.add(toExclude);
    }

    public Set<String> mcc$transformerExclusions() {
        return transformerExceptions;
    }

    public void clearNegativeEntries(Set<String> entriesToClear) {
    }

    @Override
    public void addURL(URL url) {
        super.addURL(url);
    }

    public List<URL> getSources() {
        return List.of(getURLs());
    }

    /** Raw bytes of a class; null for Minecraft's (not present: legacy production behaviour for "is this vanilla?"). */
    public byte[] getClassBytes(String name) throws IOException {
        if (name.startsWith("net.minecraft.") && !name.startsWith("net.minecraft.launchwrapper.")) return null;
        URL url = findResource(name.replace('.', '/') + ".class");
        if (url == null) return null;
        try (InputStream in = url.openStream()) {
            return in.readAllBytes();
        }
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        Class<?> cached = cachedClasses.get(name);
        if (cached != null) return cached;
        byte[] bytes;
        try {
            bytes = getClassBytes(name);
        } catch (IOException e) {
            throw new ClassNotFoundException(name, e);
        }
        if (bytes == null) throw new ClassNotFoundException(name);
        bytes = mcc$definer.apply(bytes);
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            String pkg = name.substring(0, dot);
            if (getDefinedPackage(pkg) == null) {
                try {
                    definePackage(pkg, null, null, null, null, null, null, null);
                } catch (IllegalArgumentException ignored) {
                    // defined concurrently
                }
            }
        }
        Class<?> c = defineClass(name, bytes, 0, bytes.length);
        cachedClasses.put(name, c);
        return c;
    }
}
