package legacyconnector.v1_12_2.net.minecraft.item;

import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** 1.12.2 spawn egg: the static helpers mods use to name the entity an egg spawns (stored in {@code EntityTag.id}). */
public class ItemMonsterPlacer extends Item {
    /**
     * Creates the named entity at a position and spawns it (a living one with a random facing and its initial-spawn setup). Null when the name is
     * unknown or the entity cannot be created.
     */
    public static legacyconnector.v1_12_2.net.minecraft.entity.Entity spawnCreature(legacyconnector.v1_12_2.net.minecraft.world.World world,
            ResourceLocation id, double x, double y, double z) {
        legacyconnector.v1_12_2.net.minecraft.entity.Entity entity = legacyconnector.v1_12_2.net.minecraft.entity.EntityList.createEntityByIDFromName(id, world);
        if (entity == null) return null;
        if (entity instanceof legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving living) {
            float yaw = legacyconnector.v1_12_2.net.minecraft.util.math.MathHelper.wrapDegrees(world.rand.nextFloat() * 360.0F);
            living.setLocationAndAngles(x, y, z, yaw, 0.0F);
            legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase.mcc$set$rotationYawHead(living, yaw);
            legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase.mcc$set$renderYawOffset(living, yaw);
            living.onInitialSpawn(world.getDifficultyForLocation(new legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos(x, y, z)), null);
            world.spawnEntity(living);
            living.playLivingSound();
        } else {
            entity.setLocationAndAngles(x, y, z, 0.0F, 0.0F);
            world.spawnEntity(entity);
        }
        return entity;
    }

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
