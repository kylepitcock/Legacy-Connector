var Y0 = 150, X0 = 900, Z0 = 900;
var LANES = {
  splitter: ["SHAFT", "SPLITTER"],
  splitter2: ["SHAFT", "SPLITTER", "SHAFT"],
  adv: ["SHAFT", "ADVANCEDGEARS"],
  plain: ["SHAFT", "SHAFT"]
};
if (tick == 0) {
  var names = Object.keys(LANES);
  L.platform(X0 - 14, Y0, Z0, 30, names.length * 8 + 22, 8);
  G.lanes = {};
  names.forEach(function (n, i) {
    var z = Z0 + 12 + i * 8;
    G.lanes[n] = L.rig({name: n, X: X0, Z: z, Y: Y0, mid: LANES[n], stackFn: function () { return L.stackOf("SHAFT"); }, anyYaw: true, fuel: [["rc ethanol", 8000]], done: function (c) {
      L.say("lane " + n + " built dx=" + c.dx + " dz=" + c.dz + " at tick " + L.now);
    }});
  });
}
if (tick % 20 == 0 && tick >= 20 && tick <= 200) {
  Object.keys(LANES).forEach(function (n) {
    var c = G.lanes[n]; if (!c || !c.eng) return;
    var e = L.te(c.eng.xCoord, c.eng.yCoord, c.eng.zCoord);
    var parts = ["eng(" + L.snap(e) + " on=" + e.isOn() + " same=" + (e === c.eng) + ")"];
    c.shafts.forEach(function (t, k) { var cur = L.te(t.xCoord, t.yCoord, t.zCoord); parts.push(LANES[n][k].substring(0, 4) + "(" + L.snap(cur) + " same=" + (cur === t) + " w=" + L.fs(cur, "write") + " r=" + L.fs(cur, "read") + ")"); });
    if (c.mach) parts.push("end(" + L.snap(c.mach) + ")");
    L.say(tick + " " + n + ": " + parts.join(" "));
  });
}
L.run(tick);
