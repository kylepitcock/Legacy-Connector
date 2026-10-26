package legacyconnector.v1_7_10.net.minecraft.entity.passive;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityAgeable;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** 1.7.10 sheep state: fleece colour (low four bits) and the sheared flag (bit 16) in watcher slot 16. */
public class EntitySheep extends EntityAnimal {
    public static final float[][] fleeceColorTable = {{1.0F, 1.0F, 1.0F}, {0.85F, 0.5F, 0.2F}, {0.7F, 0.3F, 0.85F}, {0.4F, 0.6F, 0.85F}, {0.9F, 0.9F, 0.2F},
            {0.5F, 0.8F, 0.1F}, {0.95F, 0.5F, 0.65F}, {0.3F, 0.3F, 0.3F}, {0.6F, 0.6F, 0.6F}, {0.3F, 0.5F, 0.6F}, {0.5F, 0.25F, 0.7F}, {0.2F, 0.3F, 0.7F},
            {0.4F, 0.3F, 0.2F}, {0.4F, 0.5F, 0.2F}, {0.6F, 0.2F, 0.2F}, {0.1F, 0.1F, 0.1F}};

    /** View of a modern vanilla mob (mods test and cast vanilla mobs against this class). */
    public EntitySheep(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    public EntitySheep(World world) {
        super(world);
        setSize(0.9F, 1.3F);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataWatcher.addObject(16, (byte) 0);
    }

    public int getFleeceColor() {
        if (mcc$entity instanceof net.minecraft.world.entity.animal.Sheep s && !mcc$hosted()) return s.getColor().getId();
        return dataWatcher.getWatchableObjectByte(16) & 15;
    }

    public void setFleeceColor(int color) {
        if (mcc$entity instanceof net.minecraft.world.entity.animal.Sheep s && !mcc$hosted()) {
            s.setColor(net.minecraft.world.item.DyeColor.byId(color & 15));
            return;
        }
        byte flags = dataWatcher.getWatchableObjectByte(16);
        dataWatcher.updateObject(16, (byte) (flags & 240 | color & 15));
    }

    public boolean getSheared() {
        if (mcc$entity instanceof net.minecraft.world.entity.animal.Sheep s && !mcc$hosted()) return s.isSheared();
        return (dataWatcher.getWatchableObjectByte(16) & 16) != 0;
    }

    public void setSheared(boolean sheared) {
        if (mcc$entity instanceof net.minecraft.world.entity.animal.Sheep s && !mcc$hosted()) {
            s.setSheared(sheared);
            return;
        }
        byte flags = dataWatcher.getWatchableObjectByte(16);
        dataWatcher.updateObject(16, (byte) (sheared ? flags | 16 : flags & -17));
    }

    /** The colour of a newly spawned sheep: mostly white, a few dark, and rare pink or brown. */
    public static int getRandomSheepColor(Random random) {
        int roll = random.nextInt(100);
        return roll < 5 ? 15 : roll < 10 ? 7 : roll < 15 ? 8 : roll < 18 ? 12 : random.nextInt(500) == 0 ? 6 : 0;
    }

    @Override
    public EntityAgeable createChild(EntityAgeable mate) {
        EntitySheep child = new EntitySheep(worldObj());
        child.setFleeceColor(rand.nextBoolean() && mate instanceof EntitySheep other ? other.getFleeceColor() : getFleeceColor());
        return child;
    }

    @Override
    public int getTalkInterval() {
        return 120;
    }

    private World worldObj() {
        return mcc$world();
    }
}
