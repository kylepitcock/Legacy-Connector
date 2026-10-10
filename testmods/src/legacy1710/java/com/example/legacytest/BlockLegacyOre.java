package com.example.legacytest;

import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

/** Metadata block: 0 = copper, 1 = tin. Right click toggles between them (tests metadata writes). */
public class BlockLegacyOre extends Block {
    private IIcon[] icons;

    public BlockLegacyOre() {
        super(Material.rock);
        setHardness(3.0F);
        setResistance(5.0F);
        setStepSound(soundTypePiston);
        setHarvestLevel("pickaxe", 1);
    }

    @Override
    public void registerBlockIcons(IIconRegister reg) {
        icons = new IIcon[] {reg.registerIcon("legacytest:CopperOre"), reg.registerIcon("legacytest:tin_ore")};
    }

    @Override
    public IIcon getIcon(int side, int meta) {
        return icons[meta & 1];
    }

    @Override
    public int damageDropped(int meta) {
        return meta & 1;
    }

    @Override
    public void getSubBlocks(Item item, CreativeTabs tab, List list) {
        list.add(new ItemStack(item, 1, 0));
        list.add(new ItemStack(item, 1, 1));
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hx, float hy, float hz) {
        if (!player.isSneaking()) return false;
        int meta = world.getBlockMetadata(x, y, z);
        world.setBlockMetadataWithNotify(x, y, z, meta ^ 1, 3);
        if (!world.isRemote) player.addChatMessage(new ChatComponentText("Ore switched to " + ((meta ^ 1) == 0 ? "copper" : "tin")));
        return true;
    }
}
