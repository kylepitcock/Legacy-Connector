package org.kylepitcock.legacyconnector.v1_7_10.client;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge;

/** Client-only half of {@code World.loadedEntityList}: the entities the client level currently knows (in its own class so servers never load ClientLevel). */
public final class ClientEntities {
    private ClientEntities() {}

    public static List<Entity> all(Level level) {
        List<Entity> out = new ArrayList<>();
        if (level instanceof ClientLevel c) for (var e : c.entitiesForRendering()) out.add(EntityBridge.wrap(e));
        return out;
    }
}
