package org.kylepitcock.legacyconnector.loader.tools;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

/**
 * Links every class of translated 1.20.1 jars with the JVM's own verifier (without initializing anything): a {@code VerifyError} means the migration
 * produced invalid bytecode (frames, stack shape of a rewritten call, a bridge method); a missing class or member is not reported (that is the gap report's job).
 * Run on the Minecraft + NeoForge class path: {@code ./gradlew verify1201 -PverifyDir=<dir with translated jars> [-PverifyMax=40]}.
 */
public final class Verify1201 {
    private Verify1201() {}

    public static void main(String[] args) throws Exception {
        Path dir = Path.of(args[0]);
        int max = args.length > 1 ? Integer.parseInt(args[1]) : 40;
        List<Path> jars;
        try (Stream<Path> s = Files.list(dir)) {
            jars = s.filter(p -> p.toString().endsWith(".jar")).sorted().toList();
        }
        List<URL> urls = new ArrayList<>();
        for (Path j : jars) urls.add(j.toUri().toURL());
        int classes = 0;
        int linked = 0;
        int missing = 0;
        TreeMap<String, String> bad = new TreeMap<>();
        try (URLClassLoader cl = new URLClassLoader(urls.toArray(URL[]::new), Verify1201.class.getClassLoader())) {
            for (Path jar : jars) {
                try (ZipFile zip = new ZipFile(jar.toFile())) {
                    for (var en = zip.entries(); en.hasMoreElements(); ) {
                        String name = en.nextElement().getName();
                        if (!name.endsWith(".class") || name.startsWith("META-INF/")) continue;
                        classes++;
                        String cn = name.substring(0, name.length() - 6).replace('/', '.');
                        try {
                            Class.forName(cn, false, cl).getDeclaredMethods(); // links the class: verification
                            linked++;
                        } catch (VerifyError | ClassFormatError e) {
                            bad.put(jar.getFileName() + ": " + cn, e.getClass().getSimpleName() + " " + firstLine(e.getMessage()));
                        } catch (LinkageError | ClassNotFoundException e) {
                            missing++;
                        } catch (RuntimeException e) {
                            missing++;
                        }
                    }
                }
            }
        }
        System.out.println("verify1201: " + classes + " classes, " + linked + " linked and verified, " + missing + " not linkable here (missing dependencies), " + bad.size() + " INVALID");
        int n = 0;
        for (var e : bad.entrySet()) {
            if (n++ >= max) break;
            System.out.println("  " + e.getKey() + "  " + e.getValue());
        }
        if (!bad.isEmpty()) System.exit(1);
    }

    private static String firstLine(String s) {
        if (s == null) return "";
        String flat = s.replace('\n', ' ');
        int reason = flat.indexOf("Reason:");
        String out = reason > 0 ? flat.substring(0, Math.min(flat.indexOf("Current Frame"), flat.length()) > 0 ? flat.indexOf("Current Frame") : flat.length()) : flat;
        return out.length() > 420 ? out.substring(0, 420) : out;
    }
}
