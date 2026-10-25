package legacyconnector.v1_12_2.net.minecraft.block;

import legacyconnector.v1_12_2.net.minecraft.block.material.Material;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyBool;
import legacyconnector.v1_12_2.net.minecraft.block.properties.PropertyEnum;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.IStringSerializable;

public class BlockLever extends Block {
    public static final PropertyEnum<EnumOrientation> FACING = PropertyEnum.create("facing", EnumOrientation.class);
    public static final PropertyBool POWERED = PropertyBool.create("powered");

    protected BlockLever() {
        super(Material.CIRCUITS);
    }

    /** How the lever is attached and which way it points (metadata 0-7). */
    public enum EnumOrientation implements IStringSerializable {
        DOWN_X(0, "down_x", EnumFacing.DOWN), EAST(1, "east", EnumFacing.EAST), WEST(2, "west", EnumFacing.WEST), SOUTH(3, "south", EnumFacing.SOUTH),
        NORTH(4, "north", EnumFacing.NORTH), UP_Z(5, "up_z", EnumFacing.UP), UP_X(6, "up_x", EnumFacing.UP), DOWN_Z(7, "down_z", EnumFacing.DOWN);

        private static final EnumOrientation[] META_LOOKUP = new EnumOrientation[values().length];
        private final int meta;
        private final String name;
        private final EnumFacing facing;

        EnumOrientation(int meta, String name, EnumFacing facing) {
            this.meta = meta;
            this.name = name;
            this.facing = facing;
        }

        public int getMetadata() {
            return meta;
        }

        public EnumFacing getFacing() {
            return facing;
        }

        @Override
        public String toString() {
            return name;
        }

        public static EnumOrientation byMetadata(int meta) {
            if (meta < 0 || meta >= META_LOOKUP.length) meta = 0;
            return META_LOOKUP[meta];
        }

        public static EnumOrientation forFacings(EnumFacing clickedSide, EnumFacing entityFacing) {
            switch (clickedSide) {
                case DOWN:
                    switch (entityFacing.getAxis()) {
                        case X:
                            return DOWN_X;
                        case Z:
                            return DOWN_Z;
                        default:
                            throw new IllegalArgumentException("Invalid entityFacing " + entityFacing + " for facing " + clickedSide);
                    }
                case UP:
                    switch (entityFacing.getAxis()) {
                        case X:
                            return UP_X;
                        case Z:
                            return UP_Z;
                        default:
                            throw new IllegalArgumentException("Invalid entityFacing " + entityFacing + " for facing " + clickedSide);
                    }
                case NORTH:
                    return NORTH;
                case SOUTH:
                    return SOUTH;
                case WEST:
                    return WEST;
                case EAST:
                    return EAST;
                default:
                    throw new IllegalArgumentException("Invalid facing: " + clickedSide);
            }
        }

        @Override
        public String getName() {
            return name;
        }

        static {
            for (EnumOrientation o : values()) META_LOOKUP[o.getMetadata()] = o;
        }
    }
}
