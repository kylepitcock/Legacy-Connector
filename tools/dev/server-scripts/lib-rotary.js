// Helpers for the gameplay scenarios of tools/dev/server-scripts (run with tools/dev/run-server-script.sh <scenario>).
// The scenario runs on every server tick with the bindings server, tick, log; globals persist, so a scenario builds in tick 0,
// schedules checks with L.at(tick, fn) / L.after(delay, fn), prints with L.say(...) and ends with L.run(tick). Placement goes through the
// real item use (a fake player), so the legacy mod's own onItemUse runs. Legacy tile entities are read with L.te(x, y, z) and their
// fields with L.f / L.fs / L.dump (Nashorn cannot link tiles whose interfaces mention classes that are not installed: use L.f for fields).
if (typeof L === 'undefined') {
  L = {};
  L.R = Java.type("net.minecraft.core.registries.BuiltInRegistries");
  L.RL = Java.type("net.minecraft.resources.ResourceLocation");
  L.BP = Java.type("net.minecraft.core.BlockPos");
  L.Blocks = Java.type("net.minecraft.world.level.block.Blocks");
  L.IS = Java.type("net.minecraft.world.item.ItemStack");
  L.Hand = Java.type("net.minecraft.world.InteractionHand");
  L.BHR = Java.type("net.minecraft.world.phys.BlockHitResult");
  L.Vec3 = Java.type("net.minecraft.world.phys.Vec3");
  L.Dir = Java.type("net.minecraft.core.Direction");
  L.UOC = Java.type("net.minecraft.world.item.context.UseOnContext");
  L.LIS = Java.type("legacyconnector.v1_7_10.net.minecraft.item.ItemStack");
  G = {};
  L.out = [];
  L.say = function (s) { L.out.push(s); };
  L.fp = function () { return Java.type("net.neoforged.neoforge.common.util.FakePlayerFactory").getMinecraft(server.overworld()); };
  L.f = function (t, n) { for (var k = t.getClass(); k != null; k = k.getSuperclass()) { try { var x = k.getDeclaredField(n); x.setAccessible(true); return x.get(t); } catch (e) {} } return null; };
  L.fs = function (t, n) { return "" + L.f(t, n); };
  L.lvl = function () { return server.overworld(); };
  // clear + floor a platform, load chunks
  L.platform = function (X, Y, Z, w, d, h) {
    var lv = L.lvl();
    for (var cx = (X - 2) >> 4; cx <= (X + w + 2) >> 4; cx++) for (var cz = (Z - 2) >> 4; cz <= (Z + d + 2) >> 4; cz++) lv.getChunk(cx, cz);
    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "forceload add " + X + " " + Z + " " + (X + w) + " " + (Z + d));
    for (var x = X - 1; x <= X + w; x++) for (var z = Z - 1; z <= Z + d; z++) {
      for (var dy = 0; dy < h; dy++) lv.setBlock(new L.BP(x, Y + dy, z), L.Blocks.AIR.defaultBlockState(), 2);
      lv.setBlock(new L.BP(x, Y - 1, z), L.Blocks.STONE.defaultBlockState(), 2);
    }
  };
  // place a legacy ItemStack with the fake player at x,Y,z on top of the floor
  L.place = function (legacyStack, x, Y, z, face, yaw) {
    var lv = L.lvl(), p = L.fp(), st = legacyStack.mcc$modern();
    p.teleportTo(lv, x - 1.5, Y, z + 0.5, yaw === undefined ? -90 : yaw, 30);
    p.setItemInHand(L.Hand.MAIN_HAND, st);
    var r = st.getItem().useOn(new L.UOC(lv, p, L.Hand.MAIN_HAND, st, new L.BHR(new L.Vec3(x + 0.5, Y, z + 0.5), face || L.Dir.UP, new L.BP(x, Y - 1, z), false)));
    return {r: "" + r, te: L.te(x, Y, z)};
  };
  L.te = function (x, y, z) { var be = L.lvl().getBlockEntity(new L.BP(x, y, z)); return be && be.legacy ? be.legacy() : null; };
  L.blk = function (x, y, z) { return "" + L.lvl().getBlockState(new L.BP(x, y, z)); };
  L.rs = function (x, y, z, on) { L.lvl().setBlock(new L.BP(x, y, z), (on ? L.Blocks.REDSTONE_BLOCK : L.Blocks.AIR).defaultBlockState(), 3); };
  L.mach = function (name) { return Java.type("Reika.RotaryCraft.Registry.MachineRegistry").valueOf(name); };
  L.stackOf = function (name) { var s = L.mach(name).getCraftedProduct(); return new L.LIS(s.getItem(), 1, s.getItemDamage()); };
  L.slots = function (t) { if (!t.getSizeInventory) return "(no inventory)"; var s = []; for (var i = 0; i < t.getSizeInventory(); i++) { var st = t.getStackInSlot(i); if (st != null) s.push(i + ":" + st); } return s.join(" "); };
  L.ass = function (name, cond, info) { L.say((cond ? "PASS " : "FAIL ") + name + (info !== undefined ? " [" + info + "]" : "")); };
  L.dump = function (t) { var o = []; for (var k = t.getClass(); k != null && k.getName().indexOf("legacyconnector") < 0; k = k.getSuperclass()) { Java.from(k.getDeclaredFields()).forEach(function (f) { if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) return; try { f.setAccessible(true); var v = f.get(t); var s = String(v); if (s.length > 60) s = s.substring(0, 60); o.push(f.getName() + "=" + s); } catch (e) {} }); } return o.join(" "); };
  // call a no-arg-or-simple method by name through plain reflection (Nashorn cannot link tiles whose interfaces mention classes that do not exist)
  L.call = function (t, name) { var args = Array.prototype.slice.call(arguments, 2); for (var k = t.getClass(); k != null; k = k.getSuperclass()) { var ms = null; try { ms = k.getDeclaredMethods(); } catch (e) { continue; } for (var i = 0; i < ms.length; i++) if (ms[i].getName() == name && ms[i].getParameterCount() == args.length) { ms[i].setAccessible(true); return ms[i].invoke(t, Java.to(args, "java.lang.Object[]")); } } throw new Error("no method " + name + " on " + t.getClass().getName()); };
  L.setf = function (t, n, v) { for (var k = t.getClass(); k != null; k = k.getSuperclass()) { try { var x = k.getDeclaredField(n); x.setAccessible(true); x.set(t, v); return true; } catch (e) {} } return false; };
  L.flush = function () { var s = L.out.join("\n"); L.out = []; return s; };
  // ---- task scheduler: L.at(absTick, fn); L.after(delay, fn) relative to L.now
  L.now = 0; L.tasks = [];
  L.at = function (t, fn) { L.tasks.push({t: t, fn: fn}); };
  L.after = function (d, fn) { L.at(L.now + d, fn); };
  L.run = function (tk) {
    L.now = tk;
    var due = L.tasks.filter(function (x) { return x.t <= tk; });
    L.tasks = L.tasks.filter(function (x) { return x.t > tk; });
    due.forEach(function (x) { try { x.fn(); } catch (e) { var st = ""; if (e.getStackTrace) { var a = e.getStackTrace(); for (var k = 0; k < Math.min(a.length, 14); k++) st += "\n      at " + a[k]; } else if (e.stack) st = String(e.stack).split("\n").slice(0, 4).join(" | ");
      L.say("TASK ERROR: " + e + st); } });
    return L.flush();
  };
  L.fd = function (name) { return Java.type("legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection").valueOf(name); };
  L.FD = Java.type("legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection");
  L.FR = Java.type("legacyconnector.v1_7_10.net.minecraftforge.fluids.FluidRegistry");
  L.FS = Java.type("legacyconnector.v1_7_10.net.minecraftforge.fluids.FluidStack");
  try { L.ET = Java.type("Reika.RotaryCraft.Registry.EngineType"); } catch (e) {}
  L.fluid = function (name, amt) { return new L.FS(L.FR.getFluid(name), amt); };
  // fill a fluid handler from whichever side accepts it
  L.feed = function (t, fluidName, amt) {
    var fl = L.FR.getFluid(fluidName);
    if (fl == null) return "nofluid:" + fluidName;
    var dirs = L.FD.VALID_DIRECTIONS;
    for (var i = 0; i < dirs.length; i++) if (t.canFill(dirs[i], fl)) return t.fill(dirs[i], new L.FS(fl, amt), true) + "@" + dirs[i];
    return "nodir";
  };
  L.air = function (x, y, z) { L.lvl().setBlock(new L.BP(x, y, z), L.Blocks.AIR.defaultBlockState(), 3); };
  L.engStack = function (type) { var s = L.stackOf("ENGINE"); return new L.LIS(s.getItem(), 1, type); };
  L.stackMeta = function (name, meta) { var s = L.stackOf(name); return new L.LIS(s.getItem(), 1, meta); };
  /**
   * Build engine -> shaft -> machine in a line. o = {X, Z, Y, name (MachineRegistry), eng (EngineType ordinal, default 3 GAS), fuel: [[fluid, amt]...],
   * mid: [MachineRegistry names between engine and machine, default ['SHAFT']], done: function(ctx)}  ctx: {eng, shafts, mach, w, ...}
   */
  L.rig = function (o) {
    var Y = o.Y, ctx = {o: o, shafts: []};
    var engType = o.eng === undefined ? 3 : o.eng;
    L.place(L.engStack(engType), o.X, Y, o.Z);
    L.after(3, function () {
      ctx.eng = L.te(o.X, Y, o.Z);
      var w = L.f(ctx.eng, "write");
      if (w == null) { L.say("RIG " + o.name + ": engine write dir null"); return; }
      ctx.w = w; ctx.dx = w.offsetX; ctx.dz = w.offsetZ;
      var mid = o.mid || ["SHAFT"];
      var yawFor = function (dx, dz) { return dx > 0 ? -90 : dx < 0 ? 90 : dz > 0 ? 0 : 180; };
      // note: placement yaw must make the shaft run along the line
      for (var i = 0; i < mid.length; i++) {
        var px = o.X + ctx.dx * (i + 1), pz = o.Z + ctx.dz * (i + 1);
        var r = L.place(L.stackOf(mid[i]), px, Y, pz, null, yawFor(ctx.dx, ctx.dz) + 180);
        ctx.shafts.push(L.te(px, Y, pz));
      }
      ctx.mx = o.X + ctx.dx * (mid.length + 1); ctx.mz = o.Z + ctx.dz * (mid.length + 1);
      ctx.tries = [0, 90, 180, -90]; ctx.ti = 0;
      var tryPlace = function () {
        if (ctx.ti >= ctx.tries.length) { L.say("RIG " + o.name + ": no yaw feeds the machine; read=" + (ctx.mach ? L.fs(ctx.mach, "read") : "?")); if (o.done) o.done(ctx); return; }
        L.air(ctx.mx, Y, ctx.mz);
        var r = L.place(o.stack || L.stackOf(o.name), ctx.mx, Y, ctx.mz, null, ctx.tries[ctx.ti]);
        L.after(2, function () {
          ctx.mach = L.te(ctx.mx, Y, ctx.mz);
          if (!ctx.mach) { L.say("RIG " + o.name + ": no tile (place=" + r.r + ")"); return; }
          var rd = L.f(ctx.mach, "read");
          var ok = rd != null && rd.offsetX == -ctx.dx && rd.offsetZ == -ctx.dz;
          if (!ok) { ["read2", "read3", "read4"].forEach(function (n) { var q = L.f(ctx.mach, n); if (q != null && q.offsetX == -ctx.dx && q.offsetZ == -ctx.dz) ok = true; }); }
          if (ok || o.anyYaw) {
            ctx.yaw = ctx.tries[ctx.ti];
            if (o.fuel) o.fuel.forEach(function (f) { ctx.fuelLog = (ctx.fuelLog || "") + L.feed(ctx.eng, f[0], f[1]) + " "; });
            L.rs(o.X, Y + 1, o.Z, true);
            if (o.done) o.done(ctx);
          } else { ctx.ti++; tryPlace(); }
        });
      };
      tryPlace();
    });
    return ctx;
  };
}
