package legacyconnector.v1_20_1.net.minecraftforge.event.entity.player;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Forge 1.20.1's "player tries to sleep" event: a listener may set a {@code BedSleepingProblem} that stops the sleep. NeoForge 1.21.1 calls it
 * {@code CanPlayerSleepEvent} (which carries the vanilla problem); the runtime posts this one first and applies a problem set here.
 */
public class PlayerSleepInBedEvent extends PlayerEvent {
    private final BlockPos pos;
    private Player.BedSleepingProblem result;

    public PlayerSleepInBedEvent(Player player, BlockPos pos) {
        super(player);
        this.pos = pos;
    }

    public Player.BedSleepingProblem getResultStatus() {
        return result;
    }

    public void setResult(Player.BedSleepingProblem result) {
        this.result = result;
    }

    public BlockPos getPos() {
        return pos;
    }

    public Optional<BlockPos> getOptionalPos() {
        return Optional.ofNullable(pos);
    }
}
