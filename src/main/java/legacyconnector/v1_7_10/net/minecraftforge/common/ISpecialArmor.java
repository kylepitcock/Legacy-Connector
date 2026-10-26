package legacyconnector.v1_7_10.net.minecraftforge.common;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.util.DamageSource;

/** Armor that absorbs damage by its own rules (Forge). The modern armor bridge reads the armor value; the rules are kept for mods that call them. */
public interface ISpecialArmor {
    ArmorProperties getProperties(EntityLivingBase player, ItemStack armor, DamageSource source, double damage, int slot);

    int getArmorDisplay(EntityPlayer player, ItemStack armor, int slot);

    void damageArmor(EntityLivingBase entity, ItemStack stack, DamageSource source, int damage, int slot);

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
            this.AbsorbMax = max;
        }

        @Override
        public int compareTo(ArmorProperties o) {
            if (o.Priority != Priority) return o.Priority - Priority;
            double left = AbsorbRatio == 0 ? 0 : AbsorbMax / AbsorbRatio;
            double right = o.AbsorbRatio == 0 ? 0 : o.AbsorbMax / o.AbsorbRatio;
            return Double.compare(left, right);
        }

        public static void applyArmor(EntityLivingBase entity, ItemStack[] inventory, DamageSource source, double damage) {
            // The modern damage pipeline applies armor itself.
        }

        public ArmorProperties copy() {
            ArmorProperties copy = new ArmorProperties(Priority, AbsorbRatio, AbsorbMax);
            copy.Armor = Armor;
            copy.Toughness = Toughness;
            copy.Slot = Slot;
            return copy;
        }

        /**
         * Forge's armor calculation over {@code inventory} (armor slots): each piece absorbs its share of the damage
         * (ISpecialArmor by its own properties, plain armor by its defence, blocked by unblockable damage), by priority,
         * and takes durability damage for what it absorbed. The damage left over is returned; pieces that break are removed.
         */
        public static float ApplyArmor(EntityLivingBase entity, ItemStack[] inventory, DamageSource source, double damage) {
            damage *= 25;
            java.util.List<ArmorProperties> values = new java.util.ArrayList<>();
            for (int x = 0; x < inventory.length; x++) {
                ItemStack stack = inventory[x];
                if (stack == null || stack.getItem() == null) continue;
                ArmorProperties prop = null;
                if (stack.getItem() instanceof ISpecialArmor armor) {
                    ArmorProperties p = armor.getProperties(entity, stack, source, damage / 25.0D, x);
                    prop = p == null ? null : p.copy();
                } else if (stack.getItem() instanceof legacyconnector.v1_7_10.net.minecraft.item.ItemArmor armor && !source.isUnblockable()) {
                    prop = new ArmorProperties(0, armor.damageReduceAmount / 25.0D, armor.getMaxDamage() + 1 - stack.getItemDamage());
                }
                if (prop != null) {
                    prop.Slot = x;
                    values.add(prop);
                }
            }
            if (!values.isEmpty()) {
                ArmorProperties[] props = values.toArray(new ArmorProperties[0]);
                standardizeList(props, damage);
                int level = props[0].Priority;
                double ratio = 0;
                for (ArmorProperties prop : props) {
                    if (level != prop.Priority) {
                        damage -= damage * ratio;
                        ratio = 0;
                        level = prop.Priority;
                    }
                    ratio += prop.AbsorbRatio;
                    double absorb = damage * prop.AbsorbRatio;
                    if (absorb > 0) {
                        ItemStack stack = inventory[prop.Slot];
                        int itemDamage = (int) (absorb / 25.0D < 1 ? 1 : absorb / 25.0D);
                        if (stack.getItem() instanceof ISpecialArmor armor) armor.damageArmor(entity, stack, source, itemDamage, prop.Slot);
                        else stack.damageItem(itemDamage, entity);
                        if (stack.mcc$count() <= 0) inventory[prop.Slot] = null;
                    }
                }
                damage -= damage * ratio;
            }
            return (float) (damage / 25.0D);
        }

        /** Sorts by priority and scales each priority group's ratios so they absorb at most all the damage and each at most its maximum. */
        private static void standardizeList(ArmorProperties[] armor, double damage) {
            java.util.Arrays.sort(armor);
            int start = 0;
            double total = 0;
            for (int x = 0; x < armor.length; x++) {
                total += armor[x].AbsorbRatio;
                if (x == armor.length - 1 || armor[x].Priority != armor[x + 1].Priority) {
                    for (int y = start; y <= x; y++) {
                        double ratio = total > 1 ? armor[y].AbsorbRatio / total : armor[y].AbsorbRatio;
                        armor[y].AbsorbRatio = ratio * damage > armor[y].AbsorbMax ? (double) armor[y].AbsorbMax / damage : ratio;
                    }
                    start = x + 1;
                    total = 0;
                }
            }
        }
    }
}
