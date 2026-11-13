package legacyconnector.v1_20_1.net.minecraftforge.registries;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Forge 1.20.1's {@code ForgeRegistries}: handles on the game's registries. NeoForge 1.21.1 removed the class; each field is a view
 * ({@link ForgeRegistryWrapper}) of the vanilla registry (or the NeoForge one for Forge-only registries).
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public final class ForgeRegistries {
    private ForgeRegistries() {
    }

    public static final IForgeRegistry<Block> BLOCKS = new ForgeRegistryWrapper<>(BuiltInRegistries.BLOCK);
    public static final IForgeRegistry<Fluid> FLUIDS = new ForgeRegistryWrapper<>(BuiltInRegistries.FLUID);
    public static final IForgeRegistry<Item> ITEMS = new ForgeRegistryWrapper<>(BuiltInRegistries.ITEM);
    public static final IForgeRegistry<MobEffect> MOB_EFFECTS = new ForgeRegistryWrapper(BuiltInRegistries.MOB_EFFECT);
    public static final IForgeRegistry<SoundEvent> SOUND_EVENTS = new ForgeRegistryWrapper<>(BuiltInRegistries.SOUND_EVENT);
    public static final IForgeRegistry<Potion> POTIONS = new ForgeRegistryWrapper<>(BuiltInRegistries.POTION);
    public static final IForgeRegistry<org.kylepitcock.legacyconnector.v1_20_1.migrate.LegacyEnchantment> ENCHANTMENTS = new ForgeRegistryWrapper<>(org.kylepitcock.legacyconnector.v1_20_1.migrate.LegacyEnchantments.KEY);
    public static final IForgeRegistry<EntityType<?>> ENTITY_TYPES = new ForgeRegistryWrapper(BuiltInRegistries.ENTITY_TYPE);
    public static final IForgeRegistry<BlockEntityType<?>> BLOCK_ENTITY_TYPES = new ForgeRegistryWrapper(BuiltInRegistries.BLOCK_ENTITY_TYPE);
    public static final IForgeRegistry<net.minecraft.core.particles.ParticleType<?>> PARTICLE_TYPES = new ForgeRegistryWrapper(BuiltInRegistries.PARTICLE_TYPE);
    public static final IForgeRegistry<MenuType<?>> MENU_TYPES = new ForgeRegistryWrapper(BuiltInRegistries.MENU);
    public static final IForgeRegistry<net.minecraft.world.entity.decoration.PaintingVariant> PAINTING_VARIANTS = new ForgeRegistryWrapper<>(Registries.PAINTING_VARIANT);
    public static final IForgeRegistry<RecipeType<?>> RECIPE_TYPES = new ForgeRegistryWrapper(BuiltInRegistries.RECIPE_TYPE);
    public static final IForgeRegistry<RecipeSerializer<?>> RECIPE_SERIALIZERS = new ForgeRegistryWrapper(BuiltInRegistries.RECIPE_SERIALIZER);
    public static final IForgeRegistry<Attribute> ATTRIBUTES = new ForgeRegistryWrapper(BuiltInRegistries.ATTRIBUTE);
    public static final IForgeRegistry<net.minecraft.stats.StatType<?>> STAT_TYPES = new ForgeRegistryWrapper(BuiltInRegistries.STAT_TYPE);
    public static final IForgeRegistry<net.minecraft.commands.synchronization.ArgumentTypeInfo<?, ?>> COMMAND_ARGUMENT_TYPES = new ForgeRegistryWrapper(BuiltInRegistries.COMMAND_ARGUMENT_TYPE);
    public static final IForgeRegistry<VillagerProfession> VILLAGER_PROFESSIONS = new ForgeRegistryWrapper<>(BuiltInRegistries.VILLAGER_PROFESSION);
    public static final IForgeRegistry<PoiType> POI_TYPES = new ForgeRegistryWrapper<>(BuiltInRegistries.POINT_OF_INTEREST_TYPE);
    public static final IForgeRegistry<net.minecraft.world.entity.ai.memory.MemoryModuleType<?>> MEMORY_MODULE_TYPES = new ForgeRegistryWrapper(BuiltInRegistries.MEMORY_MODULE_TYPE);
    public static final IForgeRegistry<net.minecraft.world.entity.ai.sensing.SensorType<?>> SENSOR_TYPES = new ForgeRegistryWrapper(BuiltInRegistries.SENSOR_TYPE);
    public static final IForgeRegistry<net.minecraft.world.entity.schedule.Schedule> SCHEDULES = new ForgeRegistryWrapper<>(BuiltInRegistries.SCHEDULE);
    public static final IForgeRegistry<net.minecraft.world.entity.schedule.Activity> ACTIVITIES = new ForgeRegistryWrapper<>(BuiltInRegistries.ACTIVITY);
    public static final IForgeRegistry<net.minecraft.world.level.levelgen.carver.WorldCarver<?>> WORLD_CARVERS = new ForgeRegistryWrapper(BuiltInRegistries.CARVER);
    public static final IForgeRegistry<net.minecraft.world.level.levelgen.feature.Feature<?>> FEATURES = new ForgeRegistryWrapper(BuiltInRegistries.FEATURE);
    public static final IForgeRegistry<net.minecraft.world.level.chunk.status.ChunkStatus> CHUNK_STATUS = new ForgeRegistryWrapper<>(BuiltInRegistries.CHUNK_STATUS);
    public static final IForgeRegistry<net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProviderType<?>> BLOCK_STATE_PROVIDER_TYPES = new ForgeRegistryWrapper(BuiltInRegistries.BLOCKSTATE_PROVIDER_TYPE);
    public static final IForgeRegistry<net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacerType<?>> FOLIAGE_PLACER_TYPES = new ForgeRegistryWrapper(BuiltInRegistries.FOLIAGE_PLACER_TYPE);
    public static final IForgeRegistry<net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType<?>> TREE_DECORATOR_TYPES = new ForgeRegistryWrapper(BuiltInRegistries.TREE_DECORATOR_TYPE);
    public static final IForgeRegistry<Biome> BIOMES = new ForgeRegistryWrapper<>(Registries.BIOME);
    /** A supplier in Forge 1.20.1 (the registry is created late). */
    public static final java.util.function.Supplier<IForgeRegistry<net.neoforged.neoforge.fluids.FluidType>> FLUID_TYPES = () -> new ForgeRegistryWrapper<>(NeoForgeRegistries.FLUID_TYPES);
    public static final IForgeRegistry<net.minecraft.network.syncher.EntityDataSerializer<?>> ENTITY_DATA_SERIALIZERS = new ForgeRegistryWrapper(NeoForgeRegistries.ENTITY_DATA_SERIALIZERS);
    public static final IForgeRegistry<com.mojang.serialization.MapCodec<? extends net.neoforged.neoforge.common.loot.IGlobalLootModifier>> GLOBAL_LOOT_MODIFIER_SERIALIZERS = new ForgeRegistryWrapper(NeoForgeRegistries.GLOBAL_LOOT_MODIFIER_SERIALIZERS);
    public static final IForgeRegistry<net.neoforged.neoforge.common.world.BiomeModifier> BIOME_MODIFIERS = new ForgeRegistryWrapper(NeoForgeRegistries.Keys.BIOME_MODIFIERS);
    public static final IForgeRegistry<net.neoforged.neoforge.registries.holdersets.HolderSetType> HOLDER_SET_TYPES = new ForgeRegistryWrapper<>(NeoForgeRegistries.HOLDER_SET_TYPES);

    /** The registry keys ({@code ForgeRegistries.Keys.BLOCKS}...). */
    public static final class Keys {
        private Keys() {
        }

        public static final net.minecraft.resources.ResourceKey<net.minecraft.core.Registry<Block>> BLOCKS = Registries.BLOCK;
        public static final net.minecraft.resources.ResourceKey<net.minecraft.core.Registry<Fluid>> FLUIDS = Registries.FLUID;
        public static final net.minecraft.resources.ResourceKey<net.minecraft.core.Registry<Item>> ITEMS = Registries.ITEM;
        public static final net.minecraft.resources.ResourceKey<net.minecraft.core.Registry<MobEffect>> MOB_EFFECTS = Registries.MOB_EFFECT;
        public static final net.minecraft.resources.ResourceKey<net.minecraft.core.Registry<SoundEvent>> SOUND_EVENTS = Registries.SOUND_EVENT;
        public static final net.minecraft.resources.ResourceKey<net.minecraft.core.Registry<Potion>> POTIONS = Registries.POTION;
        public static final net.minecraft.resources.ResourceKey<net.minecraft.core.Registry<org.kylepitcock.legacyconnector.v1_20_1.migrate.LegacyEnchantment>> ENCHANTMENTS = org.kylepitcock.legacyconnector.v1_20_1.migrate.LegacyEnchantments.KEY;
        public static final net.minecraft.resources.ResourceKey<net.minecraft.core.Registry<EntityType<?>>> ENTITY_TYPES = Registries.ENTITY_TYPE;
        public static final net.minecraft.resources.ResourceKey<net.minecraft.core.Registry<BlockEntityType<?>>> BLOCK_ENTITY_TYPES = Registries.BLOCK_ENTITY_TYPE;
        public static final net.minecraft.resources.ResourceKey<net.minecraft.core.Registry<net.minecraft.core.particles.ParticleType<?>>> PARTICLE_TYPES = Registries.PARTICLE_TYPE;
        public static final net.minecraft.resources.ResourceKey<net.minecraft.core.Registry<MenuType<?>>> MENU_TYPES = Registries.MENU;
        public static final net.minecraft.resources.ResourceKey<net.minecraft.core.Registry<RecipeType<?>>> RECIPE_TYPES = Registries.RECIPE_TYPE;
        public static final net.minecraft.resources.ResourceKey<net.minecraft.core.Registry<RecipeSerializer<?>>> RECIPE_SERIALIZERS = Registries.RECIPE_SERIALIZER;
        public static final net.minecraft.resources.ResourceKey<net.minecraft.core.Registry<Attribute>> ATTRIBUTES = Registries.ATTRIBUTE;
        public static final net.minecraft.resources.ResourceKey<net.minecraft.core.Registry<VillagerProfession>> VILLAGER_PROFESSIONS = Registries.VILLAGER_PROFESSION;
        public static final net.minecraft.resources.ResourceKey<net.minecraft.core.Registry<PoiType>> POI_TYPES = Registries.POINT_OF_INTEREST_TYPE;
        public static final net.minecraft.resources.ResourceKey<net.minecraft.core.Registry<Biome>> BIOMES = Registries.BIOME;
        public static final net.minecraft.resources.ResourceKey<net.minecraft.core.Registry<net.minecraft.commands.synchronization.ArgumentTypeInfo<?, ?>>> COMMAND_ARGUMENT_TYPES = Registries.COMMAND_ARGUMENT_TYPE;
        public static final net.minecraft.resources.ResourceKey<net.minecraft.core.Registry<net.minecraft.world.level.levelgen.feature.Feature<?>>> FEATURES = Registries.FEATURE;
        public static final net.minecraft.resources.ResourceKey<net.minecraft.core.Registry<net.neoforged.neoforge.fluids.FluidType>> FLUID_TYPES = NeoForgeRegistries.Keys.FLUID_TYPES;
        public static final net.minecraft.resources.ResourceKey<net.minecraft.core.Registry<net.minecraft.network.syncher.EntityDataSerializer<?>>> ENTITY_DATA_SERIALIZERS = NeoForgeRegistries.Keys.ENTITY_DATA_SERIALIZERS;
        public static final net.minecraft.resources.ResourceKey<net.minecraft.core.Registry<com.mojang.serialization.MapCodec<? extends net.neoforged.neoforge.common.loot.IGlobalLootModifier>>> GLOBAL_LOOT_MODIFIER_SERIALIZERS = NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS;
        public static final net.minecraft.resources.ResourceKey<net.minecraft.core.Registry<net.neoforged.neoforge.common.world.BiomeModifier>> BIOME_MODIFIERS = NeoForgeRegistries.Keys.BIOME_MODIFIERS;
        public static final net.minecraft.resources.ResourceKey<net.minecraft.core.Registry<com.mojang.serialization.MapCodec<? extends net.neoforged.neoforge.common.world.StructureModifier>>> STRUCTURE_MODIFIER_SERIALIZERS = NeoForgeRegistries.Keys.STRUCTURE_MODIFIER_SERIALIZERS;
        public static final net.minecraft.resources.ResourceKey<net.minecraft.core.Registry<net.neoforged.neoforge.common.world.StructureModifier>> STRUCTURE_MODIFIERS = NeoForgeRegistries.Keys.STRUCTURE_MODIFIERS;
        public static final net.minecraft.resources.ResourceKey<net.minecraft.core.Registry<com.mojang.serialization.MapCodec<? extends net.neoforged.neoforge.common.world.BiomeModifier>>> BIOME_MODIFIER_SERIALIZERS = NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS;
    }
}
