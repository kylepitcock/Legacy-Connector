package org.kylepitcock.legacyconnector.v1_7_10.entity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.kylepitcock.legacyconnector.v1_7_10.adapter.LegacyBlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Save and restart round trip of legacy tile entities, for the self test server ({@code -Dlegacyconnector.tileRoundTrip=place} on the first run,
 * {@code =check} on a second run over the same world). "place": every placed tile entity with an inventory gets a stack in its first slot and
 * {@code tile-roundtrip.txt} lists them; the self test then keeps the blocks and the server's stop saves the world. "check": nothing is placed;
 * each listed tile entity is looked up again and must have the same legacy class and the stack in its slot.
 */
final class TileRoundTrip {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/TileRoundTrip");
    private static final List<String> RECORDED = new ArrayList<>();

    private TileRoundTrip() {}

    static String mode() {
        return System.getProperty("legacyconnector.tileRoundTrip", "");
    }

    /** Puts a stack into the first slot of a tile entity that has an inventory and remembers it. */
    static void fill(BlockPos pos, String name, legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity te) {
        if (!(te instanceof legacyconnector.v1_7_10.net.minecraft.inventory.IInventory inv)) return;
        try {
            if (inv.getSizeInventory() <= 0) return;
            inv.setInventorySlotContents(0, new legacyconnector.v1_7_10.net.minecraft.item.ItemStack(legacyconnector.v1_7_10.net.minecraft.init.Items.coal, 7));
            te.markDirty();
            RECORDED.add(pos.getX() + " " + pos.getY() + " " + pos.getZ() + " " + te.getClass().getName() + " " + name);
        } catch (RuntimeException | LinkageError e) {
            LOG.warn("Could not fill {} at {}: {}", te.getClass().getName(), pos.toShortString(), e.toString());
        }
    }

    /** Two classes registered under one id and one class nobody registered must all save and load back as their own class. */
    public static final class IdA extends legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity {}
    public static final class IdB extends legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity {}
    public static final class IdC extends legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity {}
    public static final class IdD extends legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity {}

    static void idTest() {
        var te = legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity.class;
        te_add(IdA.class, "mcc_roundtrip_test");
        te_add(IdB.class, "mcc_roundtrip_test");
        List<String> bad = new ArrayList<>();
        for (legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity t : new legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity[] {new IdA(), new IdB(), new IdC()}) {
            try {
                var tag = new legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound();
                t.writeToNBT(tag);
                var back = legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity.createAndLoadEntity(tag);
                if (back == null || back.getClass() != t.getClass()) bad.add(t.getClass().getSimpleName() + " saved as " + tag.getString("id") + " came back as " + (back == null ? null : back.getClass().getSimpleName()));
            } catch (RuntimeException e) {
                bad.add(t.getClass().getSimpleName() + " threw " + e);
            }
        }
        // after a restart nothing wrote the fallback id of this class yet: it must resolve by the class name alone
        var fresh = new legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound();
        fresh.setString("id", "mcc:" + IdD.class.getName());
        var loaded = legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity.createAndLoadEntity(fresh);
        if (!(loaded instanceof IdD)) bad.add("fallback id of a class not seen yet came back as " + loaded);
        LOG.info("TILE ID TEST: {}", bad.isEmpty() ? "ok (a shared id and an unregistered class save and load back)" : "FAILED " + bad);
    }

    private static void te_add(Class<?> c, String id) {
        legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity.addMapping(c, id);
    }

    static void write(Path dir) {
        try {
            Files.write(dir.resolve("tile-roundtrip.txt"), RECORDED, StandardCharsets.UTF_8);
            LOG.info("TILE ROUNDTRIP place: {} tile entities filled: {}", RECORDED.size(), RECORDED);
        } catch (IOException e) {
            LOG.error("Could not write the round trip list", e);
        }
    }

    static void check(ServerLevel level, Path dir) {
        List<String> lines;
        try {
            lines = Files.readAllLines(dir.resolve("tile-roundtrip.txt"), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.error("TILE ROUNDTRIP check: no list from the place run");
            return;
        }
        int ok = 0;
        List<String> lost = new ArrayList<>();
        for (String line : lines) {
            String[] p = line.split(" ");
            if (p.length < 5) continue;
            BlockPos pos = new BlockPos(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]));
            level.getChunk(pos);
            BlockEntity be = level.getBlockEntity(pos);
            String why = null;
            if (!(be instanceof LegacyBlockEntity l) || l.legacy() == null) {
                why = "no legacy tile entity";
            } else if (!l.legacy().getClass().getName().equals(p[3])) {
                why = "class " + l.legacy().getClass().getName();
            } else if (l.legacy() instanceof legacyconnector.v1_7_10.net.minecraft.inventory.IInventory inv) {
                var stack = inv.getStackInSlot(0);
                if (stack == null || legacyconnector.v1_7_10.net.minecraft.item.ItemStack.mcc$get$stackSize(stack) != 7) why = "slot 0 is " + stack;
            }
            if (why == null) ok++;
            else lost.add(p[4] + " at " + pos.toShortString() + ": " + why);
        }
        LOG.info("TILE ROUNDTRIP check: {} of {} tile entities came back with their items; lost: {}", ok, lines.size(), lost);
    }
}
