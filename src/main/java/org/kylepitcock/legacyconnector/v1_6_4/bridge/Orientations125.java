package org.kylepitcock.legacyconnector.v1_6_4.bridge;

import net.minecraft.core.Direction;

/** BuildCraft 3.1's {@code buildcraft.api.Orientations} constants (the enum ships inside the mods) for a modern side. */
final class Orientations125 {
    private Orientations125() {}

    /** The constant name of a side: BuildCraft's Y axis is up, Z north-south (ZNeg = north), X east-west (XNeg = west); null is {@code Unknown}. */
    static String name(Direction side) {
        if (side == null) return "Unknown";
        return switch (side) {
            case DOWN -> "YNeg";
            case UP -> "YPos";
            case NORTH -> "ZNeg";
            case SOUTH -> "ZPos";
            case WEST -> "XNeg";
            case EAST -> "XPos";
        };
    }

    /** The constant of {@code orientationEnum} for {@code side}, or null when the enum has no such constant. */
    static Object of(Class<?> orientationEnum, Direction side) {
        if (orientationEnum == null || !orientationEnum.isEnum()) return null;
        String name = name(side);
        for (Object c : orientationEnum.getEnumConstants()) if (c.toString().equals(name)) return c;
        return null;
    }
}
