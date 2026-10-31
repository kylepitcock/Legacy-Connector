var Y0 = 150, X0 = 500, Z0 = 500;
var NAMES = ["GRINDER", "PUMP", "FAN", "BORER", "EXTRACTOR", "COMPACTOR", "FERMENTER", "BLASTFURNACE", "FRACTIONATOR", "CRAFTER", "OBSIDIAN",
  "WOODCUTTER", "SPRINKLER", "FILLINGSTATION", "BIGFURNACE", "DISTILLER", "CENTRIFUGE", "REFRIGERATOR", "COMPRESSOR", "GENERATOR", "ELECTRICMOTOR", "DYNAMO",
  "MAGNETIZER", "PILEDRIVER", "VACUUM", "HEATER", "FRICTION", "SORTING", "BEDROCKBREAKER", "FERTILIZER", "LAVAMAKER", "DRYING", "BLOWER", "AEROSOLIZER",
  "CRYSTALLIZER", "PULSEJET", "BOILER", "STEAMTURBINE", "MOBHARVESTER", "PURIFIER", "DROPS", "COMPOSTER", "GRINDSTONE", "MAGNETIC", "BELT", "CHAIN"];
if (tick == 0) {
  L.platform(X0 - 12, Y0, Z0, 30, NAMES.length * 8 + 2, 8);
  G.rigs = {};
  NAMES.forEach(function (n, i) {
    var z = Z0 + 1 + i * 8;
    try {
      G.rigs[n] = L.rig({name: n, X: X0, Z: z, Y: Y0, fuel: [["rc ethanol", 4000]], done: function (c) {
        var m = c.mach;
        if (!m) L.say("rig " + n + " FAILED"); else if (c.yaw === null) L.say("rig " + n + " misaligned, read=" + L.fs(m, "read"));
      }});
    } catch (e) { L.say("RIG ERR " + n + ": " + e); }
  });
}
if (tick == 1500) {
  L.say("--- tick " + tick);
  NAMES.forEach(function (n) {
    var c = G.rigs[n];
    if (!c || !c.mach) { L.say(n + ": no machine"); return; }
    var t = L.te(c.mx, Y0, c.mz);
    var inv = ""; try { inv = L.slots(t); } catch (e) {}
    L.say(n + ": " + L.snap(t) + " " + inv);
  });
}
if (tick % 300 == 0) L.say("avg tick ms=" + (server.getAverageTickTimeNanos() / 1e6).toFixed(2) + " tick=" + tick);
L.run(tick);
