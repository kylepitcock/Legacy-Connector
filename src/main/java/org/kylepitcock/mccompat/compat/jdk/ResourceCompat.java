package org.kylepitcock.mccompat.compat.jdk;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
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
        return in;
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
