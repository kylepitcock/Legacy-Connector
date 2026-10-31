var Y0 = 150, X0 = 400, Z0 = 400;
var FD = Java.type("legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection");
var FR = Java.type("legacyconnector.v1_7_10.net.minecraftforge.fluids.FluidRegistry");
var FS = Java.type("legacyconnector.v1_7_10.net.minecraftforge.fluids.FluidStack");
var ET = Java.type("Reika.RotaryCraft.Registry.EngineType");
function feed(t, fluidName, amt) {
  var fl = FR.getFluid(fluidName);
  if (fl == null) { L.say("  no fluid " + fluidName); return -1; }
  var dirs = FD.VALID_DIRECTIONS;
  for (var i = 0; i < dirs.length; i++) {
    if (t.canFill(dirs[i], fl)) { var n = t.fill(dirs[i], new FS(fl, amt), true); return n + "@" + dirs[i]; }
  }
  return "nodir";
}
if (tick == 0) {
  L.platform(X0, Y0, Z0, 40, 6, 6);
  var types = ET.values();
  G.eng = [];
  for (var i = 0; i < types.length; i++) {
    var x = X0 + i * 4, z = Z0 + 2;
    var s = L.stackOf("ENGINE"); s = new L.LIS(s.getItem(), 1, i);
    var r = L.place(s, x, Y0, z);
    var t = r.te;
    L.say(types[i] + " place=" + r.r + " te=" + (t ? t.getClass().getSimpleName() : null) + " write=" + (t ? L.fs(t, "write") : "") +
      " fluids=" + types[i].usesFluids() + " fuel=" + types[i].getFuelType() + " lube=" + types[i].requiresLubricant() + " water=" + types[i].needsWater());
    G.eng.push({x: x, z: z, t: t, type: types[i]});
  }
}
if (tick == 40) {
  for (var i = 0; i < G.eng.length; i++) {
    var e = G.eng[i], t = e.t; if (!t) continue;
    var w = L.f(t, "write");
    // shaft on the write side
    var sx = e.x + w.offsetX, sz = e.z + w.offsetZ;
    var sr = L.place(L.stackOf("SHAFT"), sx, Y0, sz, null, 0);
    e.shaft = sr.te; e.sx = sx; e.sz = sz;
    var ty = e.type, ft = ty.getFuelType();
    var info = [];
    if (ty.requiresLubricant()) info.push("lube:" + feed(t, "lubricant", 4000));
    if (ft != null) info.push("fuel:" + feed(t, "" + ft.getName(), 4000));
    if (ty.needsWater()) info.push("water:" + feed(t, "water", 4000));
    L.say(ty + " shaft=" + (e.shaft ? "ok" : "none") + " " + info.join(" "));
    L.rs(e.x, Y0 + 1, e.z, true);
  }
}
if (tick == 45 || tick == 100 || tick == 250 || tick == 400) {
  L.say("--- tick " + tick);
  for (var i = 0; i < G.eng.length; i++) {
    var e = G.eng[i], t = e.t; if (!t) continue;
    var sh = e.shaft;
    L.say(e.type + ": power=" + L.fs(t, "power") + " torque=" + L.fs(t, "torque") + " omega=" + L.fs(t, "omega") + " temp=" + L.fs(t, "temperature") + " on=" + t.isOn() + " fuel=" + t.getFuelLevel() +
      (sh ? " | shaft omega=" + L.fs(sh, "omega") + " torque=" + L.fs(sh, "torque") : ""));
  }
}
L.flush();
