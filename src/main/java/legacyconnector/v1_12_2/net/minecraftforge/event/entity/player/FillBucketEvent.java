package legacyconnector.v1_12_2.net.minecraftforge.event.entity.player;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/** A player uses an empty or filled bucket: cancel to do nothing, or set the result to ALLOW with a filled bucket to hand that back. */
@Cancelable
@Event.HasResult
public class FillBucketEvent extends PlayerEvent {
    private final ItemStack current;
    private final World world;
    private final legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult target;
    private ItemStack result;

    public FillBucketEvent(EntityPlayer player, ItemStack current, World world, legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult target) {
        super(player);
        this.current = current;
        this.world = world;
        this.target = target;
    }

    public ItemStack getEmptyBucket() {
        return current;
    }

    public World getWorld() {
        return world;
    }

    public legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult getTarget() {
        return target;
    }

    public ItemStack getFilledBucket() {
        return result;
    }

    public void setFilledBucket(ItemStack bucket) {
        this.result = bucket;
    }
}
