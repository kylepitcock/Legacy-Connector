package legacyconnector.v1_12_2.net.minecraftforge.oredict;

import java.util.Optional;
import legacyconnector.v1_12_2.net.minecraft.item.EnumDyeColor;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;

/** Forge: dyes recognised by their ore dictionary names ({@code dyeBlack} ... {@code dyeWhite}), so mods' dyes colour like vanilla's. */
public class DyeUtils {
    private static final String[] dyeOredicts = new String[] {"dyeBlack", "dyeRed", "dyeGreen", "dyeBrown", "dyeBlue", "dyePurple", "dyeCyan", "dyeLightGray",
            "dyeGray", "dyePink", "dyeLime", "dyeYellow", "dyeLightBlue", "dyeMagenta", "dyeOrange", "dyeWhite"};

    public static boolean isDye(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        for (int id : OreDictionary.getOreIDs(stack)) if ("dye".equals(OreDictionary.getOreName(id))) return true;
        return rawDyeDamageFromStack(stack) >= 0;
    }

    /** The dye damage value (0 black ... 15 white) of a dye stack, -1 when it is none. */
    public static int rawDyeDamageFromStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return -1;
        for (int oreId : OreDictionary.getOreIDs(stack)) {
            String name = OreDictionary.getOreName(oreId);
            for (int i = 0; i < dyeOredicts.length; i++) if (dyeOredicts[i].equals(name)) return i;
        }
        return -1;
    }

    public static Optional<EnumDyeColor> metaFromStack(ItemStack stack) {
        int damage = rawDyeDamageFromStack(stack);
        return damage < 0 ? Optional.empty() : Optional.of(EnumDyeColor.byMetadata(15 - damage));
    }

    public static Optional<EnumDyeColor> colorFromStack(ItemStack stack) {
        int damage = rawDyeDamageFromStack(stack);
        return damage < 0 ? Optional.empty() : Optional.of(EnumDyeColor.byDyeDamage(damage));
    }
}
