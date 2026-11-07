package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import buildcraft.api.power.IPowerProvider;
import buildcraft.api.power.IPowerReceptor;
import ic2.api.energy.tile.IEnergySink;
import legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import universalelectricity.api.energy.IEnergyContainer;
import universalelectricity.api.energy.IEnergyInterface;

/** The reflection adapters of IC2 EU, BuildCraft MJ 3.x and Universal Electricity against test doubles of those APIs (defaults: 4 FE/EU, 10 FE/MJ, 1 FE/J). */
class ForeignEnergy1710Test {
    static class Sink extends TileEntity implements IEnergySink {
        double demand = 100;
        double injected;
        int maxSafe = 32;

        @Override
        public boolean acceptsEnergyFrom(TileEntity emitter, ForgeDirection direction) {
            return direction != ForgeDirection.UP;
        }

        @Override
        public double demandedEnergyUnits() {
            return demand - injected;
        }

        @Override
        public double injectEnergyUnits(ForgeDirection directionFrom, double amount) {
            double take = Math.min(amount, demand - injected);
            injected += take;
            return amount - take;
        }

        @Override
        public int getMaxSafeInput() {
            return maxSafe;
        }
    }

    @Test
    void anIc2SinkTakesWholeEuLimitedByDemandAndItsSafeInput() {
        Sink sink = new Sink();
        var storage = ForeignEnergy1710.forTile(sink, Direction.NORTH);
        assertNotNull(storage);
        assertNull(ForeignEnergy1710.forTile(sink, Direction.UP)); // acceptsEnergyFrom says no
        assertEquals(128, storage.receiveEnergy(1000, true)); // one 32 EU packet = 128 FE
        assertEquals(0.0, sink.injected); // simulate changed nothing
        assertEquals(128, storage.receiveEnergy(1000, false));
        assertEquals(32.0, sink.injected);
        assertEquals(24, storage.receiveEnergy(27, false)); // 27 FE buy 6 EU
        assertEquals(38.0, sink.injected);
        sink.maxSafe = 512;
        sink.injected = 90;
        assertEquals(40, storage.receiveEnergy(10_000, false)); // only 10 EU still demanded
        assertEquals(0, storage.receiveEnergy(100, false));
        assertFalse(storage.canExtract());
        assertTrue(storage.canReceive());
    }

    static class Bc3 extends TileEntity implements IPowerReceptor {
        float stored;
        final IPowerProvider provider = new IPowerProvider() {
            @Override
            public int getMinEnergyReceived() {
                return 2;
            }

            @Override
            public int getMaxEnergyReceived() {
                return 50;
            }

            @Override
            public float getEnergyStored() {
                return stored;
            }

            @Override
            public int getMaxEnergyStored() {
                return 100;
            }

            @Override
            public void receiveEnergy(float quantity, ForgeDirection from) {
                stored += quantity;
            }
        };

        @Override
        public IPowerProvider getPowerProvider() {
            return provider;
        }

        @Override
        public int powerRequest(ForgeDirection from) {
            return from == ForgeDirection.DOWN ? 0 : (int) (100 - stored);
        }
    }

    @Test
    void aBuildCraft3ReceptorGetsMjWithinRequestMaxAndMinimum() {
        Bc3 tile = new Bc3();
        var storage = ForeignEnergy1710.forTile(tile, Direction.NORTH);
        assertNotNull(storage);
        assertEquals(30, storage.receiveEnergy(30, true)); // 3 MJ
        assertEquals(0.0f, tile.stored);
        assertEquals(500, storage.receiveEnergy(100_000, false)); // capped at the 50 MJ it takes per call
        assertEquals(50.0f, tile.stored);
        assertEquals(0, storage.receiveEnergy(15, false)); // 1 MJ is below the minimum of 2
        assertEquals(500, storage.getEnergyStored());
        assertEquals(1000, storage.getMaxEnergyStored());
        assertEquals(0, storage.extractEnergy(100, false));
        assertFalse(storage.canExtract());
        // nothing requested from below
        assertEquals(0, ForeignEnergy1710.forTile(tile, Direction.DOWN).receiveEnergy(100, false));
    }

    static class Ue extends TileEntity implements IEnergyInterface, IEnergyContainer {
        long energy = 500;
        final long capacity = 1000;

        @Override
        public boolean canConnect(ForgeDirection direction) {
            return direction != ForgeDirection.WEST;
        }

        @Override
        public long onReceiveEnergy(ForgeDirection from, long receive, boolean doReceive) {
            long n = Math.min(receive, capacity - energy);
            if (doReceive) energy += n;
            return n;
        }

        @Override
        public long onExtractEnergy(ForgeDirection from, long extract, boolean doExtract) {
            long n = Math.min(extract, energy);
            if (doExtract) energy -= n;
            return n;
        }

        @Override
        public long getEnergy(ForgeDirection from) {
            return energy;
        }

        @Override
        public long getEnergyCapacity(ForgeDirection from) {
            return capacity;
        }
    }

    @Test
    void aUniversalElectricityTileSimulatesAndMovesJoulesBothWays() {
        Ue tile = new Ue();
        var storage = ForeignEnergy1710.forTile(tile, Direction.NORTH);
        assertNotNull(storage);
        assertNull(ForeignEnergy1710.forTile(tile, Direction.WEST));
        assertEquals(300, storage.receiveEnergy(300, true));
        assertEquals(500, tile.energy); // simulate changed nothing
        assertEquals(500, storage.receiveEnergy(5000, false)); // room for 500 only
        assertEquals(1000, tile.energy);
        assertEquals(250, storage.extractEnergy(250, false));
        assertEquals(750, tile.energy);
        assertEquals(750, storage.getEnergyStored());
        assertEquals(1000, storage.getMaxEnergyStored());
        assertTrue(storage.canExtract());
    }
}
