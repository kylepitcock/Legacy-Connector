package legacyconnector.v1_7_10.net.minecraft.entity.ai.attributes;

import java.util.UUID;
import net.minecraft.resources.ResourceLocation;

public class AttributeModifier {
    private final double amount;
    private final int operation;
    private final String name;
    private final UUID id;
    private boolean isSaved = true;

    public AttributeModifier(String name, double amount, int operation) {
        this(UUID.randomUUID(), name, amount, operation);
    }

    public AttributeModifier(UUID id, String name, double amount, int operation) {
        this.id = id;
        this.name = name;
        this.amount = amount;
        this.operation = operation;
        if (operation < 0 || operation > 2) throw new IllegalArgumentException("Invalid operation " + operation);
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

    @Override
    public boolean equals(Object o) {
        return o instanceof AttributeModifier m && id.equals(m.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "AttributeModifier{amount=" + amount + ", operation=" + operation + ", name='" + name + "', id=" + id + "}";
    }

    // ------------------------------------------------------------------ modern conversion

    public static ResourceLocation mcc$id(UUID id) {
        return ResourceLocation.fromNamespaceAndPath("legacyconnector", "legacy/" + id);
    }

    public net.minecraft.world.entity.ai.attributes.AttributeModifier mcc$modern() {
        var op = switch (operation) {
            case 1 -> net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
            case 2 -> net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
            default -> net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE;
        };
        return new net.minecraft.world.entity.ai.attributes.AttributeModifier(mcc$id(id), amount, op);
    }

    public static AttributeModifier mcc$wrap(net.minecraft.world.entity.ai.attributes.AttributeModifier m) {
        ResourceLocation rl = m.id();
        UUID id;
        if (rl.getNamespace().equals("legacyconnector") && rl.getPath().startsWith("legacy/")) {
            try {
                id = UUID.fromString(rl.getPath().substring("legacy/".length()));
            } catch (IllegalArgumentException e) {
                id = UUID.nameUUIDFromBytes(rl.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
        } else {
            id = UUID.nameUUIDFromBytes(rl.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        return new AttributeModifier(id, rl.toString(), m.amount(), m.operation().id());
    }
}
