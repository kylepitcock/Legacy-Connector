package legacyconnector.v1_7_10.net.minecraft.network.play.server;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;

/**
 * A name on the tab list (1.7.10 {@code S38PacketPlayerListItem}): the player's name, whether it is listed and its ping. The name is
 * not a real account, so the entry gets the offline UUID of the name; listing it adds a tab entry, unlisting removes it.
 */
public class S38PacketPlayerListItem extends Packet {
    public String field_149126_a;
    public boolean field_149124_b;
    public int field_149125_c;

    public S38PacketPlayerListItem() {
    }

    public S38PacketPlayerListItem(String name, boolean online, int ping) {
        this.field_149126_a = name;
        this.field_149124_b = online;
        this.field_149125_c = ping;
    }

    @Override
    public void readPacketData(PacketBuffer data) throws IOException {
        field_149126_a = data.readStringFromBuffer(16);
        field_149124_b = data.readBoolean();
        field_149125_c = data.readShort();
    }

    @Override
    public void writePacketData(PacketBuffer data) throws IOException {
        data.writeStringToBuffer(field_149126_a);
        data.writeBoolean(field_149124_b);
        data.writeShort(field_149125_c);
    }

    public String func_149122_c() {
        return field_149126_a;
    }

    public boolean func_149121_d() {
        return field_149124_b;
    }

    public int func_149120_e() {
        return field_149125_c;
    }

    @Override
    public void mcc$send(net.minecraft.server.level.ServerPlayer player) {
        if (field_149126_a == null || player.connection == null) return;
        UUID id = net.minecraft.core.UUIDUtil.createOfflinePlayerUUID(field_149126_a);
        if (!field_149124_b) {
            player.connection.send(new ClientboundPlayerInfoRemovePacket(List.of(id)));
            return;
        }
        // the entry constructor of the modern packet is private: write its wire form and decode it
        var buf = new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), player.registryAccess());
        buf.writeEnumSet(EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED,
                ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY), ClientboundPlayerInfoUpdatePacket.Action.class);
        buf.writeVarInt(1);
        buf.writeUUID(id);
        buf.writeUtf(field_149126_a, 16);
        buf.writeVarInt(0);
        buf.writeBoolean(true);
        buf.writeVarInt(Math.max(0, field_149125_c));
        player.connection.send(ClientboundPlayerInfoUpdatePacket.STREAM_CODEC.decode(buf));
    }
}
