package org.kylepitcock.legacyconnector.compat.jdk;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resource URLs as legacy mods expect them. Mod jars are served through NeoForge's union file system
 * ({@code union:/path/mod.jar%23123!/assets/x}); 1.7.10 mods check for the {@code jar:}/{@code file:} protocols to list
 * their own files or find their jar, and give up on anything else. Union URLs become the equivalent
 * {@code jar:file:} URL (or, for a jar's own location, its {@code file:} URL); both read the same bytes.
 */
public final class ResourceCompat {
    private static final Pattern UNION = Pattern.compile("^union:(.*?)(?:%23\\d+)?!(/.*)?$");

    private ResourceCompat() {}

    public static URL getResource(Class<?> cls, String name) {
        URL url = cls.getResource(name);
        if (url == null) {
            String lower = lowerAssets(name);
            if (lower != null) url = cls.getResource(lower);
        }
        // Legacy mods shared one class loader: a library class could read a resource of any other mod's jar.
        if (url == null && cls.getClassLoader() != null) url = fromLoader(cls, name, false);
        return legacy(url);
    }

    public static URL getResource(ClassLoader loader, String name) {
        URL url = loader.getResource(name);
        if (url == null) {
            String lower = lowerAssets(name);
            if (lower != null) url = loader.getResource(lower);
        }
        return legacy(url);
    }

    public static java.io.InputStream getResourceAsStream(Class<?> cls, String name) {
        java.io.InputStream in = cls.getResourceAsStream(name);
        if (in == null) {
            String lower = lowerAssets(name);
            if (lower != null) in = cls.getResourceAsStream(lower);
        }
        if (in == null && cls.getClassLoader() != null) in = fromLoader(cls, name);
        if (in == null) in = betaVanilla(cls, name);
        if (in == null && name.startsWith("/") && cls.getClassLoader() != null) in = vanillaFile(cls.getClassLoader(), name);
        return in;
    }

    /**
     * b1.7.3 mods read the game's own images from minecraft.jar ({@code /font/default.png}, which ComputerCraft's terminal renders with).
     * Those that kept their layout come from the modern game's assets instead (the ASCII font sheet is still a 16x16 glyph grid).
     */
    private static java.io.InputStream betaVanilla(Class<?> cls, String name) {
        String n = cls.getName();
        if (!n.startsWith("legacyconnector.vb1_7_3.") && !n.startsWith("legacyconnector_synth.vb1_7_3.") && !n.startsWith("legacyconnector_b173.")) {
            return null;
        }
        String modern = switch (name.startsWith("/") ? name : "/" + name) {
            case "/font/default.png" -> "assets/minecraft/textures/font/ascii.png";
            default -> null;
        };
        if (modern == null) return null;
        java.io.InputStream in = ResourceCompat.class.getClassLoader().getResourceAsStream(modern);
        if (in == null) in = ClassLoader.getSystemResourceAsStream(modern);
        return in;
    }

    /**
     * Forge 1.5.2 mods read vanilla's own files from the class path ({@code /font/default.png}, {@code /terrain.png}: ComputerCraft's
     * terminal font); they are the game's textures at their modern places.
     */
    private static java.io.InputStream vanillaFile(ClassLoader loader, String name) {
        try {
            if (name.equals("/title/splashes.txt")) return loader.getResourceAsStream("assets/minecraft/texts/splashes.txt");
            var loc = org.kylepitcock.legacyconnector.v1_6_4.bridge.Textures152.location(name);
            if (loc == null || !loc.getResourceDomain().equals("minecraft") || loc.getResourcePath().startsWith("/")) return null;
            return loader.getResourceAsStream("assets/minecraft/" + loc.getResourcePath());
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    /** Class-relative names resolve against the package, absolute ones against the root, then through the whole loader. */
    private static String loaderPath(Class<?> cls, String name) {
        if (name.startsWith("/")) return name.substring(1);
        String pkg = cls.getPackageName();
        return pkg.isEmpty() ? name : pkg.replace('.', '/') + "/" + name;
    }

    private static java.io.InputStream fromLoader(Class<?> cls, String name) {
        String path = loaderPath(cls, name);
        java.io.InputStream in = cls.getClassLoader().getResourceAsStream(path);
        if (in == null) {
            String lower = lowerAssets(path);
            if (lower != null) in = cls.getClassLoader().getResourceAsStream(lower);
        }
        return in;
    }

    private static URL fromLoader(Class<?> cls, String name, boolean unused) {
        String path = loaderPath(cls, name);
        URL url = cls.getClassLoader().getResource(path);
        if (url == null) {
            String lower = lowerAssets(path);
            if (lower != null) url = cls.getClassLoader().getResource(lower);
        }
        return url;
    }

    public static java.io.InputStream getResourceAsStream(ClassLoader loader, String name) {
        java.io.InputStream in = loader.getResourceAsStream(name);
        if (in == null) {
            String lower = lowerAssets(name);
            if (lower != null) in = loader.getResourceAsStream(lower);
        }
        return in;
    }

    /**
     * The asset path with the part below {@code assets/<domain>/} lower-cased: legacy resource names are case
     * sensitive, but the translated jars (modern resource pack rules) keep them lower case.
     */
    private static String lowerAssets(String name) {
        int at = name.indexOf("assets/");
        if (at < 0) return null;
        String lower = name.substring(0, at) + name.substring(at).toLowerCase(java.util.Locale.ROOT);
        return lower.equals(name) ? null : lower;
    }

    public static Enumeration<URL> getResources(ClassLoader loader, String name) throws IOException {
        List<URL> out = new ArrayList<>();
        for (Enumeration<URL> e = loader.getResources(name); e.hasMoreElements(); ) out.add(legacy(e.nextElement()));
        return Collections.enumeration(out);
    }

    public static URL getLocation(CodeSource source) {
        URL url = source.getLocation();
        if (url == null || !"union".equals(url.getProtocol())) return url;
        Matcher m = UNION.matcher(url.toString());
        if (!m.matches()) return url;
        try {
            return URI.create("file:" + m.group(1)).toURL();
        } catch (MalformedURLException | IllegalArgumentException e) {
            return url;
        }
    }

    /** Spell icon indices are saved in AM2's items, so keep its case-insensitive filename order. */
    public static List<String> listAm2SpellIcons(Class<?> anchor) throws IOException, URISyntaxException {
        String directory = "assets/arsmagica2/textures/items/spells/icons/";
        List<String> icons = new ArrayList<>();
        CodeSource source = anchor.getProtectionDomain().getCodeSource();
        URL location = source == null ? null : getLocation(source);
        if (location == null || !location.getProtocol().equals("file")) return icons;

        Path path = Path.of(location.toURI());
        if (Files.isDirectory(path)) {
            Path iconDirectory = path.resolve(directory);
            if (!Files.isDirectory(iconDirectory)) return icons;
            try (var files = Files.list(iconDirectory)) {
                files.filter(Files::isRegularFile)
                        .map(file -> file.getFileName().toString())
                        .filter(name -> name.endsWith(".png"))
                        .forEach(name -> icons.add("spells/icons/" + name.substring(0, name.length() - 4)));
            }
        } else {
            try (JarFile jar = new JarFile(path.toFile())) {
                jar.stream().map(java.util.zip.ZipEntry::getName)
                        .filter(name -> name.startsWith(directory) && name.endsWith(".png"))
                        .forEach(name -> icons.add("spells/icons/" + name.substring(directory.length(), name.length() - 4)));
            }
        }
        icons.sort(String.CASE_INSENSITIVE_ORDER);
        return icons;
    }

    static URL legacy(URL url) {
        if (url == null || !"union".equals(url.getProtocol())) return url;
        Matcher m = UNION.matcher(url.toString());
        if (!m.matches() || m.group(1).isEmpty()) return url;
        try {
            return new URL("jar:file:" + m.group(1) + "!" + (m.group(2) == null ? "/" : m.group(2)));
        } catch (MalformedURLException e) {
            return url;
        }
    }
}
