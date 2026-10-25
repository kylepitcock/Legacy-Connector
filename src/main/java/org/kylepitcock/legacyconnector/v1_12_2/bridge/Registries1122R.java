package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import java.util.function.Consumer;
import legacyconnector.v1_12_2.net.minecraftforge.event.RegistryEvent;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/** Posts the registry events of the registries typed at runtime. */
public final class Registries1122R {
    private Registries1122R() {}

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void postSynthesized(Consumer<Event> post) {
        Registries1122.POTION_TYPES.mcc$type("net.minecraft.potion.PotionType");
        Registries1122.BIOMES.mcc$type("net.minecraft.world.biome.Biome");
        Registries1122.VILLAGER_PROFESSIONS.mcc$type("net.minecraftforge.fml.common.registry.VillagerRegistry$VillagerProfession");
        Registries1122.ENTITIES.mcc$type("net.minecraftforge.fml.common.registry.EntityEntry");
        for (Registries1122.Reg reg : new Registries1122.Reg[] {Registries1122.POTION_TYPES, Registries1122.BIOMES, Registries1122.VILLAGER_PROFESSIONS, Registries1122.ENTITIES}) {
            if (reg.getRegistrySuperType() == null) continue;
            post.accept(new RegistryEvent.Register(reg));
        }
    }
}
