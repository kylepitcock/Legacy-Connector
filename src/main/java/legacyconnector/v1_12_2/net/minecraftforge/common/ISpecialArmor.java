package legacyconnector.v1_12_2.net.minecraftforge.common;

import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.DamageSource;

/**
 * Armor that decides how much damage it absorbs ({@link ArmorProperties}) and how it is damaged. The damage pipeline that asks it
 * belongs to the living-entity bridge.
 */
public interface ISpecialArmor {
    ArmorProperties getProperties(EntityLivingBase player, ItemStack armor, DamageSource source, double damage, int slot);

    int getArmorDisplay(EntityPlayer player, ItemStack armor, int slot);

    void damageArmor(EntityLivingBase entity, ItemStack stack, DamageSource source, int damage, int slot);

    default boolean handleUnblockableDamage(EntityLivingBase entity, ItemStack armor, DamageSource source, double damage, int slot) {
        return false;
    }

    class ArmorProperties implements Comparable<ArmorProperties> {
        public int Priority = 0;
        public int AbsorbMax = Integer.MAX_VALUE;
        public double AbsorbRatio = 0;
        public double Armor = 0;
        public double Toughness = 0;
        public int Slot = 0;

        public ArmorProperties(int priority, double ratio, int max) {
            this.Priority = priority;
            this.AbsorbRatio = ratio;
            this.Armor = 0;
            this.Toughness = 0;
            this.AbsorbMax = max;
        }

        @Override
        public int compareTo(ArmorProperties o) {
            if (o.Priority != Priority) return o.Priority - Priority;
            double left = (AbsorbRatio == 0 ? 0 : AbsorbMax * 100.0D / AbsorbRatio);
            double right = (o.AbsorbRatio == 0 ? 0 : o.AbsorbMax * 100.0D / o.AbsorbRatio);
            return (int) (left - right);
        }

        @Override
        public String toString() {
            return String.format("%d, %d, %f, %d", Priority, AbsorbMax, AbsorbRatio, AbsorbRatio == 0 ? 0 : (int) (AbsorbMax * 100.0D / AbsorbRatio));
        }

        /**
         * Forge's damage pipeline for armour: every worn piece (ISpecialArmor properties, or plain armour's points) absorbs its share of
         * {@code damage}, worn pieces are damaged by what they took, and the armour and toughness formula reduces the rest.
         */
        public static float applyArmor(EntityLivingBase entity, legacyconnector.v1_12_2.net.minecraft.util.NonNullList<ItemStack> inventory, DamageSource source,
                double damage) {
            double totalArmor = 0;
            double totalToughness = 0;
            java.util.ArrayList<ArmorProperties> dmgVals = new java.util.ArrayList<>();
            for (int x = 0; x < inventory.size(); x++) {
                ItemStack stack = inventory.get(x);
                if (stack.isEmpty()) continue;
                ArmorProperties prop = null;
                if (stack.getItem() instanceof ISpecialArmor armor) {
                    prop = armor.getProperties(entity, stack, source, damage, x).copy();
                    totalArmor += prop.Armor;
                    totalToughness += prop.Toughness;
                } else if (stack.getItem() instanceof legacyconnector.v1_12_2.net.minecraft.item.ItemArmor armor && !source.isUnblockable()) {
                    prop = new ArmorProperties(0, 0, Integer.MAX_VALUE);
                    prop.Armor = armor.damageReduceAmount;
                    prop.Toughness = armor.toughness;
                    totalArmor += prop.Armor;
                    totalToughness += prop.Toughness;
                }
                if (prop != null) {
                    prop.Slot = x;
                    dmgVals.add(prop);
                }
            }
            if (!dmgVals.isEmpty()) {
                ArmorProperties[] props = dmgVals.toArray(new ArmorProperties[0]);
                standardizeList(props, damage);
                int level = props[0].Priority;
                double ratio = 0;
                for (ArmorProperties prop : props) {
                    if (level != prop.Priority) {
                        damage -= (damage * ratio);
                        ratio = 0;
                        level = prop.Priority;
                    }
                    ratio += prop.AbsorbRatio;
                    double absorb = damage * prop.AbsorbRatio;
                    if (absorb > 0) {
                        ItemStack stack = inventory.get(prop.Slot);
                        int itemDamage = (int) Math.max(1, absorb);
                        if (stack.getItem() instanceof ISpecialArmor armor) armor.damageArmor(entity, stack, source, itemDamage, prop.Slot);
                        else stack.damageItem(itemDamage, entity);
                        if (stack.isEmpty()) inventory.set(prop.Slot, ItemStack.EMPTY);
                    }
                }
                damage -= (damage * ratio);
            }
            return legacyconnector.v1_12_2.net.minecraft.util.CombatRules.getDamageAfterAbsorb((float) damage, (float) totalArmor, (float) totalToughness);
        }

        /** Sorts the pieces by priority and scales each priority group so its ratios sum to at most 1 and no piece absorbs more than its maximum. */
        private static void standardizeList(ArmorProperties[] armor, double damage) {
            java.util.Arrays.sort(armor);
            int start = 0;
            while (start < armor.length) {
                int end = start;
                while (end + 1 < armor.length && armor[end + 1].Priority == armor[start].Priority) end++;
                double total = 0;
                for (int y = start; y <= end; y++) total += armor[y].AbsorbRatio;
                for (int y = start; y <= end; y++) {
                    if (total > 1) armor[y].AbsorbRatio = armor[y].AbsorbRatio / total;
                    if (damage > 0 && damage * armor[y].AbsorbRatio > armor[y].AbsorbMax) armor[y].AbsorbRatio = armor[y].AbsorbMax / damage;
                }
                // what this group absorbs is no longer there for the next one
                double group = 0;
                for (int y = start; y <= end; y++) group += armor[y].AbsorbRatio;
                damage -= damage * group;
                start = end + 1;
            }
        }

        public ArmorProperties copy() {
            ArmorProperties copy = new ArmorProperties(Priority, AbsorbRatio, AbsorbMax);
            copy.Armor = Armor;
            copy.Toughness = Toughness;
            copy.Slot = Slot;
            return copy;
        }
    }
}
