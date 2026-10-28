package legacyconnector.v1_12_2.net.minecraft.enchantment;

import java.util.LinkedHashMap;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagList;

/** The enchantments of a stack, read from and written to its {@code ench} list (an enchanted book's {@code StoredEnchantments}). */
public class EnchantmentHelper {
    private static boolean isBook(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == legacyconnector.v1_12_2.net.minecraft.init.Items.ENCHANTED_BOOK;
    }

    private static NBTTagList list(ItemStack stack) {
        if (isBook(stack)) {
            NBTTagCompound tag = stack.getTagCompound();
            return tag != null ? tag.getTagList("StoredEnchantments", 10) : new NBTTagList();
        }
        return stack.getEnchantmentTagList();
    }

    public static int getEnchantmentLevel(Enchantment enchantment, ItemStack stack) {
        if (stack.isEmpty() || enchantment == null) return 0;
        int modern = mcc$modernLevel(enchantment, stack);
        if (modern > 0) return modern;
        NBTTagList list = stack.getEnchantmentTagList();
        int id = Enchantment.getEnchantmentID(enchantment);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound e = list.getCompoundTagAt(i);
            if (e.getShort("id") == id) return e.getShort("lvl");
        }
        return 0;
    }

    public static Map<Enchantment, Integer> getEnchantments(ItemStack stack) {
        Map<Enchantment, Integer> out = new LinkedHashMap<>();
        NBTTagList list = list(stack);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound e = list.getCompoundTagAt(i);
            Enchantment enchantment = Enchantment.getEnchantmentByID(e.getShort("id"));
            if (enchantment != null) out.put(enchantment, (int) e.getShort("lvl"));
        }
        return out;
    }

    public static void setEnchantments(Map<Enchantment, Integer> enchantments, ItemStack stack) {
        NBTTagList list = new NBTTagList();
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            if (entry.getKey() == null) continue;
            NBTTagCompound e = new NBTTagCompound();
            e.setShort("id", (short) Enchantment.getEnchantmentID(entry.getKey()));
            e.setShort("lvl", (short) (int) entry.getValue());
            list.appendTag(e);
        }
        if (list.tagCount() == 0) {
            if (stack.hasTagCompound()) stack.getTagCompound().removeTag(isBook(stack) ? "StoredEnchantments" : "ench");
        } else {
            stack.setTagInfo(isBook(stack) ? "StoredEnchantments" : "ench", list);
        }
    }

    /** 1.12 names of vanilla enchantments the modern game renamed. */
    private static final Map<String, String> RENAMED = Map.of("sweeping", "sweeping_edge");

    /**
     * The level of a vanilla enchantment on the modern stack a view stands for (modern stacks keep enchantments in a component, not
     * the {@code ench} tag); 0 for mod enchantments and plain stacks.
     */
    private static int mcc$modernLevel(Enchantment enchantment, ItemStack stack) {
        var backing = stack.mcc$backing();
        var name = enchantment.getRegistryName();
        if (backing == null || name == null || !"minecraft".equals(name.getNamespace())) return 0;
        String path = RENAMED.getOrDefault(name.getPath(), name.getPath());
        var enchants = backing.getItem() == net.minecraft.world.item.Items.ENCHANTED_BOOK
                ? backing.getOrDefault(net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS, net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY)
                : backing.getEnchantments();
        for (var e : enchants.entrySet()) {
            var key = e.getKey().unwrapKey();
            if (key.isPresent() && key.get().location().getNamespace().equals("minecraft") && key.get().location().getPath().equals(path)) return e.getIntValue();
        }
        return 0;
    }

    /** The highest level of the enchantment on the entity's equipment that it applies to. */
    public static int getMaxEnchantmentLevel(Enchantment enchantment, legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase entity) {
        if (enchantment == null) return 0;
        int max = 0;
        for (ItemStack s : enchantment.getEntityEquipment(entity)) max = Math.max(max, getEnchantmentLevel(enchantment, s));
        return max;
    }

    private static int mcc$level(String name, legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase entity) {
        return getMaxEnchantmentLevel(Enchantment.getEnchantmentByLocation(name), entity);
    }

    public static int getKnockbackModifier(legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase entity) {
        return mcc$level("knockback", entity);
    }

    public static int getFireAspectModifier(legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase entity) {
        return mcc$level("fire_aspect", entity);
    }

    public static int getEfficiencyModifier(legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase entity) {
        return mcc$level("efficiency", entity);
    }

    public static int getLootingModifier(legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase entity) {
        return mcc$level("looting", entity);
    }

    public static int getRespirationModifier(legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase entity) {
        return mcc$level("respiration", entity);
    }

    public static int getDepthStriderModifier(legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase entity) {
        return mcc$level("depth_strider", entity);
    }

    public static boolean getAquaAffinityModifier(legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase entity) {
        return mcc$level("aqua_affinity", entity) > 0;
    }

    public static boolean hasFrostWalkerEnchantment(legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase entity) {
        return mcc$level("frost_walker", entity) > 0;
    }

    public static float getSweepingDamageRatio(legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase entity) {
        int i = mcc$level("sweeping", entity);
        return i > 0 ? 1.0F - 1.0F / (float) (i + 1) : 0.0F;
    }

    public static boolean hasBindingCurse(ItemStack stack) {
        return getEnchantmentLevel(Enchantment.getEnchantmentByLocation("binding_curse"), stack) > 0;
    }

    public static boolean hasVanishingCurse(ItemStack stack) {
        return getEnchantmentLevel(Enchantment.getEnchantmentByLocation("vanishing_curse"), stack) > 0;
    }

    /** {@code ItemEnchantedBook.getEnchantedItemStack}: an enchanted book storing the enchantment. */
    public static ItemStack mcc$enchantedBook(EnchantmentData data) {
        ItemStack book = new ItemStack(legacyconnector.v1_12_2.net.minecraft.init.Items.ENCHANTED_BOOK);
        Map<Enchantment, Integer> stored = new LinkedHashMap<>();
        stored.put(data.enchantment, data.enchantmentLevel);
        setEnchantments(stored, book);
        return book;
    }

    /** The extra melee damage of the stack's enchantments against a kind of creature. */
    public static float getModifierForCreature(ItemStack stack, legacyconnector.v1_12_2.net.minecraft.entity.EnumCreatureAttribute creatureAttribute) {
        float total = 0.0F;
        for (Map.Entry<Enchantment, Integer> e : getEnchantments(stack).entrySet()) {
            if (e.getKey() != null) total += e.getKey().calcDamageByCreature(e.getValue(), creatureAttribute);
        }
        return total;
    }

    /** The equipment of the entity (held items and armour) whose enchantments react to it being hurt or hurting. */
    private static java.util.List<ItemStack> mcc$equipment(legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase entity) {
        java.util.List<ItemStack> out = new java.util.ArrayList<>();
        for (var slot : legacyconnector.v1_12_2.net.minecraft.inventory.EntityEquipmentSlot.values()) {
            ItemStack s = entity.getItemStackFromSlot(slot);
            if (s != null && !s.isEmpty()) out.add(s);
        }
        return out;
    }

    /** Thorns-like enchantments of the hurt entity's equipment answer the attacker ({@code Enchantment.onUserHurt}). */
    public static void applyThornEnchantments(legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase user, legacyconnector.v1_12_2.net.minecraft.entity.Entity attacker) {
        if (user == null) return;
        for (ItemStack stack : mcc$equipment(user)) {
            for (Map.Entry<Enchantment, Integer> e : getEnchantments(stack).entrySet()) {
                if (e.getKey() != null) e.getKey().onUserHurt(user, attacker, e.getValue());
            }
        }
        if (attacker instanceof legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer p) {
            ItemStack held = p.getHeldItemMainhand();
            for (Map.Entry<Enchantment, Integer> e : getEnchantments(held).entrySet()) {
                if (e.getKey() != null) e.getKey().onUserHurt(user, attacker, e.getValue());
            }
        }
    }

    /** Bane-of-arthropods-like enchantments of the attacker's equipment act on the target ({@code Enchantment.onEntityDamaged}). */
    public static void applyArthropodEnchantments(legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase user, legacyconnector.v1_12_2.net.minecraft.entity.Entity target) {
        if (user == null) return;
        for (ItemStack stack : mcc$equipment(user)) {
            for (Map.Entry<Enchantment, Integer> e : getEnchantments(stack).entrySet()) {
                if (e.getKey() != null) e.getKey().onEntityDamaged(user, target, e.getValue());
            }
        }
    }

    /** The total protection the entity's armour gives against a damage source ({@code Enchantment.calcModifierDamage}), capped as vanilla does at the caller. */
    public static int getEnchantmentModifierDamage(Iterable<ItemStack> stacks, legacyconnector.v1_12_2.net.minecraft.util.DamageSource source) {
        int total = 0;
        for (ItemStack stack : stacks) {
            for (Map.Entry<Enchantment, Integer> e : getEnchantments(stack).entrySet()) {
                if (e.getKey() != null) total += e.getKey().calcModifierDamage(e.getValue(), source);
            }
        }
        return total;
    }


    /** A random piece of the entity's equipment carrying the enchantment (EMPTY when none does). */
    public static ItemStack getEnchantedItem(Enchantment enchantment, legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase entity) {
        if (enchantment == null) return ItemStack.EMPTY;
        java.util.List<ItemStack> list = enchantment.getEntityEquipment(entity);
        if (list.isEmpty()) return ItemStack.EMPTY;
        java.util.List<ItemStack> with = new java.util.ArrayList<>();
        for (ItemStack s : list) if (!s.isEmpty() && getEnchantmentLevel(enchantment, s) > 0) with.add(s);
        return with.isEmpty() ? ItemStack.EMPTY : with.get(entity.getRNG().nextInt(with.size()));
    }

    public static int getFishingSpeedBonus(ItemStack stack) {
        return getEnchantmentLevel(Enchantment.getEnchantmentByLocation("lure"), stack);
    }

    public static int getFishingLuckBonus(ItemStack stack) {
        return getEnchantmentLevel(Enchantment.getEnchantmentByLocation("luck_of_the_sea"), stack);
    }

    /** The enchanting table level offered in a row (0, 1, 2) for a bookshelf power, as 1.12 rolled it. */
    public static int calcItemStackEnchantability(java.util.Random rand, int enchantNum, int power, ItemStack stack) {
        int enchantability = stack.getItem().getItemEnchantability();
        if (enchantability <= 0) return 0;
        if (power > 15) power = 15;
        int j = rand.nextInt(8) + 1 + (power >> 1) + rand.nextInt(power + 1);
        if (enchantNum == 0) return Math.max(j / 3, 1);
        return enchantNum == 1 ? j * 2 / 3 + 1 : Math.max(j, power * 2);
    }

    /** Enchants the stack as the enchanting table would at a level (a book becomes an enchanted book). */
    public static ItemStack addRandomEnchantment(java.util.Random random, ItemStack stack, int level, boolean allowTreasure) {
        java.util.List<EnchantmentData> list = buildEnchantmentList(random, stack, level, allowTreasure);
        boolean book = stack.getItem() == legacyconnector.v1_12_2.net.minecraft.init.Items.BOOK;
        if (book) {
            stack = new ItemStack(legacyconnector.v1_12_2.net.minecraft.init.Items.ENCHANTED_BOOK);
            Map<Enchantment, Integer> stored = new LinkedHashMap<>();
            for (EnchantmentData d : list) stored.put(d.enchantment, d.enchantmentLevel);
            setEnchantments(stored, stack);
        } else {
            for (EnchantmentData d : list) stack.addEnchantment(d.enchantment, d.enchantmentLevel);
        }
        return stack;
    }

    /** The enchantments the enchanting table would roll for the stack at a level. */
    public static java.util.List<EnchantmentData> buildEnchantmentList(java.util.Random randomIn, ItemStack itemStackIn, int level, boolean allowTreasure) {
        java.util.List<EnchantmentData> list = new java.util.ArrayList<>();
        int enchantability = itemStackIn.getItem().getItemEnchantability();
        if (enchantability <= 0) return list;
        level = level + 1 + randomIn.nextInt(enchantability / 4 + 1) + randomIn.nextInt(enchantability / 4 + 1);
        float f = (randomIn.nextFloat() + randomIn.nextFloat() - 1.0F) * 0.15F;
        level = Math.max(1, Math.round((float) level + (float) level * f));
        java.util.List<EnchantmentData> candidates = getEnchantmentDatas(level, itemStackIn, allowTreasure);
        if (!candidates.isEmpty()) {
            list.add(legacyconnector.v1_12_2.net.minecraft.util.WeightedRandom.getRandomItem(randomIn, candidates));
            while (randomIn.nextInt(50) <= level) {
                removeIncompatible(candidates, list.get(list.size() - 1));
                if (candidates.isEmpty()) break;
                list.add(legacyconnector.v1_12_2.net.minecraft.util.WeightedRandom.getRandomItem(randomIn, candidates));
                level /= 2;
            }
        }
        return list;
    }

    public static void removeIncompatible(java.util.List<EnchantmentData> list, EnchantmentData data) {
        list.removeIf(e -> !data.enchantment.isCompatibleWith(e.enchantment));
    }

    /** Every enchantment (at the highest fitting level) the table may put on the stack at a level. */
    public static java.util.List<EnchantmentData> getEnchantmentDatas(int level, ItemStack stack, boolean allowTreasure) {
        java.util.List<EnchantmentData> list = new java.util.ArrayList<>();
        boolean book = stack.getItem() == legacyconnector.v1_12_2.net.minecraft.init.Items.BOOK;
        Enchantment.getEnchantmentByID(0); // vanilla constants registered
        for (Enchantment enchantment : Enchantment.REGISTRY) {
            if (enchantment == null || enchantment.isTreasureEnchantment() && !allowTreasure) continue;
            if (!enchantment.canApplyAtEnchantingTable(stack) && !(book && enchantment.isAllowedOnBooks())) continue;
            for (int i = enchantment.getMaxLevel(); i > enchantment.getMinLevel() - 1; --i) {
                if (level >= enchantment.getMinEnchantability(i) && level <= enchantment.getMaxEnchantability(i)) {
                    list.add(new EnchantmentData(enchantment, i));
                    break;
                }
            }
        }
        return list;
    }
}
