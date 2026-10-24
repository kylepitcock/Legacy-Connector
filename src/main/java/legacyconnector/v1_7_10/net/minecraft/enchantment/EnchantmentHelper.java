package legacyconnector.v1_7_10.net.minecraft.enchantment;

import java.util.LinkedHashMap;
import java.util.Map;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagList;

/** Enchantment levels read from the stacks' {@code ench} lists (vanilla enchantments included). */
public class EnchantmentHelper {
    public static int getEnchantmentLevel(int id, ItemStack stack) {
        if (stack == null) return 0;
        NBTTagList list = stack.getEnchantmentTagList();
        if (list == null) return 0;
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound e = list.getCompoundTagAt(i);
            if (e.getShort("id") == id) return e.getShort("lvl");
        }
        return 0;
    }

    public static Map getEnchantments(ItemStack stack) {
        Map<Integer, Integer> out = new LinkedHashMap<>();
        NBTTagList list = stack == null ? null : stack.getEnchantmentTagList();
        if (list == null) return out;
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound e = list.getCompoundTagAt(i);
            out.put((int) e.getShort("id"), (int) e.getShort("lvl"));
        }
        return out;
    }

    public static void setEnchantments(Map enchantments, ItemStack stack) {
        NBTTagList list = new NBTTagList();
        for (Object o : enchantments.entrySet()) {
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
            NBTTagCompound e = new NBTTagCompound();
            e.setShort("id", (short) ((Number) entry.getKey()).intValue());
            e.setShort("lvl", (short) ((Number) entry.getValue()).intValue());
            list.appendTag(e);
        }
        if (list.tagCount() > 0) {
            stack.setTagInfo("ench", list);
        } else if (stack.hasTagCompound()) {
            stack.getTagCompound().removeTag("ench");
        }
    }

    public static int getMaxEnchantmentLevel(int id, ItemStack[] stacks) {
        int best = 0;
        if (stacks != null) for (ItemStack s : stacks) best = Math.max(best, getEnchantmentLevel(id, s));
        return best;
    }

    private static int held(int id, EntityLivingBase entity) {
        return entity == null ? 0 : getEnchantmentLevel(id, entity.getHeldItem());
    }

    /** Melee bonus of {@code stack} against a creature attribute (func_152377_a). */
    public static float func_152377_a(ItemStack stack, legacyconnector.v1_7_10.net.minecraft.entity.EnumCreatureAttribute attribute) {
        float total = 0.0F;
        for (Object o : getEnchantments(stack).entrySet()) {
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            int id = ((Number) e.getKey()).intValue();
            if (id >= 0 && id < Enchantment.enchantmentsList.length && Enchantment.enchantmentsList[id] != null) {
                total += Enchantment.enchantmentsList[id].func_152376_a(((Number) e.getValue()).intValue(), attribute);
            }
        }
        return total;
    }

    public static float getEnchantmentModifierLiving(EntityLivingBase attacker, EntityLivingBase target) {
        return func_152377_a(attacker == null ? null : attacker.getHeldItem(), target == null ? legacyconnector.v1_7_10.net.minecraft.entity.EnumCreatureAttribute.UNDEFINED : target.getCreatureAttribute());
    }

    /** Hit effects of the attacker's held enchantments (func_151384_a). */
    public static void func_151384_a(EntityLivingBase user, legacyconnector.v1_7_10.net.minecraft.entity.Entity attacker) {
        for (ItemStack s : new ItemStack[] {user == null ? null : user.getHeldItem()}) {
            for (Object o : getEnchantments(s).entrySet()) {
                Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
                int id = ((Number) e.getKey()).intValue();
                if (id >= 0 && id < Enchantment.enchantmentsList.length && Enchantment.enchantmentsList[id] != null) {
                    Enchantment.enchantmentsList[id].func_151367_b(user, attacker, ((Number) e.getValue()).intValue());
                }
            }
        }
    }

    public static void func_151385_b(EntityLivingBase user, legacyconnector.v1_7_10.net.minecraft.entity.Entity target) {
        ItemStack held = user == null ? null : user.getHeldItem();
        for (Object o : getEnchantments(held).entrySet()) {
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            int id = ((Number) e.getKey()).intValue();
            if (id >= 0 && id < Enchantment.enchantmentsList.length && Enchantment.enchantmentsList[id] != null) {
                Enchantment.enchantmentsList[id].func_151368_a(user, target, ((Number) e.getValue()).intValue());
            }
        }
    }

    public static int getEnchantmentModifierDamage(ItemStack[] armor, legacyconnector.v1_7_10.net.minecraft.util.DamageSource source) {
        int total = 0;
        if (armor != null) {
            for (ItemStack s : armor) {
                for (Object o : getEnchantments(s).entrySet()) {
                    Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
                    int id = ((Number) e.getKey()).intValue();
                    if (id >= 0 && id < Enchantment.enchantmentsList.length && Enchantment.enchantmentsList[id] != null) {
                        total += Enchantment.enchantmentsList[id].calcModifierDamage(((Number) e.getValue()).intValue(), source);
                    }
                }
            }
        }
        return total;
    }

    public static int getKnockbackModifier(EntityLivingBase attacker, EntityLivingBase target) {
        return held(Enchantment.knockback.effectId, attacker);
    }

    public static int getFireAspectModifier(EntityLivingBase entity) {
        return held(Enchantment.fireAspect.effectId, entity);
    }

    public static int getEfficiencyModifier(EntityLivingBase entity) {
        return held(Enchantment.efficiency.effectId, entity);
    }

    public static boolean getSilkTouchModifier(EntityLivingBase entity) {
        return held(Enchantment.silkTouch.effectId, entity) > 0;
    }

    public static int getFortuneModifier(EntityLivingBase entity) {
        return held(Enchantment.fortune.effectId, entity);
    }

    public static int getLootingModifier(EntityLivingBase entity) {
        return held(Enchantment.looting.effectId, entity);
    }

    public static int func_151386_g(EntityLivingBase entity) {
        return held(Enchantment.luckOfTheSea.effectId, entity);
    }

    public static int func_151387_h(EntityLivingBase entity) {
        return held(Enchantment.lure.effectId, entity);
    }
}
