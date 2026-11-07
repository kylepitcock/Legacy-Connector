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

    /** EU to offer a sink for {@code fe} FE: bounded by the sink's demand and by its tier's packet limit, whole EU. */
    public static double euToInject(long fe, int fePerEu, double demanded, int tier) {
        long eu = unitsFromFe(fe, fePerEu);
        double n = Math.min(Math.min((double) eu, Math.floor(demanded)), packetLimit(tier));
        return n < 1 ? 0 : n;
    }

    /** The IC2 tier whose packet limit (8 * 4^tier) first covers {@code maxSafeInput} EU: 32 is tier 1, 33 to 128 tier 2, 512 tier 3, 2048 tier 4; 8 or less is tier 0. */
    public static int tierFromMaxSafeInput(int maxSafeInput) {
        int tier = 0;
        while (tier < 12 && packetLimit(tier) < maxSafeInput) tier++;
        return tier;
    }

    /** The largest packet a sink survives: its own max safe input when it reports one (above 0), else the limit of its tier. */
    public static double sinkLimit(double maxSafeInput, int tier) {
        return maxSafeInput > 0 ? maxSafeInput : packetLimit(tier);
    }

    /** Like {@link #euToInject} with an explicit packet limit instead of a tier. */
    public static double euToInjectLimited(long fe, int fePerEu, double demanded, double limit) {
        long eu = unitsFromFe(fe, fePerEu);
        double n = Math.min(Math.min((double) eu, Math.floor(demanded)), Math.floor(limit));
        return n < 1 ? 0 : n;
    }

    /** What a sink took of an offer when it returned {@code leftover} (IC2's inject calls return the rest): never below 0 nor above the offer. */
    public static double accepted(double offered, double leftover) {
        return Math.max(0, Math.min(offered, offered - leftover));
    }

    /** MJ to hand a BuildCraft receptor for {@code fe} FE: bounded by what it wants; 0 when that is below its minimum. */
    public static double mjToOffer(long fe, int fePerMj, double wanted, double minimum) {
        double offer = Math.min((double) unitsFromFe(fe, fePerMj), wanted);
        return offer <= 0 || offer < minimum ? 0 : offer;
    }

    /** The side index of a legacy ForgeDirection for a modern Direction ordinal (both use DOWN, UP, NORTH, SOUTH, WEST, EAST); -1 (unsided) is UNKNOWN (6). */
    public static int legacySide(int direction3dDataValue) {
        return direction3dDataValue < 0 || direction3dDataValue > 5 ? 6 : direction3dDataValue;
    }
}
