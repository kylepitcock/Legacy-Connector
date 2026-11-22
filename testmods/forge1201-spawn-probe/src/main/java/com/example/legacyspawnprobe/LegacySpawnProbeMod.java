package com.example.legacyspawnprobe;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Minimal Forge 1.20.1 mod entry point. The probe is invoked by the
 * development-only Legacy Connector server-script runner.
 */
@Mod(LegacySpawnProbeMod.MOD_ID)
public final class LegacySpawnProbeMod {
    public static final String MOD_ID = "legacyspawnprobe";

    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MOD_ID);

    public static final RegistryObject<EntityType<SpawnProbeMob>> SPAWN_PROBE =
            ENTITY_TYPES.register("spawn_probe", () -> EntityType.Builder
                    .<SpawnProbeMob>of(SpawnProbeMob::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F)
                    .build(MOD_ID + ":spawn_probe"));

    public LegacySpawnProbeMod() {
        var modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ENTITY_TYPES.register(modEventBus);
        modEventBus.addListener(this::createEntityAttributes);
    }

    private void createEntityAttributes(EntityAttributeCreationEvent event) {
        AttributeSupplier attributes = Zombie.createAttributes().build();
        event.put(SPAWN_PROBE.get(), attributes);
    }
}