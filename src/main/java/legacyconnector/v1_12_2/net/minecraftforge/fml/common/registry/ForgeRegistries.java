package legacyconnector.v1_12_2.net.minecraftforge.fml.common.registry;

import legacyconnector.v1_12_2.net.minecraftforge.registries.IForgeRegistry;

import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122;

/** The registries mods register into from their {@code RegistryEvent.Register} handlers. */
public class ForgeRegistries {
    public static final IForgeRegistry<Block> BLOCKS = Registries1122.BLOCKS;
    public static final IForgeRegistry<Item> ITEMS = Registries1122.ITEMS;
    public static final IForgeRegistry BIOMES = Registries1122.BIOMES;
    public static final IForgeRegistry ENTITIES = Registries1122.ENTITIES;
    public static final IForgeRegistry VILLAGER_PROFESSIONS = Registries1122.VILLAGER_PROFESSIONS;
    public static final IForgeRegistry POTION_TYPES = Registries1122.POTION_TYPES;
    public static final IForgeRegistry<legacyconnector.v1_12_2.net.minecraft.item.crafting.IRecipe> RECIPES = Registries1122.RECIPES;
    public static final IForgeRegistry<legacyconnector.v1_12_2.net.minecraft.potion.Potion> POTIONS = Registries1122.POTIONS;
    public static final IForgeRegistry<legacyconnector.v1_12_2.net.minecraft.enchantment.Enchantment> ENCHANTMENTS = Registries1122.ENCHANTMENTS;
    public static final IForgeRegistry<legacyconnector.v1_12_2.net.minecraft.util.SoundEvent> SOUND_EVENTS = Registries1122.SOUND_EVENTS;
}
