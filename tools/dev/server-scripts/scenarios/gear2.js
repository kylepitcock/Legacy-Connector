var Y0 = 150, X0 = 2400, Z0 = 2400;
var LANES = {
  direct: ["SHAFT", "GEARBOX", "GEARBOX", "SHAFT"],
  withshaft: ["SHAFT", "GEARBOX", "SHAFT", "GEARBOX", "SHAFT"],
  single: ["SHAFT", "GEARBOX", "SHAFT"]
};
if (tick == 0) {
  var names = Object.keys(LANES);
  L.platform(X0 - 14, Y0, Z0, 30, names.length * 8 + 22, 8);
  G.lanes = {};
  names.forEach(function (n, i) {
    G.lanes[n] = L.rig({name: n, X: X0, Z: Z0 + 12 + i * 8, Y: Y0, mid: LANES[n], stackFn: function () { return L.stackOf("SHAFT"); }, anyYaw: true, fuel: [["rc ethanol", 8000]], done: function (c) { L.say("lane " + n + " built"); }});
  });
}
if (tick == 100 || tick == 200) {
  Object.keys(LANES).forEach(function (n) {
    var c = G.lanes[n]; if (!c || !c.eng) return;
    L.say("== " + n + " @" + tick + " eng(" + L.snap(c.eng) + ")");
    c.shafts.forEach(function (t0, k) {
      if (!t0) { L.say("   " + LANES[n][k] + " MISSING"); return; }
      var t = L.te(t0.xCoord, t0.yCoord, t0.zCoord);
      if (!t) { L.say("   " + LANES[n][k] + " tile gone at " + t0.xCoord + "," + t0.zCoord + " blk=" + L.blk(t0.xCoord, t0.yCoord, t0.zCoord)); return; }
      var isG = t.getClass().getSimpleName() == "TileEntityGearbox";
      L.say("   " + LANES[n][k] + " " + L.snap(t) + " omegain=" + L.fs(t, "omegain") + " torquein=" + L.fs(t, "torquein") + " read=" + L.fs(t, "read") + " write=" + L.fs(t, "write") + (isG ? " ratio=" + L.fs(t, "ratio") + " reduction=" + L.fs(t, "reduction") + " failed=" + L.fs(t, "failed") + " damage=" + L.fs(t, "damage") + " type=" + L.fs(t, "type") + " temp=" + L.fs(t, "temperature") : ""));
    });
    if (c.mach) L.say("   END " + L.snap(c.mach));
  });
}
L.run(tick);
