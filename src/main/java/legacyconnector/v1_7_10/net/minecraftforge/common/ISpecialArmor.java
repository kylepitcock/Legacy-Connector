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
    }
}
