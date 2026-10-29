package org.kylepitcock.legacyconnector.compat.mods;

import java.util.Set;

/**
 * MOD-SPECIFIC (OreSpawn, mod id {@code OreSpawn}): its bosses do not implement {@code IBossDisplayData}; OreSpawn draws their
 * health only while the player aims at them from 16 blocks or less (GirlfriendOverlayGui). They get the modern boss bar that
 * legacy bosses get ({@code LegacyMobHost.updateBossBar}). Remove this class and its one call to drop the fix.
 */
public final class OreSpawnBosses {
    private static final Set<String> BOSSES = Set.of(
            "danger.orespawn.TheKing", "danger.orespawn.TheQueen", "danger.orespawn.Godzilla", "danger.orespawn.Kraken",
            "danger.orespawn.ThePrinceAdult");

    private OreSpawnBosses() {}

    public static boolean isBoss(Object legacy) {
        return legacy != null && BOSSES.contains(legacy.getClass().getName());
    }
}
