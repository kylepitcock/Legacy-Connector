var Y0 = 150, X0 = 700, Z0 = 700;
var NAMES = ["EXTRACTOR", "FRACTIONATOR", "OBSIDIAN", "BIGFURNACE", "DISTILLER", "CENTRIFUGE", "GASTANK", "HEATER", "FERTILIZER", "LAVAMAKER", "PULSEJET", "WETTER", "AGGREGATOR", "FUELENHANCER", "BLASTFURNACE", "SPRINKLER", "DRYING", "COMPOSTER", "COMPRESSOR", "DYNAMO", "BOILER", "STEAMTURBINE", "MAGNETIC", "ELECTRICMOTOR"];
if (tick == 0) {
  L.platform(X0 - 12, Y0, Z0, 30, NAMES.length * 8 + 2, 8);
  G.rigs = {};
  NAMES.forEach(function (n, i) {
    var z = Z0 + 1 + i * 8;
    G.rigs[n] = L.rig({name: n, X: X0, Z: z, Y: Y0, above: true, fuel: [["rc ethanol", 4000]], done: function (c) { if (!c.mach) L.say("rig " + n + " FAILED"); }});
  });
}
if (tick == 150) {
  L.say("--- tick " + tick);
  NAMES.forEach(function (n) {
    var c = G.rigs[n];
    if (!c || !c.mach) { L.say(n + ": no machine"); return; }
    var t = L.te(c.mx, c.my, c.mz);
    var inv = ""; try { inv = L.slots(t); } catch (e) {}
    L.say(n + ": " + L.snap(t) + " bevel(" + (c.bevel ? L.snap(c.bevel) : "-") + ") " + inv);
  });
}
L.run(tick);
