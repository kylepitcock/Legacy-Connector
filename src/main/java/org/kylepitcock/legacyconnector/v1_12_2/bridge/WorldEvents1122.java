package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.event.world.BlockEvent;
import legacyconnector.v1_12_2.net.minecraftforge.event.world.ChunkDataEvent;
import legacyconnector.v1_12_2.net.minecraftforge.event.world.ChunkEvent;
import legacyconnector.v1_12_2.net.minecraftforge.event.world.ExplosionEvent;
import legacyconnector.v1_12_2.net.minecraftforge.event.world.WorldEvent;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Posts Forge 1.12.2's world events to the legacy bus from their NeoForge counterparts: world load/unload/save, chunk load/unload
 * and chunk data, block break/place/drops/neighbour notify/farmland trample, explosions. Nothing is built when no 1.12 mod listens;
 * cancelling a legacy event cancels the modern one; a failing handler is logged (throttled) and does not stop the game.
 */
public final class WorldEvents1122 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/1.12.2");
    private static int failures;

    private WorldEvents1122() {}

    private static legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.EventBus bus() {
        return legacyconnector.v1_12_2.net.minecraftforge.common.MinecraftForge.EVENT_BUS;
    }

    private static boolean listens(Class<? extends Event> type) {
        return bus().mcc$hasListeners(type);
    }

    /** Posts the event; true when it was cancelled. */
    private static boolean post(Event event) {
        try {
            return bus().post(event);
        } catch (RuntimeException | LinkageError e) {
            if (failures++ < 5 || failures % 500 == 0) LOG.warn("A 1.12.2 handler of {} failed ({} so far): {}", event.getClass().getSimpleName(), failures, e.toString());
            return false;
        }
    }

    private static World world(net.minecraft.world.level.LevelAccessor level) {
        return level == null ? null : WorldBridge1122.wrap(level);
    }

    private static BlockPos pos(net.minecraft.core.BlockPos p) {
        return BlockPos.mcc$of(p);
    }

    private static legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state(net.minecraft.world.level.block.state.BlockState s) {
        var legacy = StatesBridge1122.toLegacy(s);
        return legacy != null ? legacy : Vanilla1122.airState();
    }

    private static legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player(net.minecraft.world.entity.Entity e) {
        return e instanceof net.minecraft.world.entity.player.Player p && EntityBridge1122.wrap(p) instanceof legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer l
                ? l : null;
    }

    public static void register() {
        var b = net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        b.addListener((net.neoforged.neoforge.event.level.LevelEvent.Load e) -> {
            if (listens(WorldEvent.Load.class) && e.getLevel() instanceof Level l) post(new WorldEvent.Load(world(l)));
        });
        b.addListener((net.neoforged.neoforge.event.level.LevelEvent.Unload e) -> {
            if (!(e.getLevel() instanceof Level l)) return;
            World w = world(l);
            if (listens(WorldEvent.Unload.class)) post(new WorldEvent.Unload(w));
            legacyconnector.v1_12_2.net.minecraftforge.common.ForgeChunkManager.mcc$unloadWorld(w);
        });
        b.addListener((net.neoforged.neoforge.event.level.LevelEvent.Save e) -> {
            if (listens(WorldEvent.Save.class) && e.getLevel() instanceof Level l) post(new WorldEvent.Save(world(l)));
        });
        b.addListener((net.neoforged.neoforge.event.level.ChunkEvent.Load e) -> {
            if (!listens(ChunkEvent.Load.class) || !(e.getLevel() instanceof Level l)) return;
            post(new ChunkEvent.Load(legacyconnector.v1_12_2.net.minecraft.world.chunk.Chunk.mcc$of(world(l), e.getChunk())));
        });
        b.addListener((net.neoforged.neoforge.event.level.ChunkEvent.Unload e) -> {
            if (!listens(ChunkEvent.Unload.class) || !(e.getLevel() instanceof Level l)) return;
            post(new ChunkEvent.Unload(legacyconnector.v1_12_2.net.minecraft.world.chunk.Chunk.mcc$of(world(l), e.getChunk())));
        });
        b.addListener((net.neoforged.neoforge.event.level.ChunkDataEvent.Load e) -> {
            if (!listens(ChunkDataEvent.Load.class) || !(e.getLevel() instanceof Level l)) return;
            post(new ChunkDataEvent.Load(legacyconnector.v1_12_2.net.minecraft.world.chunk.Chunk.mcc$of(world(l), e.getChunk()),
                    new legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound(e.getData())));
        });
        b.addListener((net.neoforged.neoforge.event.level.ChunkDataEvent.Save e) -> {
            if (!listens(ChunkDataEvent.Save.class) || !(e.getLevel() instanceof Level l)) return;
            post(new ChunkDataEvent.Save(legacyconnector.v1_12_2.net.minecraft.world.chunk.Chunk.mcc$of(world(l), e.getChunk()),
                    new legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound(e.getData())));
        });
        b.addListener((net.neoforged.neoforge.event.level.BlockEvent.BreakEvent e) -> {
            if (!listens(BlockEvent.BreakEvent.class) || !(e.getLevel() instanceof Level l)) return;
            var legacy = new BlockEvent.BreakEvent(world(l), pos(e.getPos()), state(e.getState()), player(e.getPlayer()));
            if (post(legacy)) e.setCanceled(true);
        });
        b.addListener((net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent e) -> {
            if (!listens(BlockEvent.PlaceEvent.class) || !(e.getLevel() instanceof Level l)) return;
            var hand = e.getEntity() instanceof net.minecraft.world.entity.LivingEntity living && living.getUsedItemHand() == net.minecraft.world.InteractionHand.OFF_HAND
                    ? legacyconnector.v1_12_2.net.minecraft.util.EnumHand.OFF_HAND : legacyconnector.v1_12_2.net.minecraft.util.EnumHand.MAIN_HAND;
            var legacy = new BlockEvent.PlaceEvent(world(l), pos(e.getPos()), state(e.getPlacedBlock()), e.getBlockSnapshot(), state(e.getPlacedAgainst()),
                    player(e.getEntity()), hand);
            if (post(legacy)) e.setCanceled(true);
        });
        b.addListener((net.neoforged.neoforge.event.level.BlockEvent.NeighborNotifyEvent e) -> {
            if (!listens(BlockEvent.NeighborNotifyEvent.class) || !(e.getLevel() instanceof Level l)) return;
            var sides = java.util.EnumSet.noneOf(legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.class);
            for (var d : e.getNotifiedSides()) sides.add(legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.mcc$of(d));
            var legacy = new BlockEvent.NeighborNotifyEvent(world(l), pos(e.getPos()), state(e.getState()), sides, e.getForceRedstoneUpdate());
            if (post(legacy)) e.setCanceled(true);
        });
        b.addListener((net.neoforged.neoforge.event.level.BlockEvent.FarmlandTrampleEvent e) -> {
            if (!listens(BlockEvent.FarmlandTrampleEvent.class) || !(e.getLevel() instanceof Level l)) return;
            var entity = e.getEntity() == null ? null : EntityBridge1122.wrap(e.getEntity());
            if (post(new BlockEvent.FarmlandTrampleEvent(world(l), pos(e.getPos()), state(e.getState()), e.getFallDistance(), entity))) e.setCanceled(true);
        });
        b.addListener(WorldEvents1122::onDrops);
        b.addListener((net.neoforged.neoforge.event.level.ExplosionEvent.Start e) -> {
            if (!listens(ExplosionEvent.Start.class)) return;
            World w = world(e.getLevel());
            var explosion = legacyconnector.v1_12_2.net.minecraft.world.Explosion.mcc$of(w, null, e.getExplosion());
            if (post(new ExplosionEvent.Start(w, explosion))) e.setCanceled(true);
        });
        b.addListener((net.neoforged.neoforge.event.level.ExplosionEvent.Detonate e) -> {
            if (!listens(ExplosionEvent.Detonate.class)) return;
            World w = world(e.getLevel());
            var explosion = legacyconnector.v1_12_2.net.minecraft.world.Explosion.mcc$of(w, null, e.getExplosion());
            List<legacyconnector.v1_12_2.net.minecraft.entity.Entity> entities = new ArrayList<>();
            for (var m : e.getAffectedEntities()) entities.add(EntityBridge1122.wrap(m));
            List<legacyconnector.v1_12_2.net.minecraft.entity.Entity> before = new ArrayList<>(entities);
            post(new ExplosionEvent.Detonate(w, explosion, entities));
            // what handlers removed is not destroyed or hit
            var kept = new java.util.HashSet<net.minecraft.core.BlockPos>();
            for (BlockPos p : explosion.getAffectedBlockPositions()) kept.add(new net.minecraft.core.BlockPos(p.getX(), p.getY(), p.getZ()));
            e.getAffectedBlocks().removeIf(p -> !kept.contains(p));
            for (var gone : before) {
                if (!entities.contains(gone) && gone != null) e.getAffectedEntities().remove(gone.mcc$modern());
            }
        });
    }

    /** A block's drops as Forge's HarvestDropsEvent: handlers change the list (and the chance each stack drops). */
    private static void onDrops(net.neoforged.neoforge.event.level.BlockDropsEvent e) {
        if (!listens(BlockEvent.HarvestDropsEvent.class)) return;
        var level = e.getLevel();
        List<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> drops = new ArrayList<>();
        for (var item : e.getDrops()) drops.add(Stacks1122.fromModern(item.getItem()));
        List<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> original = new ArrayList<>(drops);
        int fortune = 0;
        boolean silk = false;
        var tool = e.getTool();
        if (tool != null && !tool.isEmpty()) {
            try {
                var enchantments = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
                fortune = net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                        enchantments.getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE), tool);
                silk = net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                        enchantments.getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH), tool) > 0;
            } catch (RuntimeException ex) {
                // no enchantments known
            }
        }
        var legacy = new BlockEvent.HarvestDropsEvent(world(level), pos(e.getPos()), state(e.getState()), fortune, 1.0F, drops, player(e.getBreaker()), silk);
        post(legacy);
        boolean same = legacy.getDropChance() >= 1.0F && drops.size() == original.size();
        for (int i = 0; same && i < drops.size(); i++) same = drops.get(i) == original.get(i);
        if (same) return;
        var p = e.getPos();
        e.getDrops().clear();
        for (var stack : drops) {
            if (stack == null || level.random.nextFloat() > legacy.getDropChance()) continue;
            var modern = Stacks1122.toModern(stack);
            if (modern.isEmpty()) continue;
            var entity = new net.minecraft.world.entity.item.ItemEntity(level, p.getX() + 0.5D, p.getY() + 0.5D, p.getZ() + 0.5D, modern);
            entity.setDefaultPickUpDelay();
            e.getDrops().add(entity);
        }
    }
}
