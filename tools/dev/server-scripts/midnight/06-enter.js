// spawns a rift in the overworld at night, steps a survival player into it and watches the trip to the Midnight
var res = "";
EB = T("org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122");
WB = T("org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldBridge1122");
LBP = T("legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos");
RSH = T("com.mushroom.midnight.common.world.RiftSpawnHandler");
ow = server.overworld();
ow.setDayTime(18000);
function lbp(a, b, c) { var I = Java.type("int").class; return LBP.class.getConstructor(I, I, I).newInstance(java.lang.Integer.valueOf(a), java.lang.Integer.valueOf(b), java.lang.Integer.valueOf(c)); }
function findRift() {
  var best = null;
  ow.getAllEntities().forEach(function (e) { if (String(e.getType().builtInRegistryHolder().key().location()) == "midnight:rift") best = e; });
  return best;
}
if (typeof sp === "undefined" || sp == null) sp = realPlayer("Rifter1");
ow.getChunk(2, 2);
sp.teleportTo(ow, 40.5, ow.getHeight(T("net.minecraft.world.level.levelgen.Heightmap.Types").MOTION_BLOCKING_NO_LEAVES, 40, 40) + 1, 40.5, 0, 0);
var px = sp.blockPosition().getX(), pz = sp.blockPosition().getZ();
var py = ow.getHeight(T("net.minecraft.world.level.levelgen.Heightmap.Types").MOTION_BLOCKING_NO_LEAVES, px + 5, pz);
var m = RSH.class.getDeclaredMethod("spawnRift", T("legacyconnector.v1_12_2.net.minecraft.world.World").class, LBP.class);
m.setAccessible(true);
m.invoke(null, WB.wrap(ow), lbp(px + 5, py, pz));
rift = findRift();
res += "rift " + rift.position() + " bb " + rift.getBoundingBox() + "\n";
tt = 0;
onTick = function (n) {
  tt++;
  if (tt == 30) {
    var rp = rift.position();
    sp.teleportTo(rift.level(), rp.x, rp.y + 0.5, rp.z, 0, 0);
    log.info("ENTERWATCH moved player into rift " + sp.position() + " bb " + sp.getBoundingBox());
  }
  if (tt % 10 == 0 && tt <= 300) {
    var r = EB.wrap(rift);
    log.info("ENTERWATCH t=" + tt + " player " + sp.level().dimension().location() + " " + sp.position() + " hp=" + sp.getHealth() + " riftOpen=" + r.isOpen() + " used=" + r.wasUsed() + " riftRemoved=" + rift.isRemoved() + " endpointLoaded=" + r.isEndpointLoaded());
  }
  return tt > 300;
};
res;
