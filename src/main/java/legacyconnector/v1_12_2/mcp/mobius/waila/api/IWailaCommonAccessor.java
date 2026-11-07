package legacyconnector.v1_12_2.mcp.mobius.waila.api;

import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

import legacyconnector.v1_12_2.net.minecraft.block.Block;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d;

public interface IWailaCommonAccessor {
    World getWorld();

    EntityPlayer getPlayer();

    Block getBlock();

    int getBlockID();

    String getBlockQualifiedName();

    int getMetadata();

    TileEntity getTileEntity();

    Entity getEntity();

    BlockPos getPosition();

    Vec3d getRenderingPosition();

    NBTTagCompound getNBTData();

    int getNBTInteger(NBTTagCompound tag, String keyname);

    double getPartialFrame();

    EnumFacing getSide();

    ItemStack getStack();
}
