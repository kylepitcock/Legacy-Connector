package legacyconnector.v1_7_10.net.minecraft.item;

import legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.IIconRegister;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.entity.projectile.EntityFishHook;
import legacyconnector.v1_7_10.net.minecraft.util.IIcon;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * The fishing rod, ported from 1.7.10: right-click casts an {@link EntityFishHook} or reels the cast one in (which damages the
 * rod). Its item icon is the uncast rod; {@link #func_94597_g} is the cast one renderers draw while the hook is out.
 */
public class ItemFishingRod extends Item {
    /** The cast rod's icon. */
    private IIcon theIcon;

    public ItemFishingRod(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public ItemFishingRod() {
        super();
        setMaxDamage(64);
        setMaxStackSize(1);
        setCreativeTab(legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs.tabTools);
    }

    @Override
    public boolean isFull3D() {
        return true;
    }

    @Override
    public boolean shouldRotateAroundWhenRendering() {
        return true;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        EntityFishHook hook = EntityPlayer.mcc$get$fishEntity(player);
        if (hook != null) {
            int damage = hook.handleHookRetraction();
            stack.damageItem(damage, player);
        } else {
            world.playSoundAtEntity(player, "random.bow", 0.5F, 0.4F / (itemRand.nextFloat() * 0.4F + 0.8F));
            if (!world.isRemote) world.spawnEntityInWorld(new EntityFishHook(world, player));
        }
        player.swingItem();
        return stack;
    }

    @Override
    public void registerIcons(IIconRegister register) {
        itemIcon = register.registerIcon(getIconString() + "_uncast");
        theIcon = register.registerIcon(getIconString() + "_cast");
    }

    /** The cast rod's icon. */
    public IIcon func_94597_g() {
        if (theIcon == null) return org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaOps.itemIcon("fishing_rod_cast");
        return theIcon;
    }

    @Override
    public int getItemEnchantability() {
        return 1;
    }
}
