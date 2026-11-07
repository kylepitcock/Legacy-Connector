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
 * ({@code IEnergySink}/{@code IEnergySource}, {@code energy.fePerEu} FE per EU, default 4; {@code .in}/{@code .out} per direction, 0 = off) and BuildCraft MJ
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

    private static final ClassValue<Ue> UE = new ClassValue<>() {
        @Override
        protected Ue computeValue(Class<?> type) {
            return Ue.of(type);
        }
    };

    /** An IC2, BuildCraft or Universal Electricity view of the tile for this side, or null. */
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
        Ue ue = UE.get(tile.getClass());
        if (ue.iface != null) return side != null && !ue.connects(tile, from) ? null : new UeStorage(tile, from, ue);
        return null;
    }

    // ------------------------------------------------------------------ IC2 electric items

    /** Registers {@code Capabilities.EnergyStorage.ITEM} for every item implementing IC2's {@code IElectricItem} (EU at {@code energy.fePerEu}). */
    public static void registerItems(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) {
        for (legacyconnector.v1_7_10.net.minecraft.item.Item item : ItemBridge.modItems()) {
            Class<?> electric = named(item.getClass(), "ic2.api.item.IElectricItem");
            if (electric == null) continue;
            ElectricItem api = ElectricItem.of(item, electric);
            if (api == null) continue;
            event.registerItem(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ITEM, (stack, ctx) -> new ItemStorage(api, stack),
                    item.mcc$variants.values().toArray(new net.minecraft.world.item.Item[0]));
        }
    }

    private static final class ElectricItem {
        Method getMaxCharge;
        Method getTier;
        Method canProvide;
        Method getManager;
        Method charge;
        Method discharge;
        Method getCharge;
        Object defaultManager;
        legacyconnector.v1_7_10.net.minecraft.item.Item item;

        static ElectricItem of(legacyconnector.v1_7_10.net.minecraft.item.Item item, Class<?> electric) {
            ElectricItem e = new ElectricItem();
            e.item = item;
            e.getMaxCharge = method(electric, "getMaxCharge", legacyconnector.v1_7_10.net.minecraft.item.ItemStack.class);
            e.getTier = method(electric, "getTier", legacyconnector.v1_7_10.net.minecraft.item.ItemStack.class);
            e.canProvide = method(electric, "canProvideEnergy", legacyconnector.v1_7_10.net.minecraft.item.ItemStack.class);
            Class<?> special = named(item.getClass(), "ic2.api.item.ISpecialElectricItem");
            if (special != null) e.getManager = method(special, "getManager", legacyconnector.v1_7_10.net.minecraft.item.ItemStack.class);
            try {
                Class<?> facade = Class.forName("ic2.api.item.ElectricItem", false, item.getClass().getClassLoader());
                e.defaultManager = facade.getField("manager").get(null);
            } catch (ReflectiveOperationException | LinkageError ex) {
                // only a special item's own manager is available
            }
            Object any = e.defaultManager != null ? e.defaultManager : null;
            Class<?> managerType = any != null ? any.getClass() : null;
            if (managerType == null && e.getManager == null) return null;
            return e.getMaxCharge == null ? null : e;
        }

        Object manager(legacyconnector.v1_7_10.net.minecraft.item.ItemStack stack) {
            try {
                if (getManager != null) return getManager.invoke(item, stack);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                // fall back to the default manager
            }
            return defaultManager;
        }

        double call(Object manager, String name, Object... args) throws ReflectiveOperationException {
            for (Method m : manager.getClass().getMethods()) {
                if (m.getName().equals(name) && m.getParameterCount() == args.length) {
                    m.setAccessible(true);
                    return ((Number) m.invoke(manager, args)).doubleValue();
                }
            }
            return 0;
        }
    }

    private record ItemStorage(ElectricItem api, net.minecraft.world.item.ItemStack stack) implements IEnergyStorage {
        private legacyconnector.v1_7_10.net.minecraft.item.ItemStack view() {
            return legacyconnector.v1_7_10.net.minecraft.item.ItemStack.mcc$wrap(stack);
        }

        private int tier(legacyconnector.v1_7_10.net.minecraft.item.ItemStack s) {
            try {
                return api.getTier == null ? 1 : ((Number) api.getTier.invoke(api.item, s)).intValue();
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                return 1;
            }
        }

        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            var s = view();
            Object manager = api.manager(s);
            int rate = LegacyConfig.fePerEuIn();
            long eu = EnergyRates.unitsFromFe(amount, rate);
            if (manager == null || eu <= 0) return 0;
            try {
                // charge(stack, amount, tier, ignoreTransferLimit, simulate) returns the EU accepted
                double charged = api.call(manager, "charge", s, (double) eu, tier(s), false, simulate);
                return EnergyRates.feFromUnits(charged, rate);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                return 0;
            }
        }

        @Override
        public int extractEnergy(int amount, boolean simulate) {
            var s = view();
            Object manager = api.manager(s);
            int rate = LegacyConfig.fePerEuOut();
            long eu = EnergyRates.unitsFromFe(amount, rate);
            if (manager == null || eu <= 0 || !canExtract()) return 0;
            try {
                // discharge(stack, amount, tier, ignoreTransferLimit, externally, simulate) returns the EU taken
                double taken = api.call(manager, "discharge", s, (double) eu, tier(s), false, true, simulate);
                return EnergyRates.feFromUnits(taken, rate);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                return 0;
            }
        }

        @Override
        public int getEnergyStored() {
            var s = view();
            Object manager = api.manager(s);
            if (manager == null) return 0;
            try {
                return EnergyRates.feFromUnits(api.call(manager, "getCharge", s), LegacyConfig.fePerEu());
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                return 0;
            }
        }

        @Override
        public int getMaxEnergyStored() {
            try {
                return EnergyRates.feFromUnits(((Number) api.getMaxCharge.invoke(api.item, view())).doubleValue(), LegacyConfig.fePerEu());
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                return 0;
            }
        }

        @Override
        public boolean canExtract() {
            if (LegacyConfig.fePerEuOut() <= 0) return false;
            try {
                return api.canProvide != null && (boolean) api.canProvide.invoke(api.item, view());
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                return false;
            }
        }

        @Override
        public boolean canReceive() {
            return LegacyConfig.fePerEuIn() > 0;
        }
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
        Method maxSafe;
        Method offered;
        Method draw;
        Method emits;
        Method sourceTier;

        /**
         * Handles the IC2 tile APIs of every supported version: 1.7.10 ({@code getDemandedEnergy}, {@code injectEnergy(dir, amount, voltage)}), 1.6.4
         * ({@code demandedEnergyUnits}, {@code injectEnergyUnits(dir, amount)}) and 1.5.2/1.4.7 ({@code demandsEnergy}, {@code injectEnergy(ic2.api.Direction, int)},
         * sinks only: sources there push through {@code EnergyTileSourceEvent}). All inject calls return the leftover.
         */
        static Eu of(Class<?> type) {
            Eu e = new Eu();
            e.sink = named(type, "ic2.api.energy.tile.IEnergySink");
            e.source = named(type, "ic2.api.energy.tile.IEnergySource");
            if (e.sink != null) {
                e.demanded = firstOf(e.sink, "getDemandedEnergy", "demandedEnergyUnits", "demandsEnergy");
                e.inject = injector(e.sink);
                e.accepts = twoArg(e.sink, "acceptsEnergyFrom");
                e.sinkTier = method(e.sink, "getSinkTier");
                e.maxSafe = method(e.sink, "getMaxSafeInput");
                if (e.demanded == null || e.inject == null) e.sink = null;
            }
            if (e.source != null) {
                e.offered = method(e.source, "getOfferedEnergy");
                e.draw = method(e.source, "drawEnergy", double.class);
                e.emits = twoArg(e.source, "emitsEnergyTo");
                e.sourceTier = method(e.source, "getSourceTier");
                if (e.offered == null || e.draw == null) e.source = null;
            }
            return e;
        }

        private static Method firstOf(Class<?> c, String... names) {
            for (String n : names) {
                Method m = method(c, n);
                if (m != null) return m;
            }
            return null;
        }

        /** {@code acceptsEnergyFrom(TileEntity, ForgeDirection)} or, in 1.5.2, {@code (TileEntity, ic2.api.Direction)}. */
        private static Method twoArg(Class<?> c, String name) {
            for (Method m : c.getMethods()) if (m.getName().equals(name) && m.getParameterCount() == 2) return m;
            return null;
        }

        private static Method injector(Class<?> c) {
            for (Method m : c.getMethods()) {
                int n = m.getParameterCount();
                if (!m.getName().equals("injectEnergy") && !m.getName().equals("injectEnergyUnits")) continue;
                Class<?>[] p = m.getParameterTypes();
                if (n >= 2 && n <= 3 && (p[1] == double.class || p[1] == int.class) && (n == 2 || p[2] == double.class)) return m;
            }
            return null;
        }

        /** A side as the parameter type wants it: a ForgeDirection as is, IC2 1.5.2's {@code ic2.api.Direction} through its {@code fromForgeDirection}. */
        static Object side(Class<?> type, ForgeDirection from) {
            if (type.isAssignableFrom(ForgeDirection.class)) return from;
            try {
                return type.getMethod("fromForgeDirection", ForgeDirection.class).invoke(null, from == ForgeDirection.UNKNOWN ? ForgeDirection.DOWN : from);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                return null;
            }
        }

        boolean connects(TileEntity tile, ForgeDirection from) {
            return accepts(tile, from) || emits(tile, from);
        }

        boolean accepts(TileEntity tile, ForgeDirection from) {
            if (sink == null) return false;
            return allows(accepts, tile, from);
        }

        boolean emits(TileEntity tile, ForgeDirection from) {
            if (source == null) return false;
            return allows(emits, tile, from);
        }

        private static boolean allows(Method m, TileEntity tile, ForgeDirection from) {
            if (m == null) return true;
            try {
                return (boolean) m.invoke(tile, tile, side(m.getParameterTypes()[1], from));
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

        /** The largest packet the sink survives: its own {@code getMaxSafeInput} when it has one, else the limit of its tier. */
        double limit(TileEntity tile) {
            double safe = maxSafe == null ? 0 : tier(maxSafe, tile, 0);
            return EnergyRates.sinkLimit(safe, tier(sinkTier, tile, EnergyRates.tierFromMaxSafeInput((int) Math.min(safe, Integer.MAX_VALUE))));
        }

        /** Offers {@code amount} EU; returns the EU the sink did not take. */
        double inject(TileEntity tile, ForgeDirection from, double amount, double limit) throws ReflectiveOperationException {
            Class<?>[] p = inject.getParameterTypes();
            Object[] args = new Object[p.length];
            args[0] = side(p[0], from);
            args[1] = p[1] == int.class ? (Object) (int) amount : (Object) amount;
            if (p.length == 3) args[2] = limit;
            return ((Number) inject.invoke(tile, args)).doubleValue();
        }
    }

    private record EuStorage(TileEntity tile, ForgeDirection from, Eu eu) implements IEnergyStorage {
        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            int rate = LegacyConfig.fePerEuIn();
            if (rate <= 0 || !eu.accepts(tile, from)) return 0;
            double limit = eu.limit(tile);
            double offer = EnergyRates.euToInjectLimited(amount, rate, eu.demand(tile), limit);
            if (offer <= 0) return 0;
            if (simulate) return EnergyRates.feFromUnits(offer, rate);
            try {
                return EnergyRates.feFromUnits(EnergyRates.accepted(offer, eu.inject(tile, from, offer, limit)), rate);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                return 0;
            }
        }

        @Override
        public int extractEnergy(int amount, boolean simulate) {
            int rate = LegacyConfig.fePerEuOut();
            if (rate <= 0 || !eu.emits(tile, from)) return 0;
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
            return LegacyConfig.fePerEuOut() > 0 && eu.emits(tile, from);
        }

        @Override
        public boolean canReceive() {
            return LegacyConfig.fePerEuIn() > 0 && eu.accepts(tile, from);
        }
    }

    // ------------------------------------------------------------------ BuildCraft MJ

    /**
     * BuildCraft's power API in both generations: 4.x ({@code getPowerReceiver(dir)} returning a {@code PowerReceiver}, 1.6.4) and 3.x
     * ({@code getPowerProvider()} plus {@code powerRequest(dir)} on the tile and {@code receiveEnergy(float, dir)} on the provider, 1.5.2/1.4.7).
     */
    private static final class Mj {
        Class<?> receptor;
        boolean legacy3;
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
                if (m.getReceiver != null) {
                    Class<?> receiver = Class.forName("buildcraft.api.power.PowerHandler$PowerReceiver", false, type.getClassLoader());
                    Class<?> kind = Class.forName("buildcraft.api.power.PowerHandler$Type", false, type.getClassLoader());
                    m.receive = method(receiver, "receiveEnergy", kind, double.class, ForgeDirection.class);
                    m.request = method(receiver, "powerRequest");
                    m.min = method(receiver, "getMinEnergyReceived");
                    m.max = method(receiver, "getMaxEnergyReceived");
                    m.stored = method(receiver, "getEnergyStored");
                    m.capacity = method(receiver, "getMaxEnergyStored");
                    for (Object c : kind.getEnumConstants()) if (c.toString().equals("PIPE")) m.type = c;
                    if (m.receive == null || m.request == null || m.type == null) m.receptor = null;
                } else {
                    m.legacy3 = true;
                    m.getReceiver = method(m.receptor, "getPowerProvider");
                    m.request = method(m.receptor, "powerRequest", ForgeDirection.class);
                    Class<?> provider = Class.forName("buildcraft.api.power.IPowerProvider", false, type.getClassLoader());
                    m.receive = method(provider, "receiveEnergy", float.class, ForgeDirection.class);
                    m.min = method(provider, "getMinEnergyReceived");
                    m.max = method(provider, "getMaxEnergyReceived");
                    m.stored = method(provider, "getEnergyStored");
                    m.capacity = method(provider, "getMaxEnergyStored");
                    if (m.getReceiver == null || m.request == null || m.receive == null) m.receptor = null;
                }
            } catch (ReflectiveOperationException | LinkageError e) {
                m.receptor = null;
            }
            return m;
        }

        /** The object that takes energy: a {@code PowerReceiver} (4.x) or the tile's {@code IPowerProvider} (3.x); null when the side takes none. */
        Object receiver(TileEntity tile, ForgeDirection from) {
            try {
                return legacy3 ? getReceiver.invoke(tile) : getReceiver.invoke(tile, from);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                return null;
            }
        }

        /** MJ the receptor wants from this side right now. */
        double wanted(TileEntity tile, Object receiver, ForgeDirection from) {
            if (legacy3) {
                try {
                    return ((Number) request.invoke(tile, from)).doubleValue();
                } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                    return 0;
                }
            }
            return number(request, receiver, 0);
        }

        /** Hands {@code offer} MJ over; returns what the receptor reports as used (3.x reports nothing: all of it). */
        double deliver(Object receiver, double offer, ForgeDirection from) throws ReflectiveOperationException {
            if (legacy3) {
                receive.invoke(receiver, (float) offer, from);
                return offer;
            }
            return ((Number) receive.invoke(receiver, type, offer, from)).doubleValue();
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
            int rate = LegacyConfig.fePerMjIn();
            if (rate <= 0) return 0;
            Object r = mj.receiver(tile, from);
            if (r == null) return 0;
            double want = Math.min(mj.wanted(tile, r, from), mj.number(mj.max, r, Double.MAX_VALUE));
            double offer = EnergyRates.mjToOffer(amount, rate, want, mj.number(mj.min, r, 0));
            if (offer <= 0) return 0;
            if (simulate) return EnergyRates.feFromUnits(offer, rate);
            try {
                return EnergyRates.feFromUnits(Math.max(0, mj.deliver(r, offer, from)), rate);
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
            return LegacyConfig.fePerMjIn() > 0 && mj.receiver(tile, from) != null;
        }
    }

    // ------------------------------------------------------------------ Universal Electricity

    /**
     * Universal Electricity 3.x (1.6.4: ICBM, Minechem, Mad Science, Resonant Engine): {@code IEnergyInterface} tiles take and give joules (long) through
     * {@code onReceiveEnergy}/{@code onExtractEnergy(dir, joules, doIt)}, which simulate natively; {@code IEnergyContainer} reports the stored amount.
     * {@code energy.fePerJoule} FE per joule (default 1).
     */
    private static final class Ue {
        Class<?> iface;
        Method receive;
        Method extract;
        Method canConnect;
        Method getEnergy;
        Method getCapacity;

        static Ue of(Class<?> type) {
            Ue u = new Ue();
            u.iface = named(type, "universalelectricity.api.energy.IEnergyInterface");
            if (u.iface == null) return u;
            u.receive = method(u.iface, "onReceiveEnergy", ForgeDirection.class, long.class, boolean.class);
            u.extract = method(u.iface, "onExtractEnergy", ForgeDirection.class, long.class, boolean.class);
            for (Method m : u.iface.getMethods()) if (m.getName().equals("canConnect") && m.getParameterCount() <= 2) u.canConnect = m;
            Class<?> container = named(type, "universalelectricity.api.energy.IEnergyContainer");
            if (container != null) {
                u.getEnergy = method(container, "getEnergy", ForgeDirection.class);
                u.getCapacity = method(container, "getEnergyCapacity", ForgeDirection.class);
            }
            if (u.receive == null || u.extract == null) u.iface = null;
            return u;
        }

        boolean connects(TileEntity tile, ForgeDirection from) {
            if (canConnect == null) return true;
            try {
                return (boolean) (canConnect.getParameterCount() == 1 ? canConnect.invoke(tile, from) : canConnect.invoke(tile, from, tile));
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                return false;
            }
        }

        long call(Method m, TileEntity tile, ForgeDirection from, long joules, boolean doIt) {
            try {
                return ((Number) m.invoke(tile, from, joules, doIt)).longValue();
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                return 0;
            }
        }

        long read(Method m, TileEntity tile, ForgeDirection from) {
            if (m == null) return 0;
            try {
                return ((Number) m.invoke(tile, from)).longValue();
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                return 0;
            }
        }
    }

    private record UeStorage(TileEntity tile, ForgeDirection from, Ue ue) implements IEnergyStorage {
        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            int rate = LegacyConfig.fePerJouleIn();
            long joules = EnergyRates.unitsFromFe(amount, rate);
            if (rate <= 0 || joules <= 0 || !ue.connects(tile, from)) return 0;
            long taken = Math.max(0, Math.min(joules, ue.call(ue.receive, tile, from, joules, !simulate)));
            return EnergyRates.feFromUnits(taken, rate);
        }

        @Override
        public int extractEnergy(int amount, boolean simulate) {
            int rate = LegacyConfig.fePerJouleOut();
            long joules = EnergyRates.unitsFromFe(amount, rate);
            if (rate <= 0 || joules <= 0 || !ue.connects(tile, from)) return 0;
            long taken = Math.max(0, Math.min(joules, ue.call(ue.extract, tile, from, joules, !simulate)));
            return EnergyRates.feFromUnits(taken, rate);
        }

        @Override
        public int getEnergyStored() {
            return EnergyRates.feFromUnits(ue.read(ue.getEnergy, tile, from), LegacyConfig.fePerJoule());
        }

        @Override
        public int getMaxEnergyStored() {
            return EnergyRates.feFromUnits(ue.read(ue.getCapacity, tile, from), LegacyConfig.fePerJoule());
        }

        @Override
        public boolean canExtract() {
            return LegacyConfig.fePerJouleOut() > 0 && ue.connects(tile, from);
        }

        @Override
        public boolean canReceive() {
            return LegacyConfig.fePerJouleIn() > 0 && ue.connects(tile, from);
        }
    }
}
