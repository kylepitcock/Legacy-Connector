package com.example.legacytest;

import java.util.List;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

public class ItemIngot extends Item {
    private IIcon[] icons;

    public ItemIngot() {
        setHasSubtypes(true);
        setMaxDamage(0);
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return super.getUnlocalizedName() + (stack.getItemDamage() == 0 ? ".copper" : ".tin");
    }

    @Override
    public void getSubItems(Item item, CreativeTabs tab, List list) {
        list.add(new ItemStack(item, 1, 0));
        list.add(new ItemStack(item, 1, 1));
    }

    @Override
    public void registerIcons(IIconRegister reg) {
        icons = new IIcon[] {reg.registerIcon("legacytest:ingot_copper"), reg.registerIcon("legacytest:ingot_tin")};
    }

    @Override
    public IIcon getIconFromDamage(int damage) {
        return icons[damage & 1];
    }

    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean advanced) {
        list.add("§7A legacy " + (stack.getItemDamage() == 0 ? "copper" : "tin") + " ingot");
    }
}
