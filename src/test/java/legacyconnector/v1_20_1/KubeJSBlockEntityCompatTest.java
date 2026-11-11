package legacyconnector.v1_20_1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.latvian.mods.kubejs.block.entity.BlockEntityInfo;
import dev.latvian.mods.kubejs.block.entity.BlockEntityTickKubeEvent;
import dev.latvian.mods.kubejs.block.entity.KubeBlockEntity;
import dev.latvian.mods.kubejs.core.InventoryKJS;
import dev.latvian.mods.kubejs.plugin.builtin.event.BlockEvents;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Regression tests for KubeJS 6 to KubeJS 7 block-entity compatibility. */
class KubeJSBlockEntityCompatTest {
    @BeforeEach
    void resetKubeJsEventFixture() {
        BlockEvents.BLOCK_ENTITY_TICK.reset();
    }

    @Test
    void restoresLegacyInventoryDimensionsUsingTheDefaultAttachmentId() {
        BlockEntityInfo info = new BlockEntityInfo();

        KubeJSBlockEntityCompat.inventory(info, 2, 3);

        BlockEntityInfo.Attachment attachment = info.attachments.get("inventory");
        assertNotNull(attachment);
        assertEquals(2, attachment.width());
        assertEquals(3, attachment.height());
        assertEquals(Set.of(), attachment.directions());
    }

    @Test
    void givesRepeatedLegacyInventoriesUniqueAttachmentIds() {
        BlockEntityInfo info = new BlockEntityInfo();

        KubeJSBlockEntityCompat.inventory(info, 2, 2);
        KubeJSBlockEntityCompat.inventory(info, 1, 3);

        assertTrue(info.attachments.containsKey("inventory"));
        assertTrue(info.attachments.containsKey("inventory_1"));
        assertEquals(2, info.attachments.size());
    }

    @Test
    void legacyRightClickOpensTheFirstInventoryWhenCalledAfterRegistration() {
        BlockEntityInfo info = new BlockEntityInfo();
        KubeJSBlockEntityCompat.inventory(info, 3, 3);
        KubeJSBlockEntityCompat.inventory(info, 1, 1);

        KubeJSBlockEntityCompat.rightClickOpensInventory(info);

        assertEquals("inventory", info.openedInventory);
    }

    @Test
    void legacyRightClickCanBeDeclaredBeforeInventoryRegistration() {
        BlockEntityInfo info = new BlockEntityInfo();
        info.attachments.put("inventory", new BlockEntityInfo.Attachment(Set.of(), 1, 1));

        KubeJSBlockEntityCompat.rightClickOpensInventory(info);
        KubeJSBlockEntityCompat.inventory(info, 2, 2);

        assertEquals("inventory_1", info.openedInventory);
        assertNotNull(info.attachments.get("inventory_1"));
    }

    @Test
    void serverTickRegistersCallbackAgainstTheOwningBlockAndEnablesServerTicking() {
        BlockEntityInfo info = new BlockEntityInfo();
        Object callback = new Object();

        KubeJSBlockEntityCompat.serverTick(info, callback);

        assertTrue(info.serverTicking);
        assertEquals(1, BlockEvents.BLOCK_ENTITY_TICK.calls);
        assertEquals("alienevo:crystal_block", BlockEvents.BLOCK_ENTITY_TICK.lastArguments[0]);
        assertSame(callback, BlockEvents.BLOCK_ENTITY_TICK.lastArguments[1]);
        assertNotNull(BlockEvents.BLOCK_ENTITY_TICK.lastContext);
        assertEquals(1, info.tickFrequency);
        assertEquals(0, info.tickOffset);
    }

    @Test
    void serverTickPreservesFrequencyAndOffset() {
        BlockEntityInfo info = new BlockEntityInfo();

        KubeJSBlockEntityCompat.serverTick(info, 20, 7, new Object());

        assertTrue(info.serverTicking);
        assertEquals(20, info.tickFrequency);
        assertEquals(7, info.tickOffset);
    }

    @Test
    void frequencyOnePreservesLegacyAlwaysTickBehaviorEvenWithAnOffset() {
        BlockEntityInfo info = new BlockEntityInfo();

        KubeJSBlockEntityCompat.serverTick(info, 1, 9, new Object());

        assertEquals(1, info.tickFrequency);
        assertEquals(0, info.tickOffset);
    }

    @Test
    void clientTickOnlyEnablesClientTicking() {
        BlockEntityInfo info = new BlockEntityInfo();

        KubeJSBlockEntityCompat.clientTick(info, new Object());

        assertTrue(info.clientTicking);
        assertEquals(false, info.serverTicking);
    }

    @Test
    void tickEnablesBothLogicalSides() {
        BlockEntityInfo info = new BlockEntityInfo();

        KubeJSBlockEntityCompat.tick(info, new Object());

        assertTrue(info.serverTicking);
        assertTrue(info.clientTicking);
    }

    @Test
    void legacyTickEventExposesBlockEntityStateAndHelpers() {
        KubeBlockEntity entity = new KubeBlockEntity();
        Object expectedData = entity.data;
        Object expectedPlacer = entity.placer;
        Object inventory = new FakeInventory();
        entity.attachments.put("inventory", inventory);
        BlockEntityTickKubeEvent event = new BlockEntityTickKubeEvent(entity);

        assertSame(expectedData, KubeJSBlockEntityCompat.eventData(event));
        assertEquals(11, KubeJSBlockEntityCompat.eventCoordinate(event, "x"));
        assertEquals(22, KubeJSBlockEntityCompat.eventCoordinate(event, "y"));
        assertEquals(33, KubeJSBlockEntityCompat.eventCoordinate(event, "z"));
        assertSame(inventory, KubeJSBlockEntityCompat.eventInventory(event));
        assertSame(expectedPlacer, KubeJSBlockEntityCompat.eventPlacer(event));
        assertSame(entity.placerId, KubeJSBlockEntityCompat.eventPlacerId(event));
        assertSame(entity, KubeJSBlockEntityCompat.eventBlockEntity(event));
    }

    @Test
    void legacyTickEventForwardsSaveSyncAndBlockEvents() {
        KubeBlockEntity entity = new KubeBlockEntity();
        BlockEntityTickKubeEvent event = new BlockEntityTickKubeEvent(entity);

        KubeJSBlockEntityCompat.eventSave(event);
        KubeJSBlockEntityCompat.eventSync(event);
        KubeJSBlockEntityCompat.eventSendBlockEvent(event, 9, 42);

        assertEquals(1, entity.saves);
        assertEquals(1, entity.syncs);
        assertEquals(9, entity.lastEventId);
        assertEquals(42, entity.lastEventData);
    }

    @Test
    void legacyTickEventWithoutAnInventoryReturnsNull() {
        assertNull(KubeJSBlockEntityCompat.eventInventory(
                new BlockEntityTickKubeEvent(new KubeBlockEntity())));
    }

    private static final class FakeInventory implements InventoryKJS {
    }
}
