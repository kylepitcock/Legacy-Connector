package legacyconnector.v1_12_2.net.minecraftforge.client.event;

/** The local player's movement input was read this tick; handlers change it (a live view of the game's input). */
public class InputUpdateEvent extends legacyconnector.v1_12_2.net.minecraftforge.event.entity.player.PlayerEvent {
    private final legacyconnector.v1_12_2.net.minecraft.util.MovementInput movementInput;

    public InputUpdateEvent(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer player, legacyconnector.v1_12_2.net.minecraft.util.MovementInput movementInput) {
        super(player);
        this.movementInput = movementInput;
    }

    public legacyconnector.v1_12_2.net.minecraft.util.MovementInput getMovementInput() {
        return movementInput;
    }
}
