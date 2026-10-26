package legacyconnector.v1_7_10.net.minecraft.entity.monster;

import legacyconnector.v1_7_10.net.minecraft.world.World;

/** 1.7.10 spider: the climbing flag is bit 0 of watcher slot 16. */
public class EntitySpider extends EntityMob {
    /** View of a modern vanilla mob (mods test and cast vanilla mobs against this class). */
    public EntitySpider(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    public EntitySpider(World world) {
        super(world);
        setSize(1.4F, 0.9F);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataWatcher.addObject(16, (byte) 0);
    }

    public boolean isBesideClimbableBlock() {
        return (dataWatcher.getWatchableObjectByte(16) & 1) != 0;
    }

    public void setBesideClimbableBlock(boolean climbing) {
        byte flags = dataWatcher.getWatchableObjectByte(16);
        dataWatcher.updateObject(16, (byte) (climbing ? flags | 1 : flags & -2));
    }

    /** The spawn data of a spider: which potion effect a spider jockey's rider gets. */
    public static class GroupData implements legacyconnector.v1_7_10.net.minecraft.entity.IEntityLivingData {
        public int field_111105_a;

        public void func_111104_a(java.util.Random random) {
            int roll = random.nextInt(5);
            if (roll <= 1) field_111105_a = 1;
            else if (roll <= 2) field_111105_a = 5;
            else if (roll <= 3) field_111105_a = 14;
            else if (roll <= 4) field_111105_a = 8;
        }
    }
}
