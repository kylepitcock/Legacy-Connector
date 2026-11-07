package legacyconnector.v1_20_1.net.minecraftforge.event;

import java.util.function.BooleanSupplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.LogicalSide;

/**
 * Forge 1.20.1's tick event: one class per kind with a {@code phase} field. NeoForge 1.21.1 split every kind into {@code Pre} and {@code Post}
 * events; {@code Forge1201Runtime} listens to those and posts these facades (same handlers, same {@code phase} checks) on the NeoForge bus.
 */
public abstract class TickEvent extends Event {
    public final Type type;
    public final LogicalSide side;
    public final Phase phase;

    protected TickEvent(Type type, LogicalSide side, Phase phase) {
        this.type = type;
        this.side = side;
        this.phase = phase;
    }

    public enum Type {
        LEVEL, PLAYER, CLIENT, SERVER, RENDER;
    }

    public enum Phase {
        START, END;
    }

    public static class ServerTickEvent extends TickEvent {
        private final BooleanSupplier haveTime;

        public ServerTickEvent(Phase phase, BooleanSupplier haveTime) {
            super(Type.SERVER, LogicalSide.SERVER, phase);
            this.haveTime = haveTime;
        }

        public boolean haveTime() {
            return haveTime.getAsBoolean();
        }
    }

    public static class ClientTickEvent extends TickEvent {
        public ClientTickEvent(Phase phase) {
            super(Type.CLIENT, LogicalSide.CLIENT, phase);
        }
    }

    public static class LevelTickEvent extends TickEvent {
        public final Level level;
        private final BooleanSupplier haveTime;

        public LevelTickEvent(LogicalSide side, Phase phase, Level level, BooleanSupplier haveTime) {
            super(Type.LEVEL, side, phase);
            this.level = level;
            this.haveTime = haveTime;
        }

        public boolean haveTime() {
            return haveTime.getAsBoolean();
        }
    }

    public static class PlayerTickEvent extends TickEvent {
        public final Player player;

        public PlayerTickEvent(Phase phase, Player player) {
            super(Type.PLAYER, player.level().isClientSide ? LogicalSide.CLIENT : LogicalSide.SERVER, phase);
            this.player = player;
        }
    }

    public static class RenderTickEvent extends TickEvent {
        public final float renderTickTime;

        public RenderTickEvent(Phase phase, float renderTickTime) {
            super(Type.RENDER, LogicalSide.CLIENT, phase);
            this.renderTickTime = renderTickTime;
        }
    }
}
