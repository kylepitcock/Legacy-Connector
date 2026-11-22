package org.kylepitcock.legacyconnector.loader.transform;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

/**
 * Avoids BiomeManager's jittered, multi-quart lookup for AlienEvo's campground
 * trail feature. During FEATURES generation that lookup can sample beyond the
 * WorldGenRegion's available chunk window. The feature only needs the biome
 * containing its candidate block, so read that exact quart biome instead.
 */
final class AlienEvoWorldGen1201 implements Opcodes {
    static final String FEATURE = "net/habb/alienevo/worldgen/CampgroundTrailFeature";
    static final String WORLD_GEN_LEVEL = "net/minecraft/world/level/WorldGenLevel";
    static final String BLOCK_POS = "net/minecraft/core/BlockPos";
    static final String HOLDER = "net/minecraft/core/Holder";
    static final String HELPER =
            "org/kylepitcock/legacyconnector/v1_20_1/migrate/AlienEvoWorldGenCompat";

    static final String LEGACY_GET_BIOME =
            "(L" + BLOCK_POS + ";)L" + HOLDER + ";";
    static final String COMPAT_GET_BIOME =
            "(L" + WORLD_GEN_LEVEL + ";L" + BLOCK_POS + ";)L" + HOLDER + ";";

    private AlienEvoWorldGen1201() {}

    /** Returns the number of biome lookups adapted. */
    static int apply(ClassNode node) {
        if (!FEATURE.equals(node.name)) return 0;

        int changed = 0;
        int matched = 0;
        boolean alreadyAdapted = false;
        for (var method : node.methods) {
            if (method.instructions == null) continue;
            for (var instruction : method.instructions.toArray()) {
                if (!(instruction instanceof MethodInsnNode call)) continue;

                if (call.getOpcode() == INVOKESTATIC
                        && call.owner.equals(HELPER)
                        && call.name.equals("getBiomeForFeature")
                        && call.desc.equals(COMPAT_GET_BIOME)) {
                    alreadyAdapted = true;
                    matched++;
                    continue;
                }

                if (call.getOpcode() != INVOKEINTERFACE
                        || !call.owner.equals(WORLD_GEN_LEVEL)
                        || !call.name.equals("getBiome")
                        || !call.desc.equals(LEGACY_GET_BIOME)) {
                    continue;
                }

                matched++;
                call.setOpcode(INVOKESTATIC);
                call.owner = HELPER;
                call.name = "getBiomeForFeature";
                call.desc = COMPAT_GET_BIOME;
                call.itf = false;
                changed++;
            }
        }

        if (matched > 1) {
            throw new IllegalStateException("AlienEvo CampgroundTrailFeature contains "
                    + matched + " matching getBiome calls; expected exactly one inspected call");
        }
        if (changed == 0 && !alreadyAdapted) {
            throw new IllegalStateException("AlienEvo CampgroundTrailFeature no longer contains the inspected "
                    + "WorldGenLevel.getBiome(BlockPos) call; review its bytecode before adapting it");
        }
        return changed;
    }
}
