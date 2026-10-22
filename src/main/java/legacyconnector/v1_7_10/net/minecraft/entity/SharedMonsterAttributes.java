package legacyconnector.v1_7_10.net.minecraft.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import legacyconnector.v1_7_10.net.minecraft.entity.ai.attributes.AttributeModifier;
import legacyconnector.v1_7_10.net.minecraft.entity.ai.attributes.BaseAttributeMap;
import legacyconnector.v1_7_10.net.minecraft.entity.ai.attributes.IAttribute;
import legacyconnector.v1_7_10.net.minecraft.entity.ai.attributes.IAttributeInstance;
import legacyconnector.v1_7_10.net.minecraft.entity.ai.attributes.RangedAttribute;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagList;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** The 1.7.10 shared attributes, each backed by its modern counterpart on modern living entities. */
public class SharedMonsterAttributes {
    public static final IAttribute maxHealth = new RangedAttribute("generic.maxHealth", 20.0, 0.0, Double.MAX_VALUE).setDescription("Max Health").setShouldWatch(true);
    public static final IAttribute followRange = new RangedAttribute("generic.followRange", 32.0, 0.0, 2048.0).setDescription("Follow Range");
    public static final IAttribute knockbackResistance = new RangedAttribute("generic.knockbackResistance", 0.0, 0.0, 1.0).setDescription("Knockback Resistance");
    public static final IAttribute movementSpeed = new RangedAttribute("generic.movementSpeed", 0.7, 0.0, Double.MAX_VALUE).setDescription("Movement Speed").setShouldWatch(true);
    public static final IAttribute attackDamage = new RangedAttribute("generic.attackDamage", 2.0, 0.0, Double.MAX_VALUE);

    public static NBTTagList writeBaseAttributeMapToNBT(BaseAttributeMap map) {
        NBTTagList list = new NBTTagList();
        for (IAttributeInstance i : map.getAllAttributes()) list.appendTag(writeAttributeInstanceToNBT(i));
        return list;
    }

    private static NBTTagCompound writeAttributeInstanceToNBT(IAttributeInstance instance) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("Name", instance.getAttribute().getAttributeUnlocalizedName());
        tag.setDouble("Base", instance.getBaseValue());
        NBTTagList mods = new NBTTagList();
        for (AttributeModifier m : instance.func_111122_c()) if (m.isSaved()) mods.appendTag(writeAttributeModifierToNBT(m));
        if (mods.tagCount() > 0) tag.setTag("Modifiers", mods);
        return tag;
    }

    private static NBTTagCompound writeAttributeModifierToNBT(AttributeModifier m) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("Name", m.getName());
        tag.setDouble("Amount", m.getAmount());
        tag.setInteger("Operation", m.getOperation());
        tag.setLong("UUIDMost", m.getID().getMostSignificantBits());
        tag.setLong("UUIDLeast", m.getID().getLeastSignificantBits());
        return tag;
    }

    public static void func_151475_a(BaseAttributeMap map, NBTTagList list) {
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            IAttributeInstance instance = map.getAttributeInstanceByName(tag.getString("Name"));
            if (instance == null) continue;
            instance.setBaseValue(tag.getDouble("Base"));
            if (tag.hasKey("Modifiers")) {
                NBTTagList mods = tag.getTagList("Modifiers", 10);
                for (int j = 0; j < mods.tagCount(); j++) {
                    AttributeModifier m = readAttributeModifierFromNBT(mods.getCompoundTagAt(j));
                    AttributeModifier old = instance.getModifier(m.getID());
                    if (old != null) instance.removeModifier(old);
                    instance.applyModifier(m);
                }
            }
        }
    }

    public static AttributeModifier readAttributeModifierFromNBT(NBTTagCompound tag) {
        UUID id = new UUID(tag.getLong("UUIDMost"), tag.getLong("UUIDLeast"));
        return new AttributeModifier(id, tag.getString("Name"), tag.getDouble("Amount"), tag.getInteger("Operation"));
    }

    // ------------------------------------------------------------------ modern backing

    public static List<IAttribute> mcc$vanilla() {
        return List.of(maxHealth, followRange, knockbackResistance, movementSpeed, attackDamage);
    }

    public static IAttribute mcc$byName(String name) {
        for (IAttribute a : mcc$vanilla()) if (a.getAttributeUnlocalizedName().equals(name)) return a;
        return null;
    }

    public static Holder<Attribute> mcc$modern(IAttribute attribute) {
        String name = attribute.getAttributeUnlocalizedName();
        if (name == null) return null;
        return switch (name) {
            case "generic.maxHealth" -> Attributes.MAX_HEALTH;
            case "generic.followRange" -> Attributes.FOLLOW_RANGE;
            case "generic.knockbackResistance" -> Attributes.KNOCKBACK_RESISTANCE;
            case "generic.movementSpeed" -> Attributes.MOVEMENT_SPEED;
            case "generic.attackDamage" -> Attributes.ATTACK_DAMAGE;
            case "horse.jumpStrength" -> Attributes.JUMP_STRENGTH;
            case "zombie.spawnReinforcements" -> Attributes.SPAWN_REINFORCEMENTS_CHANCE;
            default -> null;
        };
    }

    /** Legacy view of the modern instance of a vanilla attribute, or null. */
    public static IAttributeInstance mcc$instance(LivingEntity entity, IAttribute attribute) {
        if (entity == null) return null;
        Holder<Attribute> holder = mcc$modern(attribute);
        if (holder == null) return null;
        AttributeInstance instance = entity.getAttribute(holder);
        return instance == null ? null : new ModernInstance(attribute, instance);
    }

    private record ModernInstance(IAttribute attribute, AttributeInstance modern) implements IAttributeInstance {
        @Override
        public IAttribute getAttribute() {
            return attribute;
        }

        @Override
        public double getBaseValue() {
            return modern.getBaseValue();
        }

        @Override
        public void setBaseValue(double value) {
            modern.setBaseValue(value);
        }

        @Override
        public Collection<AttributeModifier> func_111130_a(int operation) {
            List<AttributeModifier> out = new ArrayList<>();
            for (var m : modern.getModifiers()) if (m.operation().id() == operation) out.add(AttributeModifier.mcc$wrap(m));
            return out;
        }

        @Override
        public Collection<AttributeModifier> func_111122_c() {
            List<AttributeModifier> out = new ArrayList<>();
            for (var m : modern.getModifiers()) out.add(AttributeModifier.mcc$wrap(m));
            return out;
        }

        @Override
        public AttributeModifier getModifier(UUID id) {
            var m = modern.getModifier(AttributeModifier.mcc$id(id));
            return m == null ? null : AttributeModifier.mcc$wrap(m);
        }

        @Override
        public void applyModifier(AttributeModifier modifier) {
            if (modern.hasModifier(AttributeModifier.mcc$id(modifier.getID())))
                throw new IllegalArgumentException("Modifier is already applied on this attribute!");
            if (modifier.isSaved()) modern.addPermanentModifier(modifier.mcc$modern());
            else modern.addTransientModifier(modifier.mcc$modern());
        }

        @Override
        public void removeModifier(AttributeModifier modifier) {
            modern.removeModifier(AttributeModifier.mcc$id(modifier.getID()));
        }

        @Override
        public void removeAllModifiers() {
            modern.removeModifiers();
        }

        @Override
        public double getAttributeValue() {
            return modern.getValue();
        }
    }
}
