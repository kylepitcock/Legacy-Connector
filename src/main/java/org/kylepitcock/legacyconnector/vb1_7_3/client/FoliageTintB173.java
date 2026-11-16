package org.kylepitcock.legacyconnector.vb1_7_3.client;

/** Client biome foliage colour through the native cached resolver (a render region only caches the vanilla resolvers). */
public final class FoliageTintB173 {
    private FoliageTintB173(){}
    public static int color(net.minecraft.world.level.BlockAndTintGetter tint,net.minecraft.core.BlockPos pos){
        return net.minecraft.client.renderer.BiomeColors.getAverageFoliageColor(tint,pos);
    }
}
