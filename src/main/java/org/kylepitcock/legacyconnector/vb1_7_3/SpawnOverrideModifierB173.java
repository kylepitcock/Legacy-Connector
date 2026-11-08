package org.kylepitcock.legacyconnector.vb1_7_3;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** ModLoader replacements/removals run together after native add/remove phases, so replacements survive. */
public record SpawnOverrideModifierB173(HolderSet<Biome> biomes,EntityType<?> entity,MobCategory category,int weight) implements BiomeModifier {
    public static final MapCodec<SpawnOverrideModifierB173> CODEC=RecordCodecBuilder.mapCodec(instance->instance.group(
            Biome.LIST_CODEC.fieldOf("biomes").forGetter(SpawnOverrideModifierB173::biomes),
            BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("entity").forGetter(SpawnOverrideModifierB173::entity),
            MobCategory.CODEC.fieldOf("category").forGetter(SpawnOverrideModifierB173::category),
            Codec.intRange(0,Integer.MAX_VALUE).fieldOf("weight").forGetter(SpawnOverrideModifierB173::weight)
    ).apply(instance,SpawnOverrideModifierB173::new));
    public static final DeferredRegister<MapCodec<? extends BiomeModifier>> REGISTER=DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS,"legacyconnector");
    static {REGISTER.register("beta_spawn_override",()->CODEC);}
    @Override public MapCodec<? extends BiomeModifier> codec(){return CODEC;}
    @Override public void modify(Holder<Biome> biome,Phase phase,ModifiableBiomeInfo.BiomeInfo.Builder builder){
        if(phase!=Phase.MODIFY||!biomes.contains(biome))return;
        var spawns=builder.getMobSpawnSettings().getSpawner(category);
        spawns.removeIf(entry->entry.type==entity);
        if(weight>0)spawns.add(new MobSpawnSettings.SpawnerData(entity,weight,1,4));
    }
}
