package legacyconnector.v1_12_2.net.minecraft.entity.ai;

import legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityOcelot;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** A tamed cat walks to a chest nobody uses, a lit furnace or the foot of a bed and sits on it. */
public class EntityAIOcelotSit extends EntityAIMoveToBlock {
    private final EntityOcelot ocelot;

    public EntityAIOcelotSit(EntityOcelot ocelot, double speed) {
        super(ocelot, speed, 8);
        this.ocelot = ocelot;
    }

    @Override
    public boolean shouldExecute() {
        return ocelot.isTamed() && !ocelot.isSitting() && super.shouldExecute();
    }

    @Override
    public void startExecuting() {
        super.startExecuting();
        if (ocelot.getAISit() != null) ocelot.getAISit().setSitting(false);
    }

    @Override
    public void resetTask() {
        super.resetTask();
        ocelot.setSitting(false);
    }

    @Override
    public void updateTask() {
        super.updateTask();
        if (ocelot.getAISit() != null) ocelot.getAISit().setSitting(false);
        if (!getIsAboveDestination()) ocelot.setSitting(false);
        else if (!ocelot.isSitting()) ocelot.setSitting(true);
    }

    @Override
    protected boolean shouldMoveTo(World world, BlockPos pos) {
        if (world == null || !world.isAirBlock(pos.up())) return false;
        var access = world.mcc$access();
        if (access == null) return false;
        var p = pos.mcc$modern();
        var state = access.getBlockState(p);
        if (state.getBlock() instanceof net.minecraft.world.level.block.ChestBlock) {
            return net.minecraft.world.level.block.entity.ChestBlockEntity.getOpenCount(access, p) < 1;
        }
        if (state.getBlock() instanceof net.minecraft.world.level.block.FurnaceBlock) {
            return state.getValue(net.minecraft.world.level.block.FurnaceBlock.LIT);
        }
        if (state.getBlock() instanceof net.minecraft.world.level.block.BedBlock) {
            return state.getValue(net.minecraft.world.level.block.BedBlock.PART) != net.minecraft.world.level.block.state.properties.BedPart.HEAD;
        }
        return false;
    }
}
