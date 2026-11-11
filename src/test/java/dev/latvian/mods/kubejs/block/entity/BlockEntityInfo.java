package dev.latvian.mods.kubejs.block.entity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Test fixture matching the KubeJS 7 public BlockEntityInfo surface. */
public final class BlockEntityInfo {
    public final Map<String, Attachment> attachments = new LinkedHashMap<>();
    public String openedInventory;

    public void inventory(String id, Set<?> directions, int width, int height) {
        attachments.put(id, new Attachment(directions, width, height));
    }

    public void rightClickOpensInventory(String id) {
        openedInventory = id;
    }

    public record Attachment(Set<?> directions, int width, int height) {}
}
