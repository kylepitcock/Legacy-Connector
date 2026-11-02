// Spawns a Midnight rift next to a survival player in the overworld at night and watches the player get pulled in.
var out = "";
sp = realPlayer("Rifter1");
ow = server.overworld();
WB = T("org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldBridge1122");
LBP = T("legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos");
RSH = T("com.mushroom.midnight.common.world.RiftSpawnHandler");
ER = T("com.mushroom.midnight.common.entity.EntityRift");
// find a ground position near spawn
var x = 40, z = 40;
ow.getChunk(x >> 4, z >> 4);
var y = ow.getHeight(T("net.minecraft.world.level.levelgen.Heightmap.Types").MOTION_BLOCKING_NO_LEAVES, x, z);
sp.teleportTo(ow, x + 0.5, y + 1, z + 0.5, 0, 0);
sp.setGameMode(T("net.minecraft.world.level.GameType").SURVIVAL);
ow.setDayTime(18000);
out += "player at " + sp.blockPosition() + " in " + sp.level().dimension().location() + " night=" + ow.isNight() + "\n";
function lbp(a, b, c) { var I = Java.type("int").class; return LBP.class.getConstructor(I, I, I).newInstance(java.lang.Integer.valueOf(a), java.lang.Integer.valueOf(b), java.lang.Integer.valueOf(c)); }
var m = RSH.class.getDeclaredMethod("spawnRift", T("legacyconnector.v1_12_2.net.minecraft.world.World").class, LBP.class);
m.setAccessible(true);
out += P("spawnRift", function () { m.invoke(null, WB.wrap(ow), lbp(x + 6, y, z)); });
rifts = [];
ow.getAllEntities().forEach(function (e) { if (String(e.getType()).indexOf("rift") >= 0) rifts.push(e); });
out += "rifts: " + rifts.map(function (e) { return e.getType() + "@" + e.blockPosition(); }).join(",") + "\n";
tt = 0;
onTick = function (n) {
  tt++;
  if (tt % 20 == 0 && tt <= 1200) {
    var dim = String(sp.level().dimension().location());
    log.info("RIFTWATCH t=" + tt + " player " + dim + " " + sp.blockPosition() + " hp=" + sp.getHealth() + " alive=" + sp.isAlive());
  }
  return tt > 1200;
};
out;
