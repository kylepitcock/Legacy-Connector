package legacyconnector.v1_7_10.net.minecraft.command;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;

public interface IEntitySelector {
    IEntitySelector selectAnything = e -> e.isEntityAlive();

    boolean isEntityApplicable(Entity entity);

    /** Living entities that could put on {@code armor}: its slot is free and they pick up loot (mobs) or are players. */
    class ArmoredMob implements IEntitySelector {
        private final legacyconnector.v1_7_10.net.minecraft.item.ItemStack field_96567_c;

        public ArmoredMob(legacyconnector.v1_7_10.net.minecraft.item.ItemStack armor) {
            this.field_96567_c = armor;
        }

        @Override
        public boolean isEntityApplicable(Entity entity) {
            if (!entity.isEntityAlive() || !(entity instanceof legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase living)) return false;
            if (living.getEquipmentInSlot(legacyconnector.v1_7_10.net.minecraft.entity.EntityLiving.getArmorPosition(field_96567_c)) != null) return false;
            return living instanceof legacyconnector.v1_7_10.net.minecraft.entity.EntityLiving mob ? mob.canPickUpLoot()
                    : living instanceof legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
        }
    }
}
