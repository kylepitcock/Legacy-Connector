var Y0 = 200, X0 = 1500, Z0 = 1500;
var MB = Java.type("net.minecraft.world.level.block.Blocks");
function setv(x, y, z, b) { L.lvl().setBlock(new L.BP(x, y, z), b.defaultBlockState(), 3); }
if (tick == 0) {
  L.platform(X0, Y0, Z0, 40, 40, 14);
  // wind engines on pillars of various heights (all neighbours air)
  [1, 2, 3].forEach(function (h, i) {
    var x = X0 + 4 + i * 8, z = Z0 + 4;
    for (var k = 0; k < h; k++) setv(x, Y0 + k, z, MB.STONE);
    var r = L.place(L.engStack(1), x, Y0 + h, z);
    L.say("wind on pillar h=" + h + " place=" + r.r + " te=" + (r.te ? r.te.getClass().getSimpleName() : null));
    if (r.te) G["wind" + h] = r.te;
  });
  G.hydro = [];
  // hydro: the 3x3 plane around the engine must be air or liquid (the floor is dug out and filled with water)
  [[Z0 + 14, 'water cube'], [Z0 + 24, 'flowing water cube']].forEach(function (cfg, k) {
    var hx = X0 + 8, hz = cfg[0];
    for (var a = -2; a <= 2; a++) for (var b = -2; b <= 1; b++) for (var c = -2; c <= 2; c++) {
      if (a == 0 && b == 0 && c == 0) { setv(hx, Y0 + b, hz, MB.AIR); continue; }
      var fluid = (k == 0) ? MB.WATER.defaultBlockState() : Java.type('net.minecraft.world.level.material.Fluids').FLOWING_WATER.getFlowing(3, false).createLegacyBlock();
      L.lvl().setBlock(new L.BP(hx + a, Y0 + b, hz + c), fluid, 3);
    }
    var r = L.place(L.engStack(6), hx, Y0, hz);
    L.say('hydro ' + cfg[1] + ' place=' + r.r + ' te=' + (r.te ? r.te.getClass().getSimpleName() : null));
    var ht = L.te(hx, Y0 - 1, hz); L.say('  te at y-1: ' + ht + ' blk=' + L.blk(hx, Y0 - 1, hz)); if (ht) G.hydro.push(ht);
  });
}
if (tick == 120 || tick == 220) {
  [1, 2, 3].forEach(function (h) { var t = G["wind" + h]; if (t) { var c = L.te(t.xCoord, t.yCoord, t.zCoord); L.say("wind h=" + h + " " + L.snap(c) + " temp=" + L.fs(c, "temperature")); } });
  G.hydro.forEach(function (t, i) { if (t) { var c = L.te(t.xCoord, t.yCoord, t.zCoord); L.say("hydro " + i + " " + L.snap(c) + " lube=" + L.fs(c, "lubricant")); } });
}
L.run(tick);
