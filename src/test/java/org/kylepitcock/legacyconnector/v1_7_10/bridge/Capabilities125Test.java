package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import buildcraft.api.Orientations;
import buildcraft.api.liquids.ILiquidTank;
import buildcraft.api.liquids.ITankContainer;
import buildcraft.api.liquids.LiquidStack;
import ic2.api.Direction;
import ic2.api.IEnergySink;
import ic2.api.IEnergyStorage;
import legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.api.internal.EnergyRates;
import org.kylepitcock.legacyconnector.v1_6_4.bridge.Liquids125;
import org.kylepitcock.legacyconnector.v1_6_4.bridge.LiquidsBc32;

/** The 1.2.5-era capability adapters (IC2 1.103 storage blocks, BuildCraft liquid variants) on test doubles of the real interfaces, without a game. */
class Capabilities125Test {
    @org.junit.jupiter.api.BeforeAll
    static void mc125On() {
        System.setProperty(org.kylepitcock.legacyconnector.v1_6_4.bridge.Mc125Host.PROPERTY, "true"); // the loader sets it when it hosts a 1.2.5 jar
    }

    @org.junit.jupiter.api.AfterAll
    static void mc125Off() {
        System.clearProperty(org.kylepitcock.legacyconnector.v1_6_4.bridge.Mc125Host.PROPERTY);
    }

    /** An IC2 1.103 storage block (BatBox, MFE, MFSU): sink and storage, with the tier's output. */
    static class StorageBlock extends TileEntity implements IEnergySink, IEnergyStorage {
        final int output;
        final int capacity;
        int stored;
        int lastPacket;

        StorageBlock(int output, int capacity, int stored) {
            this.output = output;
            this.capacity = capacity;
            this.stored = stored;
        }

        @Override
        public boolean acceptsEnergyFrom(TileEntity emitter, Direction direction) {
            return direction != Direction.YP;
        }

        @Override
        public boolean demandsEnergy() {
            return stored < capacity;
        }

        @Override
        public int injectEnergy(Direction direction, int amount) {
            lastPacket = amount;
            int take = Math.min(amount, capacity - stored);
            stored += take;
            return amount - take;
        }

        @Override
        public int getStored() {
            return stored;
        }

        @Override
        public int getCapacity() {
            return capacity;
        }

        @Override
        public int getOutput() {
            return output;
        }
    }

    /** A generator-like block that only reports its storage. */
    static class MeterOnly extends TileEntity implements IEnergyStorage {
        @Override
        public int getStored() {
            return 50;
        }

        @Override
        public int getCapacity() {
            return 400;
        }

        @Override
        public int getOutput() {
            return 10;
        }
    }

    @Test
    void packetSizeFollowsTheTierOfTheStorageOutput() {
        assertEquals(32, EnergyRates.packetForOutput(0));
        assertEquals(32, EnergyRates.packetForOutput(32));      // BatBox
        assertEquals(32, EnergyRates.packetForOutput(100));
        assertEquals(128, EnergyRates.packetForOutput(128));    // MFE
        assertEquals(512, EnergyRates.packetForOutput(512));    // MFSU
        assertEquals(2048, EnergyRates.packetForOutput(100000));
    }

    @Test
    void aStorageBlockTakesPacketsOfItsTierAndReportsItsRealCharge() {
        StorageBlock mfe = new StorageBlock(128, 4_000_000, 1000);
        var fe = ForeignEnergy1710.forTile(mfe, net.minecraft.core.Direction.NORTH);
        assertNotNull(fe);
        int rate = org.kylepitcock.legacyconnector.core.LegacyConfig.fePerEuIn();
        assertEquals(1000 * org.kylepitcock.legacyconnector.core.LegacyConfig.fePerEu(), fe.getEnergyStored());
        assertEquals(4_000_000 * org.kylepitcock.legacyconnector.core.LegacyConfig.fePerEu(), fe.getMaxEnergyStored());
        int accepted = fe.receiveEnergy(10_000_000, false);
        assertEquals(128, mfe.lastPacket, "the MFE takes 128 EU packets, not the low-voltage 32");
        assertEquals(128 * rate, accepted);
        StorageBlock bat = new StorageBlock(32, 40_000, 0);
        ForeignEnergy1710.forTile(bat, net.minecraft.core.Direction.NORTH).receiveEnergy(10_000_000, false);
        assertEquals(32, bat.lastPacket);
        assertNull(ForeignEnergy1710.forTile(mfe, net.minecraft.core.Direction.UP), "a side the sink does not accept from has no capability");
    }

    @Test
    void aStorageOnlyBlockIsAReadOnlyMeter() {
        var fe = ForeignEnergy1710.forTile(new MeterOnly(), net.minecraft.core.Direction.EAST);
        assertNotNull(fe);
        int per = org.kylepitcock.legacyconnector.core.LegacyConfig.fePerEu();
        assertEquals(50 * per, fe.getEnergyStored());
        assertEquals(400 * per, fe.getMaxEnergyStored());
        assertFalse(fe.canReceive());
        assertFalse(fe.canExtract());
        assertEquals(0, fe.receiveEnergy(1000, false));
        assertEquals(0, fe.extractEnergy(1000, false));
        assertNull(ForeignEnergy1710.forTile(new TileEntity(), net.minecraft.core.Direction.EAST));
    }

    @Test
    void liquidContainerCapacityUsesWhatTheTileDeclaredAndNeverLessThanItHolds() {
        assertEquals(10_000, Liquids125.capacityOf(10_000, 500));
        assertEquals(10_000, Liquids125.capacityOf(2_000, 10_000));   // over-full: the stored amount wins
        assertEquals(1_000 + 3_000, Liquids125.capacityOf(-1, 3_000)); // undeclared: a bucket more than it holds
        assertEquals(1_000, Liquids125.capacityOf(0, 0));
    }

    static class Boiler extends TileEntity implements ITankContainer {
        final ILiquidTank[] tanks = {new Tank(new LiquidStack(8, 250, 0), 4000), new Tank(null, 1000)};

        @Override
        public int fill(Orientations from, LiquidStack resource, boolean doFill) {
            return 0;
        }

        @Override
        public LiquidStack drain(Orientations from, int maxDrain, boolean doDrain) {
            return null;
        }

        @Override
        public ILiquidTank[] getTanks() {
            return tanks;
        }
    }

    record Tank(LiquidStack stack, int capacity) implements ILiquidTank {
        @Override
        public LiquidStack getLiquid() {
            return stack;
        }

        @Override
        public int getCapacity() {
            return capacity;
        }
    }

    @Test
    void aForestryEraTankContainerHasOneHandlerTankPerLegacyTank() {
        var handler = LiquidsBc32.forTile(new Boiler(), net.minecraft.core.Direction.NORTH);
        assertNotNull(handler);
        assertEquals(2, handler.getTanks());
        assertEquals(4000, handler.getTankCapacity(0));
        assertEquals(1000, handler.getTankCapacity(1));
        assertEquals(0, handler.getTankCapacity(7));
        assertNull(LiquidsBc32.forTile(new Object(), net.minecraft.core.Direction.NORTH));
        assertNull(LiquidsBc32.forTile(null, null));
    }
}
