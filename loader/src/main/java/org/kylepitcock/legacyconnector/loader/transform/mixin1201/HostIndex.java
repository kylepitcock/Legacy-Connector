package org.kylepitcock.legacyconnector.loader.transform.mixin1201;

import java.util.List;

/**
 * What the mixin repair knows about the Minecraft 1.21.1 / NeoForge classes (the member index the gap report uses). An implementation answers only
 * from facts: an unknown class or a chain that leaves the index yields {@code null} or {@code chainComplete == false}, and nothing is claimed then.
 */
public interface HostIndex {
    /**
     * Methods called {@code name} as seen from {@code owner}.
     *
     * @param declared       descriptors of the methods {@code owner} itself declares (Mixin only injects into these)
     * @param chain          descriptors of every method of that name on {@code owner} or any class above it (includes {@code declared})
     * @param chainComplete  every class on the way up was in the index, so an empty {@code chain} is a proof of absence
     */
    record Lookup(List<String> declared, List<String> chain, boolean chainComplete) {
        /** The name is proven not to exist anywhere in 1.21.1. */
        public boolean gone() {
            return chain.isEmpty() && chainComplete;
        }
    }

    /** True when {@code owner} is a class of the index (Minecraft or NeoForge), false when it is not (a mod, a library, or removed in 1.21.1). */
    boolean hasClass(String owner);

    /** Methods of this name, or null when {@code owner} is not a class of the index. */
    Lookup methods(String owner, String name);

    /** True when {@code owner} declares {@code name+desc} as a static method, false when as an instance method, null when not known. */
    Boolean isStatic(String owner, String name, String desc);

    /** True/false when the field's presence on {@code owner} or above is known, null when not. */
    Boolean hasField(String owner, String name);
}
