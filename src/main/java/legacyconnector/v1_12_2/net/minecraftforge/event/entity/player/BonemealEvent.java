package legacyconnector.v1_12_2.net.minecraftforge.event.entity.player;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/** Bone meal is used on a block: cancel to do nothing, or set the result to ALLOW after growing it yourself (the bone meal is used up). */
@Cancelable
@Event.HasResult
public class BonemealEvent extends PlayerEvent {
    private final World world;
    private final BlockPos pos;
    private final legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState block;
    private final EnumHand hand;
    private final ItemStack stack;

    public BonemealEvent(EntityPlayer player, World world, BlockPos pos, legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState block, EnumHand hand,
            ItemStack stack) {
        super(player);
        this.world = world;
        this.pos = pos;
        this.block = block;
        this.hand = hand;
        this.stack = stack;
    }

    public World getWorld() {
        return world;
    }

    public BlockPos getPos() {
        return pos;
    }

    public legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState getBlock() {
        return block;
    }

    public EnumHand getHand() {
        return hand;
    }

    public ItemStack getStack() {
        return stack;
    }
}
