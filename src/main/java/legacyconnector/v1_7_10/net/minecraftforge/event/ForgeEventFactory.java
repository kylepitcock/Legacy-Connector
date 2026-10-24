package legacyconnector.v1_7_10.net.minecraftforge.event;

import java.util.ArrayList;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraftforge.common.MinecraftForge;
import legacyconnector.v1_7_10.net.minecraftforge.event.world.BlockEvent;

/** Helpers vanilla called to post common events; mods call them when they break or harvest blocks themselves. */
public class ForgeEventFactory {
    /** Posts {@code HarvestDropsEvent} for drops a mod computed; returns the chance each drop should be kept. */
    public static float fireBlockHarvesting(ArrayList<ItemStack> drops, World world, Block block, int x, int y, int z, int meta,
            int fortune, float dropChance, boolean silkTouch, EntityPlayer player) {
        BlockEvent.HarvestDropsEvent event = new BlockEvent.HarvestDropsEvent(x, y, z, world, block, meta, fortune, dropChance, drops, player, silkTouch);
        MinecraftForge.EVENT_BUS.post(event);
        return event.dropChance;
    }
}
