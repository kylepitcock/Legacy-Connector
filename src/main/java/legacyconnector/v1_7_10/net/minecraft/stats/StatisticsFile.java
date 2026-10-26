package legacyconnector.v1_7_10.net.minecraft.stats;

/** The server's statistics file of one player (see {@link StatFileWriter}); nothing is written to disk. */
public class StatisticsFile extends StatFileWriter {
    public StatisticsFile() {
    }

    public StatisticsFile(java.util.UUID player) {
        super(player);
    }

    public void func_150882_a() {
    }

    public void func_150883_b() {
    }

    public java.util.Set func_150878_c() {
        return new java.util.HashSet<>(mcc$values.keySet());
    }

    public void func_150877_d() {
    }

    public void func_150876_a(legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayerMP player) {
    }

    public void func_150884_b(legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayerMP player) {
    }

    public boolean func_150879_e() {
        return false;
    }
}
