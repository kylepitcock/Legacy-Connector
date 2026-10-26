package legacyconnector.v1_7_10.net.minecraft.item;

/**
 * Generated shell of the 1.7.10 class {@code net.minecraft.item.ItemPotion}: correct type hierarchy for {@code instanceof} checks and
 * subclassing, legacy constructors, inherited default behaviour. Vanilla-specific logic is not ported yet.
 * Regenerate with tools/gen_vanilla_shims.py; replace with a hand-written class to add behaviour.
 */
public class ItemPotion extends legacyconnector.v1_7_10.net.minecraft.item.Item {
    public ItemPotion(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public static boolean isSplash(int meta) {
        return (meta & 16384) != 0;
    }

    /** The effects of a potion's metadata; the legacy brewing tables are not ported, so every potion lists none. */
    public java.util.List<legacyconnector.v1_7_10.net.minecraft.potion.PotionEffect> getEffects(int meta) {
        return new java.util.ArrayList<>();
    }

    /** Vanilla's potion icons by their short names ("bottle_drinkable", "bottle_splash", "overlay"); null for others. */
    public static legacyconnector.v1_7_10.net.minecraft.util.IIcon func_94589_d(String name) {
        String texture = switch (name) {
            case "bottle_drinkable" -> "potion_bottle_drinkable";
            case "bottle_splash" -> "potion_bottle_splash";
            case "overlay" -> "potion_overlay";
            default -> null;
        };
        return texture == null ? null : IconHolder.ICONS.registerIcon("minecraft:" + texture);
    }

    private static final class IconHolder {
        static final legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureMap ICONS =
                new legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureMap(1, "textures/items");
    }

    protected ItemPotion() {
        super();
    }

    public java.util.List<legacyconnector.v1_7_10.net.minecraft.potion.PotionEffect> getEffects(ItemStack stack) {
        return getEffects(stack.getItemDamage());
    }
}
