package legacyconnector.v1_7_10.net.minecraft.item;

/** Dye: the colour tables mods index by dye metadata (0 black .. 15 white). */
public class ItemDye extends Item {
    public static final String[] dyeColorNames = {"black", "red", "green", "brown", "blue", "purple", "cyan", "silver", "gray", "pink", "lime", "yellow",
            "lightBlue", "magenta", "orange", "white"};
    /** Icon name suffixes (vanilla's dye_powder_ + these; mods build their own texture names from them, e.g. HBM's concrete_silver). */
    public static final String[] dyeIcons = {"black", "red", "green", "brown", "blue", "purple", "cyan", "silver", "gray", "pink", "lime", "yellow",
            "light_blue", "magenta", "orange", "white"};
    public static final int[] dyeColors = {1973019, 11743532, 3887386, 5320730, 2437522, 8073150, 2651799, 11250603, 4408131, 14188952, 4312372, 14602026,
            6719955, 12801229, 15435844, 15790320};

    public ItemDye() {
        setHasSubtypes(true);
        setMaxDamage(0);
    }

    /**
     * Bone meal on the block at the position (the modern bone meal logic, with Forge's bonemeal event): one item of {@code stack}
     * is used up when it worked, as in 1.7.10.
     */
    public static boolean applyBonemeal(legacyconnector.v1_7_10.net.minecraft.item.ItemStack stack, legacyconnector.v1_7_10.net.minecraft.world.World world,
            int x, int y, int z, legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player) {
        var level = world.mcc$fullLevel();
        if (level == null) return false;
        net.minecraft.world.item.ItemStack modern = stack == null || ItemStack.mcc$toModern(stack).isEmpty()
                ? new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BONE_MEAL) : ItemStack.mcc$toModern(stack);
        return net.minecraft.world.item.BoneMealItem.applyBonemeal(modern, level, new net.minecraft.core.BlockPos(x, y, z),
                player == null ? null : (net.minecraft.world.entity.player.Player) player.mcc$entity);
    }

    /** Bone meal without a player (dispensers, machines): applied as the world's fake player on the server, nothing on the client. */
    public static boolean func_150919_a(ItemStack stack, legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z) {
        if (world == null || world.isRemote || !(world.mcc$fullLevel() instanceof net.minecraft.server.level.ServerLevel)) return false;
        return applyBonemeal(stack, world, x, y, z, null);
    }

    /** The green growth particles over a block that was bone mealed ({@code count} 0 means 15). */
    public static void func_150918_a(legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z, int count) {
        if (count == 0) count = 15;
        legacyconnector.v1_7_10.net.minecraft.block.Block block = world.getBlock(x, y, z);
        if (block == null || block.getMaterial() == legacyconnector.v1_7_10.net.minecraft.block.material.Material.air) return;
        block.setBlockBoundsBasedOnState(world, x, y, z);
        java.util.Random rand = world.rand;
        for (int i = 0; i < count; ++i) {
            world.spawnParticle("happyVillager", x + rand.nextFloat(), y + rand.nextFloat() * block.getBlockBoundsMaxY(), z + rand.nextFloat(),
                    rand.nextGaussian() * 0.02D, rand.nextGaussian() * 0.02D, rand.nextGaussian() * 0.02D);
        }
    }
}
