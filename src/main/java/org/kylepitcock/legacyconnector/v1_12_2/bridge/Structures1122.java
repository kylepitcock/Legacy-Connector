package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import java.util.List;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import net.minecraft.resources.ResourceLocation;

/**
 * 1.12.2 structure names ("Village", "Stronghold", "Mineshaft", "Temple", "Fortress", "Monument", "Mansion", "EndCity") answered
 * from the modern structures they became: the nearest one (as {@code /locate}) and whether a position lies inside one. Only a
 * live server level answers (never from a generation thread).
 */
public final class Structures1122 {
    private static final Map<String, List<String>> NAMES = Map.of(
            "Village", List.of("village_plains", "village_desert", "village_savanna", "village_snowy", "village_taiga"),
            "Stronghold", List.of("stronghold"),
            "Mineshaft", List.of("mineshaft", "mineshaft_mesa"),
            "Temple", List.of("desert_pyramid", "jungle_pyramid", "swamp_hut", "igloo"),
            "Fortress", List.of("fortress"),
            "Monument", List.of("monument"),
            "Mansion", List.of("mansion"),
            "EndCity", List.of("end_city"));

    private Structures1122() {}

    private static net.minecraft.server.level.ServerLevel level(World world) {
        return world != null && world.mcc$liveLevel() instanceof net.minecraft.server.level.ServerLevel s && s.getServer().isSameThread() ? s : null;
    }

    private static net.minecraft.core.HolderSet<net.minecraft.world.level.levelgen.structure.Structure> holders(net.minecraft.server.level.ServerLevel level,
            String name) {
        List<String> ids = NAMES.get(name);
        if (ids == null) return null;
        var registry = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
        List<net.minecraft.core.Holder<net.minecraft.world.level.levelgen.structure.Structure>> out = new java.util.ArrayList<>();
        for (String id : ids) {
            registry.getHolder(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.STRUCTURE, ResourceLocation.withDefaultNamespace(id)))
                    .ifPresent(out::add);
        }
        return out.isEmpty() ? null : net.minecraft.core.HolderSet.direct(out);
    }

    /** The nearest structure of a 1.12 name, or null (unknown name, none within 100 chunks, not a live server level). */
    public static BlockPos nearest(World world, String name, BlockPos pos, boolean findUnexplored) {
        var level = level(world);
        if (level == null || pos == null) return null;
        var set = holders(level, name);
        if (set == null) return null;
        try {
            var found = level.getChunkSource().getGenerator().findNearestMapStructure(level, set, pos.mcc$modern(), 100, findUnexplored);
            return found == null ? null : BlockPos.mcc$of(found.getFirst());
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Whether a position lies in a piece of a structure of a 1.12 name. */
    public static boolean inside(World world, String name, BlockPos pos) {
        var level = level(world);
        if (level == null || pos == null) return false;
        var set = holders(level, name);
        if (set == null) return false;
        var p = pos.mcc$modern();
        if (!level.hasChunkAt(p)) return false;
        for (var holder : set) {
            if (level.structureManager().getStructureWithPieceAt(p, holder.value()).isValid()) return true;
        }
        return false;
    }
}
