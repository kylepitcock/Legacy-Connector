package legacyconnector.v1_7_10.net.minecraft.entity.projectile;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.util.MovingObjectPosition;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** A thrown splash potion. The splash effect is not applied by the layer yet; the bottle shatters where it lands. */
public class EntityPotion extends EntityThrowable {
    private ItemStack potionDamage;

    public EntityPotion(World world) {
        super(world);
    }

    public EntityPotion(World world, EntityLivingBase thrower, int damage) {
        this(world, thrower, new ItemStack(legacyconnector.v1_7_10.net.minecraft.init.Items.potionitem, 1, damage));
    }

    public EntityPotion(World world, EntityLivingBase thrower, ItemStack stack) {
        super(world, thrower);
        this.potionDamage = stack;
    }

    public EntityPotion(World world, double x, double y, double z, int damage) {
        this(world, x, y, z, new ItemStack(legacyconnector.v1_7_10.net.minecraft.init.Items.potionitem, 1, damage));
    }

    public EntityPotion(World world, double x, double y, double z, ItemStack stack) {
        super(world, x, y, z);
        this.potionDamage = stack;
    }

    @Override
    protected float func_70182_d() {
        return 0.5F;
    }

    public int getPotionDamage() {
        return potionDamage == null ? 0 : potionDamage.getItemDamage();
    }

    public void setPotionDamage(int damage) {
        if (potionDamage == null) potionDamage = new ItemStack(legacyconnector.v1_7_10.net.minecraft.init.Items.potionitem, 1, 0);
        potionDamage.setItemDamage(damage);
    }

    @Override
    protected void onImpact(MovingObjectPosition hit) {
        if (!mcc$world().isRemote) setDead();
    }
}
