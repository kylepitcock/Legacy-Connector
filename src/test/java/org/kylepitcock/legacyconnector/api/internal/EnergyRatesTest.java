package org.kylepitcock.legacyconnector.api.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EnergyRatesTest {
    @Test
    void convertsWithRounding() {
        assertEquals(2, EnergyRates.unitsFromFe(9, 4));
        assertEquals(0, EnergyRates.unitsFromFe(3, 4));
        assertEquals(0, EnergyRates.unitsFromFe(-5, 4));
        assertEquals(20, EnergyRates.feFromUnits(5.9, 4));
        assertEquals(Integer.MAX_VALUE, EnergyRates.feFromUnits(1e12, 4));
        assertEquals(0, EnergyRates.feFromUnits(-1, 4));
    }

    @Test
    void zeroRateDisablesADirection() {
        assertEquals(0, EnergyRates.unitsFromFe(1000, 0));
        assertEquals(0, EnergyRates.feFromUnits(1000, 0));
        assertEquals(0, EnergyRates.euToInject(1000, 0, 100, 3));
    }

    @Test
    void packetLimitFollowsTier() {
        assertEquals(8, EnergyRates.packetLimit(0));
        assertEquals(32, EnergyRates.packetLimit(1));
        assertEquals(128, EnergyRates.packetLimit(2));
        assertEquals(2048, EnergyRates.packetLimit(4));
    }

    @Test
    void injectionIsBoundedByDemandAndTier() {
        assertEquals(10, EnergyRates.euToInject(40, 4, 100, 3));
        assertEquals(7, EnergyRates.euToInject(400, 4, 7.9, 3));
        assertEquals(32, EnergyRates.euToInject(4000, 4, 1000, 1));
        assertEquals(0, EnergyRates.euToInject(3, 4, 100, 1));
        assertEquals(0, EnergyRates.euToInject(400, 4, 0.5, 1));
    }

    @Test
    void sidesMapToLegacyIndices() {
        assertEquals(0, EnergyRates.legacySide(0));
        assertEquals(5, EnergyRates.legacySide(5));
        assertEquals(6, EnergyRates.legacySide(-1));
    }

    @Test
    void tierFollowsTheMaxSafeInputOfTheSink() {
        assertEquals(0, EnergyRates.tierFromMaxSafeInput(8));
        assertEquals(1, EnergyRates.tierFromMaxSafeInput(9));
        assertEquals(1, EnergyRates.tierFromMaxSafeInput(32));
        assertEquals(2, EnergyRates.tierFromMaxSafeInput(33));
        assertEquals(2, EnergyRates.tierFromMaxSafeInput(128));
        assertEquals(3, EnergyRates.tierFromMaxSafeInput(512));
        assertEquals(4, EnergyRates.tierFromMaxSafeInput(2048));
        assertEquals(12, EnergyRates.tierFromMaxSafeInput(Integer.MAX_VALUE));
    }

    @Test
    void aSinkOwnSafeInputBeatsItsTierLimit() {
        assertEquals(100.0, EnergyRates.sinkLimit(100, 2)); // 1.5.2 sinks report an exact limit, below the tier-2 packet of 128
        assertEquals(128.0, EnergyRates.sinkLimit(0, 2));
        assertEquals(30.0, EnergyRates.euToInjectLimited(1000, 4, 500, 30));
        assertEquals(10.0, EnergyRates.euToInjectLimited(1000, 4, 10.9, 30)); // demand rounds down
        assertEquals(0.0, EnergyRates.euToInjectLimited(3, 4, 500, 30)); // 3 FE do not buy one EU
    }

    @Test
    void whatASinkTookIsTheOfferMinusTheLeftoverWithinBounds() {
        assertEquals(24.0, EnergyRates.accepted(32, 8));
        assertEquals(0.0, EnergyRates.accepted(32, 32));
        assertEquals(0.0, EnergyRates.accepted(32, 50)); // a leftover above the offer is a bug of the mod, not negative energy
        assertEquals(32.0, EnergyRates.accepted(32, -5)); // nor more than offered
    }

    @Test
    void buildCraftOfferIsBoundedByRequestAndMinimum() {
        assertEquals(5.0, EnergyRates.mjToOffer(1000, 10, 5, 1));
        assertEquals(0.0, EnergyRates.mjToOffer(15, 10, 50, 2)); // 1 MJ is under the minimum
        assertEquals(2.0, EnergyRates.mjToOffer(20, 10, 50, 2));
        assertEquals(0.0, EnergyRates.mjToOffer(1000, 10, 0, 0));
    }
}
