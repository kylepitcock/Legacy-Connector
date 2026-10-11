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
        return legacy(cls.getResource(name));
    }

    public static URL getResource(ClassLoader loader, String name) {
        return legacy(loader.getResource(name));
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
