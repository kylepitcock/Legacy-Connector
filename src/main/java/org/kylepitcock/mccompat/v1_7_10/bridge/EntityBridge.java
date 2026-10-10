package org.kylepitcock.mccompat.v1_7_10.bridge;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import mccompat.v1_7_10.net.minecraft.entity.Entity;
import mccompat.v1_7_10.net.minecraft.entity.EntityLivingBase;
import mccompat.v1_7_10.net.minecraft.entity.item.EntityItem;
import mccompat.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import mccompat.v1_7_10.net.minecraft.entity.player.EntityPlayerClient;
import mccompat.v1_7_10.net.minecraft.entity.player.EntityPlayerMP;
import mccompat.v1_7_10.net.minecraft.world.World;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Legacy views of modern entities. One view per modern entity (weakly cached) so that legacy identity checks
 * ({@code entity == player}) and per-entity legacy state keep working.
 */
public final class EntityBridge {
    private static final Logger LOG = LoggerFactory.getLogger("MCCompat/Entities");
    private static final Map<net.minecraft.world.entity.Entity, Entity> VIEWS = Collections.synchronizedMap(new WeakHashMap<>());
    private static boolean warnedCustom;

    private EntityBridge() {}

    public static Entity wrap(net.minecraft.world.entity.Entity modern) {
        if (modern == null) return null;
        Entity view = VIEWS.get(modern);
        if (view != null) return view;
        view = create(modern);
        VIEWS.put(modern, view);
        return view;
    }

    private static Entity create(net.minecraft.world.entity.Entity modern) {
        if (modern instanceof ServerPlayer sp) return new EntityPlayerMP(sp);
        if (modern instanceof Player p) {
            if (!p.level().isClientSide()) return new EntityPlayerClient(p);
            // The local player is a LocalPlayer (client class); compare by name to keep this class server-safe.
            boolean local = false;
            for (Class<?> c = p.getClass(); c != null && !local; c = c.getSuperclass()) {
                local = c.getName().equals("net.minecraft.client.player.LocalPlayer");
            }
            return local ? new mccompat.v1_7_10.net.minecraft.client.entity.EntityClientPlayerMP(p)
                    : new mccompat.v1_7_10.net.minecraft.client.entity.EntityOtherPlayerMP(p);
        }
        if (modern instanceof ItemEntity item) return new EntityItem(item);
        if (modern instanceof LivingEntity living) return new LivingView(living);
        return new View(modern);
    }

    /** Registers an existing legacy object as the view of a freshly created modern entity. */
    public static void bind(Entity legacy, net.minecraft.world.entity.Entity modern) {
        legacy.mcc$entity = modern;
        VIEWS.put(modern, legacy);
    }

    public static boolean spawn(World world, Entity entity) {
        Level level = world.mcc$fullLevel();
        if (level == null) return false;
        if (entity.mcc$entity == null) {
            if (!warnedCustom) {
                warnedCustom = true;
                LOG.warn("Legacy mod tried to spawn custom entity {}; legacy entity classes are not supported yet", entity.getClass().getName());
            }
            return false;
        }
        VIEWS.put(entity.mcc$entity, entity);
        return world.mcc$level().addFreshEntity(entity.mcc$entity);
    }

    public static void travelToDimension(Entity entity, int dimension) {
        if (entity.mcc$entity == null || !(entity.mcc$entity.level() instanceof ServerLevel current)) return;
        ServerLevel target = DimensionBridge.level(current.getServer(), dimension);
        if (target == null) return;
        var m = entity.mcc$entity;
        if (m instanceof ServerPlayer sp) {
            sp.teleportTo(target, sp.getX(), sp.getY(), sp.getZ(), sp.getYRot(), sp.getXRot());
        } else {
            m.changeDimension(new net.minecraft.world.level.portal.DimensionTransition(target, m.position(), m.getDeltaMovement(), m.getYRot(),
                    m.getXRot(), net.minecraft.world.level.portal.DimensionTransition.DO_NOTHING));
        }
    }

    public static String legacyName(Entity entity) {
        if (entity.mcc$entity == null) return entity.getClass().getSimpleName();
        var key = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.mcc$entity.getType());
        return key.getPath();
    }

    public static EntityPlayer clientPlayer() {
        if (!FMLEnvironment.dist.isClient()) return null;
        return ClientAccess.player();
    }

    /** Isolated so the dedicated server never loads client classes. */
    static final class ClientAccess {
        static EntityPlayer player() {
            var p = net.minecraft.client.Minecraft.getInstance().player;
            return p == null ? null : (EntityPlayer) wrap(p);
        }
    }

    /** View for modern living entities without a dedicated legacy class. */
    public static final class LivingView extends EntityLivingBase {
        LivingView(LivingEntity modern) {
            super(modern);
        }
    }

    /** View for any other modern entity. */
    public static final class View extends Entity {
        View(net.minecraft.world.entity.Entity modern) {
            super(modern);
        }
    }
}
