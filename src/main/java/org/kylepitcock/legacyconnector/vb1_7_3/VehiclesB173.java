package org.kylepitcock.legacyconnector.vb1_7_3;

import legacyconnector.vb1_7_3.net.minecraft.entity.vehicle.BoatEntity;
import legacyconnector.vb1_7_3.net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;

/** Minecarts and boats of Beta mods over the native vehicles: views of native ones, and native ones for a mod's {@code new MinecartEntity(...)}. */
public final class VehiclesB173 {
    private VehiclesB173() {}

    /** The typed view of a native cart, with Beta's cart type and wobble. */
    public static MinecartEntity view(legacyconnector.vb1_7_3.net.minecraft.world.World world, AbstractMinecart cart) {
        var view = new MinecartEntity(world);
        view.type = cart instanceof net.minecraft.world.entity.vehicle.MinecartChest ? 1 : cart instanceof net.minecraft.world.entity.vehicle.MinecartFurnace ? 2 : 0;
        view.damageWobbleTicks = cart.getHurtTime();
        view.damageWobbleStrength = (int) cart.getDamage();
        view.damageWobbleSide = cart.getHurtDir();
        return view;
    }

    public static BoatEntity view(legacyconnector.vb1_7_3.net.minecraft.world.World world, Boat boat) {
        var view = new BoatEntity(world);
        view.damageWobbleTicks = boat.getHurtTime();
        view.damageWobbleStrength = (int) boat.getDamage();
        view.damageWobbleSide = boat.getHurtDir();
        return view;
    }

    /** Spawns the native vehicle for a plain Beta cart or boat (a subclass keeps its own callbacks and is hosted elsewhere). */
    public static boolean spawn(legacyconnector.vb1_7_3.net.minecraft.entity.Entity original) {
        var world = original.world;
        if (world == null || world.mcc$level() == null || original.mcc$modern != null) return false;
        net.minecraft.world.entity.Entity nativeEntity;
        if (original instanceof MinecartEntity cart) {
            EntityType<? extends AbstractMinecart> type = switch (cart.type) {
                case 1 -> EntityType.CHEST_MINECART;
                case 2 -> EntityType.FURNACE_MINECART;
                default -> EntityType.MINECART;
            };
            nativeEntity = type.create(world.mcc$level());
        } else {
            nativeEntity = EntityType.BOAT.create(world.mcc$level());
        }
        if (nativeEntity == null) return false;
        nativeEntity.moveTo(original.x, original.y - original.standingEyeHeight, original.z, original.yaw, original.pitch);
        if (!world.mcc$spawnNative(nativeEntity)) return false;
        original.mcc$modern = nativeEntity;
        EntityViewsB173.spawned(nativeEntity, original);
        return true;
    }
}
