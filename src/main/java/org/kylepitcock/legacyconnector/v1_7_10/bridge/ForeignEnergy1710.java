package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import java.lang.reflect.Method;
import legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.kylepitcock.legacyconnector.api.internal.EnergyRates;
import org.kylepitcock.legacyconnector.core.LegacyConfig;

/**
 * Energy systems that are not part of the shim (their API classes ship inside the mods) as NeoForge energy: IC2 EU
 * ({@code IEnergySink}/{@code IEnergySource}, {@code energy.fePerEu} FE per EU, default 4) and BuildCraft MJ
 * ({@code IPowerReceptor}/{@code PowerHandler}, {@code energy.fePerMj}, default 10). The interfaces are found by name
 * on the tile's class and called by reflection. IC2's cables and BuildCraft's pipes are legacy blocks that never
 * ask for the capability, so a tile registered in IC2's EnergyNet is not handled twice.
 */
public final class ForeignEnergy1710 {
    private ForeignEnergy1710() {}

    private static final ClassValue<Eu> EU = new ClassValue<>() {
        @Override
        protected Eu computeValue(Class<?> type) {
            return Eu.of(type);
        }
    };
    private static final ClassValue<Mj> MJ = new ClassValue<>() {
        @Override
        protected Mj computeValue(Class<?> type) {
            return Mj.of(type);
        }
    };

    /** An IC2 or BuildCraft view of the tile for this side, or null. */
    public static IEnergyStorage forTile(TileEntity tile, Direction side) {
        if (tile == null) return null;
        ForgeDirection from = ForgeDirection.getOrientation(EnergyRates.legacySide(side == null ? -1 : side.get3DDataValue()));
        Eu eu = EU.get(tile.getClass());
        if (eu.sink != null || eu.source != null) {
            if (side != null && !eu.connects(tile, from)) return null;
            return new EuStorage(tile, from, eu);
        }
        Mj mj = MJ.get(tile.getClass());
        if (mj.receptor != null) return side != null && mj.receiver(tile, from) == null ? null : new MjStorage(tile, from, mj);
        return null;
    }

    private static Class<?> named(Class<?> type, String name) {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            Class<?> found = search(c, name);
            if (found != null) return found;
        }
        return null;
    }

    private static Class<?> search(Class<?> c, String name) {
        if (c.getName().equals(name)) return c;
        for (Class<?> i : c.getInterfaces()) {
            Class<?> f = search(i, name);
            if (f != null) return f;
        }
        return null;
    }

    private static Method method(Class<?> c, String name, Class<?>... args) {
        try {
            return c.getMethod(name, args);
        } catch (ReflectiveOperationException | LinkageError e) {
            return null;
        }
    }

    // ------------------------------------------------------------------ IC2 EU

    private static final class Eu {
        Class<?> sink;
        Class<?> source;
        Method demanded;
        Method inject;
        Method accepts;
        Method sinkTier;
        Method offered;
        Method draw;
        Method emits;
        Method sourceTier;

        static Eu of(Class<?> type) {
            Eu e = new Eu();
            e.sink = named(type, "ic2.api.energy.tile.IEnergySink");
            e.source = named(type, "ic2.api.energy.tile.IEnergySource");
            if (e.sink != null) {
                e.demanded = method(e.sink, "getDemandedEnergy");
                e.inject = method(e.sink, "injectEnergy", ForgeDirection.class, double.class, double.class);
                e.accepts = method(e.sink, "acceptsEnergyFrom", TileEntity.class, ForgeDirection.class);
                e.sinkTier = method(e.sink, "getSinkTier");
                if (e.demanded == null || e.inject == null) e.sink = null;
            }
            if (e.source != null) {
                e.offered = method(e.source, "getOfferedEnergy");
                e.draw = method(e.source, "drawEnergy", double.class);
                e.emits = method(e.source, "emitsEnergyTo", TileEntity.class, ForgeDirection.class);
                e.sourceTier = method(e.source, "getSourceTier");
                if (e.offered == null || e.draw == null) e.source = null;
            }
            return e;
        }

        boolean connects(TileEntity tile, ForgeDirection from) {
            return accepts(tile, from) || emits(tile, from);
        }

        boolean accepts(TileEntity tile, ForgeDirection from) {
            if (sink == null) return false;
            try {
                return accepts == null || (boolean) accepts.invoke(tile, tile, from);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                return false;
            }
        }

        boolean emits(TileEntity tile, ForgeDirection from) {
            if (source == null) return false;
            try {
                return emits == null || (boolean) emits.invoke(tile, tile, from);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                return false;
            }
        }

        double demand(TileEntity tile) {
            try {
                return ((Number) demanded.invoke(tile)).doubleValue();
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                return 0;
            }
        }

        int tier(Method m, TileEntity tile, int fallback) {
            if (m == null) return fallback;
            try {
                return ((Number) m.invoke(tile)).intValue();
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                return fallback;
            }
        }
    }

    private record EuStorage(TileEntity tile, ForgeDirection from, Eu eu) implements IEnergyStorage {
        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            if (!eu.accepts(tile, from)) return 0;
            int rate = LegacyConfig.fePerEu();
            double offer = EnergyRates.euToInject(amount, rate, eu.demand(tile), eu.tier(eu.sinkTier, tile, 1));
            if (offer <= 0) return 0;
            if (simulate) return EnergyRates.feFromUnits(offer, rate);
            try {
                double leftover = ((Number) eu.inject.invoke(tile, from, offer, EnergyRates.packetLimit(eu.tier(eu.sinkTier, tile, 1)))).doubleValue();
                return EnergyRates.feFromUnits(Math.max(0, offer - leftover), rate);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                return 0;
            }
        }

        @Override
        public int extractEnergy(int amount, boolean simulate) {
            if (!eu.emits(tile, from)) return 0;
            int rate = LegacyConfig.fePerEu();
            try {
                double offered = ((Number) eu.offered.invoke(tile)).doubleValue();
                double take = Math.min(Math.min((double) EnergyRates.unitsFromFe(amount, rate), Math.floor(offered)), EnergyRates.packetLimit(eu.tier(eu.sourceTier, tile, 1)));
                if (take < 1) return 0;
                if (!simulate) eu.draw.invoke(tile, take);
                return EnergyRates.feFromUnits(take, rate);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                return 0;
            }
        }

        @Override
        public int getEnergyStored() {
            return 0;
        }

        @Override
        public int getMaxEnergyStored() {
            return eu.sink == null ? 0 : EnergyRates.feFromUnits(eu.demand(tile), LegacyConfig.fePerEu());
        }

        @Override
        public boolean canExtract() {
            return eu.emits(tile, from);
        }

        @Override
        public boolean canReceive() {
            return eu.accepts(tile, from);
        }
    }

    // ------------------------------------------------------------------ BuildCraft MJ

    private static final class Mj {
        Class<?> receptor;
        Method getReceiver;
        Method receive;
        Method request;
        Method min;
        Method max;
        Method stored;
        Method capacity;
        Object type;

        static Mj of(Class<?> type) {
            Mj m = new Mj();
            m.receptor = named(type, "buildcraft.api.power.IPowerReceptor");
            if (m.receptor == null) return m;
            m.getReceiver = method(m.receptor, "getPowerReceiver", ForgeDirection.class);
            try {
                Class<?> receiver = Class.forName("buildcraft.api.power.PowerHandler$PowerReceiver", false, type.getClassLoader());
                Class<?> kind = Class.forName("buildcraft.api.power.PowerHandler$Type", false, type.getClassLoader());
                m.receive = method(receiver, "receiveEnergy", kind, double.class, ForgeDirection.class);
                m.request = method(receiver, "powerRequest");
                m.min = method(receiver, "getMinEnergyReceived");
                m.max = method(receiver, "getMaxEnergyReceived");
                m.stored = method(receiver, "getEnergyStored");
                m.capacity = method(receiver, "getMaxEnergyStored");
                for (Object c : kind.getEnumConstants()) if (c.toString().equals("PIPE")) m.type = c;
            } catch (ReflectiveOperationException | LinkageError e) {
                m.receptor = null;
            }
            if (m.getReceiver == null || m.receive == null || m.request == null || m.type == null) m.receptor = null;
            return m;
        }

        Object receiver(TileEntity tile, ForgeDirection from) {
            try {
                return getReceiver.invoke(tile, from);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                return null;
            }
        }

        double number(Method m, Object receiver, double fallback) {
            if (m == null) return fallback;
            try {
                return ((Number) m.invoke(receiver)).doubleValue();
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                return fallback;
            }
        }
    }

    private record MjStorage(TileEntity tile, ForgeDirection from, Mj mj) implements IEnergyStorage {
        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            Object r = mj.receiver(tile, from);
            if (r == null) return 0;
            int rate = LegacyConfig.fePerMj();
            double want = Math.min(mj.number(mj.request, r, 0), mj.number(mj.max, r, Double.MAX_VALUE));
            double offer = Math.min((double) EnergyRates.unitsFromFe(amount, rate), want);
            if (offer <= 0 || offer < mj.number(mj.min, r, 0)) return 0;
            if (simulate) return EnergyRates.feFromUnits(offer, rate);
            try {
                double used = ((Number) mj.receive.invoke(r, mj.type, offer, from)).doubleValue();
                return EnergyRates.feFromUnits(Math.max(0, used), rate);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                return 0;
            }
        }

        @Override
        public int extractEnergy(int amount, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            Object r = mj.receiver(tile, from);
            return r == null ? 0 : EnergyRates.feFromUnits(mj.number(mj.stored, r, 0), LegacyConfig.fePerMj());
        }

        @Override
        public int getMaxEnergyStored() {
            Object r = mj.receiver(tile, from);
            return r == null ? 0 : EnergyRates.feFromUnits(mj.number(mj.capacity, r, 0), LegacyConfig.fePerMj());
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return mj.receiver(tile, from) != null;
        }
    }
}
