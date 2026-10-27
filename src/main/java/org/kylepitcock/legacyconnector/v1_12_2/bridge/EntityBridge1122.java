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
            else if (modern instanceof net.minecraft.world.entity.player.Player) view = new PlainPlayer(world);
            else if (modern instanceof net.minecraft.world.entity.LivingEntity) view = new PlainLiving(world);
            else view = new PlainEntity(world);
            view.mcc$entity = modern;
            ENTITIES.put(modern, view);
        }
        return view;
    }

    /** A client-side player (1.12.2's EntityPlayerSP and EntityOtherPlayerMP are not modelled yet). */
    private static final class PlainPlayer extends legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer {
        PlainPlayer(World world) {
            super(world);
        }
    }

    /** A modern living entity with no legacy class of its own. */
    private static final class PlainLiving extends legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase {
        PlainLiving(World world) {
            super(world);
        }
    }

    /** A modern entity with no legacy class of its own. */
    private static final class PlainEntity extends Entity {
        PlainEntity(World world) {
            super(world);
        }
    }
}
