package org.kylepitcock.legacyconnector.v1_12_2;

import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.server.packs.PackType;
import org.kylepitcock.legacyconnector.core.pack.GeneratedPack;

/** Generated data for 1.12.2 content: enderman-carriable blocks, spawns (recipes and ore dictionary tags: the recipe pack, {@link org.kylepitcock.legacyconnector.v1_12_2.bridge.RecipeBridge1122}). */
public final class Data1122 implements GeneratedPack.Contributor {
    @Override
    public void contribute(PackType type, Map<String, Supplier<byte[]>> out) {
        if (type != PackType.SERVER_DATA) return;
        org.kylepitcock.legacyconnector.v1_12_2.bridge.RuntimeApis1122.generate(out);
        org.kylepitcock.legacyconnector.v1_12_2.bridge.Spawns1122.contribute(out);
        org.kylepitcock.legacyconnector.v1_12_2.dimension.Dimensions1122.contribute(out);
        if (net.neoforged.fml.ModList.get().isLoaded("curios")) org.kylepitcock.legacyconnector.compat.curios.CuriosBaubles1122.data(out);
    }
}
