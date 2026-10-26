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

    /** Respiration level of the entity's worn and held items (longer breath under water, clearer fog). */
    public static int getRespiration(EntityLivingBase entity) {
        return entity == null ? 0 : getMaxEnchantmentLevel(Enchantment.respiration.effectId, entity.getLastActiveItems());
    }

    /** Whether the entity wears or holds Aqua Affinity (normal mining speed under water). */
    public static boolean getAquaAffinityModifier(EntityLivingBase entity) {
        return entity != null && getMaxEnchantmentLevel(Enchantment.aquaAffinity.effectId, entity.getLastActiveItems()) > 0;
    }

    /**
     * The enchantments an enchanting table may roll for {@code stack} at enchantability {@code level}: effect id -> the highest
     * applicable level's {@link EnchantmentData}; null when none applies (1.7.10's table logic over the legacy enchantment list).
     */
    public static Map mapEnchantmentData(int level, ItemStack stack) {
        if (stack == null) return null;
        boolean book = stack.getItem() == legacyconnector.v1_7_10.net.minecraft.init.Items.book;
        Map<Integer, EnchantmentData> out = null;
        for (Enchantment enchantment : Enchantment.enchantmentsList) {
            if (enchantment == null) continue;
            if (!enchantment.canApplyAtEnchantingTable(stack) && !(book && enchantment.isAllowedOnBooks())) continue;
            for (int lvl = enchantment.getMinLevel(); lvl <= enchantment.getMaxLevel(); ++lvl) {
                if (level >= enchantment.getMinEnchantability(lvl) && level <= enchantment.getMaxEnchantability(lvl)) {
                    if (out == null) out = new java.util.HashMap<>();
                    out.put(enchantment.effectId, new EnchantmentData(enchantment, lvl));
                }
            }
        }
        return out;
    }

    /** The level offered in one of the enchanting table's three slots, from the bookshelf power around it (capped at 15). */
    public static int calcItemStackEnchantability(java.util.Random random, int slot, int power, ItemStack stack) {
        if (stack == null || stack.getItem() == null || stack.getItem().getItemEnchantability() <= 0) return 0;
        if (power > 15) power = 15;
        int base = random.nextInt(8) + 1 + (power >> 1) + random.nextInt(power + 1);
        return slot == 0 ? Math.max(base / 3, 1) : slot == 1 ? base * 2 / 3 + 1 : Math.max(base, power * 2);
    }

    /** 1.7.10's enchanting-table roll: a random list of compatible {@link EnchantmentData} for the stack, or null. */
    public static java.util.List buildEnchantmentList(java.util.Random random, ItemStack stack, int level) {
        if (stack == null || stack.getItem() == null) return null;
        int enchantability = stack.getItem().getItemEnchantability();
        if (enchantability <= 0) return null;
        enchantability /= 2;
        enchantability = 1 + random.nextInt((enchantability >> 1) + 1) + random.nextInt((enchantability >> 1) + 1);
        int total = enchantability + level;
        float spread = (random.nextFloat() + random.nextFloat() - 1.0F) * 0.15F;
        int modified = (int) (total * (1.0F + spread) + 0.5F);
        if (modified < 1) modified = 1;
        java.util.List<EnchantmentData> out = null;
        Map<Integer, EnchantmentData> candidates = mapEnchantmentData(modified, stack);
        if (candidates != null && !candidates.isEmpty()) {
            EnchantmentData first = (EnchantmentData) legacyconnector.v1_7_10.net.minecraft.util.WeightedRandom.getRandomItem(random, candidates.values());
            if (first != null) {
                out = new java.util.ArrayList<>();
                out.add(first);
                for (int chance = modified; random.nextInt(50) <= chance; chance >>= 1) {
                    var it = candidates.keySet().iterator();
                    while (it.hasNext()) {
                        Enchantment candidate = Enchantment.enchantmentsList[it.next()];
                        for (EnchantmentData chosen : out) {
                            if (!chosen.enchantmentobj.canApplyTogether(candidate)) {
                                it.remove();
                                break;
                            }
                        }
                    }
                    if (!candidates.isEmpty()) {
                        out.add((EnchantmentData) legacyconnector.v1_7_10.net.minecraft.util.WeightedRandom.getRandomItem(random, candidates.values()));
                    }
                }
            }
        }
        return out;
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

    /**
     * Enchants {@code stack} in place as an enchanting table would at {@code level} (modern tables: the enchantments tagged
     * {@code in_enchanting_table}); a book becomes an enchanted book. Unchanged while no server is running.
     */
    public static ItemStack addRandomEnchantment(java.util.Random random, ItemStack stack, int level) {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (stack == null || server == null) return stack;
        var registry = server.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
        var modern = stack.mcc$modern();
        var result = net.minecraft.world.item.enchantment.EnchantmentHelper.enchantItem(net.minecraft.util.RandomSource.create(random.nextLong()),
                modern, level, server.registryAccess(), registry.getTag(net.minecraft.tags.EnchantmentTags.IN_ENCHANTING_TABLE));
        if (result != modern) { // a book: vanilla turned this very stack into an enchanted book
            stack.setItem(legacyconnector.v1_7_10.net.minecraft.init.Items.enchanted_book);
            stack.mcc$modern().set(net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS,
                    result.get(net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS));
        }
        return stack;
    }
}
