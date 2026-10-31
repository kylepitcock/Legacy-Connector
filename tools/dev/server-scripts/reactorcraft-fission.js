// run: ./gradlew runServerSubTest -PautostopTicks=600 -PserverScriptFrom=100 -PserverScript=<abs path of this file>
// needs the Reika jars (DragonAPI, RotaryCraft, ElectriCraft, ReactorCraft) + CoFHCore in run-subtest/mods/1.7.10; see docs/wiki/Development.md
var __p = String(java.lang.System.getProperty("legacyconnector.serverScript")).split("\\").join("/");
load(__p.substring(0, __p.lastIndexOf("/") + 1) + "lib.js");
var Y = 100, X = 300, Z = 300;
var RT = Java.type("Reika.ReactorCraft.Registry.ReactorTiles");
var RF = Java.type("Reika.ReactorCraft.Registry.ReactorFuel");
var LS = Java.type("Reika.ReactorCraft.TileEntities.Fission.TileEntityWaterCell$LiquidStates");
var out = "";
var ENG = Java.type("Reika.RotaryCraft.Registry.EngineType");
var FS = Java.type("legacyconnector.v1_7_10.net.minecraftforge.fluids.FluidStack");
var FR = Java.type("legacyconnector.v1_7_10.net.minecraftforge.fluids.FluidRegistry");
function engStack(t) { return new LIS(ENG.valueOf(t).getCraftedProduct().getItem(), 1, ENG.valueOf(t).ordinal()).mcc$modern(); }
function rt(n) { return ms(RT.valueOf(n).getCraftedProduct()); }
if (tick == 0) { flat(X - 6, Y, Z - 6, 14, 14, 8); cmd("forceload add " + (X - 70) + " " + (Z - 70) + " " + (X + 70) + " " + (Z + 70)); }
YAW = -90;
var N = 3;
var parts = [];
for (var dy = 0; dy < N; dy++) for (var dx = 0; dx < N; dx++) for (var dz = 0; dz < N; dz++) {
  var kind = ((dx + dy + dz) % 2 == 0) ? "FUEL" : "COOLANT";
  if (dx == 1 && dz == 1 && dy == N - 1) kind = "CONTROL";
  parts.push([kind, dx, dy, dz]);
}
if (typeof WITHCPU === "undefined") var WITHCPU = true;
parts.push(["CPU", -1, 0, 0]);
parts.forEach(function (p, i) {
  if (tick == 3 + i) {
    var dy = p[2];
    if (dy && lvl().getBlockState(bp(X + p[1], Y + dy - 1, Z + p[3])).isAir()) setBlock(X + p[1], Y + dy - 1, Z + p[3], Blocks.STONE);
    var r = place(rt(p[0]), X + p[1], Y + dy, Z + p[3]);
    if (r != "CONSUME") out += p[0] + "@" + p[1] + "," + dy + "," + p[3] + ":" + r + " ";
  }
});
var T0 = 3 + parts.length + 5;
var ENGPOS = [X - 2, Y, Z];
if (tick == T0 - 3) { YAW = 90; out += "engine:" + place(engStack("GAS"), ENGPOS[0], ENGPOS[1], ENGPOS[2]); }
if (tick == T0 - 1) {
  var e = te(ENGPOS[0], ENGPOS[1], ENGPOS[2]);
  f(e, "fuel").fill(new FS(ENG.GAS.getFuelType(), 4000), true); f(e, "lubricant").fill(new FS(FR.getFluid("rc lubricant"), 4000), true);
  setBlock(ENGPOS[0], ENGPOS[1] + 1, ENGPOS[2], Blocks.REDSTONE_BLOCK);
}
function tiles(n) { var r = []; parts.forEach(function (p) { if (p[0] == n) { var t = te(X + p[1], Y + p[2], Z + p[3]); if (t) r.push(t); } }); return r; }
if (tick == T0) {
  tiles("CPU").forEach(function (c) { });
  tiles("FUEL").forEach(function (t) { for (var s = 0; s < t.getSizeInventory(); s++) t.setInventorySlotContents(s, RF.URANIUM.getFuelItem()); });
  tiles("COOLANT").forEach(function (t) { t.setLiquidState(LS.WATER); });
  var c = tiles("CPU")[0]; out += "CPU min omega/torque/power=" + c.getMinSpeed() + "/" + c.getMinTorque() + "/" + c.getMinPower() + " ";
  out += "loaded " + tiles("FUEL").length + " rods, " + tiles("COOLANT").length + " cells, " + tiles("CONTROL").length + " control";
}
if (tick == T0 + 30) tiles("CPU")[0].raiseAllRods();
function neutrons() {
  var n = 0, ents = lvl().getAllEntities();
  for (var it = ents.iterator(); it.hasNext();) { var e = it.next(); if (("" + e.getType()).indexOf("neutron") >= 0) n++; }
  return n;
}
function mx(arr) { return Math.max.apply(null, arr); }
if (tick > T0 && (tick - T0) % 100 == 0) {
  out += "T+" + (tick - T0) + " neutrons=" + neutrons();
  out += " maxFuelT=" + mx(tiles("FUEL").map(function (t) { return t.getTemperature(); }));
  out += " maxCoolT=" + mx(tiles("COOLANT").map(function (t) { return t.getTemperature(); }));
  out += " ctrl=" + tiles("CONTROL").map(function (t) { return (t.isActive() ? "LOWERED" : "up") + "/" + t.getRodPosition(); }).join(",");
  var c = tiles("CPU")[0]; out += " cpuIn=" + c.getOmega() + "x" + c.getTorque() + "=" + c.getPower();
  out += " cpuT=" + tiles("CPU").map(function (t) { return t.getTemperature(); }).join(",");
  out += " blocksLeft=" + tiles("FUEL").length;
}
out;
