package org.kylepitcock.legacyconnector.v1_12_2.entity;

import java.util.Collection;
import java.util.UUID;
import legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes.AttributeModifier;
import legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes.IAttribute;
import legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes.IAttributeInstance;

/** A legacy instance over a modern one; modifiers keep their legacy identity by the UUID inside the modern modifier id. */
public final class AttributeView1122 extends legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes.ModifiableAttributeInstance {
    private final IAttribute attribute;
    private final net.minecraft.world.entity.ai.attributes.AttributeInstance modern;

    public AttributeView1122(IAttribute attribute, net.minecraft.world.entity.ai.attributes.AttributeInstance modern) {
        // a ModifiableAttributeInstance (1.12's AttributeMap hands those out) whose state is all the modern instance's
        super(null, attribute);
        this.attribute = attribute;
        this.modern = modern;
    }

    private static net.minecraft.resources.ResourceLocation id(UUID id) {
        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("legacyconnector", "modifier/" + id);
    }

    private static AttributeModifier legacy(net.minecraft.world.entity.ai.attributes.AttributeModifier m) {
        String path = m.id().getPath();
        UUID uuid;
        try {
            uuid = path.startsWith("modifier/") ? UUID.fromString(path.substring(9)) : UUID.nameUUIDFromBytes(m.id().toString().getBytes());
        } catch (IllegalArgumentException e) {
            uuid = UUID.nameUUIDFromBytes(m.id().toString().getBytes());
        }
        return new AttributeModifier(uuid, m.id().toString(), m.amount(), m.operation().ordinal());
    }

    @Override
    protected void flagForUpdate() {
    }

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
    public Collection<AttributeModifier> getModifiersByOperation(int operation) {
        var op = net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.values()[operation];
        java.util.List<AttributeModifier> out = new java.util.ArrayList<>();
        for (var m : modern.getModifiers()) if (m.operation() == op) out.add(legacy(m));
        return out;
    }

    @Override
    public Collection<AttributeModifier> getModifiers() {
        java.util.List<AttributeModifier> out = new java.util.ArrayList<>();
        for (var m : modern.getModifiers()) out.add(legacy(m));
        return out;
    }

    @Override
    public boolean hasModifier(AttributeModifier modifier) {
        return modifier != null && modern.hasModifier(id(modifier.getID()));
    }

    @Override
    public AttributeModifier getModifier(UUID id) {
        var m = modern.getModifier(id(id));
        return m == null ? null : legacy(m);
    }

    /** 1.12 threw when the modifier was already applied; mods guard with hasModifier, so a repeat is ignored here. */
    @Override
    public void applyModifier(AttributeModifier modifier) {
        if (modifier == null || hasModifier(modifier)) return;
        if (modifier.isSaved()) modern.addPermanentModifier(modifier.mcc$modern());
        else modern.addTransientModifier(modifier.mcc$modern());
    }

    @Override
    public void removeModifier(AttributeModifier modifier) {
        if (modifier != null) modern.removeModifier(id(modifier.getID()));
    }

    @Override
    public void removeModifier(UUID id) {
        modern.removeModifier(id(id));
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
