var Y0 = 150, X0 = 2200, Z0 = 2200;
var MB = Java.type("net.minecraft.world.level.block.Blocks");
function setv(x, y, z, b) { L.lvl().setBlock(new L.BP(x, y, z), b.defaultBlockState(), 3); }
if (tick == 0) {
  L.platform(X0 - 6, Y0, Z0, 30, 40, 8);
  G.p = L.rig({name: "PUMP", X: X0, Z: Z0 + 30, Y: Y0, fuel: [["rc ethanol", 20000]], done: function (c) {
    var m = c.mach; if (!m) return;
    // water source column under the pump (pump draws from below)
    for (var d = 1; d <= 4; d++) setv(c.mx, Y0 - d, c.mz, MB.WATER);
    // pipes along the side of the pump: perpendicular to the shaft line, 4 long, then a reservoir
    var sx = c.dz != 0 ? 1 : 0, sz = c.dx != 0 ? 1 : 0;
    G.pipes = [];
    for (var k = 1; k <= 4; k++) { var r = L.place(L.stackOf("PIPE"), c.mx + sx * k, Y0, c.mz + sz * k, null, 0); G.pipes.push([c.mx + sx * k, c.mz + sz * k, r.r]); }
    var rr = L.place(L.stackOf("RESERVOIR"), c.mx + sx * 5, Y0, c.mz + sz * 5, null, 0);
    G.res = [c.mx + sx * 5, c.mz + sz * 5];
    L.say("pump rig built; pipes " + G.pipes.map(function (p) { return p[2]; }).join(",") + " reservoir " + rr.r + " dir " + sx + "," + sz);
  }});
}
if (tick % 60 == 0 && tick >= 60 && G.p && G.p.mach) {
  var c = G.p, pm = L.te(c.mx, Y0, c.mz), rs = L.te(G.res[0], Y0, G.res[1]);
  var pipes = G.pipes.map(function (q) { var t = L.te(q[0], Y0, q[1]); return t ? L.fs(t, "liquid").substring(0, 40) + "/lvl=" + (t.getLevel ? t.getLevel() : "?") : "none"; });
  L.say("t" + tick + " pump tank=" + L.fs(pm, "tank") + " | pipes: " + pipes.join(" ; ") + " | reservoir tank=" + L.fs(rs, "tank"));
}
L.run(tick);
