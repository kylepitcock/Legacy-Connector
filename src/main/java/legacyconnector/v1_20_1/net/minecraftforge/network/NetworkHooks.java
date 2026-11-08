package legacyconnector.v1_20_1.net.minecraftforge.network;

import java.util.function.Consumer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;

/** Forge 1.20.1's menu-opening helpers; NeoForge 1.21.1 has them on the player. */
public final class NetworkHooks {
    private NetworkHooks() {
    }

    public static void openScreen(ServerPlayer player, MenuProvider containerSupplier) {
        player.openMenu(containerSupplier);
    }

    public static void openScreen(ServerPlayer player, MenuProvider containerSupplier, net.minecraft.core.BlockPos pos) {
        player.openMenu(containerSupplier, buf -> buf.writeBlockPos(pos));
    }

    public static void openScreen(ServerPlayer player, MenuProvider containerSupplier, Consumer<FriendlyByteBuf> extraDataWriter) {
        player.openMenu(containerSupplier, extraDataWriter::accept);
    }

    /** The packet that spawns an entity on the clients (NeoForge 1.21.1 builds it from the tracking entry; a stand-in one is made here). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getEntitySpawningPacket(net.minecraft.world.entity.Entity entity) {
        if (!(entity.level() instanceof net.minecraft.server.level.ServerLevel level)) return null;
        var tracker = new net.minecraft.server.level.ServerEntity(level, entity, entity.getType().updateInterval(), entity.getType().trackDeltas(), p -> {});
        return (net.minecraft.network.protocol.Packet) new net.minecraft.network.protocol.game.ClientboundAddEntityPacket(entity, tracker);
    }
}
