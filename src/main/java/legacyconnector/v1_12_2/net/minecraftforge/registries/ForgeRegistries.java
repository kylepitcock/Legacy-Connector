package legacyconnector.v1_12_2.net.minecraftforge.registries;

import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Registries1122;

/** The registries mods register into from their {@code RegistryEvent.Register} handlers. */
public class ForgeRegistries {
    public static final IForgeRegistry<Block> BLOCKS = Registries1122.BLOCKS;
    public static final IForgeRegistry<Item> ITEMS = Registries1122.ITEMS;
    public static final IForgeRegistry<legacyconnector.v1_12_2.net.minecraft.potion.Potion> POTIONS = Registries1122.POTIONS;
    public static final IForgeRegistry<legacyconnector.v1_12_2.net.minecraft.enchantment.Enchantment> ENCHANTMENTS = Registries1122.ENCHANTMENTS;
    public static final IForgeRegistry<legacyconnector.v1_12_2.net.minecraft.util.SoundEvent> SOUND_EVENTS = Registries1122.SOUND_EVENTS;
}
