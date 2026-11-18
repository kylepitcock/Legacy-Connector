package legacyconnector.v1_7_10.net.minecraft.network.play.server;

import com.mojang.authlib.GameProfile;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_7_10.net.minecraft.util.MathHelper;

/**
 * A player appears: id, profile, position in 1/32 blocks, angles in 1/256 turns, the id of the held item and the watched data, in the
 * 1.7.10 wire format. The modern game spawns players itself, so sending the packet to a player adds nothing there.
 */
public class S0CPacketSpawnPlayer extends Packet {
    private int field_148957_a;
    private GameProfile field_148955_b;
    private int field_148956_c;
    private int field_148953_d;
    private int field_148954_e;
    private byte field_148951_f;
    private byte field_148952_g;
    private int field_148959_h;
    private List field_148958_j;

    public S0CPacketSpawnPlayer() {
    }

    public S0CPacketSpawnPlayer(EntityPlayer player) {
        field_148957_a = player.getEntityId();
        field_148955_b = player.getGameProfile();
        field_148956_c = MathHelper.floor_double(Entity.mcc$get$posX(player) * 32.0D);
        field_148953_d = MathHelper.floor_double(Entity.mcc$get$posY(player) * 32.0D);
        field_148954_e = MathHelper.floor_double(Entity.mcc$get$posZ(player) * 32.0D);
        field_148951_f = (byte) (int) (Entity.mcc$get$rotationYaw(player) * 256.0F / 360.0F);
        field_148952_g = (byte) (int) (Entity.mcc$get$rotationPitch(player) * 256.0F / 360.0F);
        ItemStack held = player.inventory.getCurrentItem();
        field_148959_h = held == null || held.getItem() == null ? 0 : Item.getIdFromItem(held.getItem());
        field_148958_j = player.getDataWatcher().getAllWatched();
    }

    @Override
    public void readPacketData(PacketBuffer data) throws IOException {
        field_148957_a = data.readVarIntFromBuffer();
        UUID id = UUID.fromString(data.readStringFromBuffer(36));
        field_148955_b = new GameProfile(id, data.readStringFromBuffer(16));
        field_148956_c = data.readInt();
        field_148953_d = data.readInt();
        field_148954_e = data.readInt();
        field_148951_f = data.readByte();
        field_148952_g = data.readByte();
        field_148959_h = data.readShort();
        field_148958_j = S1CPacketEntityMetadata.mcc$readWatched(data);
    }

    @Override
    public void writePacketData(PacketBuffer data) throws IOException {
        data.writeVarIntToBuffer(field_148957_a);
        UUID id = field_148955_b.getId();
        data.writeStringToBuffer(id == null ? "" : id.toString());
        data.writeStringToBuffer(field_148955_b.getName());
        data.writeInt(field_148956_c);
        data.writeInt(field_148953_d);
        data.writeInt(field_148954_e);
        data.writeByte(field_148951_f);
        data.writeByte(field_148952_g);
        data.writeShort(field_148959_h);
        S1CPacketEntityMetadata.mcc$writeWatched(data, field_148958_j);
    }

    public List func_148944_c() {
        return field_148958_j;
    }

    public int func_148943_d() {
        return field_148957_a;
    }

    public GameProfile func_148948_e() {
        return field_148955_b;
    }

    public int func_148942_f() {
        return field_148956_c;
    }

    public int func_148949_g() {
        return field_148953_d;
    }

    public int func_148946_h() {
        return field_148954_e;
    }

    public byte func_148941_i() {
        return field_148951_f;
    }

    public byte func_148945_j() {
        return field_148952_g;
    }

    public int func_148947_k() {
        return field_148959_h;
    }

    @Override
    public void mcc$send(net.minecraft.server.level.ServerPlayer player) {
    }
}
