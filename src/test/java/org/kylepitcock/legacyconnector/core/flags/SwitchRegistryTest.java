package org.kylepitcock.legacyconnector.core.flags;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.loader.LoaderFlags;

/**
 * Keeps the switch registries ({@link Flags}, {@code CompatFlags}, {@link LoaderFlags}), the call sites and
 * {@code docs/wiki/Diagnostic-Switches.md} together: the page is generated (run the test with {@code SWITCH_DOCS_UPDATE=1} to rewrite it)
 * and a {@code legacyconnector.*} property cannot be read or set anywhere but through the registry.
 */
class SwitchRegistryTest {
    private static final String BEGIN = "<!-- switches:begin (generated from Flags, CompatFlags and LoaderFlags; run SwitchRegistryTest with SWITCH_DOCS_UPDATE=1 to rewrite) -->";
    private static final String END = "<!-- switches:end -->";
    private static final Path DOC = Path.of("docs/wiki/Diagnostic-Switches.md");

    /** One row of the page: the same switch declared in both registries is one row. */
    private record Row(String property, String type, String defaultText, String category, String description, boolean runtime, boolean loader) {}

    private static Map<String, Row> rows() {
        Map<String, Row> rows = new TreeMap<>();
        for (Switch s : Flags.all()) {
            Row row = new Row(s.property(), s.type().name(), s.defaultText(), s.category().name(), s.description(), true, false);
            assertNull(rows.put(s.property(), row), "declared twice in the runtime registry: " + s.property());
        }
        for (LoaderFlags s : LoaderFlags.values()) {
            Row row = new Row(s.property(), s.type().name(), s.defaultText(), s.category().name(), s.description(), false, true);
            Row runtime = rows.get(s.property());
            if (runtime == null) {
                rows.put(s.property(), row);
            } else {
                assertEquals(new Row(runtime.property(), runtime.type(), runtime.defaultText(), runtime.category(), runtime.description(), false, false),
                        new Row(row.property(), row.type(), row.defaultText(), row.category(), row.description(), false, false),
                        "declared in the runtime and the loader registry with different text: " + s.property());
                rows.put(s.property(), new Row(runtime.property(), runtime.type(), runtime.defaultText(), runtime.category(), runtime.description(), true, true));
            }
        }
        return rows;
    }

    private static String cell(String s) {
        return s.replace("|", "\\|").replace("<", "&lt;").replace(">", "&gt;");
    }

    static String markdownTable() {
        StringBuilder sb = new StringBuilder();
        Map<String, List<Row>> byCategory = new TreeMap<>();
        for (Row r : rows().values()) byCategory.computeIfAbsent(r.category(), k -> new ArrayList<>()).add(r);
        sb.append(rows().size()).append(" switches. \"Read by\" says which half of the layer reads (or, for internal ones, sets) the property.\n");
        for (String category : List.of("DEBUG", "TEST", "PERF", "COMPAT", "WORKAROUND", "TOOL", "INTERNAL")) {
            List<Row> list = byCategory.get(category);
            if (list == null) continue;
            sb.append("\n### ").append(category.charAt(0)).append(category.substring(1).toLowerCase()).append(" (").append(list.size()).append(")\n\n");
            sb.append("| Switch | Type | Default | Read by | What it does |\n|---|---|---|---|---|\n");
            for (Row r : list) {
                sb.append("| `").append(r.property().replace("|", "\\|")).append("` | ").append(r.type().toLowerCase()).append(" | ")
                        .append(r.defaultText().isEmpty() ? "empty" : "`" + r.defaultText().replace("|", "\\|") + "`").append(" | ")
                        .append(r.runtime() && r.loader() ? "both" : r.runtime() ? "runtime" : "loader").append(" | ")
                        .append(cell(r.description())).append(" |\n");
            }
        }
        return sb.toString();
    }

    @Test
    void documentationPageIsGeneratedFromTheRegistries() throws IOException {
        String text = Files.readString(DOC, StandardCharsets.UTF_8).replace("\r\n", "\n");
        int begin = text.indexOf(BEGIN);
        int end = text.indexOf(END);
        assertTrue(begin >= 0 && end > begin, DOC + " has the switches:begin and switches:end markers");
        String expected = BEGIN + "\n\n" + markdownTable() + "\n" + END;
        String actual = text.substring(begin, end + END.length());
        if (System.getenv("SWITCH_DOCS_UPDATE") != null && !expected.equals(actual)) {
            Files.writeString(DOC, text.substring(0, begin) + expected + text.substring(end + END.length()), StandardCharsets.UTF_8);
            return;
        }
        assertEquals(expected, actual, "a switch is missing from or out of date on the page: run SwitchRegistryTest with SWITCH_DOCS_UPDATE=1");
        for (Row r : rows().values()) assertTrue(text.contains("`" + r.property() + "`"), r.property());
    }

    @Test
    void everySwitchIsDescribed() {
        Set<String> seen = new HashSet<>();
        for (Row r : rows().values()) {
            assertTrue(seen.add(r.property()), r.property());
            assertFalse(r.description().isBlank(), "no description: " + r.property());
            assertFalse(r.defaultText() == null, "no default text: " + r.property());
            assertTrue(r.property().matches("[A-Za-z0-9_.<>]+"), r.property());
        }
    }

    // A property call with a literal "legacyconnector..." name (or a switch of the registry under another spelling) outside the registries.
    private static final Pattern RAW = Pattern.compile(
            "(System\\.(?:getProperty|setProperty|clearProperty)|Boolean\\.getBoolean|Integer\\.getInteger|Long\\.getLong)\\(\\s*\"(legacyconnector[.\\-][^\"]*)\"");

    @Test
    void noPropertyIsReadOrSetOutsideTheRegistry() throws IOException {
        List<String> offenders = new ArrayList<>();
        for (String dir : new String[] {"src/main/java", "src/test/java", "loader/src/main/java", "loader/src/test/java"}) {
            try (Stream<Path> files = Files.walk(Path.of(dir))) {
                for (Path file : (Iterable<Path>) files.filter(f -> f.toString().endsWith(".java"))::iterator) {
                    String name = file.getFileName().toString();
                    if (name.equals("SwitchRegistryTest.java")) continue;
                    Matcher m = RAW.matcher(Files.readString(file, StandardCharsets.UTF_8));
                    while (m.find()) offenders.add(file + ": " + m.group(0));
                }
            }
        }
        assertTrue(offenders.isEmpty(), "read the switch through Flags (runtime) or LoaderFlags (loader) and declare it there:\n" + String.join("\n", offenders));
    }

    @Test
    void accessorsKeepTheSemanticsOfTheCallsTheyReplace() {
        Flags flag = Flags.SOUND_DEBUG;
        String key = flag.property();
        String old = System.getProperty(key);
        try {
            flag.clear();
            assertFalse(flag.on());
            assertNull(flag.get());
            assertNull(flag.nonDefaultValue());
            flag.set("TRUE");
            assertEquals(Boolean.getBoolean(key), flag.on(), "Boolean.getBoolean is true only for the text true, in any case");
            flag.set("true");
            assertTrue(flag.on());
            assertEquals("true", flag.nonDefaultValue());
            assertTrue(Flags.nonDefaultSummary().contains(key + "=true"));
            flag.set("false");
            assertFalse(flag.on());
            assertNull(flag.nonDefaultValue(), "the default value is not listed");
            Flags.DIM_TEST_RADIUS.clear();
            assertEquals(2, Flags.DIM_TEST_RADIUS.getInt(2));
            assertNull(Flags.DIM_TEST_RADIUS.getInt());
            Flags.DIM_TEST_RADIUS.set("junk");
            assertEquals(2, Flags.DIM_TEST_RADIUS.getInt(2), "Integer.getInteger falls back to the default for text that is not a number");
            Flags.DIM_TEST_RADIUS.clear();
        } finally {
            if (old == null) System.clearProperty(key); else System.setProperty(key, old);
        }
    }
}
