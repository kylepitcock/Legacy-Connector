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

        public ArmorProperties copy() {
            ArmorProperties copy = new ArmorProperties(Priority, AbsorbRatio, AbsorbMax);
            copy.Armor = Armor;
            copy.Toughness = Toughness;
            copy.Slot = Slot;
            return copy;
        }
    }
}
