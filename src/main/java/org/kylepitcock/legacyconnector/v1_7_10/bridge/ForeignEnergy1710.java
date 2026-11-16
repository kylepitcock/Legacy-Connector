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
 * ({@code IPowerReceptor}/{@code PowerHandler}, {@code energy.fePerMj}, default 10), in the early forms of Minecraft 1.2.x too (IC2 1.103's flat
 * {@code ic2.api.IEnergySink}, BuildCraft 3.1's {@code buildcraft.api.IPowerReceptor} with its {@code PowerProvider}). The interfaces are found by name
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
    private static final ClassValue<EarlyEu> EARLY_EU = new ClassValue<>() {
        @Override
        protected EarlyEu computeValue(Class<?> type) {
            return EarlyEu.of(type);
        }
    };
    private static final ClassValue<EarlyMj> EARLY_MJ = new ClassValue<>() {
        @Override
        protected EarlyMj computeValue(Class<?> type) {
            return EarlyMj.of(type);
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
        EarlyEu early = EARLY_EU.get(tile.getClass());
        if (early.sink != null) return side != null && !early.accepts(tile, from) ? null : new EarlyEuStorage(tile, from, early);
        EarlyMj earlyMj = EARLY_MJ.get(tile.getClass());
        if (earlyMj.receptor != null) return new EarlyMjStorage(tile, from, earlyMj);
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
            int rate = LegacyConfig.fePerEuIn();
            if (rate <= 0 || !eu.accepts(tile, from)) return 0;
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

    // ------------------------------------------------------------------ IC2 1.103 (Minecraft 1.2.x) EU

    /** The flat 1.2.x API: {@code ic2.api.IEnergySink} with {@code demandsEnergy()} and {@code injectEnergy(Direction, int)} (returns what it could not take). */
    private static final class EarlyEu {
        Class<?> sink;
        Class<?> direction;
        Method demands;
        Method inject;
        Method accepts;

        static EarlyEu of(Class<?> type) {
            EarlyEu e = new EarlyEu();
            e.sink = named(type, "ic2.api.IEnergySink");
            if (e.sink == null) return e;
            try {
                e.direction = Class.forName("ic2.api.Direction", false, type.getClassLoader());
            } catch (ReflectiveOperationException | LinkageError x) {
                e.sink = null;
                return e;
            }
            e.demands = method(e.sink, "demandsEnergy");
            e.inject = method(e.sink, "injectEnergy", e.direction, int.class);
            e.accepts = method(e.sink, "acceptsEnergyFrom", TileEntity.class, e.direction);
            if (e.demands == null || e.inject == null) e.sink = null;
            return e;
        }

        /** IC2's Direction constant named like the side ({@code XN}, {@code XP}, {@code YN}, {@code YP}, {@code ZN}, {@code ZP}). */
        Object direction(ForgeDirection from) {
            String name = switch (from) {
                case WEST -> "XN";
                case EAST -> "XP";
                case DOWN -> "YN";
                case UP -> "YP";
                case NORTH -> "ZN";
                case SOUTH -> "ZP";
                default -> null;
            };
            if (name == null) return null;
            for (Object c : direction.getEnumConstants()) if (c.toString().equals(name)) return c;
            return null;
        }

        boolean accepts(TileEntity tile, ForgeDirection from) {
            Object d = direction(from);
            if (d == null) return from == ForgeDirection.UNKNOWN;
            try {
                return accepts == null || (boolean) accepts.invoke(tile, tile, d);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                return false;
            }
        }

        boolean demands(TileEntity tile) {
            try {
                return (boolean) demands.invoke(tile);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                return false;
            }
        }
    }

    /** One packet at most (IC2 1.103's low-voltage packet is 32 EU), and only while the sink asks for energy. */
    private record EarlyEuStorage(TileEntity tile, ForgeDirection from, EarlyEu eu) implements IEnergyStorage {
        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            int rate = LegacyConfig.fePerEuIn();
            if (rate <= 0 || !eu.accepts(tile, from) || !eu.demands(tile)) return 0;
            int offer = (int) Math.min(32, EnergyRates.unitsFromFe(amount, rate));
            if (offer <= 0) return 0;
            if (simulate) return EnergyRates.feFromUnits(offer, rate);
            try {
                Object d = eu.direction(from);
                if (d == null) return 0;
                int leftover = ((Number) eu.inject.invoke(tile, d, offer)).intValue();
                return EnergyRates.feFromUnits(Math.max(0, offer - leftover), rate);
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
            return 0;
        }

        @Override
        public int getMaxEnergyStored() {
            return eu.demands(tile) ? EnergyRates.feFromUnits(32, LegacyConfig.fePerEu()) : 0;
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return LegacyConfig.fePerEuIn() > 0 && eu.accepts(tile, from);
        }
    }

    // ------------------------------------------------------------------ BuildCraft 3.1 (Minecraft 1.2.x) MJ

    /** The flat 1.2.x API: {@code buildcraft.api.IPowerReceptor.getPowerProvider()} and the provider's public energy fields. */
    private static final class EarlyMj {
        Class<?> receptor;
        Class<?> provider;
        Class<?> orientation;
        Method getProvider;
        Method receive;
        Method request;
        java.lang.reflect.Field stored;
        java.lang.reflect.Field capacity;
        java.lang.reflect.Field min;
        java.lang.reflect.Field max;

        static EarlyMj of(Class<?> type) {
            EarlyMj m = new EarlyMj();
            m.receptor = named(type, "buildcraft.api.IPowerReceptor");
            if (m.receptor == null) return m;
            try {
                ClassLoader loader = type.getClassLoader();
                m.provider = Class.forName("buildcraft.api.PowerProvider", false, loader);
                m.orientation = Class.forName("buildcraft.api.Orientations", false, loader);
                m.getProvider = method(m.receptor, "getPowerProvider");
                m.request = method(m.receptor, "powerRequest");
                m.receive = method(m.provider, "receiveEnergy", float.class, m.orientation);
                m.stored = m.provider.getField("energyStored");
                m.capacity = m.provider.getField("maxEnergyStored");
                m.min = m.provider.getField("minEnergyReceived");
                m.max = m.provider.getField("maxEnergyReceived");
            } catch (ReflectiveOperationException | LinkageError e) {
                m.receptor = null;
            }
            if (m.getProvider == null || m.receive == null) m.receptor = null;
            return m;
        }

        Object orientation(ForgeDirection from) {
            String name = switch (from) {
                case DOWN -> "YNeg";
                case UP -> "YPos";
                case NORTH -> "ZNeg";
                case SOUTH -> "ZPos";
                case WEST -> "XNeg";
                case EAST -> "XPos";
                default -> "Unknown";
            };
            for (Object c : orientation.getEnumConstants()) if (c.toString().equals(name)) return c;
            return null;
        }

        Object provider(TileEntity tile) {
            try {
                return getProvider.invoke(tile);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                return null;
            }
        }

        double field(java.lang.reflect.Field f, Object provider, double fallback) {
            if (f == null) return fallback;
            try {
                return ((Number) f.get(provider)).doubleValue();
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                return fallback;
            }
        }
    }

    private record EarlyMjStorage(TileEntity tile, ForgeDirection from, EarlyMj mj) implements IEnergyStorage {
        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            int rate = LegacyConfig.fePerMjIn();
            if (rate <= 0) return 0;
            Object p = mj.provider(tile);
            if (p == null) return 0;
            double want = mj.field(mj.max, p, Double.MAX_VALUE);
            if (mj.request != null) {
                try {
                    want = Math.min(want, ((Number) mj.request.invoke(tile)).doubleValue());
                } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                    return 0;
                }
            }
            double room = mj.field(mj.capacity, p, Double.MAX_VALUE) - mj.field(mj.stored, p, 0);
            double offer = Math.min(Math.min((double) EnergyRates.unitsFromFe(amount, rate), want), room);
            if (offer <= 0 || offer < mj.field(mj.min, p, 0)) return 0;
            if (simulate) return EnergyRates.feFromUnits(offer, rate);
            try {
                mj.receive.invoke(p, (float) offer, mj.orientation(from));
                return EnergyRates.feFromUnits(offer, rate);
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
            Object p = mj.provider(tile);
            return p == null ? 0 : EnergyRates.feFromUnits(mj.field(mj.stored, p, 0), LegacyConfig.fePerMj());
        }

        @Override
        public int getMaxEnergyStored() {
            Object p = mj.provider(tile);
            return p == null ? 0 : EnergyRates.feFromUnits(mj.field(mj.capacity, p, 0), LegacyConfig.fePerMj());
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return LegacyConfig.fePerMjIn() > 0 && mj.provider(tile) != null;
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
            int rate = LegacyConfig.fePerMjIn();
            if (rate <= 0) return 0;
            Object r = mj.receiver(tile, from);
            if (r == null) return 0;
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
            return LegacyConfig.fePerMjIn() > 0 && mj.receiver(tile, from) != null;
        }
    }
}
