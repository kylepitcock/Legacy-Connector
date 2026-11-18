package legacyconnector.v1_7_10.net.minecraft.network.play.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_7_10.net.minecraft.scoreboard.ScorePlayerTeam;

/**
 * A scoreboard team update (1.7.10 {@code S3EPacketTeams}): mode 0 creates the team with its members, 1 removes it, 2 updates its display name, prefix, suffix
 * and flags, 3 adds members, 4 removes members. The wire format and accessors are the legacy ones; the modern scoreboard keeps its own teams, so sending
 * the packet to a player does nothing.
 */
public class S3EPacketTeams extends Packet {
    private String field_149320_a = "";
    private String field_149318_b = "";
    private String field_149319_c = "";
    private String field_149316_d = "";
    private Collection field_149317_e = new ArrayList();
    private int field_149314_f;
    private int field_149315_g;

    public S3EPacketTeams() {
    }

    public S3EPacketTeams(ScorePlayerTeam team, int mode) {
        this.field_149320_a = team.getRegisteredName();
        this.field_149314_f = mode;
        if (mode == 0 || mode == 2) {
            this.field_149318_b = team.func_96669_c();
            this.field_149319_c = team.getColorPrefix();
            this.field_149316_d = team.getColorSuffix();
            this.field_149315_g = team.func_98299_i();
        }
        if (mode == 0) this.field_149317_e.addAll(team.getMembershipCollection());
    }

    public S3EPacketTeams(ScorePlayerTeam team, Collection players, int mode) {
        if (mode != 3 && mode != 4) throw new IllegalArgumentException("Method must be join or leave for player constructor");
        if (players == null || players.isEmpty()) throw new IllegalArgumentException("Players cannot be null/empty");
        this.field_149314_f = mode;
        this.field_149320_a = team.getRegisteredName();
        this.field_149317_e.addAll(players);
    }

    @Override
    public void readPacketData(PacketBuffer data) throws IOException {
        field_149320_a = data.readStringFromBuffer(16);
        field_149314_f = data.readByte();
        if (field_149314_f == 0 || field_149314_f == 2) {
            field_149318_b = data.readStringFromBuffer(32);
            field_149319_c = data.readStringFromBuffer(16);
            field_149316_d = data.readStringFromBuffer(16);
            field_149315_g = data.readByte();
        }
        if (field_149314_f == 0 || field_149314_f == 3 || field_149314_f == 4) {
            int count = data.readShort();
            for (int i = 0; i < count; i++) field_149317_e.add(data.readStringFromBuffer(40));
        }
    }

    @Override
    public void writePacketData(PacketBuffer data) throws IOException {
        data.writeStringToBuffer(field_149320_a);
        data.writeByte(field_149314_f);
        if (field_149314_f == 0 || field_149314_f == 2) {
            data.writeStringToBuffer(field_149318_b);
            data.writeStringToBuffer(field_149319_c);
            data.writeStringToBuffer(field_149316_d);
            data.writeByte(field_149315_g);
        }
        if (field_149314_f == 0 || field_149314_f == 3 || field_149314_f == 4) {
            data.writeShort(field_149317_e.size());
            for (Object name : field_149317_e) data.writeStringToBuffer((String) name);
        }
    }

    public String func_149312_c() {
        return field_149320_a;
    }

    public String func_149306_d() {
        return field_149318_b;
    }

    public String func_149311_e() {
        return field_149319_c;
    }

    public String func_149309_f() {
        return field_149316_d;
    }

    public Collection func_149310_g() {
        return field_149317_e;
    }

    public int func_149307_h() {
        return field_149314_f;
    }

    public int func_149308_i() {
        return field_149315_g;
    }
}
