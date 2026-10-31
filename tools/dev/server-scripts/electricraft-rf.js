// run: ./gradlew runServerSubTest -PautostopTicks=600 -PserverTickScriptFrom=100 -PserverTickScript=<abs path of this file>
// needs the Reika jars (DragonAPI, RotaryCraft, ElectriCraft, ReactorCraft) + CoFHCore in run-subtest/mods/1.7.10; see docs/wiki/Development.md
var __p = String(java.lang.System.getProperty("legacyconnector.serverScript")).split("\\").join("/");
load(__p.substring(0, __p.lastIndexOf("/") + 1) + "lib-reika.js");
var Y = -60, X = 300, Z = 300;
var ET = Java.type("Reika.ElectriCraft.Registry.ElectriTiles");
var CAP = Java.type("net.neoforged.neoforge.capabilities.Capabilities");
var out = "";
function tile(t) { return ms(ET.valueOf(t).getCraftedProduct()); }
function cap(dx, dy, side) { return lvl().getCapability(CAP.EnergyStorage.BLOCK, bp(X + dx, Y + dy, Z), side || null); }
if (tick == 0) flat(X - 12, Y, Z - 4, 20, 9);
YAW = -90;
// [type, dx, dy]
var parts = [["RFBATTERY", 0, 0], ["CABLE", 0, 1], ["CABLE", -1, 1], ["CABLE", -2, 1], ["CABLE", -3, 1], ["CABLE", -4, 1], ["CABLE", -5, 1], ["CABLE", -5, 0], ["RFBATTERY", -6, 0]];
parts.forEach(function (p, i) {
  if (tick == 3 + 2 * i) {
    if (!p[2]) setBlock(X + p[1], Y, Z, Blocks.AIR);
    if (p[2] && lvl().getBlockState(bp(X + p[1], Y, Z)).isAir()) setBlock(X + p[1], Y, Z, Blocks.STONE);
    out += p[0] + ":" + place(tile(p[0]), X + p[1], Y + p[2], Z) + " ";
  }
});
if (tick == 30) {
  var a = te(X, Y, Z), b = te(X - 6, Y, Z);
  out += "inject into A via cap: " + cap(0, 0, Dir.WEST).receiveEnergy(500000, false) + " A=" + a.getStoredEnergy() + " B=" + b.getStoredEnergy() + "\n";
  setBlock(X, Y, Z + 1, Blocks.REDSTONE_BLOCK);
}
if (tick == 35) te(X - 2, Y + 1, Z).setRFLimit(100000);
if (tick == 100) {
  var a = te(X, Y, Z), b = te(X - 6, Y, Z), c1 = te(X - 2, Y + 1, Z);
  out += "T" + tick + " A=" + a.getStoredEnergy() + " B=" + b.getStoredEnergy() + " cableLimit=" + (c1 ? c1.getRFLimit() : "none") + " cables=" + (c1 ? String(f(c1, "network")).split("\n").join(" ").substring(0, 900) + " | lastcable=" + String(f(te(X - 5, Y, Z), "network")).split(String.fromCharCode(10)).join(" ").substring(0, 40) : "") + "\n";
}
if (tick == 300) {
  var cb = cap(-6, 0, Dir.WEST); out += "extract from B via cap(WEST): " + cb.extractEnergy(5000, false) + "; via cap(UP): " + cap(-6, 0, Dir.UP).extractEnergy(5000, false) + " B now " + te(X - 6, Y, Z).getStoredEnergy();
}
out;
