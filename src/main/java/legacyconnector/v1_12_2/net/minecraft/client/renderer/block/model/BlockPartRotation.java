package legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model;

import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.org.lwjgl.util.vector.Vector3f;

/** 1.12.2 rotation of a model element: origin (0..16), axis, angle (-45..45 in steps of 22.5) and whether to rescale. */
public class BlockPartRotation {
    public final Vector3f origin;
    public final EnumFacing.Axis axis;
    public final float angle;
    public final boolean rescale;

    public BlockPartRotation(Vector3f originIn, EnumFacing.Axis axisIn, float angleIn, boolean rescaleIn) {
        this.origin = originIn;
        this.axis = axisIn;
        this.angle = angleIn;
        this.rescale = rescaleIn;
    }

    net.minecraft.client.renderer.block.model.BlockElementRotation mcc$modern() {
        net.minecraft.core.Direction.Axis modern = switch (axis) {
            case X -> net.minecraft.core.Direction.Axis.X;
            case Y -> net.minecraft.core.Direction.Axis.Y;
            default -> net.minecraft.core.Direction.Axis.Z;
        };
        return new net.minecraft.client.renderer.block.model.BlockElementRotation(new org.joml.Vector3f(origin.x / 16.0F, origin.y / 16.0F, origin.z / 16.0F), modern,
                angle, rescale);
    }
}
