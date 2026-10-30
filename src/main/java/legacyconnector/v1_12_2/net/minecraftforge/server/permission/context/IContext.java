package legacyconnector.v1_12_2.net.minecraftforge.server.permission.context;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** Where and on what a permission is checked: the world, the player and keyed values (position, target, ...). */
public interface IContext {
    World getWorld();

    EntityPlayer getPlayer();

    <T> T get(ContextKey<T> key);

    boolean has(ContextKey<?> key);
}
