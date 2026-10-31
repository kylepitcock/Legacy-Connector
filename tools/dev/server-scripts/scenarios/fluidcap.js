var Y0 = 150, X0 = 1800, Z0 = 1800;
var Caps = Java.type("net.neoforged.neoforge.capabilities.Capabilities");
var NFS = Java.type("net.neoforged.neoforge.fluids.FluidStack");
var FA = Java.type("net.neoforged.neoforge.fluids.capability.IFluidHandler$FluidAction");
var MF = Java.type("net.minecraft.world.level.material.Fluids");
var Dirs = Java.from(Java.type("net.minecraft.core.Direction").values());
if (tick == 0) {
  L.platform(X0, Y0, Z0, 50, 12, 8);
  var fl = [];
  Java.from(L.R.FLUID.keySet().toArray()).forEach(function (k) { if (("" + k).indexOf("minecraft:") != 0) fl.push("" + k); });
  L.say("modern fluids (non-vanilla): " + fl.join(", "));
  G.names = ["ENGINE:3", "ENGINE:5", "RESERVOIR", "PUMP", "FERMENTER", "FRACTIONATOR", "BOILER", "FILLINGSTATION", "BUCKETFILLER", "GRINDER"];
  G.names.forEach(function (n, i) {
    var p = n.split(":"), st = p[0] == "ENGINE" ? L.engStack(parseInt(p[1])) : L.stackOf(p[0]);
    var r = L.place(st, X0 + 2 + i * 4, Y0, Z0 + 2, null, 0); if (r.r != "CONSUME") L.say("place " + n + " " + r.r);
  });
}
if (tick == 30) {
  var eth = null, lub = null;
  Java.from(L.R.FLUID.keySet().toArray()).forEach(function (k) { var s = "" + k; if (s.indexOf("rc_ethanol") >= 0 && !eth) eth = L.R.FLUID.get(k); if (s.indexOf("lubricant") >= 0 && !lub) lub = L.R.FLUID.get(k); });
  L.say("ethanol fluid=" + eth + " lubricant=" + lub);
  G.names.forEach(function (n, i) {
    var x = X0 + 2 + i * 4, z = Z0 + 2, o = [];
    Dirs.forEach(function (d) {
      var fh = L.lvl().getCapability(Caps.FluidHandler.BLOCK, new L.BP(x, Y0, z), d);
      if (!fh) { o.push(d + ":none"); return; }
      var tr = fh.getTanks();
      var wIn = fh.fill(new NFS(MF.WATER, 500), FA.SIMULATE);
      var eIn = eth ? fh.fill(new NFS(eth, 500), FA.SIMULATE) : -1;
      var lIn = lub ? fh.fill(new NFS(lub, 500), FA.SIMULATE) : -1;
      o.push(d + ":tanks" + tr + " water" + wIn + " eth" + eIn + " lube" + lIn);
    });
    L.say("FLUIDCAP " + n + " " + o.join(" | "));
  });
  // real transfer: fill the reservoir with water through the capability, then drain half through another side
  var rx = X0 + 2 + 2 * 4, rz = Z0 + 2;
  var fh = L.lvl().getCapability(Caps.FluidHandler.BLOCK, new L.BP(rx, Y0, rz), Dirs[2]);
  var put = fh.fill(new NFS(MF.WATER, 4000), FA.EXECUTE);
  var rt = L.te(rx, Y0, rz);
  var got = fh.drain(1500, FA.EXECUTE);
  L.say("reservoir: filled " + put + " via cap, tank now " + L.fs(rt, "tank") + "; drained " + got.getAmount() + " of " + got.getFluid());
  var eng = L.te(X0 + 2, Y0, Z0 + 2), efh = L.lvl().getCapability(Caps.FluidHandler.BLOCK, new L.BP(X0 + 2, Y0, Z0 + 2), Dirs[2]);
  if (eth) L.say("gas engine: eth filled via cap=" + efh.fill(new NFS(eth, 3000), FA.EXECUTE) + " fuel field=" + L.fs(eng, "fuel"));
}
L.run(tick);
