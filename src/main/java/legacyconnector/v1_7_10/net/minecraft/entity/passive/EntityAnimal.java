package legacyconnector.v1_7_10.net.minecraft.entity.passive;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityAgeable;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.init.Items;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import net.minecraft.world.entity.LivingEntity;

/** 1.7.10 EntityAnimal: breeding state and the animal's preference for grass. */
public abstract class EntityAnimal extends EntityAgeable implements IAnimals {
    private int inLove;

    public EntityAnimal(World world) {
        super(world);
    }

    protected EntityAnimal(LivingEntity modern) {
        super(modern);
    }

    public boolean isBreedingItem(ItemStack stack) {
        return stack != null && stack.getItem() == Items.wheat;
    }

    public boolean isInLove() {
        if (mcc$entity instanceof net.minecraft.world.entity.animal.Animal a && !mcc$hosted()) return a.isInLove();
        return inLove > 0;
    }

    public void func_146082_f(EntityPlayer player) {
        if (mcc$entity instanceof net.minecraft.world.entity.animal.Animal a && !mcc$hosted()) {
            a.setInLove(player == null ? null : (net.minecraft.world.entity.player.Player) player.mcc$entity);
            return;
        }
        inLove = 600;
    }

    public void resetInLove() {
        if (mcc$entity instanceof net.minecraft.world.entity.animal.Animal a && !mcc$hosted()) a.resetLove();
        inLove = 0;
    }

    public boolean canMateWith(EntityAnimal other) {
        return other != this && other.getClass() == getClass() && isInLove() && other.isInLove();
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (inLove > 0) --inLove;
    }

    @Override
    public float getBlockPathWeight(int x, int y, int z) {
        var w = mcc$world();
        if (w == null) return 0.0F;
        return w.getBlock(x, y - 1, z) == legacyconnector.v1_7_10.net.minecraft.init.Blocks.grass ? 10.0F : w.getLightBrightness(x, y, z) - 0.5F;
    }

    @Override
    public int getTalkInterval() {
        return 120;
    }

    @Override
    protected boolean canDespawn() {
        return false;
    }

    @Override
    protected int getExperiencePoints(EntityPlayer player) {
        return 1 + getRNG().nextInt(3);
    }

    public void setInLove(legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player) {
        if (mcc$entity instanceof net.minecraft.world.entity.animal.Animal animal) {
            animal.setInLove(player == null ? null : (net.minecraft.world.entity.player.Player) player.mcc$entity);
        }
    }
}
