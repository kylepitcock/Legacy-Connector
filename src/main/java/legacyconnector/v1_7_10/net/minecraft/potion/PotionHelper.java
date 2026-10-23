package legacyconnector.v1_7_10.net.minecraft.potion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * The 1.7.10 brewing arithmetic: a potion is a 15-bit number, every ingredient is a small expression over its bits
 * and each effect lists the bit pattern that produces it ({@link #potionRequirements}). Mods register effects by
 * adding to the two maps. The modern brewing stand does not use this, so it serves legacy code that calls it.
 */
public class PotionHelper {
    public static final String field_77924_a = null;
    public static final String sugarEffect = "-0+1-2-3&4-4+13";
    public static final String ghastTearEffect = "+0-1-2-3&4-4+13";
    public static final String spiderEyeEffect = "-0-1+2-3&4-4+13";
    public static final String fermentedSpiderEyeEffect = "-0+3-4+13";
    public static final String speckledMelonEffect = "+0-1+2-3&4-4+13";
    public static final String blazePowderEffect = "+0-1-2+3&4-4+13";
    public static final String magmaCreamEffect = "+0+1-2-3&4-4+13";
    public static final String redstoneEffect = "-5+6-7";
    public static final String glowstoneEffect = "+5-6-7";
    public static final String gunpowderEffect = "+14&13-13";
    public static final String goldenCarrotEffect = "-0+1+2-3+13&4-4";
    public static final String field_151423_m = "+0-1+2-3+13&4-4";

    public static final HashMap potionRequirements = new HashMap();
    public static final HashMap potionAmplifiers = new HashMap();
    private static final HashMap<Integer, Integer> potionColorCache = new HashMap<>();

    static {
        potionRequirements.put(Potion.regeneration.getId(), "0 & !1 & !2 & !3 & 0+6");
        potionRequirements.put(Potion.moveSpeed.getId(), "!0 & 1 & !2 & !3 & 1+6");
        potionRequirements.put(Potion.fireResistance.getId(), "0 & 1 & !2 & !3 & 0+6");
        potionRequirements.put(Potion.heal.getId(), "0 & !1 & 2 & !3");
        potionRequirements.put(Potion.poison.getId(), "!0 & !1 & 2 & !3 & 2+6");
        potionRequirements.put(Potion.weakness.getId(), "!0 & !1 & !2 & 3 & 3+6");
        potionRequirements.put(Potion.harm.getId(), "!0 & !1 & 2 & 3");
        potionRequirements.put(Potion.moveSlowdown.getId(), "!0 & 1 & !2 & 3 & 3+6");
        potionRequirements.put(Potion.damageBoost.getId(), "0 & !1 & !2 & 3 & 3+6");
        potionRequirements.put(Potion.nightVision.getId(), "!0 & 1 & 2 & !3 & 2+6");
        potionRequirements.put(Potion.invisibility.getId(), "!0 & 1 & 2 & 3 & 2+6");
        potionAmplifiers.put(Potion.moveSpeed.getId(), "5");
        potionAmplifiers.put(Potion.digSpeed.getId(), "5");
        potionAmplifiers.put(Potion.damageBoost.getId(), "5");
        potionAmplifiers.put(Potion.regeneration.getId(), "5");
        potionAmplifiers.put(Potion.harm.getId(), "5");
        potionAmplifiers.put(Potion.heal.getId(), "5");
        potionAmplifiers.put(Potion.resistance.getId(), "5");
        potionAmplifiers.put(Potion.poison.getId(), "5");
        potionAmplifiers.put(Potion.jump.getId(), "5");
    }

    public static boolean checkFlag(int meta, int bit) {
        return (meta & 1 << bit) != 0;
    }

    private static int isFlagSet(int meta, int bit) {
        return checkFlag(meta, bit) ? 1 : 0;
    }

    private static int isFlagUnset(int meta, int bit) {
        return checkFlag(meta, bit) ? 0 : 1;
    }

    public static int func_77909_a(int meta) {
        return func_77908_a(meta, 5, 4, 3, 2, 1);
    }

    public static int func_77908_a(int meta, int a, int b, int c, int d, int e) {
        return (checkFlag(meta, a) ? 16 : 0) | (checkFlag(meta, b) ? 8 : 0) | (checkFlag(meta, c) ? 4 : 0)
                | (checkFlag(meta, d) ? 2 : 0) | (checkFlag(meta, e) ? 1 : 0);
    }

    /** Colour of the liquid for a potion value. */
    public static int func_77915_a(int meta, boolean splash) {
        if (splash) return calcPotionLiquidColor(getPotionEffects(meta, true));
        synchronized (potionColorCache) {
            return potionColorCache.computeIfAbsent(meta, m -> calcPotionLiquidColor(getPotionEffects(m, false)));
        }
    }

    public static int calcPotionLiquidColor(Collection effects) {
        int base = 3694022;
        if (effects == null || effects.isEmpty()) return base;
        float r = 0, g = 0, b = 0, total = 0;
        for (Object o : effects) {
            PotionEffect effect = (PotionEffect) o;
            int color = Potion.potionTypes[effect.getPotionID()].getLiquidColor();
            for (int i = 0; i <= effect.getAmplifier(); i++) {
                r += (color >> 16 & 255) / 255.0F;
                g += (color >> 8 & 255) / 255.0F;
                b += (color & 255) / 255.0F;
                total++;
            }
        }
        return (int) (r / total * 255.0F) << 16 | (int) (g / total * 255.0F) << 8 | (int) (b / total * 255.0F);
    }

    public static List getPotionEffects(int meta, boolean splash) {
        List<PotionEffect> list = null;
        for (Potion potion : Potion.potionTypes) {
            if (potion == null || splash && potion.isUsable()) continue;
            String requirement = (String) potionRequirements.get(potion.getId());
            if (requirement == null) continue;
            int level = parse(requirement, 0, requirement.length(), meta);
            if (level <= 0) continue;
            int amplifier = 0;
            String amplifierExpression = (String) potionAmplifiers.get(potion.getId());
            if (amplifierExpression != null) {
                amplifier = parse(amplifierExpression, 0, amplifierExpression.length(), meta);
                if (amplifier < 0) amplifier = 0;
            }
            int duration;
            if (potion.isInstant()) {
                duration = 1;
            } else {
                duration = 1200 * (level * 3 + (level - 1) * 2);
                duration >>= amplifier;
                duration = (int) Math.round(duration * potion.getEffectiveness());
                if ((meta & 16384) != 0) duration = (int) Math.round(duration * 0.75D + 0.5D);
            }
            if (list == null) list = new ArrayList<>();
            list.add(new PotionEffect(potion.getId(), duration, amplifier));
        }
        return list;
    }

    private static int term(boolean not, boolean hasMultiplier, boolean minus, int compare, int bit, int multiplier, int meta) {
        int value;
        if (not) {
            value = isFlagUnset(meta, bit);
        } else if (compare != -1) {
            int bits = Integer.bitCount(meta & 32767);
            value = compare == 0 && bits == bit || compare == 1 && bits > bit || compare == 2 && bits < bit ? 1 : 0;
        } else {
            value = isFlagSet(meta, bit);
        }
        if (hasMultiplier) value *= multiplier;
        return minus ? -value : value;
    }

    /** Evaluates a requirement expression ('|' is the strongest, then '&', then sums of terms) against a potion value. */
    private static int parse(String s, int start, int end, int meta) {
        if (start >= s.length() || end < 0 || start >= end) return 0;
        int or = s.indexOf('|', start);
        if (or >= 0 && or < end) {
            int left = parse(s, start, or - 1, meta);
            if (left > 0) return left;
            return Math.max(parse(s, or + 1, end, meta), 0);
        }
        int and = s.indexOf('&', start);
        if (and >= 0 && and < end) {
            int left = parse(s, start, and - 1, meta);
            if (left <= 0) return 0;
            int right = parse(s, and + 1, end, meta);
            return right <= 0 ? 0 : Math.max(left, right);
        }
        boolean star = false, hasMultiplier = false, hasBit = false, not = false, minus = false;
        int compare = -1, bit = 0, multiplier = 0, sum = 0;
        for (int i = start; i < end; i++) {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9') {
                if (star) {
                    multiplier = c - '0';
                    hasMultiplier = true;
                } else {
                    bit = bit * 10 + (c - '0');
                    hasBit = true;
                }
            } else if (c == '*') {
                star = true;
            } else if (c == '!' || c == '-' || c == '+' || c == '=' || c == '<' || c == '>') {
                if (hasBit) {
                    sum += term(not, hasMultiplier, minus, compare, bit, multiplier, meta);
                    hasBit = hasMultiplier = star = minus = not = false;
                    bit = multiplier = 0;
                    compare = -1;
                }
                if (c == '!') not = true;
                else if (c == '-') minus = true;
                else if (c == '=') compare = 0;
                else if (c == '<') compare = 2;
                else if (c == '>') compare = 1;
            }
        }
        if (hasBit) sum += term(not, hasMultiplier, minus, compare, bit, multiplier, meta);
        return sum;
    }

    private static int brewBitOperations(int meta, int bit, boolean unset, boolean invert, boolean require) {
        if (require) {
            if (!checkFlag(meta, bit)) return 0;
        } else if (unset) {
            meta &= ~(1 << bit);
        } else if (invert) {
            meta = (meta & 1 << bit) == 0 ? meta | 1 << bit : meta & ~(1 << bit);
        } else {
            meta |= 1 << bit;
        }
        return meta;
    }

    /** The potion value after adding an ingredient whose effect expression is {@code expression}. */
    public static int applyIngredient(int meta, String expression) {
        if (expression == null) return meta; // an item with no potion effect
        boolean hasBit = false, invert = false, unset = false, require = false;
        int bit = 0;
        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            if (c >= '0' && c <= '9') {
                bit = bit * 10 + (c - '0');
                hasBit = true;
            } else if (c == '!' || c == '-' || c == '+' || c == '&') {
                if (hasBit) {
                    meta = brewBitOperations(meta, bit, unset, invert, require);
                    hasBit = invert = unset = require = false;
                    bit = 0;
                }
                if (c == '!') invert = true;
                else if (c == '-') unset = true;
                else if (c == '&') require = true;
            }
        }
        if (hasBit) meta = brewBitOperations(meta, bit, unset, invert, require);
        return meta & 32767;
    }
}
