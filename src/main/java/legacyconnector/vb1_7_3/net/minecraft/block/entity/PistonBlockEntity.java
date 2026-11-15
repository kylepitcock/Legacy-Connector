package legacyconnector.vb1_7_3.net.minecraft.block.entity;

import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;

/** Beta's {@code TileEntityPiston}: a view of the native moving block (what a piston pushes, its direction and progress). */
public class PistonBlockEntity extends BlockEntity {
    public PistonBlockEntity() {}

    private PistonMovingBlockEntity moving() {
        return mcc$host instanceof PistonMovingBlockEntity m ? m : null;
    }

    public int getPushedBlockId() {
        var m = moving();
        return m == null ? 0 : org.kylepitcock.legacyconnector.vb1_7_3.BlocksB173.legacyId(m.getMovedState());
    }

    public int getPushedBlockData() {
        var m = moving();
        return m == null ? 0 : org.kylepitcock.legacyconnector.vb1_7_3.BlocksB173.legacyMeta(m.getMovedState());
    }

    public boolean isExtending() { var m = moving(); return m != null && m.isExtending(); }
    public int getFacing() { var m = moving(); return m == null ? 0 : m.getDirection().get3DDataValue(); }
    public boolean isSource() { var m = moving(); return m != null && m.isSourcePiston(); }
    public float getProgress(float partial) { var m = moving(); return m == null ? 1F : m.getProgress(partial); }
    public float getRenderOffsetX(float partial) { var m = moving(); return m == null ? 0F : m.getXOff(partial); }
    public float getRenderOffsetY(float partial) { var m = moving(); return m == null ? 0F : m.getYOff(partial); }
    public float getRenderOffsetZ(float partial) { var m = moving(); return m == null ? 0F : m.getZOff(partial); }
}
