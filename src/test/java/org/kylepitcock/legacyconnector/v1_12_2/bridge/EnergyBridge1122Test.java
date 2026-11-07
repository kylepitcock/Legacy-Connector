package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import legacyconnector.v1_12_2.cofh.redstoneflux.impl.EnergyStorage;
import legacyconnector.v1_12_2.cofh.redstoneflux.impl.TileEnergyHandler;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.junit.jupiter.api.Test;

/** Redstone Flux 1.12.2 tiles and buffers as NeoForge energy: transfer arithmetic, limits, simulate versus execute, sides. */
class EnergyBridge1122Test {
    /** A machine with a 1000 RF buffer taking 100 per call and giving 40 per call, connecting everywhere but its bottom. */
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
        public boolean canConnectEnergy(EnumFacing from) {
            return from != EnumFacing.DOWN;
        }
    }

    @Test
    void receiveIsLimitedByTheMachinesRateAndCapacity() {
        Machine m = new Machine();
        IEnergyStorage fe = EnergyBridge1122.forTile(m, Direction.UP);
        assertNotNull(fe);
        assertTrue(fe.canReceive());
        assertEquals(100, fe.receiveEnergy(5000, false), "rate limit");
        assertEquals(100, m.stored());
        assertEquals(30, fe.receiveEnergy(30, false), "less than the rate");
        m.fill(980);
        assertEquals(20, fe.receiveEnergy(100, false), "capacity limit");
        assertEquals(1000, m.stored());
        assertEquals(0, fe.receiveEnergy(100, false), "full");
    }

    @Test
    void simulateChangesNothing() {
        Machine m = new Machine();
        IEnergyStorage fe = EnergyBridge1122.forTile(m, Direction.NORTH);
        assertEquals(100, fe.receiveEnergy(250, true));
        assertEquals(0, m.stored());
        m.fill(500);
        assertEquals(40, fe.extractEnergy(250, true));
        assertEquals(500, m.stored());
        assertEquals(40, fe.extractEnergy(250, false));
        assertEquals(460, m.stored());
    }

    @Test
    void extractIsLimitedByTheStoredAmount() {
        Machine m = new Machine();
        IEnergyStorage fe = EnergyBridge1122.forTile(m, Direction.EAST);
        m.fill(15);
        assertEquals(15, fe.extractEnergy(40, false));
        assertEquals(0, m.stored());
        assertEquals(0, fe.extractEnergy(40, false));
    }

    @Test
    void negativeAndZeroRequestsMoveNothing() {
        Machine m = new Machine();
        m.fill(500);
        IEnergyStorage fe = EnergyBridge1122.forTile(m, Direction.WEST);
        assertEquals(0, fe.receiveEnergy(-50, false));
        assertEquals(0, fe.extractEnergy(-50, false));
        assertEquals(0, fe.receiveEnergy(0, false));
        assertEquals(500, m.stored());
    }

    @Test
    void anOverflowingRequestIsClamped() {
        Machine m = new Machine();
        IEnergyStorage fe = EnergyBridge1122.forTile(m, Direction.WEST);
        assertEquals(100, fe.receiveEnergy(Integer.MAX_VALUE, false));
        assertEquals(1000, fe.getMaxEnergyStored());
        assertEquals(100, fe.getEnergyStored());
    }

    @Test
    void aSideTheTileDoesNotConnectOnHasNoCapabilityButSidelessDoes() {
        Machine m = new Machine();
        assertNull(EnergyBridge1122.forTile(m, Direction.DOWN));
        assertNotNull(EnergyBridge1122.forTile(m, null));
        assertNotNull(EnergyBridge1122.forTile(m, Direction.UP));
    }

    @Test
    void aTileThatIsNotAnRfTileHasNoView() {
        assertNull(EnergyBridge1122.forTile(new legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity() {}, Direction.UP));
    }

    @Test
    void referenceBufferBehavesLikeRedstoneFlux() {
        EnergyStorage s = new EnergyStorage(100, 30, 20);
        assertEquals(30, s.receiveEnergy(100, false));
        assertEquals(30, s.receiveEnergy(100, true), "simulate reports the same");
        assertEquals(30, s.getEnergyStored());
        assertEquals(20, s.extractEnergy(100, false));
        assertEquals(10, s.getEnergyStored());
        s.modifyEnergyStored(1000);
        assertEquals(100, s.getEnergyStored(), "clamped to capacity");
        s.modifyEnergyStored(-1000);
        assertEquals(0, s.getEnergyStored(), "clamped to zero");
        s.setCapacity(10).setMaxTransfer(5);
        assertEquals(5, s.receiveEnergy(100, false));
        assertFalse(s.getMaxEnergyStored() > 10);
    }
}
