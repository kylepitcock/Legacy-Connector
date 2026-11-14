package org.kylepitcock.legacyconnector.api.internal;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.kylepitcock.legacyconnector.api.LegacyApi;
import org.kylepitcock.legacyconnector.api.LegacyMod;
import org.kylepitcock.legacyconnector.api.LegacyVersion;

/** {@code LegacyApi.versionOf}: which legacy Minecraft version something belongs to. */
public final class VersionLookup {
    private VersionLookup() {}

    public static Optional<LegacyVersion> ofNamespace(String namespace) {
        return namespace == null ? Optional.empty() : InPlaceMods.versionOfNamespace(namespace);
    }

    public static Optional<LegacyVersion> of(Object o) {
        if (o == null) return Optional.empty();
        if (o instanceof LegacyMod m) return Optional.of(m.minecraftVersion());
        if (o instanceof ResourceLocation id) return ofNamespace(id.getNamespace());
        if (o instanceof BlockState s) return Convert.versionOf(s);
        if (o instanceof BlockEntity be) return Convert.tileVersion(be);
        if (o instanceof ItemStack s) return s.isEmpty() ? Optional.empty() : ofNamespace(BuiltInRegistries.ITEM.getKey(s.getItem()).getNamespace());
        if (o instanceof net.minecraft.world.item.Item i) return ofNamespace(BuiltInRegistries.ITEM.getKey(i).getNamespace());
        if (o instanceof net.minecraft.world.level.block.Block b) return ofNamespace(BuiltInRegistries.BLOCK.getKey(b).getNamespace());
        return ofClass(o instanceof Class<?> c ? c : o.getClass());
    }

    /** By the mod class loader the class came from (exact, tells 1.4.7 to 1.6.4 from 1.7.10), else by the shim package of the legacy API it is part of. */
    private static Optional<LegacyVersion> ofClass(Class<?> cls) {
        ClassLoader loader = cls.getClassLoader();
        if (loader != null && loader != VersionLookup.class.getClassLoader()) {
            Set<LegacyVersion> found = new HashSet<>();
            for (LegacyMod m : LegacyApi.mods()) {
                try {
                    if (m.classLoader() == loader) found.add(m.minecraftVersion());
                } catch (RuntimeException e) {
                    // a mod without a class loader yet
                }
            }
            if (found.size() == 1) return Optional.of(found.iterator().next());
        }
        String n = cls.getName();
        if (n.startsWith("legacyconnector.v1_7_10.")) return Optional.of(LegacyVersion.V1_7_10);
        if (n.startsWith("legacyconnector.v1_6_4.")) return Optional.of(LegacyVersion.V1_6_4);
        if (n.startsWith("legacyconnector.v1_12_2.")) return Optional.of(LegacyVersion.V1_12_2);
        if (n.startsWith("legacyconnector.vb1_7_3.")) return Optional.of(LegacyVersion.VB1_7_3);
        if (n.startsWith("legacyconnector.v1_20_1.")) return Optional.of(LegacyVersion.V1_20_1);
        return Optional.empty();
    }
}
