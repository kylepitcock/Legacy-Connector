package legacyconnector.v1_7_10.baubles.common.items;

import legacyconnector.v1_7_10.baubles.api.BaubleType;
import legacyconnector.v1_7_10.baubles.api.IBauble;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.IIconRegister;
import legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.util.IIcon;

/** Baubles' own ring item, which addons extend to get a ring with the usual defaults (single, no durability, ring slot). */
public class ItemRing extends Item implements IBauble {
    public IIcon icon;

    public ItemRing() {
        super();
        this.maxStackSize = 1;
        this.setMaxDamage(0);
        this.setCreativeTab(CreativeTabs.tabTools);
    }

    @Override
    public void registerIcons(IIconRegister register) {
        this.icon = register.registerIcon("baubles:ring");
    }

    @Override
    public IIcon getIconFromDamage(int damage) {
        return icon;
    }

    @Override
    public BaubleType getBaubleType(ItemStack itemstack) {
        return BaubleType.RING;
    }

    @Override
    public void onWornTick(ItemStack itemstack, EntityLivingBase player) {
    }

    @Override
    public void onEquipped(ItemStack itemstack, EntityLivingBase player) {
    }

    @Override
    public void onUnequipped(ItemStack itemstack, EntityLivingBase player) {
    }

    @Override
    public boolean canEquip(ItemStack itemstack, EntityLivingBase player) {
        return true;
    }

    @Override
    public boolean canUnequip(ItemStack itemstack, EntityLivingBase player) {
        return true;
    }
}
