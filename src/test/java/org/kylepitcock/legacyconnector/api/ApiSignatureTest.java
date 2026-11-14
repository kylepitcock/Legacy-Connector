package org.kylepitcock.legacyconnector.api;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * The stability promise: the public API (package {@code api} and {@code api.event}, not {@code api.internal}) never mentions a type of the
 * legacy shim ({@code legacyconnector.v1_7_10}, {@code v1_12_2}, {@code vb1_7_3}, {@code v1_6_4}, {@code v1_20_1}) or of the runtime
 * ({@code org.kylepitcock.legacyconnector.core}, bridges) in a public or protected signature (parameters, return types, field types,
 * generic arguments, thrown exceptions, superclasses and interfaces).
 */
class ApiSignatureTest {
    private static final Pattern FORBIDDEN = Pattern.compile("legacyconnector\\.vb?\\d|org\\.kylepitcock\\.legacyconnector\\.(core|compat|mixin|loader)\\.");

    @Test
    void publicSignaturesNameNoShimOrRuntimeTypes() throws Exception {
        Path root = Path.of(LegacyApi.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Path api = root.resolve("org/kylepitcock/legacyconnector/api");
        List<Class<?>> classes = new ArrayList<>();
        try (Stream<Path> files = Files.walk(api)) {
            for (Path f : files.filter(p -> p.toString().endsWith(".class")).toList()) {
                String rel = root.relativize(f).toString().replace('\\', '/').replace('/', '.');
                rel = rel.substring(0, rel.length() - ".class".length());
                if (rel.startsWith("org.kylepitcock.legacyconnector.api.internal") || rel.endsWith("package-info")) continue;
                classes.add(Class.forName(rel, false, ApiSignatureTest.class.getClassLoader()));
            }
        }
        assertTrue(classes.size() > 10, "found the api classes: " + classes.size());
        List<String> bad = new ArrayList<>();
        for (Class<?> c : classes) {
            check(bad, c, "superclass", c.getGenericSuperclass() == null ? "" : c.getGenericSuperclass().getTypeName());
            for (var i : c.getGenericInterfaces()) check(bad, c, "interface", i.getTypeName());
            for (Field f : c.getDeclaredFields()) {
                if (visible(f.getModifiers())) check(bad, c, "field " + f.getName(), f.getGenericType().getTypeName());
            }
            for (Constructor<?> k : c.getDeclaredConstructors()) {
                if (!visible(k.getModifiers())) continue;
                for (var p : k.getGenericParameterTypes()) check(bad, c, "constructor parameter", p.getTypeName());
            }
            for (Method m : c.getDeclaredMethods()) {
                if (!visible(m.getModifiers()) || m.isSynthetic()) continue;
                check(bad, c, "return of " + m.getName(), m.getGenericReturnType().getTypeName());
                for (var p : m.getGenericParameterTypes()) check(bad, c, "parameter of " + m.getName(), p.getTypeName());
                for (var e : m.getGenericExceptionTypes()) check(bad, c, "throws of " + m.getName(), e.getTypeName());
            }
        }
        assertTrue(bad.isEmpty(), "shim or runtime types in the public API: " + bad);
    }

    private static boolean visible(int modifiers) {
        return Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers);
    }

    private static void check(List<String> bad, Class<?> owner, String what, String typeName) {
        if (FORBIDDEN.matcher(typeName).find()) bad.add(owner.getSimpleName() + " " + what + ": " + typeName);
    }
}
