package legacyconnector.v1_12_2.net.minecraft.item;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 dyes (one item, 16 colours by metadata) and bone meal. */
public class ItemDye extends Item {
    /** The RGB of each dye colour, by dye damage (0 black ... 15 white). */
    public static final int[] DYE_COLORS = {1973019, 11743532, 3887386, 5320730, 2437522, 8073150, 2651799, 11250603, 4408131, 14188952, 4312372, 14602026,
            6719955, 12801229, 15435844, 15790320};

    public ItemDye() {
        setHasSubtypes(true);
        setMaxDamage(0);
        setCreativeTab(legacyconnector.v1_12_2.net.minecraft.creativetab.CreativeTabs.MATERIALS);
    }

    /** Bone meal on a block: grows what can grow there (through the modern bone meal, which reaches legacy growables too). */
    public static boolean applyBonemeal(ItemStack stack, World worldIn, BlockPos target) {
        var level = worldIn == null ? null : worldIn.mcc$level();
        if (level == null || stack.isEmpty()) return false;
        var modern = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BONE_MEAL, stack.getCount());
        int before = modern.getCount();
        var pos = org.kylepitcock.legacyconnector.v1_12_2.bridge.Inventories1122.pos(target);
        if (!net.minecraft.world.item.BoneMealItem.growCrop(modern, level, pos)) return false;
        if (!level.isClientSide()) level.levelEvent(1505, pos, 15);
        stack.shrink(before - modern.getCount());
        return true;
    }

    public static boolean applyBonemeal(ItemStack stack, World worldIn, BlockPos target, EntityPlayer player, legacyconnector.v1_12_2.net.minecraft.util.EnumHand hand) {
        return applyBonemeal(stack, worldIn, target);
    }

    /** The bone meal particles at a position (client side). */
    public static void spawnBonemealParticles(World worldIn, BlockPos pos, int amount) {
        var level = worldIn == null ? null : worldIn.mcc$level();
        if (level != null && level.isClientSide()) {
            net.minecraft.world.item.BoneMealItem.addGrowthParticles(level, org.kylepitcock.legacyconnector.v1_12_2.bridge.Inventories1122.pos(pos), amount);
        }
    }
}
