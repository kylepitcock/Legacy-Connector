package legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes;

/** An entity attribute (max health, movement speed, ...): a name, a default and a valid range. */
public interface IAttribute {
    String getName();

    double clampValue(double value);

    double getDefaultValue();

    boolean getShouldWatch();

    IAttribute getParent();

    /** The modern attribute of the same meaning (vanilla attributes by their 1.12 names), or null for mod attributes. */
    default net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> mcc$modern() {
        String modern = switch (getName()) {
            case "generic.maxHealth" -> "generic.max_health";
            case "generic.followRange" -> "generic.follow_range";
            case "generic.knockbackResistance" -> "generic.knockback_resistance";
            case "generic.movementSpeed" -> "generic.movement_speed";
            case "generic.flyingSpeed" -> "generic.flying_speed";
            case "generic.attackDamage" -> "generic.attack_damage";
            case "generic.attackSpeed" -> "generic.attack_speed";
            case "generic.armor" -> "generic.armor";
            case "generic.armorToughness" -> "generic.armor_toughness";
            case "generic.luck" -> "generic.luck";
            case "generic.reachDistance" -> "player.block_interaction_range";
            case "horse.jumpStrength" -> "generic.jump_strength";
            case "zombie.spawnReinforcements" -> "zombie.spawn_reinforcements";
            case "forge.swimSpeed" -> "neoforge:swim_speed";
            default -> null;
        };
        if (modern == null) return null;
        var id = modern.indexOf(':') >= 0 ? net.minecraft.resources.ResourceLocation.parse(modern) : net.minecraft.resources.ResourceLocation.withDefaultNamespace(modern);
        return net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.getHolder(id).map(h -> (net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute>) h).orElse(null);
    }

    /** The vanilla attribute constants (SharedMonsterAttributes.MAX_HEALTH, ...) for the synthesized classes declaring them, by "owner.field". */
    static IAttribute mcc$synthDefault(String field) {
        String name = field.substring(field.lastIndexOf('.') + 1);
        if (field.contains("SharedMonsterAttributes")) {
            try {
                var f = legacyconnector.v1_12_2.net.minecraft.entity.SharedMonsterAttributes.class.getField(name);
                return (IAttribute) f.get(null);
            } catch (ReflectiveOperationException | RuntimeException e) {
                // not one of the hand-written constants
            }
        }
        if (name.equals("REACH_DISTANCE")) return legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer.REACH_DISTANCE;
        return switch (name) {
            case "MAX_HEALTH" -> new RangedAttribute(null, "generic.maxHealth", 20.0, Float.MIN_VALUE, 1024.0).setDescription("Max Health").setShouldWatch(true);
            case "FOLLOW_RANGE" -> new RangedAttribute(null, "generic.followRange", 32.0, 0.0, 2048.0).setDescription("Follow Range");
            case "KNOCKBACK_RESISTANCE" -> new RangedAttribute(null, "generic.knockbackResistance", 0.0, 0.0, 1.0).setDescription("Knockback Resistance");
            case "MOVEMENT_SPEED" -> new RangedAttribute(null, "generic.movementSpeed", 0.699999988079071, 0.0, 1024.0).setDescription("Movement Speed").setShouldWatch(true);
            case "FLYING_SPEED" -> new RangedAttribute(null, "generic.flyingSpeed", 0.4, 0.0, 1024.0).setDescription("Flying Speed").setShouldWatch(true);
            case "ATTACK_DAMAGE" -> new RangedAttribute(null, "generic.attackDamage", 2.0, 0.0, 2048.0);
            case "ATTACK_SPEED" -> new RangedAttribute(null, "generic.attackSpeed", 4.0, 0.0, 1024.0).setShouldWatch(true);
            case "ARMOR" -> new RangedAttribute(null, "generic.armor", 0.0, 0.0, 30.0).setShouldWatch(true);
            case "ARMOR_TOUGHNESS" -> new RangedAttribute(null, "generic.armorToughness", 0.0, 0.0, 20.0).setShouldWatch(true);
            case "LUCK" -> new RangedAttribute(null, "generic.luck", 0.0, -1024.0, 1024.0).setShouldWatch(true);
            case "REACH_DISTANCE" -> new RangedAttribute(null, "generic.reachDistance", 5.0, 0.0, 1024.0).setShouldWatch(true);
            case "SWIM_SPEED" -> new RangedAttribute(null, "forge.swimSpeed", 1.0, 0.0, 1024.0).setShouldWatch(true);
            case "SPAWN_REINFORCEMENTS_CHANCE" -> new RangedAttribute(null, "zombie.spawnReinforcements", 0.0, 0.0, 1.0).setDescription("Spawn Reinforcements Chance");
            case "JUMP_STRENGTH" -> new RangedAttribute(null, "horse.jumpStrength", 0.7, 0.0, 2.0).setDescription("Jump Strength").setShouldWatch(true);
            default -> new RangedAttribute(null, field, 0.0, -Double.MAX_VALUE, Double.MAX_VALUE);
        };
    }
}
