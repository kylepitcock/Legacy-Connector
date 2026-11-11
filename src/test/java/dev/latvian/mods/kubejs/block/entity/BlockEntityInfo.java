package dev.latvian.mods.kubejs.block.entity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Test fixture matching the KubeJS 7 public BlockEntityInfo surface. */
public final class BlockEntityInfo {
    public final Map<String, Attachment> attachments = new LinkedHashMap<>();
    public final BlockBuilder blockBuilder = new BlockBuilder("alienevo:crystal_block");
    public String openedInventory;
    public boolean serverTicking;
    public boolean clientTicking;
    public int tickFrequency = 1;
    public int tickOffset;

    public void inventory(String id, Set<?> directions, int width, int height) {
        attachments.put(id, new Attachment(directions, width, height));
    }

    public void rightClickOpensInventory(String id) {
        openedInventory = id;
    }

    public void serverTicking() {
        serverTicking = true;
    }

    public void clientTicking() {
        clientTicking = true;
    }

    public void tickFrequency(int frequency) {
        tickFrequency = Math.max(1, frequency);
    }

    public void tickOffset(int offset) {
        tickOffset = Math.max(0, offset);
    }

    public static final class BlockBuilder {
        public final String id;

        public BlockBuilder(String id) {
            this.id = id;
        }
    }

    public record Attachment(Set<?> directions, int width, int height) {}
}
