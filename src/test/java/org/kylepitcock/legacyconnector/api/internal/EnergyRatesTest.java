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
}
