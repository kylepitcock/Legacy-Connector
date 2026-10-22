package legacyconnector.v1_7_10.mcp.mobius.waila.api;

import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection;

/** What the player looks at. Position/rendering-position accessors are not provided by the Jade bridge. */
public interface IWailaDataAccessor {
    World getWorld();

    EntityPlayer getPlayer();

    Block getBlock();

    int getBlockID();

    String getBlockQualifiedName();

    int getMetadata();

    TileEntity getTileEntity();

    NBTTagCompound getNBTData();

    int getNBTInteger(NBTTagCompound tag, String key);

    double getPartialFrame();

    ForgeDirection getSide();

    ItemStack getStack();
}
