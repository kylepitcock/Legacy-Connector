package org.kylepitcock.legacyconnector.api.internal;

/** Pure arithmetic of the energy capability bridges (FE <-> IC2 EU / BuildCraft MJ), kept free of game types so it can be unit tested. */
public final class EnergyRates {
    private EnergyRates() {}

    /** Whole units that {@code fe} buys at {@code fePerUnit} FE each (rounded down). */
    public static long unitsFromFe(long fe, int fePerUnit) {
        return fe <= 0 || fePerUnit <= 0 ? 0 : fe / fePerUnit;
    }

    /** FE for {@code units}, saturating at Integer.MAX_VALUE (NeoForge energy is an int). */
    public static int feFromUnits(double units, int fePerUnit) {
        if (units <= 0 || fePerUnit <= 0) return 0;
        double fe = Math.floor(units) * fePerUnit;
        return fe >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) fe;
    }

    /** IC2's packet size limit for a sink tier: 8 * 4^tier EU (tier 1 = 32, 2 = 128, 3 = 512, 4 = 2048). A bigger packet blows a machine up. */
    public static double packetLimit(int tier) {
        if (tier < 0) tier = 0;
        if (tier > 12) return Double.MAX_VALUE;
        return 8.0 * (1L << (2 * tier));
    }

    /**
     * The packet size to offer an IC2 1.103 storage block ({@code IEnergyStorage.getOutput()}: BatBox 32, MFE 128, MFSU 512): the largest packet limit of a tier
     * that does not exceed its output, at least the low-voltage 32 and at most the 2048 of the fourth tier. A machine given a packet above its tier explodes.
     */
    public static int packetForOutput(int output) {
        int best = 32;
        for (int tier = 1; tier <= 4; tier++) {
            int limit = (int) packetLimit(tier);
            if (limit <= output) best = limit;
        }
        return best;
    }

    /** EU to offer a sink for {@code fe} FE: bounded by the sink's demand and by its tier's packet limit, whole EU. */
    public static double euToInject(long fe, int fePerEu, double demanded, int tier) {
        long eu = unitsFromFe(fe, fePerEu);
        double n = Math.min(Math.min((double) eu, Math.floor(demanded)), packetLimit(tier));
        return n < 1 ? 0 : n;
    }

    /** The side index of a legacy ForgeDirection for a modern Direction ordinal (both use DOWN, UP, NORTH, SOUTH, WEST, EAST); -1 (unsided) is UNKNOWN (6). */
    public static int legacySide(int direction3dDataValue) {
        return direction3dDataValue < 0 || direction3dDataValue > 5 ? 6 : direction3dDataValue;
    }
}
