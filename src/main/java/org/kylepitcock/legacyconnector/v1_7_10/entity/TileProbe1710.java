package org.kylepitcock.legacyconnector.v1_7_10.entity;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection;
import legacyconnector.v1_7_10.net.minecraftforge.fluids.IFluidHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.kylepitcock.legacyconnector.v1_7_10.adapter.LegacyBlockEntity;

/**
 * Test aid for the client scenarios ({@code tools/dev/verify/scenarios}): describes a legacy tile entity from its public methods by name
 * (energy, work counters, tanks), because the scripting engine cannot reflect over mod classes that mention absent optional APIs.
 * Reads only; with {@code feed} it first pushes RF into the tile so a powered machine has something to work with.
 */
public final class TileProbe1710 {
    private TileProbe1710() {}

    public static String describe(Level level, BlockPos pos, boolean feed) {
        try {
            if (!(level.getBlockEntity(pos) instanceof LegacyBlockEntity host)) return "NOBE " + level.getBlockState(pos);
            Object t = host.legacy();
            StringBuilder r = new StringBuilder(t.getClass().getSimpleName());
            MethodHandles.Lookup lk = MethodHandles.lookup();
            Class<?> c = t.getClass();
            if (feed) {
                try {
                    r.append(" fed=").append((int) lk.findVirtual(c, "receiveEnergy", MethodType.methodType(int.class, ForgeDirection.class, int.class, boolean.class))
                            .invoke(t, ForgeDirection.UNKNOWN, 100000, false));
                } catch (Throwable e) {
                    r.append(" feed?").append(e);
                }
            }
            try {
                r.append(" energy=").append((int) lk.findVirtual(c, "getEnergyStored", MethodType.methodType(int.class, ForgeDirection.class)).invoke(t, ForgeDirection.UNKNOWN));
            } catch (Throwable e) {
                r.append(" energy?").append(e);
            }
            try {
                r.append(" work=").append((int) lk.findVirtual(c, "getWorkDone", MethodType.methodType(int.class)).invoke(t)).append('/')
                        .append((int) lk.findVirtual(c, "getWorkMax", MethodType.methodType(int.class)).invoke(t));
            } catch (Throwable e) {
                r.append(" work?").append(e);
            }
            if (t instanceof IFluidHandler h) {
                var info = h.getTankInfo(ForgeDirection.UNKNOWN);
                if (info != null) {
                    for (int i = 0; i < info.length; i++) {
                        r.append(" tank").append(i).append('=').append(info[i].fluid == null ? "empty" : info[i].fluid.getFluid().getName() + ":" + info[i].fluid.amount);
                    }
                }
            }
            return r.toString();
        } catch (Throwable e) {
            return "ERR " + e;
        }
    }

    /** Puts a vanilla record into slot 0 of a legacy inventory tile and reports what redstone and the machine's own record checks say. */
    public static String jukebox(Level level, BlockPos pos) {
        try {
            if (!(level.getBlockEntity(pos) instanceof LegacyBlockEntity host)) return "NOBE";
            var t = host.legacy();
            var w = legacyconnector.v1_7_10.net.minecraft.world.World.mcc$wrap(level);
            var inv = (legacyconnector.v1_7_10.net.minecraft.inventory.IInventory) t;
            inv.setInventorySlotContents(0, new legacyconnector.v1_7_10.net.minecraft.item.ItemStack(legacyconnector.v1_7_10.net.minecraft.init.Items.record_13, 1));
            MethodHandles.Lookup lk = MethodHandles.lookup();
            boolean canPlay = (boolean) lk.findVirtual(t.getClass(), "getCanPlay", MethodType.methodType(boolean.class)).invoke(t);
            return "slot0=" + inv.getStackInSlot(0) + " canPlay=" + canPlay + " powered=" + w.isBlockIndirectlyGettingPowered(pos.getX(), pos.getY(), pos.getZ());
        } catch (Throwable e) {
            return "ERR " + e;
        }
    }

    /** What 1.7.10 world queries answer at a position: rain, biome flags, the top segment and what is in the column above. */
    public static String worldDiag(Level level, BlockPos pos) {
        try {
            var w = legacyconnector.v1_7_10.net.minecraft.world.World.mcc$wrap(level);
            var biome = w.getBiomeGenForCoords(pos.getX(), pos.getZ());
            StringBuilder r = new StringBuilder();
            r.append("raining=").append(w.getWorldInfo().isRaining()).append(" biome=").append(biome == null ? "null" : biome.biomeName)
                    .append(" lightning=").append(biome != null && biome.canSpawnLightningBolt()).append(" snow=").append(biome != null && biome.getEnableSnow())
                    .append(" temp=").append(biome == null ? 0 : biome.getFloatTemperature(pos.getX(), pos.getY(), pos.getZ()))
                    .append(" top=").append(w.getChunkFromBlockCoords(pos.getX(), pos.getZ()).getTopFilledSegment());
            r.append(" column:");
            for (int y = pos.getY() + 1; y < pos.getY() + 6; y++) {
                var b = w.getBlock(pos.getX(), y, pos.getZ());
                r.append(' ').append(y).append('=').append(b == null ? "null" : b.getClass().getSimpleName() + (b.getCollisionBoundingBoxFromPool(w, pos.getX(), y, pos.getZ()) == null ? "(passable)" : "(solid)"));
            }
            return r.toString();
        } catch (Throwable e) {
            return "ERR " + e;
        }
    }
}
