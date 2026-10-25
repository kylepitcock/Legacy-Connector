package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import java.util.Map;
import java.util.WeakHashMap;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import net.minecraft.server.level.ServerPlayer;

/** Legacy views of modern entities, one per entity. */
public final class EntityBridge1122 {
    private static final Map<net.minecraft.world.entity.Entity, Entity> ENTITIES = java.util.Collections.synchronizedMap(new WeakHashMap<>());

    static {
        org.kylepitcock.legacyconnector.core.ViewSweeper.register("1.12.2 entity", ENTITIES,
                e -> e.isRemoved() && !(e instanceof net.minecraft.world.entity.player.Player)); // players change dimension in place
    }

    private EntityBridge1122() {}

    public static synchronized Entity wrap(net.minecraft.world.entity.Entity modern) {
        if (modern == null) return null;
        Entity view = ENTITIES.get(modern);
        if (view == null) {
            World world = WorldBridge1122.wrap(modern.level());
            if (modern instanceof ServerPlayer) view = new EntityPlayerMP(world);
            else view = new PlainEntity(world);
            view.mcc$entity = modern;
            ENTITIES.put(modern, view);
        }
        return view;
    }

    /** A modern entity with no legacy class of its own. */
    private static final class PlainEntity extends Entity {
        PlainEntity(World world) {
            super(world);
        }
    }
}
