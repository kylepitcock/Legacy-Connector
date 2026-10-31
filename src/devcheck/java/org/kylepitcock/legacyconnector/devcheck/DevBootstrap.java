package org.kylepitcock.legacyconnector.devcheck;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Map;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/**
 * Boots vanilla registries outside the game (no FML launch, no mods) so shim code that needs only vanilla content - the
 * 1.7.10 vanilla Block/Item wrappers, ItemBridge/BlockBridge translation - can run in an offline check.
 */
final class DevBootstrap {
    private DevBootstrap() {}

    @SuppressWarnings("unchecked")
    static void boot() throws ReflectiveOperationException {
        var dist = net.neoforged.fml.loading.FMLLoader.class.getDeclaredField("dist");
        dist.setAccessible(true);
        dist.set(null, net.neoforged.api.distmarker.Dist.DEDICATED_SERVER);
        net.neoforged.fml.loading.LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        // Our own data component (normally bound by the mod's DeferredRegister).
        var reg = (MappedRegistry<DataComponentType<?>>) BuiltInRegistries.DATA_COMPONENT_TYPE;
        reg.unfreeze();
        Registry.register(reg, ResourceLocation.fromNamespaceAndPath("legacyconnector", "legacy_meta"),
                DataComponentType.<Integer>builder().persistent(Codec.INT).build());
        reg.freeze();
    }
}
