package org.kylepitcock.legacyconnector.loader.transform;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Classes of the NATIVE 1.21.1 mod jars of the mods folder (libraries a translated 1.20.1 mod runs against: Architectury, Curios, GeckoLib...). The migration
 * tables only know Minecraft and NeoForge; where a rewrite depends on what such a library declares today (the parameter types of a functional interface a
 * mod's lambda implements), the class is read from its jar here. Only method names and descriptors are kept; nothing is loaded or initialised.
 */
public final class NativeClasses1201 {
    private NativeClasses1201() {}

    private static volatile List<Path> jars = List.of();
    private static final Map<String, Set<String>> METHODS = new ConcurrentHashMap<>();
    private static final Set<String> ABSENT = ConcurrentHashMap.newKeySet();

    /** The native mod jars of the mods folder (set once per launch by the locator, before any jar is translated). */
    public static void setJars(List<Path> nativeJars) {
        jars = List.copyOf(nativeJars);
        METHODS.clear();
        ABSENT.clear();
    }

    /** {@code name + descriptor} of the methods the class declares, or null when no native jar of the set holds it. */
    public static Set<String> methods(String internalName) {
        Set<String> known = METHODS.get(internalName);
        if (known != null) return known;
        if (ABSENT.contains(internalName)) return null;
        String entry = internalName + ".class";
        for (Path jar : jars) {
            try (ZipFile zip = new ZipFile(jar.toFile())) {
                ZipEntry e = zip.getEntry(entry);
                if (e == null) continue;
                Set<String> out = new HashSet<>();
                try (InputStream in = zip.getInputStream(e)) {
                    new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9) {
                        @Override
                        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                            out.add(name + descriptor);
                            return null;
                        }
                    }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                }
                METHODS.put(internalName, out);
                return out;
            } catch (IOException | RuntimeException ignored) {
                // an unreadable jar is FML's to report
            }
        }
        ABSENT.add(internalName);
        return null;
    }
}
