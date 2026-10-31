var Y0 = 150, X0 = 800, Z0 = 800;
var LANES = {
  gearbox: ["SHAFT", "GEARBOX", "SHAFT"],
  gearbox2: ["SHAFT", "GEARBOX", "GEARBOX", "SHAFT"],
  flywheel: ["SHAFT", "FLYWHEEL", "SHAFT"],
  clutch: ["SHAFT", "CLUTCH", "SHAFT"],
  multiclutch: ["SHAFT", "MULTICLUTCH", "SHAFT"],
  adv: ["SHAFT", "ADVANCEDGEARS", "SHAFT"],
  splitter: ["SHAFT", "SPLITTER", "SHAFT"],
  longline: ["SHAFT", "SHAFT", "SHAFT", "SHAFT", "SHAFT", "SHAFT", "SHAFT", "SHAFT"],
  belt: ["SHAFT", "BELT", "SHAFT"]
};
if (tick == 0) {
  var names = Object.keys(LANES);
  L.platform(X0 - 14, Y0, Z0, 30, names.length * 8 + 2, 8);
  G.lanes = {};
  names.forEach(function (n, i) {
    var z = Z0 + 12 + i * 8;
    G.lanes[n] = L.rig({name: n, X: X0, Z: z, Y: Y0, mid: LANES[n], stackFn: function () { return L.stackOf("SHAFT"); }, anyYaw: true, fuel: [["rc ethanol", 8000]], done: function (c) {
      L.say("lane " + n + " built, mid=" + c.shafts.map(function (t) { return t ? t.getClass().getSimpleName() : "null"; }).join(",") + " end=" + (c.mach ? c.mach.getClass().getSimpleName() : null));
      if (n == "gearbox" || n == "gearbox2") c.shafts.forEach(function (t) { if (t && t.getClass().getSimpleName() == "TileEntityGearbox") { t.fillWithLubricant(); } });
    }});
  });
}
function report(tag) {
  L.say("--- " + tag + " @" + tick);
  Object.keys(LANES).forEach(function (n) {
    var c = G.lanes[n]; if (!c || !c.eng) { L.say(n + " not built"); return; }
    var parts = [];
    parts.push("eng(" + L.snap(c.eng) + ")");
    c.shafts.forEach(function (t, k) { if (t) parts.push(LANES[n][k].substring(0, 4) + "(" + L.snap(t) + (t.getClass().getSimpleName() == "TileEntityGearbox" ? " ratio=" + L.fs(t, "ratio") + " red=" + L.fs(t, "reduction") + " lube=" + t.getLubricant() : "") + ")"); });
    if (c.mach) parts.push("end(" + L.snap(c.mach) + ")");
    L.say(n + ": " + parts.join(" "));
  });
}
function ctePos(c, k) { var t = c.shafts[k]; return [t.xCoord, t.yCoord, t.zCoord]; }
if (tick == 150) report("steady-1");
if (tick == 151) {
  ["clutch", "multiclutch"].forEach(function (n) { var p = ctePos(G.lanes[n], 1); L.rs(p[0], p[1] + 1, p[2], true); });
}
if (tick == 200) report("clutch redstone ON");
if (tick == 201) {
  ["clutch", "multiclutch"].forEach(function (n) { var p = ctePos(G.lanes[n], 1); L.rs(p[0], p[1] + 1, p[2], false); });
}
if (tick == 240) report("clutch redstone OFF again");
L.run(tick);
