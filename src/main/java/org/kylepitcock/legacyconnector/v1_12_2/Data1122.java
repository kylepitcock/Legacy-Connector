package org.kylepitcock.legacyconnector.v1_12_2;

import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.server.packs.PackType;
import org.kylepitcock.legacyconnector.core.pack.GeneratedPack;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.RecipeBridge1122;

/** Generated data for 1.12.2 content: code-registered recipes and the ore dictionary as tags. */
public final class Data1122 implements GeneratedPack.Contributor {
    @Override
    public void contribute(PackType type, Map<String, Supplier<byte[]>> out) {
        if (type != PackType.SERVER_DATA) return;
        RecipeBridge1122.generate(out);
    }
}
