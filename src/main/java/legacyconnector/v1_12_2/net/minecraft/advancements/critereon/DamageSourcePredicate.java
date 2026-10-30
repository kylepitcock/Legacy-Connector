package legacyconnector.v1_12_2.net.minecraft.advancements.critereon;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import legacyconnector.v1_12_2.net.minecraft.util.DamageSource;

/** A damage condition: its kind (projectile, explosion, fire, magic, armour/invulnerability bypassing) and its direct and source entities. */
public class DamageSourcePredicate {
    public static final DamageSourcePredicate ANY = new DamageSourcePredicate();
    private final Boolean isProjectile;
    private final Boolean isExplosion;
    private final Boolean bypassesArmor;
    private final Boolean bypassesInvulnerability;
    private final Boolean bypassesMagic;
    private final Boolean isFire;
    private final Boolean isMagic;
    private final EntityPredicate directEntity;
    private final EntityPredicate sourceEntity;

    public DamageSourcePredicate() {
        this(null, null, null, null, null, null, null, EntityPredicate.ANY, EntityPredicate.ANY);
    }

    public DamageSourcePredicate(Boolean isProjectile, Boolean isExplosion, Boolean bypassesArmor, Boolean bypassesInvulnerability, Boolean bypassesMagic,
            Boolean isFire, Boolean isMagic, EntityPredicate directEntity, EntityPredicate sourceEntity) {
        this.isProjectile = isProjectile;
        this.isExplosion = isExplosion;
        this.bypassesArmor = bypassesArmor;
        this.bypassesInvulnerability = bypassesInvulnerability;
        this.bypassesMagic = bypassesMagic;
        this.isFire = isFire;
        this.isMagic = isMagic;
        this.directEntity = directEntity;
        this.sourceEntity = sourceEntity;
    }

    private static boolean ok(Boolean wanted, boolean actual) {
        return wanted == null || wanted == actual;
    }

    public boolean test(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP player, DamageSource source) {
        if (this == ANY) return true;
        if (source == null) return false;
        return ok(isProjectile, source.isProjectile()) && ok(isExplosion, source.isExplosion()) && ok(bypassesArmor, source.isUnblockable())
                && ok(bypassesInvulnerability, source.canHarmInCreative()) && ok(bypassesMagic, source.isDamageAbsolute()) && ok(isFire, source.isFireDamage())
                && ok(isMagic, source.isMagicDamage()) && directEntity.test(player, source.getImmediateSource())
                && sourceEntity.test(player, source.getTrueSource());
    }

    private static Boolean bool(JsonObject o, String key) {
        return o.has(key) ? o.get(key).getAsBoolean() : null;
    }

    public static DamageSourcePredicate deserialize(JsonElement json) {
        if (json == null || json.isJsonNull()) return ANY;
        JsonObject o = json.getAsJsonObject();
        return new DamageSourcePredicate(bool(o, "is_projectile"), bool(o, "is_explosion"), bool(o, "bypasses_armor"), bool(o, "bypasses_invulnerability"),
                bool(o, "bypasses_magic"), bool(o, "is_fire"), bool(o, "is_magic"), EntityPredicate.deserialize(o.get("direct_entity")),
                EntityPredicate.deserialize(o.get("source_entity")));
    }
}
