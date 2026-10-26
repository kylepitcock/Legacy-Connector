package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityAgeable;
import legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityAnimal;
import legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityCow;
import legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityMooshroom;
import legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityPig;
import legacyconnector.v1_7_10.net.minecraft.entity.passive.EntitySheep;
import legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityChicken;
import legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityWolf;
import legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityHorse;
import legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityBat;
import legacyconnector.v1_7_10.net.minecraft.entity.monster.EntityBlaze;
import legacyconnector.v1_7_10.net.minecraft.entity.monster.EntityCaveSpider;
import legacyconnector.v1_7_10.net.minecraft.entity.monster.EntityCreeper;
import legacyconnector.v1_7_10.net.minecraft.entity.monster.EntityEnderman;
import legacyconnector.v1_7_10.net.minecraft.entity.monster.EntityMagmaCube;
import legacyconnector.v1_7_10.net.minecraft.entity.monster.EntityMob;
import legacyconnector.v1_7_10.net.minecraft.entity.monster.EntityPigZombie;
import legacyconnector.v1_7_10.net.minecraft.entity.monster.EntitySkeleton;
import legacyconnector.v1_7_10.net.minecraft.entity.monster.EntitySlime;
import legacyconnector.v1_7_10.net.minecraft.entity.monster.EntitySpider;
import legacyconnector.v1_7_10.net.minecraft.entity.monster.EntityZombie;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombifiedPiglin;

/**
 * Legacy views of modern mobs typed as their 1.7.10 classes: mods test and cast vanilla mobs ({@code instanceof EntityAnimal}
 * to breed them, {@code EntityCreeper} for its state, {@code getEntitiesWithinAABB(EntityMob.class)} to find monsters), which a
 * generic living view never matched. Mobs 1.7.10 had no class for (and modern mods' mobs) get the closest legacy base:
 * any animal is an EntityAnimal, any monster an EntityMob.
 */
final class VanillaMobViews {
    private VanillaMobViews() {}

    static Entity create(LivingEntity modern) {
        // most specific first (subclasses before their modern parents)
        if (modern instanceof MushroomCow) return new EntityMooshroom(modern);
        if (modern instanceof Cow) return new EntityCow(modern);
        if (modern instanceof Pig) return new EntityPig(modern);
        if (modern instanceof Sheep) return new EntitySheep(modern);
        if (modern instanceof Chicken) return new EntityChicken(modern);
        if (modern instanceof Wolf) return new EntityWolf(modern);
        if (modern instanceof AbstractHorse) return new EntityHorse(modern);
        if (modern instanceof Bat) return new EntityBat(modern);
        if (modern instanceof ZombifiedPiglin) return new EntityPigZombie(modern);
        if (modern instanceof Zombie) return new EntityZombie(modern);
        if (modern instanceof AbstractSkeleton) return new EntitySkeleton(modern);
        if (modern instanceof Creeper) return new EntityCreeper(modern);
        if (modern instanceof CaveSpider) return new EntityCaveSpider(modern);
        if (modern instanceof Spider) return new EntitySpider(modern);
        if (modern instanceof EnderMan) return new EntityEnderman(modern);
        if (modern instanceof MagmaCube) return new EntityMagmaCube(modern);
        if (modern instanceof Slime) return new EntitySlime(modern);
        if (modern instanceof Blaze) return new EntityBlaze(modern);
        if (modern instanceof net.minecraft.world.entity.monster.Ghast) return new legacyconnector.v1_7_10.net.minecraft.entity.monster.EntityGhast(modern);
        if (modern instanceof net.minecraft.world.entity.monster.Silverfish) return new legacyconnector.v1_7_10.net.minecraft.entity.monster.EntitySilverfish(modern);
        if (modern instanceof net.minecraft.world.entity.monster.Giant) return new legacyconnector.v1_7_10.net.minecraft.entity.monster.EntityGiantZombie(modern);
        if (modern instanceof net.minecraft.world.entity.monster.Witch) return new legacyconnector.v1_7_10.net.minecraft.entity.monster.EntityWitch(modern);
        if (modern instanceof net.minecraft.world.entity.boss.wither.WitherBoss) return new legacyconnector.v1_7_10.net.minecraft.entity.boss.EntityWither(modern);
        if (modern instanceof net.minecraft.world.entity.animal.IronGolem) return new legacyconnector.v1_7_10.net.minecraft.entity.monster.EntityIronGolem(modern);
        if (modern instanceof net.minecraft.world.entity.animal.SnowGolem) return new legacyconnector.v1_7_10.net.minecraft.entity.monster.EntitySnowman(modern);
        if (modern instanceof net.minecraft.world.entity.animal.Squid) return new legacyconnector.v1_7_10.net.minecraft.entity.passive.EntitySquid(modern);
        if (modern instanceof Animal) return new AnimalView(modern);
        if (modern instanceof Monster) return new MobView(modern);
        return null;
    }

    /** Any other animal (modern ones 1.7.10 did not have, modern mods' animals). */
    static final class AnimalView extends EntityAnimal {
        AnimalView(LivingEntity modern) {
            super(modern);
        }

        @Override
        public EntityAgeable createChild(EntityAgeable mate) {
            return null;
        }
    }

    /** Any other monster. */
    static final class MobView extends EntityMob {
        MobView(LivingEntity modern) {
            super(modern);
        }
    }
}
