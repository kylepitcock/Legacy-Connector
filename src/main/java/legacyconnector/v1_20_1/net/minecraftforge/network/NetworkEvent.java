package legacyconnector.v1_20_1.net.minecraftforge.network;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Forge 1.20.1's packet event; only its {@link Context} is used by mods (it is what a message consumer receives). */
public class NetworkEvent extends Event {
    private final Context context;

    public NetworkEvent(Context context) {
        this.context = context;
    }

    public net.minecraft.network.FriendlyByteBuf getPayload() {
        return null;
    }

    public Supplier<Context> getSource() {
        return () -> context;
    }

    /** What a message handler gets: the direction, the sending player, the main-thread queue. Backed by NeoForge's payload context. */
    public static class Context {
        private final IPayloadContext delegate;
        private final NetworkDirection direction;
        private boolean handled;

        public Context(IPayloadContext delegate) {
            this.delegate = delegate;
            this.direction = delegate.flow().isServerbound() ? NetworkDirection.PLAY_TO_SERVER : NetworkDirection.PLAY_TO_CLIENT;
        }

        public NetworkDirection getDirection() {
            return direction;
        }

        /** The player who sent a serverbound message; null for clientbound ones. */
        public ServerPlayer getSender() {
            return delegate.flow().isServerbound() && delegate.player() instanceof ServerPlayer p ? p : null;
        }

        public CompletableFuture<Void> enqueueWork(Runnable runnable) {
            return delegate.enqueueWork(runnable);
        }

        public <T> CompletableFuture<T> enqueueWork(Supplier<T> supplier) {
            return delegate.enqueueWork(supplier);
        }

        public void setPacketHandled(boolean handled) {
            this.handled = handled;
        }

        public boolean getPacketHandled() {
            return handled;
        }

        public net.minecraft.network.Connection getNetworkManager() {
            return delegate.connection();
        }

        /** The NeoForge context (not part of Forge's API). */
        public IPayloadContext mcc$delegate() {
            return delegate;
        }
    }
}
