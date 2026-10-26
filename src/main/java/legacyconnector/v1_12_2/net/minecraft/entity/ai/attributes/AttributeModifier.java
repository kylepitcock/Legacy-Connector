package legacyconnector.v1_12_2.net.minecraft.entity.ai.attributes;

import java.util.UUID;

/** A change to an attribute: operation 0 adds the amount, 1 adds amount times the base, 2 multiplies by (1 + amount). */
public class AttributeModifier {
    private final double amount;
    private final int operation;
    private final String name;
    private final UUID id;
    /** Whether the modifier is written to the entity's NBT. */
    private boolean isSaved = true;

    public AttributeModifier(String name, double amount, int operation) {
        this(UUID.randomUUID(), name, amount, operation);
    }

    public AttributeModifier(UUID id, String name, double amount, int operation) {
        if (name == null || name.isEmpty()) throw new IllegalArgumentException("Modifier name cannot be empty");
        if (operation < 0 || operation > 2) throw new IllegalArgumentException("Invalid operation");
        this.id = id;
        this.name = name;
        this.amount = amount;
        this.operation = operation;
    }

    public UUID getID() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getOperation() {
        return operation;
    }

    public double getAmount() {
        return amount;
    }

    public boolean isSaved() {
        return isSaved;
    }

    public AttributeModifier setSaved(boolean saved) {
        this.isSaved = saved;
        return this;
    }

    /** The modern modifier (id from the UUID, operation by index). */
    public net.minecraft.world.entity.ai.attributes.AttributeModifier mcc$modern() {
        var op = net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.values()[operation];
        return new net.minecraft.world.entity.ai.attributes.AttributeModifier(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("legacyconnector",
                "modifier/" + id), amount, op);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof AttributeModifier m && java.util.Objects.equals(id, m.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "AttributeModifier{amount=" + amount + ", operation=" + operation + ", name='" + name + "', id=" + id + ", serialize=" + isSaved + "}";
    }
}
