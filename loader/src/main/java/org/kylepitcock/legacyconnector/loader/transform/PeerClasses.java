package org.kylepitcock.legacyconnector.loader.transform;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.kylepitcock.legacyconnector.loader.shim.ClassInfo;

/**
 * The classes of a mod set that other jars link against. Several jars often hold a class of the same name (a library shaded into mods, usually an
 * outdated copy): as in {@code SplitPackageResolver.bestCopy}, the copy of the jar with the most classes under the same two-segment package root wins
 * ({@code codechicken/lib/}), the first jar on a tie. A plain "first jar wins" made a link check depend on the alphabetical order of the jar names
 * (an old shaded CodeChickenLib in "adventurebackpack" shadowed the real library).
 */
public final class PeerClasses {
    private final Map<String, ClassInfo> best = new HashMap<>();
    private final Map<String, Integer> bestScore = new HashMap<>();

    /** Adds the classes of one jar. */
    public void add(List<ClassInfo> jarClasses) {
        Map<String, Integer> perRoot = new HashMap<>();
        for (ClassInfo c : jarClasses) perRoot.merge(root(c.name), 1, Integer::sum);
        for (ClassInfo c : jarClasses) {
            int score = perRoot.get(root(c.name));
            Integer old = bestScore.get(c.name);
            if (old == null || score > old) {
                best.put(c.name, c);
                bestScore.put(c.name, score);
            }
        }
    }

    /** The chosen class per name. */
    public Map<String, ClassInfo> classes() {
        return best;
    }

    static String root(String internalName) {
        int first = internalName.indexOf('/');
        if (first < 0) return "";
        int second = internalName.indexOf('/', first + 1);
        return second < 0 ? internalName.substring(0, first + 1) : internalName.substring(0, second + 1);
    }
}
