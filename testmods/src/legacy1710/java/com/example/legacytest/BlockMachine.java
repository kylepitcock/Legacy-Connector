package com.example.legacytest;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

/** Tile entity block: counts ticks; feeding it a copper ingot adds 100 (tests stackSize-- on a live stack). */
public class BlockMachine extends BlockContainer {
    private IIcon side;
    private IIcon top;

    public BlockMachine() {
        super(Material.iron);
        setHardness(2.0F);
        setHarvestLevel("pickaxe", 0);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityCounter();
    }

    @Override
    public void registerBlockIcons(IIconRegister reg) {
        side = reg.registerIcon("legacytest:machine_side");
        top = reg.registerIcon("legacytest:machine_top");
    }

    @Override
    public IIcon getIcon(int s, int meta) {
        return s == 1 ? top : side;
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int s, float hx, float hy, float hz) {
        TileEntity te = world.getTileEntity(x, y, z);
        if (!(te instanceof TileEntityCounter)) return false;
        TileEntityCounter counter = (TileEntityCounter) te;
        ItemStack held = player.getCurrentEquippedItem();
        if (held != null && held.getItem() == LegacyTestMod.ingot && held.getItemDamage() == 0) {
            held.stackSize--;
            counter.ticks += 100;
            counter.markDirty();
        }
        if (!world.isRemote) player.addChatMessage(new ChatComponentTranslation("legacytest.machine.count", counter.ticks));
        return true;
    }
}
