package legacyconnector.v1_7_10.net.minecraft.item;

/** Dye: the colour tables mods index by dye metadata (0 black .. 15 white). */
public class ItemDye extends Item {
    public static final String[] dyeColorNames = {"black", "red", "green", "brown", "blue", "purple", "cyan", "silver", "gray", "pink", "lime", "yellow",
            "lightBlue", "magenta", "orange", "white"};
    public static final String[] dyeIcons = {"dye_powder_black", "dye_powder_red", "dye_powder_green", "dye_powder_brown", "dye_powder_blue",
            "dye_powder_purple", "dye_powder_cyan", "dye_powder_silver", "dye_powder_gray", "dye_powder_pink", "dye_powder_lime", "dye_powder_yellow",
            "dye_powder_light_blue", "dye_powder_magenta", "dye_powder_orange", "dye_powder_white"};
    public static final int[] dyeColors = {1973019, 11743532, 3887386, 5320730, 2437522, 8073150, 2651799, 11250603, 4408131, 14188952, 4312372, 14602026,
            6719955, 12801229, 15435844, 15790320};

    public ItemDye() {
        setHasSubtypes(true);
        setMaxDamage(0);
    }

    public static boolean applyBonemeal(legacyconnector.v1_7_10.net.minecraft.item.ItemStack stack, legacyconnector.v1_7_10.net.minecraft.world.World world,
            int x, int y, int z, legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player) {
        var level = world.mcc$fullLevel();
        if (level == null) return false;
        return net.minecraft.world.item.BoneMealItem.applyBonemeal(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BONE_MEAL),
                level, new net.minecraft.core.BlockPos(x, y, z), player == null ? null : (net.minecraft.world.entity.player.Player) player.mcc$entity);
    }
}
