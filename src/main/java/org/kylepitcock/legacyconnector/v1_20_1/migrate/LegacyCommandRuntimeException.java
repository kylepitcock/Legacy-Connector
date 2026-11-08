package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.network.chat.Component;

/** Stand-in for 1.20.1's {@code CommandRuntimeException} (gone in 1.21.1): an unchecked exception carrying a message component. */
public class LegacyCommandRuntimeException extends RuntimeException {
    private final Component message;

    public LegacyCommandRuntimeException(Component message) {
        super(message.getString());
        this.message = message;
    }

    public Component getComponent() {
        return message;
    }
}
