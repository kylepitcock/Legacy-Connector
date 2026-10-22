package legacyconnector.v1_7_10.net.minecraft.world.gen.feature;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;

/** Places a vanilla configured feature for a legacy vanilla generator (trees, grass patches). */
final class ModernFeature {
    private ModernFeature() {}

    static boolean place(World world, String id, Random random, int x, int y, int z) {
        if (!(world.mcc$level() instanceof WorldGenLevel level)) return false;
        var server = level.getLevel();
        var feature = server.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).get(ResourceLocation.withDefaultNamespace(id));
        if (feature == null) return false;
        return feature.place(level, server.getChunkSource().getGenerator(), RandomSource.create(random.nextLong()), new BlockPos(x, y, z));
    }
}
