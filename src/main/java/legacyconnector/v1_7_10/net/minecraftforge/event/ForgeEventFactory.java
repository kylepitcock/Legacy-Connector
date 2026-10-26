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

    // The helpers below post to the legacy bus exactly as Forge 1.7.10 did: mods call them when they simulate a vanilla
    // action themselves (fake players clicking, custom spawners, tools breaking), and the legacy listeners expect them.

    /** Posts {@code PlayerInteractEvent} for a click a mod performs; the caller honours {@code useBlock}/{@code useItem}. */
    public static legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.PlayerInteractEvent onPlayerInteract(EntityPlayer player,
            legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.PlayerInteractEvent.Action action, int x, int y, int z, int face, World world) {
        var event = new legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.PlayerInteractEvent(player, action, x, y, z, face, world);
        MinecraftForge.EVENT_BUS.post(event);
        return event;
    }

    /** Posts {@code HarvestCheck}; whether the player gets the block's drops. */
    public static boolean doPlayerHarvestCheck(EntityPlayer player, Block block, boolean success) {
        var event = new legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.PlayerEvent.HarvestCheck(player, block, success);
        MinecraftForge.EVENT_BUS.post(event);
        return event.success;
    }

    /** Posts {@code BreakSpeed}; the new mining speed, or -1 when a handler cancelled (the block cannot be mined). */
    public static float getBreakSpeed(EntityPlayer player, Block block, int metadata, float original, int x, int y, int z) {
        var event = new legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed(player, block, metadata, original, x, y, z);
        return MinecraftForge.EVENT_BUS.post(event) ? -1 : event.newSpeed;
    }

    /** Posts {@code PlayerDestroyItemEvent} for a held item that just broke. */
    public static void onPlayerDestroyItem(EntityPlayer player, ItemStack stack) {
        MinecraftForge.EVENT_BUS.post(new legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.PlayerDestroyItemEvent(player, stack));
    }

    /** Posts {@code CheckSpawn}: ALLOW forces a natural spawn here, DENY forbids it, DEFAULT leaves it to the mob's own checks. */
    public static legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event.Result canEntitySpawn(
            legacyconnector.v1_7_10.net.minecraft.entity.EntityLiving entity, World world, float x, float y, float z) {
        var event = new legacyconnector.v1_7_10.net.minecraftforge.event.entity.living.LivingSpawnEvent.CheckSpawn(entity, world, x, y, z);
        MinecraftForge.EVENT_BUS.post(event);
        return event.getResult();
    }

    /** Posts {@code SpecialSpawn}; true when a handler cancelled it (the spawner then skips the mob's default initialisation). */
    public static boolean doSpecialSpawn(legacyconnector.v1_7_10.net.minecraft.entity.EntityLiving entity, World world, float x, float y, float z) {
        return MinecraftForge.EVENT_BUS.post(new legacyconnector.v1_7_10.net.minecraftforge.event.entity.living.LivingSpawnEvent.SpecialSpawn(entity, world, x, y, z));
    }

    /** Posts {@code AllowDespawn}: ALLOW forces the despawn, DENY keeps the mob, DEFAULT uses the distance rules. */
    public static legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.Event.Result canEntityDespawn(
            legacyconnector.v1_7_10.net.minecraft.entity.EntityLiving entity) {
        var event = new legacyconnector.v1_7_10.net.minecraftforge.event.entity.living.LivingSpawnEvent.AllowDespawn(entity);
        MinecraftForge.EVENT_BUS.post(event);
        return event.getResult();
    }

    /** Posts {@code ItemTooltipEvent}: handlers add lines to {@code toolTip}. */
    public static legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.ItemTooltipEvent onItemTooltip(ItemStack stack, EntityPlayer player,
            List<String> toolTip, boolean showAdvancedItemTooltips) {
        var event = new legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.ItemTooltipEvent(stack, player, toolTip, showAdvancedItemTooltips);
        MinecraftForge.EVENT_BUS.post(event);
        return event;
    }

    /** Posts {@code EntityStruckByLightningEvent}; true when a handler cancelled it (the entity is spared). */
    public static boolean onEntityStruckByLightning(Entity entity, legacyconnector.v1_7_10.net.minecraft.entity.effect.EntityLightningBolt bolt) {
        return MinecraftForge.EVENT_BUS.post(new legacyconnector.v1_7_10.net.minecraftforge.event.entity.EntityStruckByLightningEvent(entity, bolt));
    }

    /** Posts {@code PlayerUseItemEvent.Start}; the use duration, or -1 when cancelled. */
    public static int onItemUseStart(EntityPlayer player, ItemStack item, int duration) {
        var event = new legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.PlayerUseItemEvent.Start(player, item, duration);
        return MinecraftForge.EVENT_BUS.post(event) ? -1 : event.duration;
    }

    /** Posts {@code PlayerUseItemEvent.Tick}; the remaining duration, or -1 when cancelled (the use stops). */
    public static int onItemUseTick(EntityPlayer player, ItemStack item, int duration) {
        var event = new legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.PlayerUseItemEvent.Tick(player, item, duration);
        return MinecraftForge.EVENT_BUS.post(event) ? -1 : event.duration;
    }

    /** Posts {@code PlayerUseItemEvent.Stop}; true when cancelled (the item's own release behaviour is skipped). */
    public static boolean onUseItemStop(EntityPlayer player, ItemStack item, int duration) {
        return MinecraftForge.EVENT_BUS.post(new legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.PlayerUseItemEvent.Stop(player, item, duration));
    }

    /** Posts {@code PlayerUseItemEvent.Finish}; the stack that replaces the used one (handlers may change it). */
    public static ItemStack onItemUseFinish(EntityPlayer player, ItemStack item, int duration, ItemStack result) {
        var event = new legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.PlayerUseItemEvent.Finish(player, item, duration, result);
        MinecraftForge.EVENT_BUS.post(event);
        return event.result;
    }

    /** Posts {@code NameFormat}; the name to display for the player. */
    public static String getPlayerDisplayName(EntityPlayer player, String username) {
        var event = new legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.PlayerEvent.NameFormat(player, username);
        MinecraftForge.EVENT_BUS.post(event);
        return event.displayname;
    }

    /** Posts {@code StartTracking} for an entity the player now sees. */
    public static void onStartEntityTracking(Entity entity, EntityPlayer player) {
        MinecraftForge.EVENT_BUS.post(new legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.PlayerEvent.StartTracking(player, entity));
    }

    /** Posts {@code StopTracking} for an entity the player no longer sees. */
    public static void onStopEntityTracking(Entity entity, EntityPlayer player) {
        MinecraftForge.EVENT_BUS.post(new legacyconnector.v1_7_10.net.minecraftforge.event.entity.player.PlayerEvent.StopTracking(player, entity));
    }

    /** Posts {@code PotentialSpawns}; the mobs that may spawn there, or null when a handler cancelled (nothing spawns). */
    public static List getPotentialSpawns(legacyconnector.v1_7_10.net.minecraft.world.WorldServer world,
            legacyconnector.v1_7_10.net.minecraft.entity.EnumCreatureType type, int x, int y, int z, List oldList) {
        var event = new legacyconnector.v1_7_10.net.minecraftforge.event.world.WorldEvent.PotentialSpawns(world, type, x, y, z, oldList);
        return MinecraftForge.EVENT_BUS.post(event) ? null : event.list;
    }
}
