package legacyconnector.v1_12_2.net.minecraftforge.event.entity.living;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.DamageSource;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/** A mob breaks a block (withers, endermen, ...; cancel to keep it). */
@Cancelable
public class LivingDestroyBlockEvent extends LivingEvent {
    private final BlockPos pos;
    private final legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state;

    public LivingDestroyBlockEvent(EntityLivingBase entity, BlockPos pos, legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState state) {
        super(entity);
        this.pos = pos;
        this.state = state;
    }

    public legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState getState() {
        return state;
    }

    public BlockPos getPos() {
        return pos;
    }
}
