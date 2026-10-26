package legacyconnector.v1_7_10.net.minecraft.network.play.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_7_10.net.minecraft.util.Vec3;
import legacyconnector.v1_7_10.net.minecraft.world.ChunkPosition;

/**
 * An explosion for the client: its sound and particles, the blocks it removed ({@link ChunkPosition}s) and the
 * knockback it gave the player. Sent as the modern explosion packet.
 */
public class S27PacketExplosion extends Packet {
    public double field_149158_a;
    public double field_149156_b;
    public double field_149157_c;
    public float field_149154_d;
    public List field_149155_e = new ArrayList();
    public float field_149152_f;
    public float field_149153_g;
    public float field_149159_h;

    public S27PacketExplosion() {
    }

    public S27PacketExplosion(double x, double y, double z, float size, List blocks, Vec3 knockback) {
        this.field_149158_a = x;
        this.field_149156_b = y;
        this.field_149157_c = z;
        this.field_149154_d = size;
        this.field_149155_e = blocks == null ? new ArrayList() : new ArrayList(blocks);
        if (knockback != null) {
            this.field_149152_f = (float) knockback.xCoord;
            this.field_149153_g = (float) knockback.yCoord;
            this.field_149159_h = (float) knockback.zCoord;
        }
    }

    @Override
    public void readPacketData(PacketBuffer data) throws IOException {
        field_149158_a = data.readFloat();
        field_149156_b = data.readFloat();
        field_149157_c = data.readFloat();
        field_149154_d = data.readFloat();
        int n = data.readInt();
        field_149155_e = new ArrayList(n);
        int bx = (int) field_149158_a, by = (int) field_149156_b, bz = (int) field_149157_c;
        for (int i = 0; i < n; ++i) {
            field_149155_e.add(new ChunkPosition(data.readByte() + bx, data.readByte() + by, data.readByte() + bz));
        }
        field_149152_f = data.readFloat();
        field_149153_g = data.readFloat();
        field_149159_h = data.readFloat();
    }

    @Override
    public void writePacketData(PacketBuffer data) throws IOException {
        data.writeFloat((float) field_149158_a);
        data.writeFloat((float) field_149156_b);
        data.writeFloat((float) field_149157_c);
        data.writeFloat(field_149154_d);
        data.writeInt(field_149155_e.size());
        int bx = (int) field_149158_a, by = (int) field_149156_b, bz = (int) field_149157_c;
        for (Object o : field_149155_e) {
            ChunkPosition p = (ChunkPosition) o;
            data.writeByte(p.chunkPosX - bx);
            data.writeByte(p.chunkPosY - by);
            data.writeByte(p.chunkPosZ - bz);
        }
        data.writeFloat(field_149152_f);
        data.writeFloat(field_149153_g);
        data.writeFloat(field_149159_h);
    }

    public float func_149149_c() {
        return field_149152_f;
    }

    public float func_149144_d() {
        return field_149153_g;
    }

    public float func_149147_e() {
        return field_149159_h;
    }

    public double func_149148_f() {
        return field_149158_a;
    }

    public double func_149143_g() {
        return field_149156_b;
    }

    public double func_149145_h() {
        return field_149157_c;
    }

    public float func_149146_i() {
        return field_149154_d;
    }

    public List func_149150_j() {
        return field_149155_e;
    }

    @Override
    public void mcc$send(net.minecraft.server.level.ServerPlayer player) {
        List<net.minecraft.core.BlockPos> blocks = new ArrayList<>();
        for (Object o : field_149155_e) {
            if (o instanceof ChunkPosition p) blocks.add(new net.minecraft.core.BlockPos(p.chunkPosX, p.chunkPosY, p.chunkPosZ));
        }
        boolean large = field_149154_d >= 2.0F;
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundExplodePacket(field_149158_a, field_149156_b, field_149157_c,
                field_149154_d, blocks, new net.minecraft.world.phys.Vec3(field_149152_f, field_149153_g, field_149159_h),
                blocks.isEmpty() ? net.minecraft.world.level.Explosion.BlockInteraction.KEEP : net.minecraft.world.level.Explosion.BlockInteraction.DESTROY,
                net.minecraft.core.particles.ParticleTypes.EXPLOSION, large ? net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER
                        : net.minecraft.core.particles.ParticleTypes.EXPLOSION,
                net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE));
    }
}
