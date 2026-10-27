package legacyconnector.v1_12_2.net.minecraft.entity.passive;

import legacyconnector.v1_12_2.net.minecraft.entity.EntityAgeable;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import net.minecraft.world.entity.animal.Animal;

/** 1.12.2 EntityAnimal: breeding (love mode, breeding items). Views of modern animals use the modern state. */
public abstract class EntityAnimal extends EntityAgeable implements IAnimals {
    protected legacyconnector.v1_12_2.net.minecraft.block.Block spawnableBlock;
    private int inLove;
    private java.util.UUID playerInLove;

    public EntityAnimal(World world) {
        super(world);
    }

    protected EntityAnimal(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    private Animal mcc$animal() {
        return mcc$entity instanceof Animal a ? a : null;
    }

    @Override
    protected void updateAITasks() {
        if (getGrowingAge() != 0) inLove = 0;
        super.updateAITasks();
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (mcc$animal() != null || mcc$entity == null) return;
        if (getGrowingAge() != 0) inLove = 0;
        if (inLove > 0) {
            --inLove;
            if (inLove % 10 == 0 && mcc$entity.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.HEART, mcc$entity.getRandomX(1.0D), mcc$entity.getRandomY() + 0.5D,
                        mcc$entity.getRandomZ(1.0D), 1, 0, 0, 0, 0);
            }
        }
    }

    @Override
    public boolean attackEntityFrom(legacyconnector.v1_12_2.net.minecraft.util.DamageSource source, float amount) {
        if (isEntityInvulnerable(source)) return false;
        inLove = 0;
        return super.attackEntityFrom(source, amount);
    }

    @Override
    public float getBlockPathWeight(legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos pos) {
        if (mcc$entity == null || !mcc$hosted()) return super.getBlockPathWeight(pos);
        var level = mcc$entity.level();
        var below = level.getBlockState(pos.mcc$modern().below());
        return below.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK) ? 10.0F : level.getPathfindingCostFromLightLevels(pos.mcc$modern());
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        if (mcc$animal() != null) return;
        tag.setInteger("InLove", inLove);
        if (playerInLove != null) tag.setUniqueId("LoveCause", playerInLove);
    }

    @Override
    public double getYOffset() {
        return 0.14D;
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        if (mcc$animal() != null) return;
        inLove = tag.getInteger("InLove");
        playerInLove = tag.hasUniqueId("LoveCause") ? tag.getUniqueId("LoveCause") : null;
    }

    @Override
    public boolean getCanSpawnHere() {
        if (mcc$entity == null) return true;
        var level = mcc$worldOverride != null ? mcc$worldOverride : mcc$entity.level();
        var pos = mcc$entity.blockPosition();
        return level.getBlockState(pos.below()).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK) && level.getRawBrightness(pos, 0) > 8;
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
        return 1 + rand.nextInt(3);
    }

    public boolean isBreedingItem(ItemStack stack) {
        Animal a = mcc$animal();
        if (a != null && !mcc$hosted()) return a.isFood(org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.toModern(stack));
        return stack.getItem() == legacyconnector.v1_12_2.net.minecraft.init.Items.WHEAT;
    }

    @Override
    protected boolean processInteract(EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!stack.isEmpty()) {
            if (isBreedingItem(stack) && getGrowingAge() == 0 && inLove <= 0) {
                consumeItemFromStack(player, stack);
                setInLove(player);
                return true;
            }
            if (isChild() && isBreedingItem(stack)) {
                consumeItemFromStack(player, stack);
                ageUp((int) (-getGrowingAge() / 20 * 0.1F), true);
                return true;
            }
        }
        return super.processInteract(player, hand);
    }

    protected void consumeItemFromStack(EntityPlayer player, ItemStack stack) {
        if (!player.isCreative()) stack.shrink(1);
    }

    public void setInLove(EntityPlayer player) {
        Animal a = mcc$animal();
        if (a != null) {
            a.setInLove(player == null ? null : player.mcc$player());
            return;
        }
        inLove = 600;
        if (player != null) playerInLove = player.getUniqueID();
        if (mcc$entity != null) mcc$entity.level().broadcastEntityEvent(mcc$entity, (byte) 18);
    }

    public EntityPlayerMP getLoveCause() {
        Animal a = mcc$animal();
        if (a != null) {
            var p = a.getLoveCause();
            return p == null ? null : org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(p) instanceof EntityPlayerMP mp ? mp : null;
        }
        if (playerInLove == null || mcc$entity == null) return null;
        var p = mcc$entity.level().getPlayerByUUID(playerInLove);
        return p == null ? null : org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(p) instanceof EntityPlayerMP mp ? mp : null;
    }

    public boolean isInLove() {
        Animal a = mcc$animal();
        return a != null ? a.isInLove() : inLove > 0;
    }

    public void resetInLove() {
        Animal a = mcc$animal();
        if (a != null) a.resetLove();
        else inLove = 0;
    }

    public boolean canMateWith(EntityAnimal other) {
        if (other == this || other.getClass() != getClass()) return false;
        return isInLove() && other.isInLove();
    }

    @Override
    public void handleStatusUpdate(byte id) {
        if (id == 18 && mcc$entity != null && mcc$hosted()) {
            var level = mcc$entity.level();
            for (int i = 0; i < 7; ++i) {
                level.addParticle(net.minecraft.core.particles.ParticleTypes.HEART, mcc$entity.getRandomX(1.0D), mcc$entity.getRandomY() + 0.5D,
                        mcc$entity.getRandomZ(1.0D), rand.nextGaussian() * 0.02D, rand.nextGaussian() * 0.02D, rand.nextGaussian() * 0.02D);
            }
            return;
        }
        super.handleStatusUpdate(id);
    }
}
