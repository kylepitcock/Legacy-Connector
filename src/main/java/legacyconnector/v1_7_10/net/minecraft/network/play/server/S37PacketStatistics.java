package legacyconnector.v1_7_10.net.minecraft.network.play.server;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import legacyconnector.v1_7_10.net.minecraft.network.Packet;
import legacyconnector.v1_7_10.net.minecraft.network.PacketBuffer;
import legacyconnector.v1_7_10.net.minecraft.stats.StatBase;
import legacyconnector.v1_7_10.net.minecraft.stats.StatList;

/**
 * The statistics of a player for the client (1.7.10 {@code S37PacketStatistics}): a map of stat to value. The wire format and accessors are the
 * legacy ones; the modern client keeps its own statistics, so sending it to a player does nothing.
 */
public class S37PacketStatistics extends Packet {
    private Map field_148976_a;

    public S37PacketStatistics() {
        this.field_148976_a = new HashMap();
    }

    public S37PacketStatistics(Map stats) {
        this.field_148976_a = stats;
    }

    @Override
    public void readPacketData(PacketBuffer data) throws IOException {
        int count = data.readVarIntFromBuffer();
        field_148976_a = new HashMap();
        for (int i = 0; i < count; i++) {
            StatBase stat = StatList.oneShotStats.get(data.readStringFromBuffer(32767));
            int value = data.readVarIntFromBuffer();
            if (stat != null) field_148976_a.put(stat, value);
        }
    }

    @Override
    public void writePacketData(PacketBuffer data) throws IOException {
        data.writeVarIntToBuffer(field_148976_a.size());
        for (Object o : field_148976_a.entrySet()) {
            Map.Entry entry = (Map.Entry) o;
            data.writeStringToBuffer(((StatBase) entry.getKey()).statId);
            data.writeVarIntToBuffer(((Number) entry.getValue()).intValue());
        }
    }

    @Override
    public String serialize() {
        StringBuilder out = new StringBuilder("count=").append(field_148976_a.size());
        return out.toString();
    }

    public Map func_148974_c() {
        return field_148976_a;
    }
}
