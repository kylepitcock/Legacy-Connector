package legacyconnector.v1_12_2.net.minecraft.world.storage.loot;

import java.util.LinkedHashSet;
import java.util.Set;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.util.DamageSource;
import legacyconnector.v1_12_2.net.minecraft.world.WorldServer;

/**
 * 1.12.2 loot context: world, luck, looted entity, killing player and damage source. It converts both ways with the modern
 * context ({@link #mcc$params} to roll a modern table, {@link #mcc$of} when a modern table runs a legacy entry).
 */
public class LootContext {
    private final float luck;
    private final WorldServer world;
    private final LootTableManager lootTableManager;
    private final Entity lootedEntity;
    private final EntityPlayer player;
    private final DamageSource damageSource;
    private final Set<LootTable> lootTables = new LinkedHashSet<>();

    public LootContext(float luck, WorldServer world, LootTableManager lootTableManager, Entity lootedEntity, EntityPlayer player, DamageSource damageSource) {
        this.luck = luck;
        this.world = world;
        this.lootTableManager = lootTableManager;
        this.lootedEntity = lootedEntity;
        this.player = player;
        this.damageSource = damageSource;
    }

    public Entity getLootedEntity() {
        return lootedEntity;
    }

    public Entity getKillerPlayer() {
        return player;
    }

    public Entity getKiller() {
        return damageSource == null ? null : damageSource.getTrueSource();
    }

    public boolean addLootTable(LootTable table) {
        return lootTables.add(table);
    }

    public void removeLootTable(LootTable table) {
        lootTables.remove(table);
    }

    public LootTableManager getLootTableManager() {
        return lootTableManager;
    }

    public float getLuck() {
        return luck;
    }

    public WorldServer getWorld() {
        return world;
    }

    public Entity getEntity(EntityTarget target) {
        if (target == null) return null;
        return switch (target) {
            case THIS -> getLootedEntity();
            case KILLER -> getKiller();
            case KILLER_PLAYER -> getKillerPlayer();
        };
    }

    /** The modern loot parameters of this context (no required parameters: any modern table can roll with them). */
    public net.minecraft.world.level.storage.loot.LootParams mcc$params() {
        var level = world == null ? null : world.mcc$serverLevel();
        if (level == null) {
            var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
            level = server == null ? null : server.overworld();
        }
        if (level == null) return null;
        var b = new net.minecraft.world.level.storage.loot.LootParams.Builder(level).withLuck(luck);
        var looted = lootedEntity == null ? null : lootedEntity.mcc$entity;
        var killer = player == null ? null : player.mcc$player();
        if (looted != null) {
            b.withOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY, looted);
            b.withOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, looted.position());
        } else if (killer != null) {
            b.withOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, killer.position());
        } else {
            b.withOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,
                    net.minecraft.world.phys.Vec3.atCenterOf(level.getSharedSpawnPos()));
        }
        if (killer != null) b.withOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.LAST_DAMAGE_PLAYER, killer);
        if (damageSource != null) {
            try {
                var modern = damageSource.mcc$modern(level);
                b.withOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.DAMAGE_SOURCE, modern);
                if (modern != null) {
                    b.withOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ATTACKING_ENTITY, modern.getEntity());
                    b.withOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.DIRECT_ATTACKING_ENTITY, modern.getDirectEntity());
                }
            } catch (RuntimeException | LinkageError e) {
                // the source stays out of the modern context
            }
        }
        return b.create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.EMPTY);
    }

    /** The legacy view of a modern loot context. */
    public static LootContext mcc$of(net.minecraft.world.level.storage.loot.LootContext modern) {
        var level = modern.getLevel();
        var world = org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldBridge1122.wrap(level) instanceof WorldServer w ? w : null;
        var looted = modern.getParamOrNull(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY);
        var killer = modern.getParamOrNull(net.minecraft.world.level.storage.loot.parameters.LootContextParams.LAST_DAMAGE_PLAYER);
        var source = modern.getParamOrNull(net.minecraft.world.level.storage.loot.parameters.LootContextParams.DAMAGE_SOURCE);
        EntityPlayer player = org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(killer) instanceof EntityPlayer p ? p : null;
        return new LootContext(modern.getLuck(), world, LootTableManager.mcc$of(level.getServer()),
                org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(looted), player, source == null ? null : DamageSource.mcc$of(source));
    }

    public static class Builder {
        private final WorldServer world;
        private float luck;
        private Entity lootedEntity;
        private EntityPlayer player;
        private DamageSource damageSource;

        public Builder(WorldServer world) {
            this.world = world;
        }

        public Builder withLuck(float luck) {
            this.luck = luck;
            return this;
        }

        public Builder withLootedEntity(Entity entity) {
            this.lootedEntity = entity;
            return this;
        }

        public Builder withPlayer(EntityPlayer player) {
            this.player = player;
            return this;
        }

        public Builder withDamageSource(DamageSource source) {
            this.damageSource = source;
            return this;
        }

        public LootContext build() {
            return new LootContext(luck, world, world == null ? null : world.getLootTableManager(), lootedEntity, player, damageSource);
        }
    }

    public enum EntityTarget {
        THIS("this"),
        KILLER("killer"),
        KILLER_PLAYER("killer_player");

        private final String targetType;

        EntityTarget(String type) {
            this.targetType = type;
        }

        public static EntityTarget fromString(String type) {
            for (EntityTarget t : values()) if (t.targetType.equals(type)) return t;
            throw new IllegalArgumentException("Invalid entity target " + type);
        }

        public static class Serializer extends com.google.gson.TypeAdapter<EntityTarget> {
            @Override
            public void write(com.google.gson.stream.JsonWriter out, EntityTarget value) throws java.io.IOException {
                out.value(value.targetType);
            }

            @Override
            public EntityTarget read(com.google.gson.stream.JsonReader in) throws java.io.IOException {
                return EntityTarget.fromString(in.nextString());
            }
        }
    }
}
