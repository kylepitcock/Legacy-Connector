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

    /**
     * Drives a legacy tile like a player or a pipe would: {@code fluid:name:amount} fills through its IFluidHandler (all sides, then UNKNOWN),
     * {@code item:slot:registry name:count[:damage]} sets an inventory slot, {@code ore:name} lists what the ore dictionary holds for a name.
     * Returns what each step answered so a scenario can show why a machine stays idle.
     */
    public static String drive(Level level, BlockPos pos, String... steps) {
        StringBuilder r = new StringBuilder();
        try {
            if (!(level.getBlockEntity(pos) instanceof LegacyBlockEntity host)) return "NOBE " + level.getBlockState(pos);
            Object t = host.legacy();
            for (String step : steps) {
                String[] p = step.split(":");
                try {
                    switch (p[0]) {
                        case "fluid" -> {
                            var fs = legacyconnector.v1_7_10.net.minecraftforge.fluids.FluidRegistry.getFluidStack(p[1], Integer.parseInt(p[2]));
                            int did = -1;
                            if (t instanceof IFluidHandler h) {
                                ForgeDirection used = null;
                                for (ForgeDirection d : new ForgeDirection[] {ForgeDirection.UP, ForgeDirection.DOWN, ForgeDirection.NORTH, ForgeDirection.UNKNOWN}) {
                                    if (h.canFill(d, fs.getFluid())) {
                                        did = h.fill(d, fs, true);
                                        used = d;
                                        break;
                                    }
                                }
                                r.append(" fluid ").append(p[1]).append(" via ").append(used).append(" filled=").append(did);
                            } else r.append(" fluid: not an IFluidHandler");
                        }
                        case "item" -> {
                            var item = (legacyconnector.v1_7_10.net.minecraft.item.Item) legacyconnector.v1_7_10.net.minecraft.item.Item.itemRegistry
                                    .getObject(p[2]);
                            if (item == null) r.append(" item ").append(p[2]).append(" unknown");
                            else {
                                int damage = p.length > 4 ? Integer.parseInt(p[4]) : 0;
                                ((legacyconnector.v1_7_10.net.minecraft.inventory.IInventory) t).setInventorySlotContents(Integer.parseInt(p[1]),
                                        new legacyconnector.v1_7_10.net.minecraft.item.ItemStack(item, Integer.parseInt(p[3]), damage));
                                r.append(" slot").append(p[1]).append('=').append(item.getUnlocalizedName());
                            }
                        }
                        case "field" -> {
                            Object v = null;
                            for (Class<?> k = t.getClass(); k != null; k = k.getSuperclass()) {
                                try {
                                    var f = k.getDeclaredField(p[1]);
                                    f.setAccessible(true);
                                    v = f.get(t);
                                    break;
                                } catch (NoSuchFieldException e) {
                                    // look in the superclass
                                }
                            }
                            r.append(" ").append(p[1]).append('=').append(v);
                        }
                        case "craftdiag" -> {
                            // the tile's own crafting grid against the legacy recipe list (a machine that matches recipes itself, e.g. a Liquicrafter)
                            var f = t.getClass().getDeclaredField("craft");
                            f.setAccessible(true);
                            var grid = (legacyconnector.v1_7_10.net.minecraft.inventory.InventoryCrafting) f.get(t);
                            var list = legacyconnector.v1_7_10.net.minecraft.item.crafting.CraftingManager.getInstance().getRecipeList();
                            var w = legacyconnector.v1_7_10.net.minecraft.world.World.mcc$wrap(level);
                            int total = list.size(), matched = 0;
                            StringBuilder cells = new StringBuilder();
                            for (int i = 0; i < 9; i++) cells.append(grid.getStackInSlot(i)).append(',');
                            for (Object o : list) {
                                if (o instanceof legacyconnector.v1_7_10.net.minecraft.item.crafting.IRecipe rec && rec.matches(grid, w)) matched++;
                            }
                            // the recipe of whatever the machine's output slot holds: its class and how it sees the grid
                            var out = ((legacyconnector.v1_7_10.net.minecraft.inventory.IInventory) t).getStackInSlot(9);
                            for (Object o : list) {
                                if (o instanceof legacyconnector.v1_7_10.net.minecraft.item.crafting.IRecipe rec && out != null && rec.getRecipeOutput() != null
                                        && rec.getRecipeOutput().getItem() == out.getItem()) {
                                    r.append(" [").append(rec.getClass().getSimpleName()).append(" size=").append(rec.getRecipeSize()).append(" matches=").append(rec.matches(grid, w));
                                    if (rec instanceof legacyconnector.v1_7_10.net.minecraftforge.oredict.ShapedOreRecipe so) r.append(" input=").append(java.util.Arrays.toString(so.getInput()));
                                    if (rec instanceof legacyconnector.v1_7_10.net.minecraft.item.crafting.ShapedRecipes sr) r.append(" items=").append(java.util.Arrays.toString(sr.recipeItems));
                                    r.append(']');
                                }
                            }
                            r.append(" recipes=").append(total).append(" matched=").append(matched).append(" grid=").append(cells);
                        }
                        case "ore" -> {
                            var list = legacyconnector.v1_7_10.net.minecraftforge.oredict.OreDictionary.getOres(p[1]);
                            r.append(" ore ").append(p[1]).append('=').append(list == null ? "null" : list.size());
                        }
                        default -> r.append(" ?").append(step);
                    }
                } catch (Throwable e) {
                    r.append(" ERR(").append(step).append(") ").append(e);
                }
            }
        } catch (Throwable e) {
            r.append(" ERR ").append(e);
        }
        return r.toString();
    }

    /**
     * Uses a legacy item the way a player right-click does: first on {@code first} (picks up a spawner with a portable spawner), then, after the
     * item's own cooldown tag is cleared, on the top of {@code second}. Reports each step's answer, the stack's NBT and what stands at the result.
     */
    public static String useItemTwice(Level level, net.minecraft.server.level.ServerPlayer player, String itemName, BlockPos first, BlockPos second) {
        StringBuilder r = new StringBuilder();
        try {
            legacyconnector.v1_7_10.net.minecraft.item.Item item = null;
            for (var it : org.kylepitcock.legacyconnector.v1_7_10.bridge.ItemBridge.modItems()) if (it.getUnlocalizedName().toLowerCase().contains(itemName.toLowerCase())) item = it;
            if (item == null) return "unknown item " + itemName;
            var w = legacyconnector.v1_7_10.net.minecraft.world.World.mcc$wrap(level);
            var p = (legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer) org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge.wrap(player);
            var stack = new legacyconnector.v1_7_10.net.minecraft.item.ItemStack(item, 1, 0);
            boolean a = item.onItemUse(stack, p, w, first.getX(), first.getY(), first.getZ(), 1, 0.5F, 1.0F, 0.5F);
            r.append("use1=").append(a).append(" nbt=").append(stack.getTagCompound()).append(" blockAfter=").append(level.getBlockState(first).getBlock());
            if (stack.getTagCompound() != null) stack.getTagCompound().setInteger("placeDelay", 0);
            boolean b = item.onItemUse(stack, p, w, second.getX(), second.getY(), second.getZ(), 1, 0.5F, 1.0F, 0.5F);
            BlockPos at = second.above();
            r.append(" use2=").append(b).append(" placed=").append(level.getBlockState(at).getBlock()).append(" stackSize=").append(legacyconnector.v1_7_10.net.minecraft.item.ItemStack.mcc$get$stackSize(stack));
            var be = level.getBlockEntity(at);
            r.append(" be=").append(be == null ? "null" : be.getClass().getSimpleName());
            if (be != null) r.append(' ').append(be.saveWithoutMetadata(level.registryAccess()));
        } catch (Throwable e) {
            r.append(" ERR ").append(e);
        }
        return r.toString();
    }

    /** One scenario line: {@code "x y z step step..."} (see {@link #drive}); answers the tile's state and inventory, then the drive results. */
    public static String run(Level level, String spec) {
        String[] w = spec.trim().split("\\s+");
        BlockPos pos = new BlockPos(Integer.parseInt(w[0]), Integer.parseInt(w[1]), Integer.parseInt(w[2]));
        String driven = w.length > 3 ? drive(level, pos, java.util.Arrays.copyOfRange(w, 3, w.length)) : "";
        return pos + " " + describe(level, pos, spec.contains("feed")) + " items:" + contents(level, pos) + " |" + driven;
    }

    /** The items of a legacy inventory tile, "slot=item x count" for the non-empty ones. */
    public static String contents(Level level, BlockPos pos) {
        try {
            if (!(level.getBlockEntity(pos) instanceof LegacyBlockEntity host)) return "NOBE";
            var inv = (legacyconnector.v1_7_10.net.minecraft.inventory.IInventory) host.legacy();
            StringBuilder r = new StringBuilder();
            for (int i = 0; i < inv.getSizeInventory(); i++) {
                var st = inv.getStackInSlot(i);
                if (st != null) r.append(' ').append(i).append('=').append(st);
            }
            return r.length() == 0 ? "(empty)" : r.toString();
        } catch (Throwable e) {
            return "ERR " + e;
        }
    }
}
