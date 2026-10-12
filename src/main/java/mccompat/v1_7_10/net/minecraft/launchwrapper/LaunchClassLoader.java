package mccompat.v1_7_10.net.minecraft.launchwrapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stand-in for LaunchWrapper's class loader. Class lookups delegate to the game's module class loader; byte lookups
 * read class files from it (legacy Minecraft names are tried under the shim namespace too). Transformer and
 * exclusion registration is accepted and ignored.
 */
public class LaunchClassLoader extends URLClassLoader {
    private static final Logger LOG = LoggerFactory.getLogger("Retrofit/LaunchWrapper");
    private final ClassLoader game;
    private final List<IClassTransformer> transformers = new ArrayList<>();

    public LaunchClassLoader(URL[] sources) {
        this(LaunchClassLoader.class.getClassLoader());
    }

    LaunchClassLoader(ClassLoader game) {
        super(new URL[0], game);
        this.game = game;
    }

    /** Target of rewritten {@code (LaunchClassLoader) x} casts: any other class loader becomes {@link Launch#classLoader}. */
    public static LaunchClassLoader mcc$cast(Object o) {
        return o == null || o instanceof LaunchClassLoader ? (LaunchClassLoader) o : Launch.classLoader;
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            return Class.forName(name, false, game);
        } catch (ClassNotFoundException e) {
            String relocated = relocate(name);
            if (relocated != null) return Class.forName(relocated, false, game);
            throw e;
        }
    }

    public byte[] getClassBytes(String name) throws IOException {
        // In a production 1.7.10 game Minecraft's classes only existed under obfuscated names: mods probe for
        // "net.minecraft.world.World" to detect a development environment (and then ask for MCP mappings).
        String plain = name.startsWith("mccompat.v1_7_10.") ? name.substring("mccompat.v1_7_10.".length()) : name;
        if (plain.startsWith("net.minecraft.") && !plain.startsWith("net.minecraftforge.")) return null;
        for (String n : new String[] {name, relocate(name)}) {
            if (n == null) continue;
            try (InputStream in = game.getResourceAsStream(n.replace('.', '/') + ".class")) {
                if (in != null) return in.readAllBytes();
            }
        }
        return null;
    }

    private static String relocate(String name) {
        if (name.startsWith("net.minecraft.") || name.startsWith("net.minecraftforge.") || name.startsWith("cpw.mods.fml.")) {
            return "mccompat.v1_7_10." + name;
        }
        return null;
    }

    public void registerTransformer(String transformerClassName) {
        LOG.info("Ignoring legacy class transformer {} (legacy bytecode patches cannot apply to 1.21.1)", transformerClassName);
    }

    public List<IClassTransformer> getTransformers() {
        return transformers;
    }

    private final Set<String> transformerExceptions = new java.util.HashSet<>();
    private final Set<String> classLoaderExceptions = new java.util.HashSet<>();
    private final Set<String> invalidClasses = new java.util.HashSet<>();

    /**
     * LaunchWrapper's private hook that libraries call by reflection (ForgeMultipart's mixin compiler) on classes it
     * assembled from already translated bytes: nothing is left to transform.
     */
    private byte[] runTransformers(String name, String transformedName, byte[] basicClass) {
        return basicClass;
    }

    public void addTransformerExclusion(String prefix) {
        transformerExceptions.add(prefix);
    }

    public void addClassLoaderExclusion(String prefix) {
        classLoaderExceptions.add(prefix);
    }

    public void clearNegativeEntries(Set<String> entries) {
    }

    public List<URL> getSources() {
        return new ArrayList<>();
    }
}
