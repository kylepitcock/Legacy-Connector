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
        // the vanilla effects and enchantments become registry entries when their constant classes load
        for (String c : new String[] {"legacyconnector.v1_12_2.net.minecraft.init.MobEffects", "legacyconnector.v1_12_2.net.minecraft.init.Enchantments"}) {
            try {
                Class.forName(c, true, Registries1122R.class.getClassLoader());
            } catch (ClassNotFoundException | LinkageError e) {
                // the constants stay unregistered
            }
        }
        Vanilla1122.registerPotionTypes(Registries1122.POTION_TYPES.getRegistrySuperType());
        Registries1122.VILLAGER_PROFESSIONS.mcc$type("net.minecraftforge.fml.common.registry.VillagerRegistry$VillagerProfession");
        for (Registries1122.Reg reg : new Registries1122.Reg[] {Registries1122.POTION_TYPES, Registries1122.VILLAGER_PROFESSIONS}) {
            if (reg.getRegistrySuperType() == null) continue;
            post.accept(new RegistryEvent.Register(reg));
        }
    }
}
