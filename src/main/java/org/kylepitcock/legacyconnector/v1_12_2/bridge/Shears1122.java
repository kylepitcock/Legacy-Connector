package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import legacyconnector.v1_12_2.net.minecraftforge.common.IShearable;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.kylepitcock.legacyconnector.v1_12_2.adapter.Legacy1122Block;

/**
 * Forge 1.12's ItemShears.onBlockStartBreak for {@link IShearable} blocks (leaves, bushes, mushrooms of mods): shears that break such a block drop what
 * the block's {@code onSheared} gives (not its normal drops), lose one point of durability and remove the block. Modern shears and legacy ItemShears alike.
 */
public final class Shears1122 {
    private Shears1122() {}

    public static void onBreak(BlockEvent.BreakEvent e) {
        if (!(e.getLevel() instanceof ServerLevel level) || e.getPlayer() == null || e.getPlayer().isCreative()) return;
        if (!(e.getState().getBlock() instanceof Legacy1122Block block) || !(block.legacy() instanceof IShearable shearable)) return;
        var player = e.getPlayer();
        var held = player.getMainHandItem();
        if (held.isEmpty()) return;
        boolean shears = held.getItem() instanceof ShearsItem
                || Stacks1122.fromModern(held).getItem() instanceof legacyconnector.v1_12_2.net.minecraft.item.ItemShears;
        if (!shears) return;
        try {
            var world = WorldBridge1122.wrap(level);
            var pos = legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos.mcc$of(e.getPos());
            var view = Stacks1122.view(held);
            if (!shearable.isShearable(view, world, pos)) return;
            var enchants = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            int fortune = EnchantmentHelper.getItemEnchantmentLevel(enchants.getOrThrow(Enchantments.FORTUNE), held);
            var drops = shearable.onSheared(view, world, pos, fortune);
            if (drops != null) {
                for (var drop : drops) {
                    var modern = Stacks1122.toModern(drop);
                    if (!modern.isEmpty()) net.minecraft.world.level.block.Block.popResource(level, e.getPos(), modern);
                }
            }
            held.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
            level.setBlock(e.getPos(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 11);
            e.setCanceled(true);
        } catch (RuntimeException | LinkageError ex) {
            // the block breaks the ordinary way
        } finally {
            Stacks1122.flushViews();
        }
    }
}
