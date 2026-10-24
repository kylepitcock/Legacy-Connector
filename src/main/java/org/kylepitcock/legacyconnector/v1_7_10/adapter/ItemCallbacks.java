package org.kylepitcock.legacyconnector.v1_7_10.adapter;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.BlockBridge;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.FuelBridge;

/**
 * Item behaviour shared by {@link LegacyItemAdapter} and {@link LegacyBlockItemAdapter}: each modern callback is
 * translated into the 1.7.10 one, with a live legacy view of the stack so legacy mutations
 * ({@code stack.stackSize--}) land on the real modern stack.
 */
final class ItemCallbacks {
    private ItemCallbacks() {}

    private static EntityPlayer player(Player p) {
        return p == null ? null : (EntityPlayer) EntityBridge.wrap(p);
    }

    static InteractionResultHolder<net.minecraft.world.item.ItemStack> use(Item legacy, Level level, Player player, InteractionHand hand) {
        net.minecraft.world.item.ItemStack modern = player.getItemInHand(hand);
        ItemStack view = ItemStack.mcc$wrap(modern);
        if (view == null) return InteractionResultHolder.pass(modern);
        int before = modern.getCount();
        ItemStack result = legacy.onItemRightClick(view, World.mcc$wrap(level), player(player));
        net.minecraft.world.item.ItemStack out = result == null ? net.minecraft.world.item.ItemStack.EMPTY : result.mcc$modern();
        if (player.isUsingItem()) return InteractionResultHolder.consume(out);
        if (out != modern || out.getCount() != before) return InteractionResultHolder.sidedSuccess(out, level.isClientSide);
        return InteractionResultHolder.pass(out);
    }

    static InteractionResult useOn(Item legacy, UseOnContext ctx, boolean first) {
        ItemStack view = ItemStack.mcc$wrap(ctx.getItemInHand());
        if (view == null) return InteractionResult.PASS;
        BlockPos pos = ctx.getClickedPos();
        Vec3 h = ctx.getClickLocation();
        World world = World.mcc$wrap(ctx.getLevel());
        float hx = (float) (h.x - pos.getX()), hy = (float) (h.y - pos.getY()), hz = (float) (h.z - pos.getZ());
        int side = ctx.getClickedFace().get3DDataValue();
        boolean done = first
                ? legacy.onItemUseFirst(view, player(ctx.getPlayer()), world, pos.getX(), pos.getY(), pos.getZ(), side, hx, hy, hz)
                : legacy.onItemUse(view, player(ctx.getPlayer()), world, pos.getX(), pos.getY(), pos.getZ(), side, hx, hy, hz);
        return done ? InteractionResult.sidedSuccess(ctx.getLevel().isClientSide) : InteractionResult.PASS;
    }

    static net.minecraft.world.item.ItemStack finishUsing(Item legacy, net.minecraft.world.item.ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof Player p)) return stack;
        ItemStack result = legacy.onEaten(ItemStack.mcc$wrap(stack), World.mcc$wrap(level), player(p));
        return result == null ? net.minecraft.world.item.ItemStack.EMPTY : result.mcc$modern();
    }

    static int useDuration(Item legacy, net.minecraft.world.item.ItemStack stack) {
        ItemStack view = ItemStack.mcc$wrap(stack);
        return view == null ? 0 : legacy.getMaxItemUseDuration(view);
    }

    static UseAnim useAnimation(Item legacy, net.minecraft.world.item.ItemStack stack) {
        ItemStack view = ItemStack.mcc$wrap(stack);
        return view == null ? UseAnim.NONE : legacy.getItemUseAction(view).mcc$modern();
    }

    static void releaseUsing(Item legacy, net.minecraft.world.item.ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (entity instanceof Player p) legacy.onPlayerStoppedUsing(ItemStack.mcc$wrap(stack), World.mcc$wrap(level), player(p), timeLeft);
    }

    static void inventoryTick(Item legacy, net.minecraft.world.item.ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        ItemStack view = ItemStack.mcc$wrap(stack);
        if (view != null) legacy.onUpdate(view, World.mcc$wrap(level), EntityBridge.wrap(entity), slot, selected);
    }

    static boolean hurtEnemy(Item legacy, net.minecraft.world.item.ItemStack stack, LivingEntity target, LivingEntity attacker) {
        return legacy.hitEntity(ItemStack.mcc$wrap(stack), (EntityLivingBase) EntityBridge.wrap(target), (EntityLivingBase) EntityBridge.wrap(attacker));
    }

    static boolean mineBlock(Item legacy, net.minecraft.world.item.ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        return legacy.onBlockDestroyed(ItemStack.mcc$wrap(stack), World.mcc$wrap(level), BlockBridge.legacyBlock(state), pos.getX(), pos.getY(), pos.getZ(),
                (EntityLivingBase) EntityBridge.wrap(miner));
    }

    static InteractionResult interactLiving(Item legacy, net.minecraft.world.item.ItemStack stack, Player player, LivingEntity target) {
        return legacy.itemInteractionForEntity(ItemStack.mcc$wrap(stack), player(player), (EntityLivingBase) EntityBridge.wrap(target))
                ? InteractionResult.sidedSuccess(player.level().isClientSide) : InteractionResult.PASS;
    }

    private static final java.util.concurrent.atomic.AtomicInteger TOOLTIP_FAILURES = new java.util.concurrent.atomic.AtomicInteger();

    @SuppressWarnings({"unchecked", "rawtypes"})
    static void tooltip(Item legacy, net.minecraft.world.item.ItemStack stack, List<Component> out, TooltipFlag flag) {
        ItemStack view = ItemStack.mcc$wrap(stack);
        if (view == null) return;
        List lines = new ArrayList();
        try {
            legacy.addInformation(view, ClientPlayer.get(), lines, flag.isAdvanced());
        } catch (RuntimeException | LinkageError e) {
            // The creative search builds every tooltip before a world (and a player) exists, and mods dereference
            // both; a broken tooltip must never take the game down.
            if (TOOLTIP_FAILURES.incrementAndGet() <= 5) {
                org.slf4j.LoggerFactory.getLogger("LegacyConnector/Items").warn("Tooltip of {} failed: {}", legacy, e.toString());
            }
        }
        for (Object o : lines) out.add(Component.literal(String.valueOf(o)));
    }

    static Component name(Item legacy, net.minecraft.world.item.ItemStack stack, boolean customName) {
        ItemStack view = ItemStack.mcc$wrap(stack);
        if (view == null) return Component.empty();
        if (customName) return Component.literal(legacy.getItemStackDisplayName(view));
        return Component.translatable(legacy.getUnlocalizedNameInefficiently(view).isEmpty()
                ? legacy.getUnlocalizedName(view) + ".name" : legacy.getUnlocalizedName(view) + ".name");
    }

    static float destroySpeed(Item legacy, net.minecraft.world.item.ItemStack stack, BlockState state) {
        ItemStack view = ItemStack.mcc$wrap(stack);
        if (view == null) return 1.0F;
        return legacy.getDigSpeed(view, BlockBridge.legacyBlock(state), BlockBridge.legacyMeta(state));
    }

    static boolean correctTool(Item legacy, net.minecraft.world.item.ItemStack stack, BlockState state) {
        ItemStack view = ItemStack.mcc$wrap(stack);
        return view != null && legacy.canHarvestBlock(BlockBridge.legacyBlock(state), view);
    }

    static net.minecraft.world.item.ItemStack remainder(Item legacy, net.minecraft.world.item.ItemStack stack) {
        ItemStack view = ItemStack.mcc$wrap(stack);
        ItemStack r = view == null ? null : legacy.getContainerItem(view);
        return r == null ? net.minecraft.world.item.ItemStack.EMPTY : r.mcc$modern().copy();
    }

    static boolean hasRemainder(Item legacy, net.minecraft.world.item.ItemStack stack) {
        ItemStack view = ItemStack.mcc$wrap(stack);
        return view != null && legacy.hasContainerItem(view);
    }

    static int burnTime(net.minecraft.world.item.ItemStack stack) {
        return FuelBridge.burnTime(ItemStack.mcc$wrap(stack));
    }

    static boolean entityItemUpdate(Item legacy, net.minecraft.world.entity.item.ItemEntity entity) {
        return legacy.onEntityItemUpdate((legacyconnector.v1_7_10.net.minecraft.entity.item.EntityItem) EntityBridge.wrap(entity));
    }

    public static boolean blockStartBreak(Item legacy, net.minecraft.world.item.ItemStack stack, BlockPos pos, Player player) {
        try {
            return legacy.onBlockStartBreak(ItemStack.mcc$wrap(stack), pos.getX(), pos.getY(), pos.getZ(), player(player));
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    static int enchantability(Item legacy, net.minecraft.world.item.ItemStack stack) {
        try {
            return legacy.getItemEnchantability(ItemStack.mcc$wrap(stack));
        } catch (RuntimeException | LinkageError e) {
            return 0;
        }
    }

    static boolean leftClickEntity(Item legacy, net.minecraft.world.item.ItemStack stack, Player player, Entity target) {
        return legacy.onLeftClickEntity(ItemStack.mcc$wrap(stack), player(player), EntityBridge.wrap(target));
    }

    /** The client player, if on the client; legacy tooltips take the viewing player. */
    static final class ClientPlayer {
        static EntityPlayer get() {
            return EntityBridge.clientPlayer();
        }
    }
}
