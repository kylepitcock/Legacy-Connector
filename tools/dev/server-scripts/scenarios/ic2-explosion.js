// Runs IC2 Classic's own explosion (ExplosionIC2, PointExplosion) over a stone block and a pig (1.7.10; jar in run-subtest/mods/1.7.10).
var Y0 = 150, X0 = 2600, Z0 = 2600;
function count(r) {
  var n = 0;
  for (var x = X0 - r; x <= X0 + r; x++) for (var y = Y0 - r; y < Y0 + r; y++) for (var z = Z0 - r; z <= Z0 + r; z++)
    if (!L.lvl().getBlockState(new L.BP(x, y, z)).isAir()) n++;
  return n;
}
if (tick == 0) {
  L.platform(X0 - 14, Y0, Z0 - 14, 28, 28, 14);
  for (var x = X0 - 8; x <= X0 + 8; x++) for (var y = Y0; y < Y0 + 8; y++) for (var z = Z0 - 8; z <= Z0 + 8; z++) L.lvl().setBlock(new L.BP(x, y, z), L.Blocks.STONE.defaultBlockState(), 2);
  G.pig = Java.type("net.minecraft.world.entity.EntityType").PIG.create(L.lvl());
  G.pig.moveTo(X0 + 9.5, Y0 + 1, Z0 + 0.5, 0, 0);
  L.lvl().addFreshEntity(G.pig);
  G.n0 = count(10);
  L.say("blocks before " + G.n0 + " pig " + G.pig.getHealth());
}
if (tick == 20) {
  try {
    var w = Java.type("legacyconnector.v1_7_10.net.minecraft.world.World").mcc$wrap(L.lvl());
    var EX = Java.type("ic2.core.ExplosionIC2");
    var e = new EX(w, null, X0 + 0.5, Y0 + 4.5, Z0 + 0.5, 30.0, 0.5, 1.0);
    e.doExplosion();
    L.say("ExplosionIC2: blocks " + G.n0 + " -> " + count(10) + " pig " + G.pig.getHealth() + " destroyed=" + e.destroyedBlockPositions.size());
  } catch (e) { L.say("ExplosionIC2 ERR " + e); if (e.getStackTrace) { var a = e.getStackTrace(); for (var k = 0; k < 10; k++) L.say("  at " + a[k]); } }
  try {
    var b = count(10);
    var w2 = Java.type("legacyconnector.v1_7_10.net.minecraft.world.World").mcc$wrap(L.lvl());
    w2.createExplosion(null, X0 + 0.5, Y0 + 4.5, Z0 + 0.5, 6.0, true);
    L.say("world.createExplosion: blocks " + b + " -> " + count(10));
  } catch (e) { L.say("createExplosion ERR " + e); if (e.getStackTrace) { var a = e.getStackTrace(); for (var k = 0; k < 10; k++) L.say("  at " + a[k]); } }
}
L.run(tick);
