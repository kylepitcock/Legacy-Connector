package legacyconnector.v1_12_2.net.minecraft.enchantment;

import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.inventory.EntityEquipmentSlot;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** Frost Walker: still water in a radius under the walker freezes to frosted ice. */
public class EnchantmentFrostWalker extends Enchantment {
    public EnchantmentFrostWalker(Enchantment.Rarity rarityIn, EntityEquipmentSlot... slots) {
        super(rarityIn, EnumEnchantmentType.ARMOR_FEET, slots);
        setName("frostWalker");
    }

    @Override
    public int getMinEnchantability(int level) {
        return level * 10;
    }

    @Override
    public int getMaxEnchantability(int level) {
        return getMinEnchantability(level) + 15;
    }

    @Override
    public boolean isTreasureEnchantment() {
        return true;
    }

    @Override
    public int getMaxLevel() {
        return 2;
    }

    /** Freezes still water within 2 + level blocks under an entity standing on the ground. */
    public static void freezeNearby(EntityLivingBase living, World worldIn, BlockPos pos, int level) {
        var entity = living == null ? null : living.mcc$entity;
        if (entity == null || worldIn == null || !entity.onGround()) return;
        var level1 = worldIn.mcc$liveLevel();
        if (level1 == null) return;
        float radius = (float) Math.min(16, 2 + level);
        var center = new net.minecraft.core.BlockPos(pos.getX(), pos.getY(), pos.getZ());
        var frosted = net.minecraft.world.level.block.Blocks.FROSTED_ICE.defaultBlockState();
        var ctx = net.minecraft.world.phys.shapes.CollisionContext.empty();
        for (var at : net.minecraft.core.BlockPos.betweenClosed(center.offset((int) -radius, -1, (int) -radius), center.offset((int) radius, -1, (int) radius))) {
            if (at.distToCenterSqr(entity.getX(), entity.getY(), entity.getZ()) > (double) (radius * radius)) continue;
            if (!level1.getBlockState(at.above()).isAir()) continue;
            var state = level1.getBlockState(at);
            if (state.is(net.minecraft.world.level.block.Blocks.WATER) && state.getValue(net.minecraft.world.level.block.LiquidBlock.LEVEL) == 0
                    && frosted.canSurvive(level1, at) && level1.isUnobstructed(frosted, at, ctx)) {
                level1.setBlockAndUpdate(at, frosted);
                level1.scheduleTick(at.immutable(), net.minecraft.world.level.block.Blocks.FROSTED_ICE, 60 + living.getRNG().nextInt(61));
            }
        }
    }

    @Override
    public boolean canApplyTogether(Enchantment ench) {
        var name = ench == null ? null : ench.getRegistryName();
        boolean strider = name != null && "minecraft".equals(name.getNamespace()) && "depth_strider".equals(name.getPath());
        return super.canApplyTogether(ench) && !strider;
    }
}
