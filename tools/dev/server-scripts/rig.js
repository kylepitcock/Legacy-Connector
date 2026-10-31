// place a machine at x,z whose input faces the source at (x-dx,z-dz); tries 4 yaws (stackFn() gives a fresh legacy stack per try), calls cb(te, yaw)
// yaw is null when no yaw reads from the source (the machine of the last try stays in place)
L.alignedPlace = function (stackFn, x, Y, z, dx, dz, cb, anyYaw) {
  var yaws = [0, 90, 180, -90], i = 0;
  var attempt = function () {
    L.air(x, Y, z);
    var r = L.place(stackFn(), x, Y, z, null, yaws[i]);
    L.after(2, function () {
      var t = L.te(x, Y, z); if (L.verbose) L.say("  attempt yaw=" + yaws[i] + " place=" + r.r + " te=" + t + " read=" + (t ? L.fs(t, "read") : ""));
      if (!t) { L.say("alignedPlace: no tile at " + x + "," + z + " place=" + r.r); cb(null, null); return; }
      var ok = !!anyYaw;
      ["read", "read2", "read3", "read4"].forEach(function (n) { var q = L.f(t, n); if (q != null && q.offsetX == -dx && q.offsetZ == -dz) ok = true; });
      if (ok) cb(t, yaws[i]); else if (i + 1 >= yaws.length) cb(t, null); else { i++; attempt(); }
    });
  };
  attempt();
};
/**
 * Build engine -> mid... -> machine in a line along the engine's write direction. o = {X, Z, Y, name (MachineRegistry), eng (EngineType ordinal, default 3 GAS),
 * fuel: [[fluid, amt]...], mid: [MachineRegistry names, default ['SHAFT']], done: function(ctx)}  ctx: {eng, shafts, mach, dx, dz}
 */
L.rig = function (o) {
  var Y = o.Y, ctx = {o: o, shafts: []};
  var engType = o.eng === undefined ? 3 : o.eng;
  L.place(L.engStack(engType), o.X, Y, o.Z);
  var waited = 0;
  var begin = function () {
    ctx.eng = L.te(o.X, Y, o.Z);
    var w = ctx.eng ? L.f(ctx.eng, "write") : null;
    if (w == null) { if (++waited < 60) { L.after(2, begin); } else L.say("RIG " + o.name + ": engine write dir null (engine=" + ctx.eng + ")"); return; }
    ctx.dx = w.offsetX; ctx.dz = w.offsetZ;
    var mid = o.mid || ["SHAFT"];
    var step = function (i) {
      if (i < mid.length) {
        var px = o.X + ctx.dx * (i + 1), pz = o.Z + ctx.dz * (i + 1);
        L.alignedPlace(function () { return L.stackOf(mid[i]); }, px, Y, pz, ctx.dx, ctx.dz, function (t, yaw) { if (!t) L.say("RIG " + o.name + ": mid " + mid[i] + " missing"); ctx.shafts.push(t); step(i + 1); }, true);
        return;
      }
      ctx.mx = o.X + ctx.dx * (mid.length + 1); ctx.mz = o.Z + ctx.dz * (mid.length + 1);
      if (o.above) {
        // bevel gear at the end of the line, the machine on top of it (machines that read their power from below)
        // the bevel gear finds its route on its first tick only: the machine goes up first (on a stand-in block), then the gear replaces the stand-in
        L.lvl().setBlock(new L.BP(ctx.mx, Y, ctx.mz), L.Blocks.STONE.defaultBlockState(), 3);
        var r = L.place(o.stackFn ? o.stackFn() : L.stackOf(o.name), ctx.mx, Y + 1, ctx.mz, null, 0);
        L.after(2, function () {
          ctx.mach = L.te(ctx.mx, Y + 1, ctx.mz); ctx.my = Y + 1; ctx.yaw = 0;
          if (!ctx.mach) { L.say("RIG " + o.name + ": machine above bevel missing (place=" + r.r + ")"); if (o.done) o.done(ctx); return; }
          L.alignedPlace(function () { return L.stackOf("BEVELGEARS"); }, ctx.mx, Y, ctx.mz, ctx.dx, ctx.dz, function (bt, byaw) {
            ctx.bevel = bt;
            if (o.fuel) o.fuel.forEach(function (f) { ctx.fuelLog = (ctx.fuelLog || "") + L.feed(ctx.eng, f[0], f[1]) + " "; });
            L.rs(o.X, Y + 1, o.Z, true);
            if (o.done) o.done(ctx);
          }, true);
        });
        return;
      }
      L.alignedPlace(o.stackFn || function () { return L.stackOf(o.name); }, ctx.mx, Y, ctx.mz, ctx.dx, ctx.dz, function (t, yaw) {
        ctx.mach = t; ctx.yaw = yaw;
        if (!t || yaw === null) L.say("RIG " + o.name + ": " + (t ? "no yaw reads from the shaft side; read=" + L.fs(t, "read") + " (kept)" : "machine missing"));
        if (o.fuel) o.fuel.forEach(function (f) { ctx.fuelLog = (ctx.fuelLog || "") + L.feed(ctx.eng, f[0], f[1]) + " "; });
        L.rs(o.X, Y + 1, o.Z, true);
        if (o.done) o.done(ctx);
      }, o.anyYaw);
    };
    step(0);
  };
  L.after(3, begin);
  return ctx;
};
L.snap = function (t) { return "power=" + L.fs(t, "power") + " torque=" + L.fs(t, "torque") + " omega=" + L.fs(t, "omega"); };
