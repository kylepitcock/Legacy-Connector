package legacyconnector.v1_12_2.net.minecraft.entity;

import java.util.Collection;
import java.util.UUID;
import legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes.AbstractAttributeMap;
import legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes.AttributeModifier;
import legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes.IAttribute;
import legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes.IAttributeInstance;
import legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes.RangedAttribute;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagList;

/** The vanilla attributes (by their 1.12 names; living entities resolve them to the modern attributes) and their NBT form. */
public class SharedMonsterAttributes {
    public static final IAttribute MAX_HEALTH = new RangedAttribute(null, "generic.maxHealth", 20.0D, Float.MIN_VALUE, 1024.0D).setDescription("Max Health").setShouldWatch(true);
    public static final IAttribute FOLLOW_RANGE = new RangedAttribute(null, "generic.followRange", 32.0D, 0.0D, 2048.0D).setDescription("Follow Range");
    public static final IAttribute KNOCKBACK_RESISTANCE = new RangedAttribute(null, "generic.knockbackResistance", 0.0D, 0.0D, 1.0D).setDescription("Knockback Resistance");
    public static final IAttribute MOVEMENT_SPEED = new RangedAttribute(null, "generic.movementSpeed", 0.699999988079071D, 0.0D, 1024.0D).setDescription("Movement Speed").setShouldWatch(true);
    public static final IAttribute FLYING_SPEED = new RangedAttribute(null, "generic.flyingSpeed", 0.4000000059604645D, 0.0D, 1024.0D).setDescription("Flying Speed").setShouldWatch(true);
    public static final IAttribute ATTACK_DAMAGE = new RangedAttribute(null, "generic.attackDamage", 2.0D, 0.0D, 2048.0D);
    public static final IAttribute ATTACK_SPEED = new RangedAttribute(null, "generic.attackSpeed", 4.0D, 0.0D, 1024.0D).setShouldWatch(true);
    public static final IAttribute ARMOR = new RangedAttribute(null, "generic.armor", 0.0D, 0.0D, 30.0D).setShouldWatch(true);
    public static final IAttribute ARMOR_TOUGHNESS = new RangedAttribute(null, "generic.armorToughness", 0.0D, 0.0D, 20.0D).setShouldWatch(true);
    public static final IAttribute LUCK = new RangedAttribute(null, "generic.luck", 0.0D, -1024.0D, 1024.0D).setShouldWatch(true);

    /** The vanilla attribute of a 1.12 name (also EntityPlayer.REACH_DISTANCE and the other Forge/vanilla extras), or null. */
    public static IAttribute mcc$byName(String name) {
        if (name == null) return null;
        return switch (name) {
            case "generic.maxHealth" -> MAX_HEALTH;
            case "generic.followRange" -> FOLLOW_RANGE;
            case "generic.knockbackResistance" -> KNOCKBACK_RESISTANCE;
            case "generic.movementSpeed" -> MOVEMENT_SPEED;
            case "generic.flyingSpeed" -> FLYING_SPEED;
            case "generic.attackDamage" -> ATTACK_DAMAGE;
            case "generic.attackSpeed" -> ATTACK_SPEED;
            case "generic.armor" -> ARMOR;
            case "generic.armorToughness" -> ARMOR_TOUGHNESS;
            case "generic.luck" -> LUCK;
            default -> null;
        };
    }

    public static NBTTagList writeBaseAttributeMapToNBT(AbstractAttributeMap map) {
        NBTTagList list = new NBTTagList();
        for (IAttributeInstance instance : map.getAllAttributes()) list.appendTag(writeAttributeInstanceToNBT(instance));
        return list;
    }

    private static NBTTagCompound writeAttributeInstanceToNBT(IAttributeInstance instance) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("Name", instance.getAttribute().getName());
        tag.setDouble("Base", instance.getBaseValue());
        Collection<AttributeModifier> modifiers = instance.getModifiers();
        if (modifiers != null && !modifiers.isEmpty()) {
            NBTTagList list = new NBTTagList();
            for (AttributeModifier m : modifiers) if (m.isSaved()) list.appendTag(writeAttributeModifierToNBT(m));
            tag.setTag("Modifiers", list);
        }
        return tag;
    }

    public static NBTTagCompound writeAttributeModifierToNBT(AttributeModifier modifier) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("Name", modifier.getName());
        tag.setDouble("Amount", modifier.getAmount());
        tag.setInteger("Operation", modifier.getOperation());
        tag.setUniqueId("UUID", modifier.getID());
        return tag;
    }

    public static void setAttributeModifiers(AbstractAttributeMap map, NBTTagList list) {
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            IAttributeInstance instance = map.getAttributeInstanceByName(tag.getString("Name"));
            if (instance != null) applyModifiersToAttributeInstance(instance, tag);
        }
    }

    private static void applyModifiersToAttributeInstance(IAttributeInstance instance, NBTTagCompound tag) {
        instance.setBaseValue(tag.getDouble("Base"));
        if (!tag.hasKey("Modifiers", 9)) return;
        NBTTagList list = tag.getTagList("Modifiers", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            AttributeModifier m = readAttributeModifierFromNBT(list.getCompoundTagAt(i));
            if (m == null) continue;
            AttributeModifier existing = instance.getModifier(m.getID());
            if (existing != null) instance.removeModifier(existing);
            instance.applyModifier(m);
        }
    }

    public static AttributeModifier readAttributeModifierFromNBT(NBTTagCompound tag) {
        UUID id = tag.getUniqueId("UUID");
        try {
            return new AttributeModifier(id, tag.getString("Name"), tag.getDouble("Amount"), tag.getInteger("Operation"));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
