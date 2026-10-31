// run: ./gradlew runServerSubTest -PautostopTicks=600 -PserverScriptFrom=100 -PserverScript=<abs path of this file>
// needs the Reika jars (DragonAPI, RotaryCraft, ElectriCraft, ReactorCraft) + CoFHCore in run-subtest/mods/1.7.10; see docs/wiki/Development.md
var __p = String(java.lang.System.getProperty("legacyconnector.serverScript")).split("\\").join("/");
load(__p.substring(0, __p.lastIndexOf("/") + 1) + "lib.js");
var Y = -60, X = 300;
var ET = Java.type("Reika.ElectriCraft.Registry.ElectriTiles");
var WT = Java.type("Reika.ElectriCraft.Registry.WireType");
var BT = Java.type("Reika.ElectriCraft.Registry.BatteryType");
var ENG = Java.type("Reika.RotaryCraft.Registry.EngineType");
var MR = Java.type("Reika.RotaryCraft.Registry.MachineRegistry");
var FS = Java.type("legacyconnector.v1_7_10.net.minecraftforge.fluids.FluidStack");
var FR = Java.type("legacyconnector.v1_7_10.net.minecraftforge.fluids.FluidRegistry");
var out = "";
function engStack(t) { return new LIS(ENG.valueOf(t).getCraftedProduct().getItem(), 1, ENG.valueOf(t).ordinal()).mcc$modern(); }
function wire(t) { return ms(WT.valueOf(t).getCraftedProduct()); }
function tile(t) { return ms(ET.valueOf(t).getCraftedProduct()); }
// part: [x offset from X (negative = west), yaw, item factory]
function eng(t) { return [1, -90, function () { return engStack(t); }]; }
var GEN = [0, -90, function () { return tile("GENERATOR"); }];
function W(dx, t) { return [dx, -90, function () { return wire(t || "STEEL"); }]; }
function T(dx, name, yaw) { return [dx, yaw === undefined ? -90 : yaw, function () { return tile(name); }]; }
var BAT = [-2, -90, function () { return ms(BT.REDSTONE.getCraftedProduct()); }];
function UP(p) { return [p[0], p[1], p[2], 1]; }
var lines = {
  B: {z: 308, parts: [eng("JET"), GEN, W(-1), W(-2), T(-3, "MOTOR")]},
  B2: {z: 316, parts: [eng("JET"), GEN, W(-1, "TIN"), W(-2, "TIN"), T(-3, "MOTOR")]},
  C: {z: 324, parts: [eng("GAS"), GEN, W(-1, "COPPER"), BAT, UP(W(-2, "COPPER")), UP(W(-3, "COPPER")), UP(T(-4, "MOTOR"))]},
  J: {z: 332, parts: [eng("JET"), GEN, W(-1, "COPPER"), T(-2, "FUSE"), W(-3, "COPPER"), T(-4, "MOTOR")]}
};
var names = Object.keys(lines);
if (tick == 0) { flat(X - 12, Y, 296, 20, 50); }
names.forEach(function (n) {
  var L = lines[n];
  L.parts.forEach(function (p, i) {
    if (tick == 3 + 2 * i) { YAW = p[1]; var dy = p[3] || 0; if (dy && lvl().getBlockState(bp(X + p[0], Y, L.z)).isAir()) setBlock(X + p[0], Y, L.z, Blocks.STONE); out += n + i + ":" + place(p[2](), X + p[0], Y + dy, L.z) + " "; }
  });
});
function tl(n, dx, dy) { return te(X + dx, Y + (dy || 0), lines[n].z); }
function fuelUp() {
  names.forEach(function (n) {
    var e = tl(n, 1); var t = f(e, "type");
    var fuelFluid = t.isJetFueled() ? FR.getFluid("rc jet fuel") : t.getFuelType();
    out += n + " fuel " + f(e, "fuel").fill(new FS(fuelFluid, 4000), true) + " lube " + f(e, "lubricant").fill(new FS(FR.getFluid("rc lubricant"), 4000), true) + " ";
    if (n != "E") setBlock(X + 1, Y + 1, lines[n].z, Blocks.REDSTONE_BLOCK);
    else setBlock(X + 1, Y + 1, lines[n].z, Blocks.REDSTONE_BLOCK);
  });
}
if (tick == 30) fuelUp();
if (tick == 40) { setBlock(X - 2, Y, 325, Blocks.REDSTONE_BLOCK); }
function nw(t) { var n = f(t, "network"); return n == "?network" ? null : n; }
function rd(n) {
  var o = "\n[" + n + "] ", L = lines[n];
  var g = tl(n, 0);
  o += "eng " + fs(tl(n, 1), ["power", "omega", "torque"]) + " | gen V=" + g.getGenVoltage() + " I=" + g.getGenCurrent();
  var net = nw(g);
  if (net) o += " | net src=" + net.getNumberSources() + " sinks=" + net.getNumberSinks() + " paths=" + net.getNumberPaths() + " totalI=" + net.getTotalCurrent();
  L.parts.forEach(function (p, i) {
    var t = tl(n, p[0], p[3]);
    if (t == null) { o += "\n   part" + i + " dx=" + p[0] + " MISSING (" + lvl().getBlockState(bp(X + p[0], Y + (p[3] || 0), L.z)).getBlock() + ")"; return; }
    var cn = t.getClass().getSimpleName();
    if (cn == "TileEntityMotor") o += "\n   motor dx=" + p[0] + " " + fs(t, ["omega", "torque", "power"]);
    else if (cn == "TileEntityBattery") o += "\n   battery energy=" + t.getStoredEnergy() + "/" + t.getMaxEnergy();
    else if (cn == "TileEntityRFBattery") o += "\n   rfbattery dx=" + p[0] + " energy=" + t.getStoredEnergy() + "/" + t.getMaxEnergy();
    else if (cn == "TileEntityTransformer") o += "\n   transformer ratio=" + t.getRatioForDisplay() + " " + fs(t, ["Vin", "Ain", "Vout", "Aout", "temperature"]);
    else if (cn == "TileEntityShaft") o += "\n   shaft " + fs(t, ["omega", "torque", "power"]);
    else if (cn == "TileEntityWire") { var wn = nw(t); o += "\n   wire dx=" + p[0] + " V=" + (wn ? wn.getPointVoltage(t) : "?") + " I=" + (wn ? wn.getPointCurrent(t) : "?") + " max=" + t.getMaxCurrent(); }
    else o += "\n   " + cn + " dx=" + p[0];
  });
  return o;
}
if (tick >= 60 && tick % 100 == 0) { out += "T" + tick; names.forEach(function (n) { try { out += rd(n); } catch (e) { out += "\n[" + n + "] EXC " + e; } }); }
out;
