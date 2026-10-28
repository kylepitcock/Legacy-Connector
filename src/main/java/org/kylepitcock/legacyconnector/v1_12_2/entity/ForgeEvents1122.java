package org.kylepitcock.legacyconnector.v1_12_2.entity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraftforge.common.MinecraftForge;
import legacyconnector.v1_12_2.net.minecraftforge.event.entity.player.AnvilRepairEvent;
import legacyconnector.v1_12_2.net.minecraftforge.event.entity.player.ArrowLooseEvent;
import legacyconnector.v1_12_2.net.minecraftforge.event.entity.player.ArrowNockEvent;
import legacyconnector.v1_12_2.net.minecraftforge.event.entity.player.BonemealEvent;
import legacyconnector.v1_12_2.net.minecraftforge.event.entity.player.CriticalHitEvent;
import legacyconnector.v1_12_2.net.minecraftforge.event.entity.player.FillBucketEvent;
import legacyconnector.v1_12_2.net.minecraftforge.event.entity.player.ItemFishedEvent;
import legacyconnector.v1_12_2.net.minecraftforge.event.entity.player.PlayerSetSpawnEvent;
import legacyconnector.v1_12_2.net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import legacyconnector.v1_12_2.net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import legacyconnector.v1_12_2.net.minecraftforge.event.entity.player.UseHoeEvent;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldBridge1122;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NeoForge player events fed into the 1.12.2 Forge bus: sleeping, waking and spawn points, bone meal, hoes, buckets, bows, anvils,
 * fishing and critical hits. What 1.12 handlers decide (cancel, results, changed values) is carried back to the modern event.
 */
public final class ForgeEvents1122 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Events");
    private static final Map<Class<?>, Long> LAST_WARNING = new ConcurrentHashMap<>();

    private ForgeEvents1122() {}

    /** Posts the event (a failing handler is logged once a minute per type); true when it was canceled. */
    private static boolean post(Event event) {
        try {
            return MinecraftForge.EVENT_BUS.post(event);
        } catch (RuntimeException | LinkageError e) {
            long now = System.currentTimeMillis();
            Long last = LAST_WARNING.get(event.getClass());
            if (last == null || now - last > 60_000L) {
                LAST_WARNING.put(event.getClass(), now);
                LOG.warn("A 1.12.2 {} handler failed (repeats are logged once a minute): {}", event.getClass().getSimpleName(), e.toString(), e);
            }
            return false;
        }
    }

    private static EntityPlayer player(net.minecraft.world.entity.player.Player p) {
        return p != null && EntityBridge1122.wrap(p) instanceof EntityPlayer ep ? ep : null;
    }

    private static legacyconnector.v1_12_2.net.minecraft.util.EnumHand hand(net.minecraft.world.InteractionHand h) {
        return h == net.minecraft.world.InteractionHand.OFF_HAND ? legacyconnector.v1_12_2.net.minecraft.util.EnumHand.OFF_HAND
                : legacyconnector.v1_12_2.net.minecraft.util.EnumHand.MAIN_HAND;
    }

    public static void register(net.neoforged.bus.api.IEventBus game) {
        game.addListener((net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent e) -> {
            EntityPlayer p = player(e.getEntity());
            if (p == null) return;
            var event = new PlayerSleepInBedEvent(p, BlockPos.mcc$of(e.getPos()));
            post(event);
            var r = event.getResultStatus();
            if (r == null) return;
            e.setProblem(switch (r) {
                case OK -> null;
                case NOT_POSSIBLE_HERE -> net.minecraft.world.entity.player.Player.BedSleepingProblem.NOT_POSSIBLE_HERE;
                case NOT_POSSIBLE_NOW -> net.minecraft.world.entity.player.Player.BedSleepingProblem.NOT_POSSIBLE_NOW;
                case TOO_FAR_AWAY -> net.minecraft.world.entity.player.Player.BedSleepingProblem.TOO_FAR_AWAY;
                case NOT_SAFE -> net.minecraft.world.entity.player.Player.BedSleepingProblem.NOT_SAFE;
                default -> net.minecraft.world.entity.player.Player.BedSleepingProblem.OTHER_PROBLEM;
            });
        });
        game.addListener((net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent e) -> {
            EntityPlayer p = player(e.getEntity());
            if (p != null) post(new PlayerWakeUpEvent(p, e.wakeImmediately(), e.updateLevel(), true));
        });
        game.addListener((net.neoforged.neoforge.event.entity.player.PlayerSetSpawnEvent e) -> {
            EntityPlayer p = player(e.getEntity());
            if (p == null) return;
            if (post(new PlayerSetSpawnEvent(p, e.getNewSpawn() == null ? null : BlockPos.mcc$of(e.getNewSpawn()), e.isForced()))) e.setCanceled(true);
        });
        game.addListener((net.neoforged.neoforge.event.entity.player.BonemealEvent e) -> {
            EntityPlayer p = player(e.getPlayer());
            if (p == null || !(e.getLevel() instanceof net.minecraft.world.level.Level level)) return;
            var event = new BonemealEvent(p, WorldBridge1122.wrap(level), BlockPos.mcc$of(e.getPos()), StatesBridge1122.toLegacy(e.getState()),
                    legacyconnector.v1_12_2.net.minecraft.util.EnumHand.MAIN_HAND, Stacks1122.view(e.getStack()));
            boolean canceled = post(event);
            if (canceled) e.setCanceled(true);
            else if (event.getResult() == Event.Result.ALLOW) e.setSuccessful(true);
        });
        game.addListener((net.neoforged.neoforge.event.level.BlockEvent.BlockToolModificationEvent e) -> {
            if (e.getItemAbility() != net.neoforged.neoforge.common.ItemAbilities.HOE_TILL || e.isSimulated()) return;
            EntityPlayer p = player(e.getPlayer());
            if (p == null || !(e.getLevel() instanceof net.minecraft.world.level.Level level)) return;
            var event = new UseHoeEvent(p, Stacks1122.view(e.getHeldItemStack()), WorldBridge1122.wrap(level), BlockPos.mcc$of(e.getPos()));
            if (post(event)) {
                e.setCanceled(true);
            } else if (event.getResult() == Event.Result.ALLOW) {
                // the handler tilled the block itself; the modern change is skipped, the hoe is worn as 1.12 did
                e.setCanceled(true);
                var context = e.getContext();
                if (context.getPlayer() != null) context.getItemInHand().hurtAndBreak(1, context.getPlayer(),
                        net.minecraft.world.entity.LivingEntity.getSlotForHand(context.getHand()));
            }
        });
        game.addListener((net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem e) -> {
            if (!(e.getItemStack().getItem() instanceof net.minecraft.world.item.BucketItem)) return;
            EntityPlayer p = player(e.getEntity());
            if (p == null) return;
            var modern = e.getEntity();
            var hit = modern.pick(modern.blockInteractionRange(), 1.0F, e.getItemStack().is(net.minecraft.world.item.Items.BUCKET));
            var event = new FillBucketEvent(p, Stacks1122.view(e.getItemStack()), WorldBridge1122.wrap(e.getLevel()),
                    hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS ? null : legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult.mcc$of(hit));
            if (post(event)) {
                e.setCanceled(true);
                e.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);
            } else if (event.getResult() == Event.Result.ALLOW && event.getFilledBucket() != null) {
                var filled = Stacks1122.toModern(event.getFilledBucket());
                if (!modern.getAbilities().instabuild) {
                    e.getItemStack().shrink(1);
                    if (e.getItemStack().isEmpty()) modern.setItemInHand(e.getHand(), filled);
                    else if (!modern.getInventory().add(filled)) modern.drop(filled, false);
                }
                e.setCanceled(true);
                e.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
            }
        });
        game.addListener((net.neoforged.neoforge.event.entity.player.ArrowLooseEvent e) -> {
            EntityPlayer p = player(e.getEntity());
            if (p == null) return;
            var event = new ArrowLooseEvent(p, Stacks1122.view(e.getBow()), WorldBridge1122.wrap(e.getLevel()), e.getCharge(), e.hasAmmo());
            if (post(event)) e.setCanceled(true);
            else e.setCharge(event.getCharge());
        });
        game.addListener((net.neoforged.neoforge.event.entity.player.ArrowNockEvent e) -> {
            EntityPlayer p = player(e.getEntity());
            if (p == null) return;
            var event = new ArrowNockEvent(p, Stacks1122.view(e.getBow()), hand(e.getHand()), WorldBridge1122.wrap(e.getLevel()), e.hasAmmo());
            post(event);
            var action = event.getAction();
            if (action != null) {
                var stack = Stacks1122.toModernLive(action.getResult());
                e.setAction(switch (action.getType()) {
                    case SUCCESS -> net.minecraft.world.InteractionResultHolder.success(stack);
                    case FAIL -> net.minecraft.world.InteractionResultHolder.fail(stack);
                    default -> net.minecraft.world.InteractionResultHolder.pass(stack);
                });
            }
        });
        game.addListener((net.neoforged.neoforge.event.entity.player.AnvilRepairEvent e) -> {
            EntityPlayer p = player(e.getEntity());
            if (p == null) return;
            var event = new AnvilRepairEvent(p, Stacks1122.fromModern(e.getLeft()), Stacks1122.fromModern(e.getRight()), Stacks1122.fromModern(e.getOutput()));
            event.setBreakChance(e.getBreakChance());
            post(event);
            e.setBreakChance(event.getBreakChance());
        });
        game.addListener((net.neoforged.neoforge.event.entity.player.ItemFishedEvent e) -> {
            java.util.List<legacyconnector.v1_12_2.net.minecraft.item.ItemStack> drops = new java.util.ArrayList<>();
            for (var s : e.getDrops()) drops.add(Stacks1122.fromModern(s));
            var hook = EntityBridge1122.wrap(e.getHookEntity()) instanceof legacyconnector.v1_12_2.net.minecraft.entity.projectile.EntityFishHook h ? h : null;
            if (hook == null || hook.getAngler() == null) return;
            var event = new ItemFishedEvent(drops, e.getRodDamage(), hook);
            if (post(event)) e.setCanceled(true);
            else if (event.getRodDamage() != e.getRodDamage()) e.damageRodBy(Math.max(0, event.getRodDamage()));
        });
        game.addListener((net.neoforged.neoforge.event.entity.player.CriticalHitEvent e) -> {
            EntityPlayer p = player(e.getEntity());
            if (p == null) return;
            var event = new CriticalHitEvent(p, EntityBridge1122.wrap(e.getTarget()), e.getDamageMultiplier(), e.isVanillaCritical());
            post(event);
            if (event.getResult() == Event.Result.ALLOW) e.setCriticalHit(true);
            else if (event.getResult() == Event.Result.DENY) e.setCriticalHit(false);
            if (event.getDamageModifier() != event.getOldDamageModifier()) e.setDamageMultiplier(event.getDamageModifier());
        });
    }
}
