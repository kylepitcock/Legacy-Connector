package legacyconnector.v1_20_1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.latvian.mods.kubejs.block.entity.BlockEntityInfo;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Regression tests for the KubeJS 6 to KubeJS 7 block-entity inventory bridge. */
class KubeJSBlockEntityCompatTest {
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
}
