package legacyconnector.v1_12_2.net.minecraft.tileentity;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.init.SoundEvents;
import legacyconnector.v1_12_2.net.minecraft.util.SoundCategory;

/**
 * 1.12.2 ender chest tile entity: counts the players looking into it and animates its lid (with the open and close sounds); the items are
 * the player's own ender inventory.
 */
public class TileEntityEnderChest extends TileEntity implements legacyconnector.v1_12_2.net.minecraft.util.ITickable {
    public float lidAngle;
    public float prevLidAngle;
    public int numPlayersUsing;
    private int ticksSinceSync;

    @Override
    public void update() {
        if (world == null) return;
        int x = getPos().getX(), y = getPos().getY(), z = getPos().getZ();
        if (++ticksSinceSync % 20 * 4 == 0) world.addBlockEvent(getPos(), getBlockType(), 1, numPlayersUsing);
        prevLidAngle = lidAngle;
        if (numPlayersUsing > 0 && lidAngle == 0.0F) {
            world.playSound(null, x + 0.5D, y + 0.5D, z + 0.5D, SoundEvents.BLOCK_ENDERCHEST_OPEN, SoundCategory.BLOCKS, 0.5F,
                    world.rand.nextFloat() * 0.1F + 0.9F);
        }
        if (numPlayersUsing == 0 && lidAngle > 0.0F || numPlayersUsing > 0 && lidAngle < 1.0F) {
            float before = lidAngle;
            if (numPlayersUsing > 0) lidAngle += 0.1F;
            else lidAngle -= 0.1F;
            if (lidAngle > 1.0F) lidAngle = 1.0F;
            if (lidAngle < 0.5F && before >= 0.5F) {
                world.playSound(null, x + 0.5D, y + 0.5D, z + 0.5D, SoundEvents.BLOCK_ENDERCHEST_CLOSE, SoundCategory.BLOCKS, 0.5F,
                        world.rand.nextFloat() * 0.1F + 0.9F);
            }
            if (lidAngle < 0.0F) lidAngle = 0.0F;
        }
    }

    @Override
    public boolean receiveClientEvent(int id, int type) {
        if (id == 1) {
            numPlayersUsing = type;
            return true;
        }
        return super.receiveClientEvent(id, type);
    }

    @Override
    public void invalidate() {
        updateContainingBlockInfo();
        super.invalidate();
    }

    public void openChest() {
        ++numPlayersUsing;
        if (world != null) world.addBlockEvent(getPos(), getBlockType(), 1, numPlayersUsing);
    }

    public void closeChest() {
        --numPlayersUsing;
        if (world != null) world.addBlockEvent(getPos(), getBlockType(), 1, numPlayersUsing);
    }

    public boolean canBeUsed(EntityPlayer player) {
        if (world == null || world.getTileEntity(getPos()) != this) return false;
        return player.getDistanceSq(getPos().getX() + 0.5D, getPos().getY() + 0.5D, getPos().getZ() + 0.5D) <= 64.0D;
    }
}
