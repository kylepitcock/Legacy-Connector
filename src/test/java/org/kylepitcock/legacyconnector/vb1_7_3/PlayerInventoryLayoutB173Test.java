package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.ClientPlayerEntity;
import legacyconnector.vb1_7_3.net.minecraft.inventory.CraftingInventory;
import legacyconnector.vb1_7_3.net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.Slot;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PlayerInventoryLayoutB173Test {
    @BeforeAll static void bootstrap() throws Exception {
        var dist = net.neoforged.fml.loading.FMLLoader.class.getDeclaredField("dist");
        dist.setAccessible(true);dist.set(null, net.neoforged.api.distmarker.Dist.CLIENT);
        net.neoforged.fml.loading.LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();
    }
    static class Canonical extends PlayerScreenHandler {
        Canonical(ClientPlayerEntity actor) {
            super(actor.inventory);
            slots.get(0).x = 134;slots.get(0).y = 62;
            for (int i = 0; i < 4; i++) {
                slots.get(i + 1).x = 125 + i % 2 * 18;slots.get(i + 1).y = 8 + i / 2 * 18;
                slots.get(i + 5).x = 62;slots.get(i + 5).y = 8 + i * 18;
            }
            var extra = new CraftingInventory(null, 2, 4);
            for (int i = 0; i < 8; i++) addSlot(new legacyconnector.vb1_7_3.net.minecraft.screen.slot.Slot(extra, i, 80 + i / 4 * 18, 8 + i % 4 * 18));
        }
    }
    static class Fixture {
        final ClientPlayerEntity actor = new ClientPlayerEntity(null) {};
        final Canonical canonical = new Canonical(actor);
        final SimpleContainer main = new SimpleContainer(41), craft = new SimpleContainer(4), result = new SimpleContainer(1), foreign = new SimpleContainer(1);
        final List<Slot> nativeSlots = new ArrayList<>();
        final Slot offhand, foreignSlot;
        Fixture() {
            nativeSlots.add(new Slot(result, 0, 154, 28));
            for (int i = 0; i < 4; i++) nativeSlots.add(new Slot(craft, i, 98 + i % 2 * 18, 18 + i / 2 * 18));
            for (int i = 0; i < 4; i++) nativeSlots.add(new Slot(main, 39 - i, 8, 8 + i * 18));
            for (int i = 9; i < 36; i++) nativeSlots.add(new Slot(main, i, 8 + i % 9 * 18, 84 + (i / 9 - 1) * 18));
            for (int i = 0; i < 9; i++) nativeSlots.add(new Slot(main, i, 8 + i * 18, 142));
            offhand = new Slot(main, 40, 77, 62);nativeSlots.add(offhand);
            foreignSlot = new Slot(foreign, 0, 260, 8);nativeSlots.add(foreignSlot);
            for (int i = 0; i < nativeSlots.size(); i++) nativeSlots.get(i).index = 900 + i;
        }
        List<PlayerInventoryLayoutB173.Position> map() {
            return map(176, 166);
        }
        List<PlayerInventoryLayoutB173.Position> map(int width, int height) {
            return PlayerInventoryLayoutB173.map(nativeSlots, main, craft, result, actor.inventory, canonical, width, height);
        }
    }

    @Test void mapsOriginalCoordinatesByBackingIdentityWithoutChangingNativeStorageIndicesOrPositions() {
        var fixture = new Fixture();
        Map<Slot, int[]> before = new IdentityHashMap<>();
        for (var slot : fixture.nativeSlots) before.put(slot, new int[] {slot.index, slot.x, slot.y});
        var positions = fixture.map();assertEquals(45, positions.size());
        var bySlot = new IdentityHashMap<Slot, PlayerInventoryLayoutB173.Position>();
        for (var position : positions) bySlot.put(position.slot(), position);
        assertEquals(134, bySlot.get(fixture.nativeSlots.get(0)).x());
        assertEquals(62, bySlot.get(fixture.nativeSlots.get(0)).y());
        assertEquals(125, bySlot.get(fixture.nativeSlots.get(1)).x());
        assertEquals(8, bySlot.get(fixture.nativeSlots.get(1)).y());
        assertEquals(62, bySlot.get(fixture.nativeSlots.get(5)).x());
        assertEquals(8, bySlot.get(fixture.nativeSlots.get(5)).y());
        assertFalse(bySlot.containsKey(fixture.offhand));assertFalse(bySlot.containsKey(fixture.foreignSlot));
        for (var slot : fixture.nativeSlots) assertArrayEquals(before.get(slot), new int[] {slot.index, slot.x, slot.y});
        Collections.reverse(fixture.nativeSlots);
        var reordered = fixture.map();assertEquals(45, reordered.size());
        for (var position : reordered) assertEquals(bySlot.get(position.slot()), position);
    }

    @Test void rejectsMissingOrAmbiguousOriginalAndNativeSlotBindings() {
        var missing = new Fixture();missing.nativeSlots.remove(5);assertTrue(missing.map().isEmpty());
        var nativeDuplicate = new Fixture();nativeDuplicate.nativeSlots.add(new Slot(nativeDuplicate.main, 0, 300, 20));
        assertTrue(nativeDuplicate.map(512, 512).isEmpty());
        var originalDuplicate = new Fixture();originalDuplicate.canonical.slots.add(
                new legacyconnector.vb1_7_3.net.minecraft.screen.slot.Slot(originalDuplicate.actor.inventory, 0, 300, 20));
        assertTrue(originalDuplicate.map(512, 512).isEmpty());
        var wrongIdentity = new Fixture();
        assertTrue(PlayerInventoryLayoutB173.map(wrongIdentity.nativeSlots, new SimpleContainer(41), wrongIdentity.craft,
                wrongIdentity.result, wrongIdentity.actor.inventory, wrongIdentity.canonical, 176, 166).isEmpty());
    }

    @Test void rejectsOverlappingExtrasAndInvalidCoordinateExtentsBeforeReturningAnyMapping() {
        var overlap = new Fixture();overlap.canonical.slots.get(45).x = overlap.canonical.slots.get(5).x;
        assertTrue(overlap.map().isEmpty());
        var negative = new Fixture();negative.canonical.slots.get(0).y = -1;assertTrue(negative.map().isEmpty());
        var overflow = new Fixture();overflow.canonical.slots.get(0).x = Integer.MAX_VALUE;assertTrue(overflow.map().isEmpty());
        var outside = new Fixture();outside.canonical.slots.get(0).x = 161;assertTrue(outside.map().isEmpty());
        assertTrue(new Fixture().map(15, 166).isEmpty());
        assertTrue(new Fixture().map(4097, 166).isEmpty());
    }

    @Test void preservesAnotherModsInPanelSlotPresentationByRejectingTheOriginalLayout() {
        var fixture=new Fixture();fixture.foreignSlot.x=20;fixture.foreignSlot.y=8;
        assertTrue(fixture.map().isEmpty());
        assertEquals(20,fixture.foreignSlot.x);assertEquals(8,fixture.foreignSlot.y);
    }
}
