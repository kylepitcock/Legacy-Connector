package legacyconnector.v1_12_2.net.minecraftforge.event.entity.player;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/** A hoe is used on a block: cancel to do nothing, or set the result to ALLOW after tilling it yourself (the hoe is damaged). */
@Cancelable
@Event.HasResult
public class UseHoeEvent extends PlayerEvent {
    private final ItemStack current;
    private final World world;
    private final BlockPos pos;

    public UseHoeEvent(EntityPlayer player, ItemStack current, World world, BlockPos pos) {
        super(player);
        this.current = current;
        this.world = world;
        this.pos = pos;
    }

    public ItemStack getCurrent() {
        return current;
    }

    public World getWorld() {
        return world;
    }

    public BlockPos getPos() {
        return pos;
    }
}
