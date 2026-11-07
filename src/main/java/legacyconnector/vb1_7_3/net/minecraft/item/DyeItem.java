package legacyconnector.vb1_7_3.net.minecraft.item;

/** Beta dye metadata tables and subtype naming, also used by mods' colored block items. */
public class DyeItem extends Item {
    public static final String[] names = {
            "black", "red", "green", "brown", "blue", "purple", "cyan", "silver",
            "gray", "pink", "lime", "yellow", "lightBlue", "magenta", "orange", "white"
    };
    // Original Beta colors (including the shared cyan/silver entry), not the later Minecraft dye palette.
    public static final int[] colors = {
            1973019, 11743532, 3887386, 5320730, 2437522, 8073150, 2651799, 2651799,
            4408131, 14188952, 4312372, 14602026, 6719955, 12801229, 15435844, 15790320
    };

    public DyeItem(int id) {
        super(id);
        setHasSubtypes(true);
        setMaxDamage(0);
    }

    @Override
    public int getTextureId(int meta) {
        return textureId + (meta % 8) * 16 + meta / 8;
    }

    @Override
    public String getTranslationKey(ItemStack stack) {
        return super.getTranslationKey() + "." + names[stack.getDamage()];
    }
}
