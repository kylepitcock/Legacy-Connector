// run: ./gradlew runServerSubTest -PautostopTicks=600 -PserverTickScriptFrom=100 -PserverTickScript=<abs path of this file>
// needs the Reika jars (DragonAPI, RotaryCraft, ElectriCraft, ReactorCraft) + CoFHCore in run-subtest/mods/1.7.10; see docs/wiki/Development.md
var __p = String(java.lang.System.getProperty("legacyconnector.serverScript")).split("\\").join("/");
load(__p.substring(0, __p.lastIndexOf("/") + 1) + "lib-reika.js");
var YL = 105, Y = 104, X = 300, Z = 300;
var RT = Java.type("Reika.ReactorCraft.Registry.ReactorTiles");
var MR = Java.type("Reika.RotaryCraft.Registry.MachineRegistry");
var FS = Java.type("legacyconnector.v1_7_10.net.minecraftforge.fluids.FluidStack");
var FR = Java.type("legacyconnector.v1_7_10.net.minecraftforge.fluids.FluidRegistry");
var out = "";
function rt(n) { return ms(RT.valueOf(n).getCraftedProduct()); }
function mr(n) { var s = MR.valueOf(n).getCraftedProduct(); return new LIS(s.getItem(), 1, s.getItemDamage()).mcc$modern(); }
if (tick == 0) { flat(X - 4, 100, Z - 4, 24, 9, 12); cmd("forceload add " + (X - 40) + " " + (Z - 40) + " " + (X + 60) + " " + (Z + 40)); }
if (tick == 1) setBlock(X, YL, Z, Blocks.STONE);
YAW = -90;

// [kind:name, dx, y, click-target(dx,y), face]
var parts = [["M:SHAFT", 1, YL, 0, YL, Dir.EAST], ["M:SHAFT", 2, YL, 1, YL, Dir.EAST], ["R:TURBINECORE", 3, YL, 2, YL, Dir.EAST], ["R:TURBINECORE", 4, YL, 3, YL, Dir.EAST], ["R:TURBINECORE", 5, YL, 4, YL, Dir.EAST]];
parts.forEach(function (p, i) {
  if (tick == 3 + 2 * i) {
    var nm = p[0].substring(2);
    var st = p[0].charAt(0) == "R" ? rt(nm) : mr(nm);
    out += p[0] + ":" + placeAt(st, X + p[3], p[4], Z, p[5]) + " ";
  }
});
var T0 = 30;
function T(dx, yy) { return te(X + dx, yy === undefined ? YL : yy, Z); }
function TB() { return te(X, Y, Z); }
if (tick == T0) { out += " turbines=" + [3, 4, 5].map(function (d) { var t = T(d); return t ? CLS(t).getSimpleName() : "none"; }).join(","); }
if (tick == T0 + 1) { [3, 4, 5].forEach(function (d) { var t = T(d); out += " lube" + d + "=" + f(t, "tank").fill(new FS(FR.getFluid("rc lubricant"), 4000), true); }); }
if (tick > T0 + 1 && tick < T0 + 900) { var RBk = Java.type("Reika.ReactorCraft.Registry.ReactorBlocks"); var LWo = Java.type("legacyconnector.v1_7_10.net.minecraft.world.World").mcc$wrap(lvl());
  if (!G("steamBlk")) G("steamBlk", RBk.STEAM.getBlockInstance());
  LWo.setBlock(X + 5, YL - 1, Z, G("steamBlk"), 2, 3); }
function rdTurb(dx) { var t = T(dx); if (!t) return "[" + dx + " none]"; return "[" + dx + " steam=" + f(t, "steam") + " omega=" + f(t, "omega") + " torque=" + f(t, "torque") + " multi=" + f(t, "hasMultiBlock") + " stage=" + f(t, "stage") + " dmg=" + f(t, "damage") + "]"; }
if (tick > T0 && (tick - T0) % 40 == 5) {
  
  out += "T+" + (tick - T0);
  out += " " + rdTurb(3) + rdTurb(4) + rdTurb(5);
  var tt = T(5); out += " T5{" + fs(tt, ["omega", "forcedlube", "damage", "enabled", "iotick", "readx", "writex", "writey", "writez", "readyForMultiBlock"]) + " lube=" + f(tt, "tank").getLevel() + " " + f(tt, "tank").getActualFluid() + "}";
  var s1 = T(1), s2 = T(2);
  out += " shaft6=" + (s1 ? fs(s1, ["omega", "torque", "power"]) : "none") + " shaft7=" + (s2 ? fs(s2, ["omega", "torque"]) : "none");
}
out;
