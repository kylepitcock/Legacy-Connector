package mccompat.v1_7_10.cpw.mods.fml.common.gameevent;

import mccompat.v1_7_10.cpw.mods.fml.common.eventhandler.Event;
import mccompat.v1_7_10.cpw.mods.fml.relauncher.Side;
import mccompat.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import mccompat.v1_7_10.net.minecraft.world.World;

public class TickEvent extends Event {
    public enum Type {
        WORLD, PLAYER, CLIENT, SERVER, RENDER
    }

    public enum Phase {
        START, END
    }

    public final Type type;
    public final Side side;
    public final Phase phase;

    public TickEvent(Type type, Side side, Phase phase) {
        this.type = type;
        this.side = side;
        this.phase = phase;
    }

    public static class ServerTickEvent extends TickEvent {
        public ServerTickEvent(Phase phase) {
            super(Type.SERVER, Side.SERVER, phase);
        }
    }

    public static class ClientTickEvent extends TickEvent {
        public ClientTickEvent(Phase phase) {
            super(Type.CLIENT, Side.CLIENT, phase);
        }
    }

    public static class WorldTickEvent extends TickEvent {
        public final World world;

        public WorldTickEvent(Side side, Phase phase, World world) {
            super(Type.WORLD, side, phase);
            this.world = world;
        }
    }

    public static class PlayerTickEvent extends TickEvent {
        public final EntityPlayer player;

        public PlayerTickEvent(Phase phase, EntityPlayer player) {
            super(Type.PLAYER, player.mcc$world().isRemote ? Side.CLIENT : Side.SERVER, phase);
            this.player = player;
        }
    }

    public static class RenderTickEvent extends TickEvent {
        public final float renderTickTime;

        public RenderTickEvent(Phase phase, float renderTickTime) {
            super(Type.RENDER, Side.CLIENT, phase);
            this.renderTickTime = renderTickTime;
        }
    }
}
