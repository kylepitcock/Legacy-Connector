package org.kylepitcock.legacyconnector.loader.transform;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One stage of the modern-version chain (docs/wiki/Legacy-1.16-1.19-Plan.md): a pure data description of "a jar of {@link #source} becomes a jar of
 * {@link #target}", whose tables live in {@code legacyconnector/forward/<dir>/}. The transform classes are the engine; a stage only says where its
 * rows are and which package holds its migration helpers. Stages chain: {@code 1.16.5 -> 1.18.2 -> 1.19.2 -> 1.20.1 -> 1.21.1}; a jar of version V goes
 * through {@link #chainFrom(String)} in order.
 *
 * @param source        game version the stage reads
 * @param target        game version the stage produces (the next stage's source, or the host version)
 * @param dir           folder below {@code legacyconnector/forward/}
 * @param helperPackage internal package prefix (trailing slash) of the stage's static migration helpers, used by {@code mc-calls.tsv} rows
 */
public record ForwardStage(String source, String target, String dir, String helperPackage) {
    /** Stage D, the only active one: Forge 1.20.1 jars to the 1.21.1 host. */
    public static final ForwardStage D_1201 = new ForwardStage("1.20.1", "1.21.1", "1.20.1", "org/kylepitcock/legacyconnector/v1_20_1/migrate/");
    /** Stage C (1.19.2 to 1.20.1): declared so the chain exists; its table folder is empty until the stage is built (not enabled for players). */
    public static final ForwardStage C_1192 = new ForwardStage("1.19.2", "1.20.1", "1.19.2", "org/kylepitcock/legacyconnector/v1_19_2/migrate/");

    /** All known stages, oldest first. */
    public static final List<ForwardStage> ALL = List.of(C_1192, D_1201);

    /** Resource path of a table file of this stage, e.g. {@code legacyconnector/forward/1.20.1/mc-classes.tsv}. */
    public String resource(String file) {
        return "legacyconnector/forward/" + dir + "/" + file;
    }

    /** Opens a table file of this stage, or null when the stage has none. */
    public InputStream open(String file) {
        return ForwardStage.class.getClassLoader().getResourceAsStream(resource(file));
    }

    /** The whole file, or null when absent. */
    public byte[] read(String file) {
        try (InputStream in = open(file)) {
            return in == null ? null : in.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Corrupt table " + resource(file), e);
        }
    }

    /** The stages a jar of {@code version} goes through, in order, ending at the host version; empty for an unknown version. */
    public static List<ForwardStage> chainFrom(String version) {
        return chainFrom(ALL, version);
    }

    static List<ForwardStage> chainFrom(List<ForwardStage> all, String version) {
        List<ForwardStage> out = new ArrayList<>();
        String at = version;
        for (ForwardStage s : all) {
            if (s.source.equals(at)) {
                out.add(s);
                at = s.target;
            }
        }
        return out;
    }

    /**
     * Composes the rename maps of consecutive stages (one map per stage, oldest first) into one that takes a name of the FIRST version to the host's:
     * a name that stage 1 renames to X and stage 2 renames X to Y becomes {@code name -> Y}. A key of a later stage that no earlier stage produces
     * is kept as it is (the class did not change before that stage). Rows that end up mapping a name to itself are dropped. Pure renaming only: stages
     * with semantic steps (helpers, bridges) keep their own tables and run in order.
     */
    public static Map<String, String> composeRenames(List<Map<String, String>> perStage) {
        Map<String, String> out = new LinkedHashMap<>();
        for (Map<String, String> stage : perStage) {
            for (Map.Entry<String, String> e : out.entrySet()) {
                String next = stage.get(e.getValue());
                if (next != null) e.setValue(next);
            }
            for (Map.Entry<String, String> e : stage.entrySet()) out.putIfAbsent(e.getKey(), e.getValue());
        }
        out.entrySet().removeIf(e -> e.getKey().equals(e.getValue()));
        return out;
    }
}
