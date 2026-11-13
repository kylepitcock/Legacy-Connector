package dev.latvian.mods.kubejs.block.entity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class KubeBlockEntity {
    public final int x = 11;
    public final int y = 22;
    public final int z = 33;
    public int tick = 44;
    public int cycle = 55;
    public final Object data = new Object();
    public final Map<String, Object> attachments = new LinkedHashMap<>();
    public final UUID placerId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public Object placer = new Object();
    public int saves;
    public int syncs;
    public int lastEventId;
    public int lastEventData;

    public Object getPlacer() {
        return placer;
    }

    public void save() {
        saves++;
    }

    public void sync() {
        syncs++;
    }

    public void sendEvent(int eventId, int data) {
        lastEventId = eventId;
        lastEventData = data;
    }
}
