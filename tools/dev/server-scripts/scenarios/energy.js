var Y0 = 150, X0 = 1200, Z0 = 1200;
var Caps = Java.type("net.neoforged.neoforge.capabilities.Capabilities");
var Dirs = Java.from(Java.type("net.minecraft.core.Direction").values());
function energyCaps(x, y, z) {
  var o = [];
  Dirs.forEach(function (d) { var e = L.lvl().getCapability(Caps.EnergyStorage.BLOCK, new L.BP(x, y, z), d); if (e) o.push(d + ":" + e.getEnergyStored() + "/" + e.getMaxEnergyStored() + (e.canReceive() ? "R" : "") + (e.canExtract() ? "E" : "")); });
  return o.join(" ");
}
if (tick == 0) {
  L.platform(X0 - 6, Y0, Z0, 40, 40, 8);
  // dynamo lane: engine -> shaft -> dynamo
  G.dyn = L.rig({name: "DYNAMO", X: X0, Z: Z0 + 20, Y: Y0, above: true, fuel: [["rc ethanol", 8000]], done: function (c) { L.say("dynamo rig built, mach=" + c.mach + " yaw=" + c.yaw); }});
  // magnetic: RF -> shaft power; magnetic at center with shafts around
  var m = L.place(L.stackOf("MAGNETIC"), X0 + 10, Y0, Z0 + 5, null, 0);
  G.mag = m.te;
  L.say("magnetic place=" + m.r + " te=" + m.te);
  G.magShafts = [];
  [[1, 0], [-1, 0], [0, 1], [0, -1]].forEach(function (d) {
    var s = L.place(L.stackOf("SHAFT"), X0 + 10 + d[0], Y0, Z0 + 5 + d[1], null, 0);
    G.magShafts.push([d, s.te]);
  });
}
if (tick > 10 && tick <= 260 && G.mag) {
  var mx = X0 + 10, mz = Z0 + 5;
  var mtile = L.te(mx, Y0, mz);
  if (tick == 12) L.say("magnetic caps: " + energyCaps(mx, Y0, mz) + " | facing=" + L.fs(mtile, "facingDir") + " write=" + L.fs(mtile, "write"));
  var pushed = 0;
  Dirs.forEach(function (d) { var e = L.lvl().getCapability(Caps.EnergyStorage.BLOCK, new L.BP(mx, Y0, mz), d); if (e && e.canReceive() && pushed == 0) pushed = e.receiveEnergy(20000, false); });
  if (tick == 100) L.say("MAGDUMP " + L.dump(mtile));
  if (tick == 12 || tick % 40 == 0) {
    L.say("t" + tick + " magnetic pushed=" + pushed + " stored=" + L.fs(mtile, "storedEnergy") + " " + L.snap(mtile) + " shafts: " + G.magShafts.map(function (e) { var t = L.te(e[1].xCoord, e[1].yCoord, e[1].zCoord); return e[0] + "=" + L.fs(t, "omega") + "/" + L.fs(t, "torque"); }).join(" "));
  }
}
if (G.dyn && G.dyn.mach && tick >= 60 && tick % 40 == 20) {
  var c = G.dyn, t = L.te(c.mx, c.my, c.mz);
  var ex = 0; Dirs.forEach(function (d) { var e = L.lvl().getCapability(Caps.EnergyStorage.BLOCK, new L.BP(c.mx, c.my, c.mz), d); if (e && e.canExtract() && ex == 0) ex = e.extractEnergy(100000, false); });
  L.say("t" + tick + " dynamo " + L.snap(t) + " genRF=" + t.getGenRF() + " caps: " + energyCaps(c.mx, c.my, c.mz) + " extracted=" + ex);
}
L.run(tick);
