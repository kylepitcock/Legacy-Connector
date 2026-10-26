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

    /** {@code field_77836_a}: effects per potion value, as the brewing tables give them. */
    private java.util.HashMap effectCache = new java.util.HashMap();

    /** The effects of a potion value (the 1.7.10 brewing tables of {@code PotionHelper}); null when it has none. */
    @SuppressWarnings("unchecked")
    public java.util.List<legacyconnector.v1_7_10.net.minecraft.potion.PotionEffect> getEffects(int meta) {
        synchronized (effectCache) {
            Object cached = effectCache.get(meta);
            if (cached == null && !effectCache.containsKey(meta)) {
                cached = legacyconnector.v1_7_10.net.minecraft.potion.PotionHelper.getPotionEffects(meta, false);
                effectCache.put(meta, cached);
            }
            return (java.util.List<legacyconnector.v1_7_10.net.minecraft.potion.PotionEffect>) cached;
        }
    }

    /** The liquid colour of a potion value. */
    public int getColorFromDamage(int meta) {
        return legacyconnector.v1_7_10.net.minecraft.potion.PotionHelper.func_77915_a(meta, false);
    }

    /** Pass 0 is the liquid (the potion's colour), pass 1 the bottle (untinted). Modern potions are tinted by their contents. */
    public int getColorFromItemStack(ItemStack stack, int pass) {
        if (pass > 0) return 16777215;
        var contents = stack == null ? null : ItemStack.mcc$toModern(stack).get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
        if (contents != null) return contents.getColor() & 0xFFFFFF;
        return getColorFromDamage(stack == null ? 0 : stack.getItemDamage());
    }

    /** Whether the potion value has an instant effect (healing, harming). */
    public boolean isEffectInstant(int meta) {
        java.util.List<legacyconnector.v1_7_10.net.minecraft.potion.PotionEffect> effects = getEffects(meta);
        if (effects == null) return false;
        for (var effect : effects) {
            var types = legacyconnector.v1_7_10.net.minecraft.potion.Potion.potionTypes;
            int id = effect.getPotionID();
            var potion = id >= 0 && id < types.length ? types[id] : null;
            if (potion != null && potion.isInstant()) return true;
        }
        return false;
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

    /**
     * The effects of a potion stack: its {@code CustomPotionEffects} tag, a modern potion's contents, or else the effects of its
     * damage value.
     */
    public java.util.List<legacyconnector.v1_7_10.net.minecraft.potion.PotionEffect> getEffects(ItemStack stack) {
        if (stack == null) return null;
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey("CustomPotionEffects", 9)) {
            java.util.List<legacyconnector.v1_7_10.net.minecraft.potion.PotionEffect> out = new java.util.ArrayList<>();
            var list = stack.getTagCompound().getTagList("CustomPotionEffects", 10);
            for (int i = 0; i < list.tagCount(); ++i) {
                var effect = legacyconnector.v1_7_10.net.minecraft.potion.PotionEffect.readCustomPotionEffectFromNBT(list.getCompoundTagAt(i));
                if (effect != null) out.add(effect);
            }
            return out;
        }
        var contents = ItemStack.mcc$toModern(stack).get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
        if (contents != null && contents.hasEffects()) {
            java.util.List<legacyconnector.v1_7_10.net.minecraft.potion.PotionEffect> out = new java.util.ArrayList<>();
            for (var instance : contents.getAllEffects()) out.add(legacyconnector.v1_7_10.net.minecraft.potion.PotionEffect.mcc$wrap(instance));
            return out;
        }
        return getEffects(stack.getItemDamage());
    }
}
