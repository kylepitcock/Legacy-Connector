package org.kylepitcock.legacyconnector.loader.shim;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Index of every class the runtime shim provides (everything under {@code legacyconnector/}) plus the modern
 * classes it extends. Generated at build time by {@link ShimIndexGenerator} and shipped as
 * {@code legacyconnector/shim-index.txt} inside the runtime jar.
 *
 * <p>Text format, one record per line:</p>
 * <pre>
 * C name super iface1,iface2|- access
 * M name desc access
 * F name desc access
 * </pre>
 */
public final class ShimIndex {
    public static final String RESOURCE = "legacyconnector/shim-index.txt";
    /** Static accessor naming convention for "virtual" fields: {@code mcc$get$posX(Entity)D}. */
    public static final String GETTER_PREFIX = "mcc$get$";
    public static final String SETTER_PREFIX = "mcc$set$";

    private final Map<String, ClassInfo> classes;

    public ShimIndex(Map<String, ClassInfo> classes) {
        this.classes = classes;
    }

    public static ShimIndex empty() {
        return new ShimIndex(new HashMap<>());
    }

    public ClassInfo get(String internalName) {
        return classes.get(internalName);
    }

    public boolean contains(String internalName) {
        return classes.containsKey(internalName);
    }

    public Collection<ClassInfo> all() {
        return classes.values();
    }

    public int size() {
        return classes.size();
    }

    public static ShimIndex read(InputStream in) throws IOException {
        Map<String, ClassInfo> classes = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            ClassInfo current = null;
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) continue;
                String[] p = line.split(" ");
                switch (p[0]) {
                    case "C" -> {
                        List<String> ifaces = p[3].equals("-") ? List.of() : Arrays.asList(p[3].split(","));
                        current = new ClassInfo(p[1], p[2].equals("-") ? null : p[2], ifaces, Integer.parseInt(p[4]));
                        classes.put(current.name, current);
                    }
                    case "M" -> current.methods.put(p[1] + p[2], Integer.parseInt(p[3]));
                    case "F" -> current.fields.put(p[1] + ":" + p[2], Integer.parseInt(p[3]));
                    default -> throw new IOException("Bad shim index line: " + line);
                }
            }
        }
        return new ShimIndex(classes);
    }

    public void write(Writer out) throws IOException {
        BufferedWriter w = new BufferedWriter(out);
        for (ClassInfo c : new TreeMap<>(classes).values()) {
            w.write("C " + c.name + " " + (c.superName == null ? "-" : c.superName) + " "
                    + (c.interfaces.isEmpty() ? "-" : String.join(",", c.interfaces)) + " " + c.access + "\n");
            for (var m : new TreeMap<>(c.methods).entrySet()) {
                int paren = m.getKey().indexOf('(');
                w.write("M " + m.getKey().substring(0, paren) + " " + m.getKey().substring(paren) + " " + m.getValue() + "\n");
            }
            for (var f : new TreeMap<>(c.fields).entrySet()) {
                int colon = f.getKey().indexOf(':');
                w.write("F " + f.getKey().substring(0, colon) + " " + f.getKey().substring(colon + 1) + " " + f.getValue() + "\n");
            }
        }
        w.flush();
    }
}
