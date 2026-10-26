package legacyconnector.v1_7_10.net.minecraftforge.event;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.world.Explosion;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import legacyconnector.v1_7_10.net.minecraftforge.common.MinecraftForge;
import legacyconnector.v1_7_10.net.minecraftforge.common.util.BlockSnapshot;
import legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection;
import legacyconnector.v1_7_10.net.minecraftforge.event.world.BlockEvent;
import legacyconnector.v1_7_10.net.minecraftforge.event.world.ExplosionEvent;

/** Helpers vanilla called to post common events; mods call them when they break, place or explode blocks themselves. */
public class ForgeEventFactory {
    /** Posts {@code HarvestDropsEvent} for drops a mod computed; returns the chance each drop should be kept. */
    public static float fireBlockHarvesting(ArrayList<ItemStack> drops, World world, Block block, int x, int y, int z, int meta,
            int fortune, float dropChance, boolean silkTouch, EntityPlayer player) {
        BlockEvent.HarvestDropsEvent event = new BlockEvent.HarvestDropsEvent(x, y, z, world, block, meta, fortune, dropChance, drops, player, silkTouch);
        MinecraftForge.EVENT_BUS.post(event);
        return event.dropChance;
    }

    /** Posts {@code PlaceEvent} for a block a mod placed over {@code snapshot}, against the side {@code direction} names. */
    public static BlockEvent.PlaceEvent onPlayerBlockPlace(EntityPlayer player, BlockSnapshot snapshot, ForgeDirection direction) {
        ForgeDirection against = direction.getOpposite();
        Block placedAgainst = snapshot.world.getBlock(snapshot.x + against.offsetX, snapshot.y + against.offsetY, snapshot.z + against.offsetZ);
        BlockEvent.PlaceEvent event = new BlockEvent.PlaceEvent(snapshot, placedAgainst, player);
        MinecraftForge.EVENT_BUS.post(event);
        return event;
    }

    /** Posts {@code ExplosionEvent.Start}; true if a listener cancelled the explosion. */
    public static boolean onExplosionStart(World world, Explosion explosion) {
        return MinecraftForge.EVENT_BUS.post(new ExplosionEvent.Start(world, explosion));
    }

    /** Posts {@code ExplosionEvent.Detonate}: listeners may remove blocks and entities from the lists the explosion will hit. */
    public static void onExplosionDetonate(World world, Explosion explosion, List<Entity> entities, double diameter) {
        MinecraftForge.EVENT_BUS.post(new ExplosionEvent.Detonate(world, explosion, entities));
    }
}
