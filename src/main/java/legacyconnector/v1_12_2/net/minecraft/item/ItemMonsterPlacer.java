package legacyconnector.v1_12_2.net.minecraft.item;

import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** 1.12.2 spawn egg: the static helpers mods use to name the entity an egg spawns (stored in {@code EntityTag.id}). */
public class ItemMonsterPlacer extends Item {
    /** Names the entity the egg spawns. */
    public static void applyEntityIdToItemStack(ItemStack stack, ResourceLocation entityId) {
        NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
        NBTTagCompound entityTag = tag.hasKey("EntityTag", 10) ? tag.getCompoundTag("EntityTag") : new NBTTagCompound();
        entityTag.setString("id", entityId.toString());
        tag.setTag("EntityTag", entityTag);
        stack.setTagCompound(tag);
    }

    /** The entity id the egg spawns, or null when it names none. */
    public static ResourceLocation getNamedIdFrom(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey("EntityTag", 10)) return null;
        NBTTagCompound entityTag = tag.getCompoundTag("EntityTag");
        return entityTag.hasKey("id", 8) ? new ResourceLocation(entityTag.getString("id")) : null;
    }
}
