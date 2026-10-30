package legacyconnector.v1_12_2.net.minecraft.entity.passive;

import static org.kylepitcock.legacyconnector.v1_12_2.entity.Sounds1122.legacy;

import legacyconnector.v1_12_2.net.minecraft.entity.SharedMonsterAttributes;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.network.datasync.DataParameter;
import legacyconnector.v1_12_2.net.minecraft.network.datasync.DataSerializers;
import legacyconnector.v1_12_2.net.minecraft.network.datasync.EntityDataManager;
import legacyconnector.v1_12_2.net.minecraft.util.DamageSource;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraft.util.SoundEvent;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import net.minecraft.sounds.SoundEvents;

/** 1.12.2 sheep: fleece colour and shearing (a view reads the modern sheep). */
public class EntitySheep extends EntityAnimal {
    public EntitySheep(World world) {
        super(world);
        setSize(0.9F, 1.3F);
    }

    protected EntitySheep(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    protected net.minecraft.world.entity.animal.Sheep mcc$self() {
        return mcc$entity instanceof net.minecraft.world.entity.animal.Sheep m ? m : null;
    }

    private static final DataParameter<Byte> DYE_COLOR = EntityDataManager.createKey(EntitySheep.class, DataSerializers.BYTE);
    private int sheepTimer;

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(DYE_COLOR, (byte) 0);
    }

    @Override
    protected void initEntityAI() {
        tasks.addTask(0, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAISwimming(this));
        tasks.addTask(1, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIPanic(this, 1.25D));
        tasks.addTask(2, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIMate(this, 1.0D));
        tasks.addTask(3, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAITempt(this, 1.25D, legacyconnector.v1_12_2.net.minecraft.init.Items.WHEAT, false));
        tasks.addTask(4, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIFollowParent(this, 1.25D));
        tasks.addTask(5, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIWanderAvoidWater(this, 1.0D));
        tasks.addTask(6, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIWatchClosest(this, EntityPlayer.class, 6.0F));
        tasks.addTask(7, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAILookIdle(this));
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(8.0D);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.23000000417232513D);
    }

    /**
     * The fleece colour of a lamb: the dye the parents' two colours craft into (red and white give pink), else one parent's colour
     * by chance.
     */
    public legacyconnector.v1_12_2.net.minecraft.item.EnumDyeColor getDyeColorMixFromParents(EntityAnimal father, EntityAnimal mother) {
        var a = father instanceof EntitySheep f ? f.getFleeceColor() : legacyconnector.v1_12_2.net.minecraft.item.EnumDyeColor.WHITE;
        var b = mother instanceof EntitySheep m ? m.getFleeceColor() : legacyconnector.v1_12_2.net.minecraft.item.EnumDyeColor.WHITE;
        var level = mcc$entity == null ? null : mcc$entity.level();
        if (level != null) {
            try {
                var input = net.minecraft.world.item.crafting.CraftingInput.of(2, 1, java.util.List.of(
                        new net.minecraft.world.item.ItemStack(net.minecraft.world.item.DyeItem.byColor(net.minecraft.world.item.DyeColor.byId(a.getMetadata()))),
                        new net.minecraft.world.item.ItemStack(net.minecraft.world.item.DyeItem.byColor(net.minecraft.world.item.DyeColor.byId(b.getMetadata())))));
                var result = level.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, level)
                        .map(r -> r.value().assemble(input, level.registryAccess())).orElse(net.minecraft.world.item.ItemStack.EMPTY);
                if (result.getItem() instanceof net.minecraft.world.item.DyeItem dye) {
                    return legacyconnector.v1_12_2.net.minecraft.item.EnumDyeColor.byMetadata(dye.getDyeColor().getId());
                }
            } catch (RuntimeException e) {
                // a parent's colour below
            }
        }
        return rand.nextBoolean() ? a : b;
    }

    public legacyconnector.v1_12_2.net.minecraft.item.EnumDyeColor getFleeceColor() {
        var m = mcc$self();
        if (m != null) return legacyconnector.v1_12_2.net.minecraft.item.EnumDyeColor.byMetadata(m.getColor().getId());
        Byte b = dataManager.get(DYE_COLOR);
        return legacyconnector.v1_12_2.net.minecraft.item.EnumDyeColor.byMetadata(b == null ? 0 : b & 15);
    }

    public void setFleeceColor(legacyconnector.v1_12_2.net.minecraft.item.EnumDyeColor color) {
        var m = mcc$self();
        if (m != null) {
            m.setColor(net.minecraft.world.item.DyeColor.byId(color.getMetadata()));
            return;
        }
        Byte b = dataManager.get(DYE_COLOR);
        dataManager.set(DYE_COLOR, (byte) ((b == null ? 0 : b) & 240 | color.getMetadata() & 15));
    }

    public boolean getSheared() {
        var m = mcc$self();
        if (m != null) return m.isSheared();
        Byte b = dataManager.get(DYE_COLOR);
        return b != null && (b & 16) != 0;
    }

    public void setSheared(boolean sheared) {
        var m = mcc$self();
        if (m != null) {
            m.setSheared(sheared);
            return;
        }
        Byte b = dataManager.get(DYE_COLOR);
        byte v = b == null ? 0 : b;
        dataManager.set(DYE_COLOR, (byte) (sheared ? v | 16 : v & -17));
    }

    /** The RGB of a dye colour (1.12's fleece colours: the dye's colour, white made brighter). */
    public static float[] getDyeRgb(legacyconnector.v1_12_2.net.minecraft.item.EnumDyeColor color) {
        var modern = net.minecraft.world.item.DyeColor.byId(color.getMetadata());
        if (modern == net.minecraft.world.item.DyeColor.WHITE) return new float[] {0.9019608F, 0.9019608F, 0.9019608F};
        int rgb = modern.getTextureDiffuseColor();
        float[] out = {(rgb >> 16 & 255) / 255.0F, (rgb >> 8 & 255) / 255.0F, (rgb & 255) / 255.0F};
        for (int i = 0; i < 3; ++i) out[i] *= 0.75F;
        return out;
    }

    /** A natural fleece colour (verbatim 1.12 odds: mostly white, some greys and black, rarely brown or pink). */
    public static legacyconnector.v1_12_2.net.minecraft.item.EnumDyeColor getRandomSheepColor(java.util.Random random) {
        int i = random.nextInt(100);
        if (i < 5) return legacyconnector.v1_12_2.net.minecraft.item.EnumDyeColor.BLACK;
        if (i < 10) return legacyconnector.v1_12_2.net.minecraft.item.EnumDyeColor.GRAY;
        if (i < 15) return legacyconnector.v1_12_2.net.minecraft.item.EnumDyeColor.SILVER;
        if (i < 18) return legacyconnector.v1_12_2.net.minecraft.item.EnumDyeColor.BROWN;
        return random.nextInt(500) == 0 ? legacyconnector.v1_12_2.net.minecraft.item.EnumDyeColor.PINK : legacyconnector.v1_12_2.net.minecraft.item.EnumDyeColor.WHITE;
    }

    @Override
    public void eatGrassBonus() {
        var m = mcc$self();
        if (m != null) {
            m.ate();
            return;
        }
        setSheared(false);
        if (isChild()) addGrowth(60);
    }

    @Override
    public legacyconnector.v1_12_2.net.minecraft.entity.IEntityLivingData onInitialSpawn(legacyconnector.v1_12_2.net.minecraft.world.DifficultyInstance difficulty,
            legacyconnector.v1_12_2.net.minecraft.entity.IEntityLivingData data) {
        data = super.onInitialSpawn(difficulty, data);
        if (mcc$hosted()) setFleeceColor(getRandomSheepColor(rand));
        return data;
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        if (mcc$self() != null) return;
        tag.setBoolean("Sheared", getSheared());
        tag.setByte("Color", (byte) getFleeceColor().getMetadata());
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        if (mcc$self() != null) return;
        setSheared(tag.getBoolean("Sheared"));
        setFleeceColor(legacyconnector.v1_12_2.net.minecraft.item.EnumDyeColor.byMetadata(tag.getByte("Color")));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return legacy(SoundEvents.SHEEP_AMBIENT);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return legacy(SoundEvents.SHEEP_HURT);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return legacy(SoundEvents.SHEEP_DEATH);
    }

    @Override
    public EntitySheep createChild(legacyconnector.v1_12_2.net.minecraft.entity.EntityAgeable ageable) {
        return new EntitySheep(mcc$get$world(this));
    }

    @Override
    protected ResourceLocation getLootTable() {
        return new ResourceLocation("minecraft", "entities/sheep");
    }
}
