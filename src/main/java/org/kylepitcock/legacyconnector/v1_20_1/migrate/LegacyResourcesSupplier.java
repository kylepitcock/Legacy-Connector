package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.server.packs.PackResources;

/**
 * Stand-in for 1.20.1's {@code Pack.ResourcesSupplier}, a single-method interface a mod could write as a lambda. 1.21.1's has two abstract methods
 * ({@code openPrimary}, {@code openFull}) and cannot be a lambda target; {@code mc-classes.tsv} renames the interface to this one and
 * {@link RoundCompat#readMetaAndCreate} adapts it.
 */
@FunctionalInterface
public interface LegacyResourcesSupplier {
    PackResources open(String id);
}
