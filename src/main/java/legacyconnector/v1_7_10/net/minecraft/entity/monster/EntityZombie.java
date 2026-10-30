package legacyconnector.v1_7_10.net.minecraft.entity.monster;

import legacyconnector.v1_7_10.net.minecraft.world.World;

/** 1.7.10 zombie state: baby flag in watcher slot 12, villager flag in slot 13. */
public class EntityZombie extends EntityMob {
    /** View of a modern vanilla mob (mods test and cast vanilla mobs against this class). */
    public EntityZombie(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    public EntityZombie(World world) {
        super(world);
        setSize(0.6F, 1.8F);
    }

    /** The chance a hurt zombie calls reinforcements (subclasses set it: Thaumcraft's brainy zombies turn it off). */
    protected static final legacyconnector.v1_7_10.net.minecraft.entity.ai.attributes.IAttribute field_110186_bp =
            new legacyconnector.v1_7_10.net.minecraft.entity.ai.attributes.RangedAttribute("zombie.spawnReinforcements", 0.0D, 0.0D, 1.0D)
                    .setDescription("Spawn Reinforcements Chance");

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(legacyconnector.v1_7_10.net.minecraft.entity.SharedMonsterAttributes.followRange).setBaseValue(40.0D);
        getEntityAttribute(legacyconnector.v1_7_10.net.minecraft.entity.SharedMonsterAttributes.movementSpeed).setBaseValue(0.23D);
        getEntityAttribute(legacyconnector.v1_7_10.net.minecraft.entity.SharedMonsterAttributes.attackDamage).setBaseValue(3.0D);
        getAttributeMap().registerAttribute(field_110186_bp).setBaseValue(rand.nextDouble() * 0.1D);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataWatcher.addObject(12, (byte) 0);
        dataWatcher.addObject(13, (byte) 0);
        dataWatcher.addObject(14, (byte) 0);
    }

    public boolean isVillager() {
        if (mcc$entity instanceof net.minecraft.world.entity.monster.Zombie z && !mcc$hosted()) return z instanceof net.minecraft.world.entity.monster.ZombieVillager;
        return dataWatcher.getWatchableObjectByte(13) == 1;
    }

    public void setVillager(boolean villager) {
        dataWatcher.updateObject(13, (byte) (villager ? 1 : 0));
    }

    public boolean isChild() {
        if (mcc$entity instanceof net.minecraft.world.entity.monster.Zombie z && !mcc$hosted()) return z.isBaby();
        return dataWatcher.getWatchableObjectByte(12) == 1;
    }

    public void setChild(boolean child) {
        if (mcc$entity instanceof net.minecraft.world.entity.monster.Zombie z && !mcc$hosted()) {
            z.setBaby(child);
            return;
        }
        dataWatcher.updateObject(12, (byte) (child ? 1 : 0));
    }

    /** Ticks until a mod's zombie villager being cured turns back into a villager. */
    private int conversionTime;

    private net.minecraft.world.entity.monster.Zombie mcc$zombie() {
        return mcc$entity instanceof net.minecraft.world.entity.monster.Zombie z && !mcc$hosted() ? z : null;
    }

    /**
     * A villager it killed rises as a zombie villager (on normal difficulty half of the time, always on hard), keeping its
     * profession, trades and experience; a modern zombie does this itself.
     */
    @Override
    public void onKillEntity(legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase victim) {
        super.onKillEntity(victim);
        if (victim == null || !(mcc$entity != null && mcc$entity.level() instanceof net.minecraft.server.level.ServerLevel level)) return;
        var z = mcc$zombie();
        if (z != null) {
            if (victim.mcc$entity instanceof net.minecraft.world.entity.LivingEntity living) z.killedEntity(level, living);
            return;
        }
        var difficulty = level.getDifficulty();
        if (difficulty != net.minecraft.world.Difficulty.NORMAL && difficulty != net.minecraft.world.Difficulty.HARD) return;
        if (!(victim.mcc$entity instanceof net.minecraft.world.entity.npc.Villager villager)) return;
        if (difficulty != net.minecraft.world.Difficulty.HARD && rand.nextBoolean()) return;
        var converted = villager.convertTo(net.minecraft.world.entity.EntityType.ZOMBIE_VILLAGER, false);
        if (converted == null) return;
        converted.finalizeSpawn(level, level.getCurrentDifficultyAt(converted.blockPosition()), net.minecraft.world.entity.MobSpawnType.CONVERSION,
                new net.minecraft.world.entity.monster.Zombie.ZombieGroupData(false, true));
        converted.setVillagerData(villager.getVillagerData());
        converted.setGossips(villager.getGossips().store(net.minecraft.nbt.NbtOps.INSTANCE));
        converted.setTradeOffers(villager.getOffers().copy());
        converted.setVillagerXp(villager.getVillagerXp());
        net.neoforged.neoforge.event.EventHooks.onLivingConvert(villager, converted);
        level.levelEvent(null, 1026, mcc$entity.blockPosition(), 0);
    }

    /** Starts curing a zombie villager: it turns back after {@code ticks} (sooner near iron bars and beds). */
    protected void startConversion(int ticks) {
        var z = mcc$zombie();
        if (z != null) {
            if (z instanceof net.minecraft.world.entity.monster.ZombieVillager zv) org.kylepitcock.legacyconnector.v1_7_10.entity.ModernAccess.startZombieConversion(zv, ticks);
            return;
        }
        conversionTime = ticks;
        dataWatcher.updateObject(14, (byte) 1);
        removePotionEffect(legacyconnector.v1_7_10.net.minecraft.potion.Potion.weakness.id);
        int amplifier = Math.min(legacyconnector.v1_7_10.net.minecraft.world.World.mcc$get$difficultySetting(mcc$world()).getDifficultyId() - 1, 0);
        addPotionEffect(new legacyconnector.v1_7_10.net.minecraft.potion.PotionEffect(legacyconnector.v1_7_10.net.minecraft.potion.Potion.damageBoost.id, ticks, amplifier));
        mcc$world().setEntityState(this, (byte) 16);
    }

    public boolean isConverting() {
        var z = mcc$zombie();
        if (z != null) return z instanceof net.minecraft.world.entity.monster.ZombieVillager zv && zv.isConverting();
        return dataWatcher.getWatchableObjectByte(14) == 1;
    }

    /** Ticks a mod's zombie's cure on. */
    @Override
    public void onUpdate() {
        if (mcc$hosted() && !mcc$world().isRemote && isConverting()) {
            conversionTime -= getConversionTimeBoost();
            if (conversionTime <= 0) {
                convertToVillager();
                return;
            }
        }
        super.onUpdate();
    }

    /** Replaces the zombie with a (nauseous) villager. */
    protected void convertToVillager() {
        legacyconnector.v1_7_10.net.minecraft.world.World world = mcc$world();
        var villager = new legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityVillager(world);
        villager.copyLocationAndAnglesFrom(this);
        villager.onSpawnWithEgg(null);
        villager.setLookingForHome();
        if (isChild()) villager.setGrowingAge(-24000);
        world.removeEntity(this);
        world.spawnEntityInWorld(villager);
        villager.addPotionEffect(new legacyconnector.v1_7_10.net.minecraft.potion.PotionEffect(legacyconnector.v1_7_10.net.minecraft.potion.Potion.confusion.id, 200, 0));
        world.playAuxSFXAtEntity(null, 1017, (int) mcc$get$posX(this), (int) mcc$get$posY(this), (int) mcc$get$posZ(this), 0);
    }

    /** How fast the cure goes: 1, plus a little for iron bars and beds nearby. */
    protected int getConversionTimeBoost() {
        int boost = 1;
        if (rand.nextFloat() < 0.01F) {
            legacyconnector.v1_7_10.net.minecraft.world.World world = mcc$world();
            int found = 0;
            int cx = (int) mcc$get$posX(this), cy = (int) mcc$get$posY(this), cz = (int) mcc$get$posZ(this);
            for (int x = cx - 4; x < cx + 4 && found < 14; ++x) {
                for (int y = cy - 4; y < cy + 4 && found < 14; ++y) {
                    for (int z = cz - 4; z < cz + 4 && found < 14; ++z) {
                        var block = world.getBlock(x, y, z);
                        if (block == legacyconnector.v1_7_10.net.minecraft.init.Blocks.iron_bars || block == legacyconnector.v1_7_10.net.minecraft.init.Blocks.bed) {
                            if (rand.nextFloat() < 0.3F) ++boost;
                            ++found;
                        }
                    }
                }
            }
        }
        return boost;
    }

    /** 1.7.10: the task AI (mods' subclasses inherit it: their walking speed follows the move helper). */
    @Override
    public boolean isAIEnabled() {
        return true;
    }
}
