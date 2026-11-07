package legacyconnector.v1_6_4.cpw.mods.fml.relauncher;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

/**
 * FML 1.5.2/1.6.4's class loader, which mods reach by casting their own: {@code (RelaunchClassLoader) Foo.class.getClassLoader()} (Immibis Core,
 * ForgeMultipart's mixin compiler, MiscPeripherals, CodeChickenCore). The real loader here is a different class, so the loader replaces such a
 * cast by {@link #wrap(Object)}, which returns a loader that delegates to the real one and offers what mods called on FML's:
 * {@code getClassBytes}, {@code registerTransformer} (coremod transformers are not run, see the report), the exclusion lists.
 */
public class RelaunchClassLoader extends URLClassLoader {
    private static final java.util.Map<ClassLoader, RelaunchClassLoader> WRAPPERS = new java.util.WeakHashMap<>();
    private final ClassLoader real;

    private RelaunchClassLoader(ClassLoader real) {
        super(new URL[0], real);
        this.real = real;
    }

    /** The replacement of {@code (RelaunchClassLoader) object}: a view of the real class loader (any other object gives one of the context loader). */
    public static synchronized RelaunchClassLoader wrap(Object object) {
        if (object instanceof RelaunchClassLoader r) return r;
        ClassLoader loader = object instanceof ClassLoader c ? c : Thread.currentThread().getContextClassLoader();
        return WRAPPERS.computeIfAbsent(loader, RelaunchClassLoader::new);
    }

    /** The bytes of a class as the loader sees it (name dotted), or null. */
    public byte[] getClassBytes(String name) throws IOException {
        try (InputStream in = real.getResourceAsStream(name.replace('.', '/') + ".class")) {
            return in == null ? null : in.readAllBytes();
        }
    }

    public void registerTransformer(String transformerClassName) {
        // the legacy coremods' transformers are not applied (the loader reports it); registering one is accepted and dropped
    }

    public void addClassLoaderExclusion(String prefix) {
    }

    public void addTransformerExclusion(String prefix) {
    }

    public List<?> getTransformers() {
        return List.of();
    }

    public java.util.Set<String> getInvalidClasses() {
        return java.util.Set.of();
    }
}
