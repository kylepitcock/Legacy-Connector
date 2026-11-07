package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import legacyconnector.v1_7_10.cofh.api.energy.EnergyStorage;
import legacyconnector.v1_7_10.cofh.api.energy.TileEnergyHandler;
import legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.junit.jupiter.api.Test;

/** CoFH RF (1.7.10, also what 1.6.4 mods link against) tiles as NeoForge energy: arithmetic, limits, simulate versus execute, sides. */
class EnergyBridgeTest {
    private static final class Machine extends TileEnergyHandler {
        Machine() {
            storage = new EnergyStorage(1000, 100, 40);
        }

        int stored() {
            return storage.getEnergyStored();
        }

        void fill(int amount) {
            storage.setEnergyStored(amount);
        }

        @Override
        public boolean canConnectEnergy(ForgeDirection from) {
            return from != ForgeDirection.DOWN;
        }
    }

    @Test
    void receiveIsLimitedByRateAndCapacityAndSimulateChangesNothing() {
        Machine m = new Machine();
        IEnergyStorage fe = EnergyBridge.forTile(m, Direction.UP);
        assertNotNull(fe);
        assertEquals(100, fe.receiveEnergy(250, true));
        assertEquals(0, m.stored());
        assertEquals(100, fe.receiveEnergy(250, false));
        m.fill(990);
        assertEquals(10, fe.receiveEnergy(100, false));
        assertEquals(0, fe.receiveEnergy(100, false));
        assertEquals(1000, fe.getEnergyStored());
    }

    @Test
    void extractIsLimitedByRateAndStoredEnergy() {
        Machine m = new Machine();
        IEnergyStorage fe = EnergyBridge.forTile(m, Direction.NORTH);
        m.fill(70);
        assertEquals(40, fe.extractEnergy(1000, true));
        assertEquals(70, m.stored());
        assertEquals(40, fe.extractEnergy(1000, false));
        assertEquals(30, fe.extractEnergy(1000, false));
        assertEquals(0, fe.extractEnergy(1000, false));
    }

    @Test
    void negativeRequestsNeverDrainTheBuffer() {
        Machine m = new Machine();
        m.fill(500);
        IEnergyStorage fe = EnergyBridge.forTile(m, Direction.NORTH);
        assertEquals(0, fe.receiveEnergy(-4000, false));
        assertEquals(500, m.stored());
    }

    @Test
    void overflowingRequestIsClamped() {
        Machine m = new Machine();
        IEnergyStorage fe = EnergyBridge.forTile(m, Direction.SOUTH);
        assertEquals(100, fe.receiveEnergy(Integer.MAX_VALUE, false));
    }

    @Test
    void sidesAreMappedAndDisconnectedSidesHaveNoCapability() {
        Machine m = new Machine();
        assertNull(EnergyBridge.forTile(m, Direction.DOWN));
        assertNotNull(EnergyBridge.forTile(m, null));
        assertTrue(EnergyBridge.forTile(m, Direction.EAST).canReceive());
        assertTrue(EnergyBridge.forTile(m, Direction.EAST).canExtract());
    }
}
