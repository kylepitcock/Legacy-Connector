package legacyconnector.v1_12_2.net.minecraft.util.datafix.fixes;

import java.util.HashMap;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.util.datafix.IFixableData;

/** 1.11's entity id upgrade: the old names ("PigZombie") become registry names ("minecraft:zombie_pigman"); mods add their own pairs. */
public class EntityId implements IFixableData {
    public static final Map<String, String> OLD_TO_NEW_ID_MAP = new HashMap<>();

    static {
        String[] pairs = {"AreaEffectCloud", "area_effect_cloud", "ArmorStand", "armor_stand", "Arrow", "arrow", "Bat", "bat", "Blaze", "blaze",
                "Boat", "boat", "CaveSpider", "cave_spider", "Chicken", "chicken", "Cow", "cow", "Creeper", "creeper", "Donkey", "donkey",
                "DragonFireball", "dragon_fireball", "ElderGuardian", "elder_guardian", "EnderCrystal", "ender_crystal", "EnderDragon", "ender_dragon",
                "Enderman", "enderman", "Endermite", "endermite", "EvocationFangs", "evocation_fangs", "EvocationIllager", "evocation_illager",
                "EyeOfEnderSignal", "eye_of_ender_signal", "FallingSand", "falling_block", "Fireball", "fireball", "FireworksRocketEntity",
                "fireworks_rocket", "Ghast", "ghast", "Giant", "giant", "Guardian", "guardian", "Horse", "horse", "Husk", "husk", "Item", "item",
                "ItemFrame", "item_frame", "LavaSlime", "magma_cube", "LeashKnot", "leash_knot", "Llama", "llama", "LlamaSpit", "llama_spit",
                "MinecartChest", "chest_minecart", "MinecartCommandBlock", "commandblock_minecart", "MinecartFurnace", "furnace_minecart",
                "MinecartHopper", "hopper_minecart", "MinecartRideable", "minecart", "MinecartSpawner", "spawner_minecart", "MinecartTNT",
                "tnt_minecart", "Mule", "mule", "MushroomCow", "mooshroom", "Ozelot", "ocelot", "Painting", "painting", "Pig", "pig", "PigZombie",
                "zombie_pigman", "PolarBear", "polar_bear", "PrimedTnt", "tnt", "Rabbit", "rabbit", "Sheep", "sheep", "Shulker", "shulker",
                "ShulkerBullet", "shulker_bullet", "Silverfish", "silverfish", "Skeleton", "skeleton", "SkeletonHorse", "skeleton_horse", "Slime",
                "slime", "SmallFireball", "small_fireball", "SnowMan", "snowman", "Snowball", "snowball", "SpectralArrow", "spectral_arrow",
                "Spider", "spider", "Squid", "squid", "Stray", "stray", "ThrownEgg", "egg", "ThrownEnderpearl", "ender_pearl", "ThrownExpBottle",
                "xp_bottle", "ThrownPotion", "potion", "Vex", "vex", "Villager", "villager", "VillagerGolem", "villager_golem", "VindicationIllager",
                "vindication_illager", "Witch", "witch", "WitherBoss", "wither", "WitherSkeleton", "wither_skeleton", "WitherSkull", "wither_skull",
                "Wolf", "wolf", "XPOrb", "xp_orb", "Zombie", "zombie", "ZombieHorse", "zombie_horse", "ZombieVillager", "zombie_villager"};
        for (int i = 0; i < pairs.length; i += 2) OLD_TO_NEW_ID_MAP.put(pairs[i], "minecraft:" + pairs[i + 1]);
    }

    @Override
    public int getFixVersion() {
        return 704;
    }

    @Override
    public NBTTagCompound fixTagCompound(NBTTagCompound compound) {
        String id = OLD_TO_NEW_ID_MAP.get(compound.getString("id"));
        if (id != null) compound.setString("id", id);
        return compound;
    }
}
