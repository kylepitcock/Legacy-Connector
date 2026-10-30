package org.kylepitcock.legacyconnector.v1_12_2.adapter;

import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.EnumActionResult;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldBridge1122;

/**
 * The item hooks a 1.12.2 item overrides, forwarded from its modern item (plain items and block items alike). Which hooks the mod
 * overrides is looked up once per item; the others keep the modern behaviour. Stacks are passed as live views, flushed after each call.
 */
final class ItemCallbacks1122 {
    private final Item legacy;
    final boolean rightClick;
    final boolean use;
    final boolean useFirst;
    final boolean update;
    final boolean useFinish;
    final boolean foodEaten;
    final boolean stoppedUsing;
    final boolean usingTick;
    final boolean useDuration;
    final boolean useAction;
    final boolean hit;
    final boolean destroyed;
    final boolean destroySpeed;
    final boolean interact;
    final boolean effect;
    final boolean durabilityBar;
    final boolean durability;
    final boolean durabilityColor;
    final boolean created;
    final boolean tooltip;
    final boolean entityItemUpdate;
    final boolean sneakBypass;
    private int failures;

    ItemCallbacks1122(Item legacy) {
        this.legacy = legacy;
        Class<?> S = ItemStack.class, W = World.class, P = EntityPlayer.class, L = EntityLivingBase.class, H = EnumHand.class, F = EnumFacing.class,
                B = BlockPos.class, f = float.class;
        rightClick = o("onItemRightClick", W, P, H);
        Class<?> T = legacyconnector.v1_12_2.net.minecraft.client.util.ITooltipFlag.class;
        tooltip = o("addInformation", S, W, java.util.List.class, T) || legacy instanceof legacyconnector.v1_12_2.net.minecraft.item.ItemBlock ib
                && ib.getBlock() != null && Legacy1122Block.overrides(ib.getBlock(), "addInformation", S, W, java.util.List.class, T);
        use = o("onItemUse", P, W, B, H, F, f, f, f) || o("placeBlockAt", S, P, W, B, F, f, f, f, IBlockState.class);
        useFirst = o("onItemUseFirst", P, W, B, F, f, f, f, H);
        update = o("onUpdate", S, W, legacyconnector.v1_12_2.net.minecraft.entity.Entity.class, int.class, boolean.class);
        useFinish = o("onItemUseFinish", S, W, L);
        foodEaten = o("onFoodEaten", S, W, P);
        stoppedUsing = o("onPlayerStoppedUsing", S, W, L, int.class);
        usingTick = o("onUsingTick", S, L, int.class);
        useDuration = o("getMaxItemUseDuration", S);
        useAction = o("getItemUseAction", S);
        hit = o("hitEntity", S, L, L);
        destroyed = o("onBlockDestroyed", S, W, IBlockState.class, B, L);
        destroySpeed = o("getDestroySpeed", S, IBlockState.class);
        interact = o("itemInteractionForEntity", S, P, L, H);
        effect = o("hasEffect", S);
        durabilityBar = o("showDurabilityBar", S);
        durability = o("getDurabilityForDisplay", S);
        durabilityColor = o("getRGBDurabilityForDisplay", S);
        created = o("onCreated", S, W, P);
        entityItemUpdate = o("onEntityItemUpdate", legacyconnector.v1_12_2.net.minecraft.entity.item.EntityItem.class);
        sneakBypass = o("doesSneakBypassUse", S, legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess.class, B, P);
    }

    private boolean o(String name, Class<?>... params) {
        return Legacy1122Block.overrides(legacy, name, params);
    }

    private void failure(String callback, Throwable e) {
        if (failures++ < 3) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/Items").error("{} of {} threw; the call is skipped{}", callback, legacy.getClass().getName(),
                    failures == 3 ? " (further failures of this item are not logged)" : "", e);
        }
    }

    static World world(Level level) {
        return WorldBridge1122.wrap(level);
    }

    static EntityPlayer player(Player p) {
        return p != null && EntityBridge1122.wrap(p) instanceof EntityPlayer e ? e : null;
    }

    static EntityLivingBase living(LivingEntity l) {
        return l != null && EntityBridge1122.wrap(l) instanceof EntityLivingBase e ? e : null;
    }

    static EnumHand hand(InteractionHand hand) {
        return hand == InteractionHand.OFF_HAND ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND;
    }

    static InteractionResult result(EnumActionResult r, Level level) {
        if (r == null) return InteractionResult.PASS;
        return switch (r) {
            case SUCCESS -> InteractionResult.sidedSuccess(level.isClientSide);
            case FAIL -> InteractionResult.FAIL;
            default -> InteractionResult.PASS;
        };
    }

    /** onItemRightClick: the result's stack goes into the hand (a view of the held stack stays the held stack). */
    InteractionResultHolder<net.minecraft.world.item.ItemStack> rightClick(Level level, Player player, InteractionHand hand) {
        net.minecraft.world.item.ItemStack held = player.getItemInHand(hand);
        EntityPlayer p = player(player);
        if (p == null) return InteractionResultHolder.pass(held);
        try {
            var r = legacy.onItemRightClick(world(level), p, hand(hand));
            if (r == null) return InteractionResultHolder.pass(held);
            net.minecraft.world.item.ItemStack out = r.getResult() instanceof ItemStack s ? Stacks1122.toModernLive(s) : held;
            return switch (r.getType()) {
                case SUCCESS -> InteractionResultHolder.sidedSuccess(out, level.isClientSide);
                case FAIL -> InteractionResultHolder.fail(out);
                default -> InteractionResultHolder.pass(out);
            };
        } catch (RuntimeException | LinkageError e) {
            failure("onItemRightClick", e);
            return InteractionResultHolder.pass(held);
        } finally {
            Stacks1122.flushViews();
        }
    }

    private interface UseCall {
        EnumActionResult call(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing side, float x, float y, float z);
    }

    private InteractionResult useCall(String name, UseOnContext context, UseCall call) {
        Level level = context.getLevel();
        EntityPlayer p = player(context.getPlayer());
        if (p == null) return InteractionResult.PASS;
        var pos = context.getClickedPos();
        var hit = context.getClickLocation();
        try {
            return result(call.call(p, world(level), BlockPos.mcc$of(pos), hand(context.getHand()), EnumFacing.mcc$of(context.getClickedFace()),
                    (float) (hit.x - pos.getX()), (float) (hit.y - pos.getY()), (float) (hit.z - pos.getZ())), level);
        } catch (RuntimeException | LinkageError e) {
            failure(name, e);
            return InteractionResult.PASS;
        } finally {
            Stacks1122.flushViews();
        }
    }

    InteractionResult use(UseOnContext context) {
        return useCall("onItemUse", context, legacy::onItemUse);
    }

    InteractionResult useFirst(UseOnContext context) {
        return useCall("onItemUseFirst", context, (pl, w, pos, hand, side, x, y, z) -> legacy.onItemUseFirst(pl, w, pos, side, x, y, z, hand));
    }

    void update(net.minecraft.world.item.ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        try {
            legacy.onUpdate(Stacks1122.view(stack), world(level), EntityBridge1122.wrap(entity), slot, selected);
        } catch (RuntimeException | LinkageError e) {
            failure("onUpdate", e);
        } finally {
            Stacks1122.flushViews();
        }
    }

    net.minecraft.world.item.ItemStack useFinish(net.minecraft.world.item.ItemStack stack, Level level, LivingEntity entity) {
        try {
            ItemStack out = legacy.onItemUseFinish(Stacks1122.view(stack), world(level), living(entity));
            return out == null ? stack : Stacks1122.toModernLive(out);
        } catch (RuntimeException | LinkageError e) {
            failure("onItemUseFinish", e);
            return stack;
        } finally {
            Stacks1122.flushViews();
        }
    }

    /** ItemFood.onFoodEaten after the modern eating (hunger, saturation, effects): mods add their own effects here. */
    void foodEaten(net.minecraft.world.item.ItemStack eaten, Level level, LivingEntity entity) {
        if (!(legacy instanceof legacyconnector.v1_12_2.net.minecraft.item.ItemFood food) || !(entity instanceof Player player)) return;
        EntityPlayer p = player(player);
        if (p == null) return;
        try {
            food.mcc$onFoodEaten(Stacks1122.fromModern(eaten), world(level), p);
        } catch (RuntimeException | LinkageError e) {
            failure("onFoodEaten", e);
        }
    }

    void stoppedUsing(net.minecraft.world.item.ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        try {
            legacy.onPlayerStoppedUsing(Stacks1122.view(stack), world(level), living(entity), timeLeft);
        } catch (RuntimeException | LinkageError e) {
            failure("onPlayerStoppedUsing", e);
        } finally {
            Stacks1122.flushViews();
        }
    }

    void usingTick(net.minecraft.world.item.ItemStack stack, LivingEntity entity, int count) {
        try {
            legacy.onUsingTick(Stacks1122.view(stack), living(entity), count);
        } catch (RuntimeException | LinkageError e) {
            failure("onUsingTick", e);
        } finally {
            Stacks1122.flushViews();
        }
    }

    int useDuration(net.minecraft.world.item.ItemStack stack, int fallback) {
        try {
            return legacy.getMaxItemUseDuration(Stacks1122.view(stack));
        } catch (RuntimeException | LinkageError e) {
            return fallback;
        }
    }

    net.minecraft.world.item.UseAnim useAction(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.UseAnim fallback) {
        try {
            var a = legacy.getItemUseAction(Stacks1122.view(stack));
            return a == null ? fallback : a.mcc$modern();
        } catch (RuntimeException | LinkageError e) {
            return fallback;
        }
    }

    boolean hit(net.minecraft.world.item.ItemStack stack, LivingEntity target, LivingEntity attacker) {
        try {
            return legacy.hitEntity(Stacks1122.view(stack), living(target), living(attacker));
        } catch (RuntimeException | LinkageError e) {
            failure("hitEntity", e);
            return false;
        } finally {
            Stacks1122.flushViews();
        }
    }

    boolean destroyed(net.minecraft.world.item.ItemStack stack, Level level, net.minecraft.world.level.block.state.BlockState state, net.minecraft.core.BlockPos pos,
            LivingEntity entity) {
        try {
            return legacy.onBlockDestroyed(Stacks1122.view(stack), world(level), StatesBridge1122.toLegacy(state), BlockPos.mcc$of(pos), living(entity));
        } catch (RuntimeException | LinkageError e) {
            failure("onBlockDestroyed", e);
            return false;
        } finally {
            Stacks1122.flushViews();
        }
    }

    float destroySpeed(net.minecraft.world.item.ItemStack stack, net.minecraft.world.level.block.state.BlockState state, float fallback) {
        IBlockState legacyState = StatesBridge1122.toLegacy(state);
        if (legacyState == null) return fallback;
        try {
            return legacy.getDestroySpeed(Stacks1122.view(stack), legacyState);
        } catch (RuntimeException | LinkageError e) {
            return fallback;
        }
    }

    InteractionResult interact(net.minecraft.world.item.ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        EntityPlayer p = player(player);
        EntityLivingBase t = living(target);
        if (p == null || t == null) return InteractionResult.PASS;
        try {
            return legacy.itemInteractionForEntity(Stacks1122.view(stack), p, t, hand(hand)) ? InteractionResult.sidedSuccess(player.level().isClientSide)
                    : InteractionResult.PASS;
        } catch (RuntimeException | LinkageError e) {
            failure("itemInteractionForEntity", e);
            return InteractionResult.PASS;
        } finally {
            Stacks1122.flushViews();
        }
    }

    boolean effect(net.minecraft.world.item.ItemStack stack, boolean fallback) {
        try {
            return legacy.hasEffect(Stacks1122.fromModern(stack));
        } catch (RuntimeException | LinkageError e) {
            return fallback;
        }
    }

    Boolean durabilityBar(net.minecraft.world.item.ItemStack stack) {
        try {
            return legacy.showDurabilityBar(Stacks1122.fromModern(stack));
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    /** The bar width (0-13) from the legacy damage fraction (1 = fully damaged). */
    Integer barWidth(net.minecraft.world.item.ItemStack stack) {
        try {
            double d = legacy.getDurabilityForDisplay(Stacks1122.fromModern(stack));
            return Math.round(13.0F - (float) d * 13.0F);
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    Integer barColor(net.minecraft.world.item.ItemStack stack) {
        try {
            return legacy.getRGBDurabilityForDisplay(Stacks1122.fromModern(stack));
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    void created(net.minecraft.world.item.ItemStack stack, Level level, Player player) {
        EntityPlayer p = player(player);
        try {
            legacy.onCreated(Stacks1122.view(stack), world(level), p);
        } catch (RuntimeException | LinkageError e) {
            failure("onCreated", e);
        } finally {
            Stacks1122.flushViews();
        }
    }

    /** The legacy tooltip lines (addInformation; a block item asks its block), added as text (section-sign formatting kept). */
    void appendHoverText(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
            java.util.List<net.minecraft.network.chat.Component> lines, net.minecraft.world.item.TooltipFlag flag) {
        try {
            java.util.List<String> out = new java.util.ArrayList<>();
            Level level = context == null ? null : context.level();
            legacy.addInformation(Stacks1122.fromModern(stack), level == null ? null : world(level), out,
                    legacyconnector.v1_12_2.net.minecraft.client.util.ITooltipFlag.mcc$of(flag != null && flag.isAdvanced()));
            for (String s : out) if (s != null) lines.add(net.minecraft.network.chat.Component.literal(s));
        } catch (RuntimeException | LinkageError e) {
            failure("addInformation", e);
        }
    }

    /** Forge's per-tick hook of a dropped stack's item entity: true skips the entity's own update. */
    boolean entityItemUpdate(net.minecraft.world.entity.item.ItemEntity entity) {
        try {
            return EntityBridge1122.wrap(entity) instanceof legacyconnector.v1_12_2.net.minecraft.entity.item.EntityItem e && legacy.onEntityItemUpdate(e);
        } catch (RuntimeException | LinkageError e) {
            failure("onEntityItemUpdate", e);
            return false;
        } finally {
            Stacks1122.flushViews();
        }
    }

    /** Whether a sneaking player still activates the clicked block with this item. */
    boolean sneakBypass(net.minecraft.world.item.ItemStack stack, net.minecraft.world.level.LevelReader level, net.minecraft.core.BlockPos pos, Player player) {
        try {
            return legacy.doesSneakBypassUse(Stacks1122.view(stack), WorldBridge1122.access(level), BlockPos.mcc$of(pos), player(player));
        } catch (RuntimeException | LinkageError e) {
            failure("doesSneakBypassUse", e);
            return false;
        }
    }
}
