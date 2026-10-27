package legacyconnector.v1_12_2.net.minecraft.potion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagList;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/**
 * The potion of a stack: its type in the {@code Potion} tag and extra effects in {@code CustomPotionEffects}, as in 1.12. Vanilla potion
 * items carry the type as their modern potion contents (converted with the stack).
 */
public class PotionUtils {
    private static final int EMPTY_COLOR = 16253176;

    private static PotionType empty() {
        PotionType t = PotionType.REGISTRY.getObject(new ResourceLocation("empty"));
        return t != null ? t : PotionType.mcc$vanilla("empty");
    }

    public static List<PotionEffect> getEffectsFromStack(ItemStack stack) {
        return getEffectsFromTag(stack.getTagCompound());
    }

    public static List<PotionEffect> mergeEffects(PotionType type, Collection<PotionEffect> effects) {
        List<PotionEffect> list = new ArrayList<>(type.getEffects());
        list.addAll(effects);
        return list;
    }

    public static List<PotionEffect> getEffectsFromTag(NBTTagCompound tag) {
        List<PotionEffect> list = new ArrayList<>(getPotionTypeFromNBT(tag).getEffects());
        addCustomPotionEffectToList(tag, list);
        return list;
    }

    public static List<PotionEffect> getFullEffectsFromItem(ItemStack stack) {
        return getFullEffectsFromTag(stack.getTagCompound());
    }

    public static List<PotionEffect> getFullEffectsFromTag(NBTTagCompound tag) {
        List<PotionEffect> list = new ArrayList<>();
        addCustomPotionEffectToList(tag, list);
        return list;
    }

    public static void addCustomPotionEffectToList(NBTTagCompound tag, List<PotionEffect> effects) {
        if (tag == null || !tag.hasKey("CustomPotionEffects", 9)) return;
        NBTTagList list = tag.getTagList("CustomPotionEffects", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            PotionEffect e = PotionEffect.readCustomPotionEffectFromNBT(list.getCompoundTagAt(i));
            if (e != null) effects.add(e);
        }
    }

    public static int getColor(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null && tag.hasKey("CustomPotionColor", 99)) return tag.getInteger("CustomPotionColor");
        return getPotionFromItem(stack) == empty() ? EMPTY_COLOR : getPotionColorFromEffectList(getEffectsFromStack(stack));
    }

    public static int getPotionColor(PotionType type) {
        return type == empty() ? EMPTY_COLOR : getPotionColorFromEffectList(type.getEffects());
    }

    /** The mixed colour of effects with particles, each weighted by its level (vanilla's water blue without any). */
    public static int getPotionColorFromEffectList(Collection<PotionEffect> effects) {
        if (effects.isEmpty()) return 3694022;
        float r = 0, g = 0, b = 0;
        int n = 0;
        for (PotionEffect e : effects) {
            if (!e.doesShowParticles() || e.getPotion() == null) continue;
            int color = e.getPotion().getLiquidColor();
            int level = e.getAmplifier() + 1;
            r += (float) (level * (color >> 16 & 255)) / 255.0F;
            g += (float) (level * (color >> 8 & 255)) / 255.0F;
            b += (float) (level * (color & 255)) / 255.0F;
            n += level;
        }
        if (n == 0) return 0;
        r = r / n * 255.0F;
        g = g / n * 255.0F;
        b = b / n * 255.0F;
        return (int) r << 16 | (int) g << 8 | (int) b;
    }

    public static PotionType getPotionFromItem(ItemStack stack) {
        return getPotionTypeFromNBT(stack.getTagCompound());
    }

    public static PotionType getPotionTypeFromNBT(NBTTagCompound tag) {
        if (tag == null) return empty();
        PotionType t = PotionType.getPotionTypeForName(tag.getString("Potion"));
        return t != null ? t : empty();
    }

    public static ItemStack addPotionToItemStack(ItemStack stack, PotionType type) {
        ResourceLocation name = type == null ? null : PotionType.REGISTRY.getNameForObject(type);
        if (name == null && type != null) name = type.getRegistryName();
        if (type == null || type == empty() || name == null) {
            if (stack.hasTagCompound()) {
                NBTTagCompound tag = stack.getTagCompound();
                tag.removeTag("Potion");
                if (tag.isEmpty()) stack.setTagCompound(null);
            }
        } else {
            NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
            tag.setString("Potion", name.toString());
            stack.setTagCompound(tag);
        }
        return stack;
    }

    public static ItemStack appendEffects(ItemStack stack, Collection<PotionEffect> effects) {
        if (effects.isEmpty()) return stack;
        NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
        NBTTagList list = tag.getTagList("CustomPotionEffects", 10);
        for (PotionEffect e : effects) list.appendTag(e.writeCustomPotionEffectToNBT(new NBTTagCompound()));
        tag.setTag("CustomPotionEffects", list);
        stack.setTagCompound(tag);
        return stack;
    }

    /** Adds the effect lines a potion item shows (effect, amplifier, duration and attribute changes, as the modern tooltip). */
    public static void addPotionTooltip(legacyconnector.v1_12_2.net.minecraft.item.ItemStack stack, java.util.List<String> lines, float durationFactor) {
        var modern = org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.toModern(stack);
        var contents = modern.getOrDefault(net.minecraft.core.component.DataComponents.POTION_CONTENTS,
                net.minecraft.world.item.alchemy.PotionContents.EMPTY);
        contents.addPotionTooltip(c -> lines.add(c.getString()), durationFactor, 20.0F);
    }
}
