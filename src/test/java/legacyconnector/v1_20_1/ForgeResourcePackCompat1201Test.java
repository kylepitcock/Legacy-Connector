package legacyconnector.v1_20_1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import legacyconnector.v1_20_1.net.minecraftforge.resource.PathPackResources;
import net.neoforged.fml.jarcontents.JarContents;
import net.neoforged.neoforgespi.language.IModFileInfo;
import net.neoforged.neoforgespi.language.IModInfo;
import net.neoforged.neoforgespi.locating.IModFile;
import org.junit.jupiter.api.Test;

/** Tests the concrete Forge 1.20.1 PathPackResources facade used by legacy mod resource packs. */
class ForgeResourcePackCompat1201Test {
    @Test
    void folderModUsesItsActualRootAndModId() throws Exception {
        Path root = Files.createTempDirectory("legacyconnector-folder-mod-");
        Path resource = root.resolve("assets/example/lang/en_us.json");
        Files.createDirectories(resource.getParent());
        Files.writeString(resource, "{\"hello\":\"world\"}");

        try (JarContents contents = JarContents.ofPath(root)) {
            PathPackResources pack = ForgeApi1201.createPackForMod(modInfo("example-folder.jar", "example_folder", contents));
            try {
                assertEquals("example_folder", pack.packId());
                assertEquals(root, pack.getSource());
                assertEquals("{\"hello\":\"world\"}", Files.readString(pack.getSource().resolve("assets/example/lang/en_us.json")));
            } finally {
                pack.close();
            }
        } finally {
            deleteTree(root);
        }
    }

    @Test
    void jarModUsesAReadableOwnedZipFileSystemAndClosesIt() throws Exception {
        Path jar = Files.createTempFile("legacyconnector-jar-mod-", ".jar");
        writeJar(jar, "assets/example/lang/en_us.json", "{\"hello\":\"jar\"}");

        try (JarContents contents = JarContents.ofPath(jar)) {
            PathPackResources pack = ForgeApi1201.createPackForMod(modInfo("example.jar", "example", contents));
            var fileSystem = pack.getSource().getFileSystem();
            try {
                assertEquals("example", pack.packId());
                assertTrue(Files.isDirectory(pack.getSource()));
                assertEquals("{\"hello\":\"jar\"}", Files.readString(pack.getSource().resolve("assets/example/lang/en_us.json")));
                assertTrue(fileSystem.isOpen());
            } finally {
                pack.close();
            }
            assertFalse(fileSystem.isOpen(), "closing the legacy pack must close the ZIP file system");
        } finally {
            Files.deleteIfExists(jar);
        }
    }

    @Test
    void compositeModContentsAreMaterializedWithOverlayPrecedenceAndCleanedUp() throws Exception {
        Path lower = Files.createTempDirectory("legacyconnector-composite-lower-");
        Path upper = Files.createTempDirectory("legacyconnector-composite-upper-");
        Path lowerResource = lower.resolve("assets/example/lang/en_us.json");
        Path upperResource = upper.resolve("assets/example/lang/en_us.json");
        Files.createDirectories(lowerResource.getParent());
        Files.createDirectories(upperResource.getParent());
        Files.writeString(lowerResource, "{\"source\":\"lower\"}");
        Files.writeString(upperResource, "{\"source\":\"upper\"}");

        try (JarContents contents = JarContents.ofPaths(List.of(lower, upper))) {
            PathPackResources pack = ForgeApi1201.createPackForMod(modInfo("example-composite.jar", "example_composite", contents));
            Path root = pack.getSource();
            try {
                assertEquals("example_composite", pack.packId());
                assertTrue(Files.isDirectory(root));
                assertEquals("{\"source\":\"upper\"}", Files.readString(root.resolve("assets/example/lang/en_us.json")));
            } finally {
                pack.close();
            }
            assertFalse(Files.exists(root), "closing the legacy pack must remove its temporary merged root");
        } finally {
            deleteTree(lower);
            deleteTree(upper);
        }
    }

    private static IModFileInfo modInfo(String fileName, String modId, JarContents contents) {
        IModInfo mod = (IModInfo) Proxy.newProxyInstance(IModInfo.class.getClassLoader(),
                new Class<?>[] {IModInfo.class},
                (proxy, method, args) -> method.getName().equals("getModId") ? modId : defaultValue(proxy, method, args));

        IModFile file = (IModFile) Proxy.newProxyInstance(IModFile.class.getClassLoader(),
                new Class<?>[] {IModFile.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getFileName" -> fileName;
                    case "getContents" -> contents;
                    default -> defaultValue(proxy, method, args);
                });

        return (IModFileInfo) Proxy.newProxyInstance(IModFileInfo.class.getClassLoader(),
                new Class<?>[] {IModFileInfo.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getFile" -> file;
                    case "getMods" -> List.of(mod);
                    default -> defaultValue(proxy, method, args);
                });
    }

    private static Object defaultValue(Object proxy, Method method, Object[] args) {
        return switch (method.getName()) {
            case "toString" -> method.getDeclaringClass().getSimpleName() + " test proxy";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> {
                Class<?> type = method.getReturnType();
                if (!type.isPrimitive()) yield null;
                if (type == boolean.class) yield false;
                if (type == char.class) yield '\0';
                if (type == byte.class) yield (byte) 0;
                if (type == short.class) yield (short) 0;
                if (type == int.class) yield 0;
                if (type == long.class) yield 0L;
                if (type == float.class) yield 0.0F;
                if (type == double.class) yield 0.0D;
                yield null;
            }
        };
    }

    private static void writeJar(Path jar, String path, String contents) throws IOException {
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar))) {
            output.putNextEntry(new JarEntry(path));
            output.write(contents.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            output.closeEntry();
            output.putNextEntry(new JarEntry("pack.mcmeta"));
            output.write("{\"pack\":{\"description\":\"test\",\"pack_format\":34}}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            output.closeEntry();
        }
    }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }
}
