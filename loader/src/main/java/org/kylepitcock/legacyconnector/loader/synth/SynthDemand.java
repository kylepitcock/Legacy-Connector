package org.kylepitcock.legacyconnector.loader.synth;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Everything one or more translated jars need from the synthesized legacy API: classes the hand-written shim lacks,
 * members on those classes, and stand-ins ("helpers") for members missing from hand-written shim classes.
 *
 * <p>Demand is recorded per jar while translating and persisted next to the cached jar, so the shared synthetic
 * jar can be rebuilt from the union on every launch without re-translating anything.</p>
 */
public final class SynthDemand {
    /** Member kinds. Lower-case kinds are helpers for members missing on hand-written shim classes. */
    public enum Kind {
        /** Instance method on a synthesized class (interface methods become defaults). */
        METHOD("M"),
        STATIC_METHOD("S"),
        FIELD("F"),
        STATIC_FIELD("G"),
        CONSTRUCTOR("N"),
        /** {@code static R name(Owner self, args)}: dynamic dispatch to a mod override, else a default. */
        HELPER_VIRTUAL("hv"),
        /** {@code super.name(...)} from a mod subclass: never dispatches (that would recurse into the caller). */
        HELPER_SPECIAL("hp"),
        HELPER_STATIC("hs"),
        HELPER_FIELD("hf"),
        HELPER_STATIC_FIELD("hg");

        final String code;

        Kind(String code) {
            this.code = code;
        }

        static Kind of(String code) {
            for (Kind k : values()) if (k.code.equals(code)) return k;
            throw new IllegalArgumentException(code);
        }

        public boolean helper() {
            return code.startsWith("h");
        }
    }

    public record Member(Kind kind, String name, String desc) implements Comparable<Member> {
        @Override
        public int compareTo(Member o) {
            int c = kind.compareTo(o.kind);
            if (c == 0) c = name.compareTo(o.name);
            return c == 0 ? desc.compareTo(o.desc) : c;
        }
    }

    /** Synthesized classes referenced at all (as owners, supertypes, or only inside descriptors). */
    public final Set<String> classes = new TreeSet<>();
    /** Synthesized classes that must be interfaces (implemented, or called through invokeinterface). */
    public final Set<String> interfaces = new TreeSet<>();
    /** Owner (synthesized class, or hand-written shim class for helpers) -> members. */
    public final Map<String, Set<Member>> members = new TreeMap<>();

    public void addClass(String name) {
        classes.add(name);
    }

    public void add(String owner, Kind kind, String name, String desc) {
        members.computeIfAbsent(owner, k -> new TreeSet<>()).add(new Member(kind, name, desc));
    }

    public boolean isEmpty() {
        return classes.isEmpty() && members.isEmpty();
    }

    public int helperCount() {
        int n = 0;
        for (Set<Member> set : members.values()) for (Member m : set) if (m.kind.helper()) n++;
        return n;
    }

    public void merge(SynthDemand other) {
        classes.addAll(other.classes);
        interfaces.addAll(other.interfaces);
        other.members.forEach((owner, set) -> members.computeIfAbsent(owner, k -> new TreeSet<>()).addAll(set));
    }

    public void write(Path file) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (String c : classes) sb.append("C ").append(c).append('\n');
        for (String c : interfaces) sb.append("I ").append(c).append('\n');
        members.forEach((owner, set) -> {
            for (Member m : set) sb.append(m.kind.code).append(' ').append(owner).append(' ').append(m.name).append(' ').append(m.desc).append('\n');
        });
        Files.writeString(file, sb, StandardCharsets.UTF_8);
    }

    public static SynthDemand read(Path file) throws IOException {
        SynthDemand d = new SynthDemand();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (line.isEmpty()) continue;
            String[] p = line.split(" ");
            switch (p[0]) {
                case "C" -> d.classes.add(p[1]);
                case "I" -> d.interfaces.add(p[1]);
                default -> d.add(p[1], Kind.of(p[0]), p[2], p[3]);
            }
        }
        return d;
    }
}
