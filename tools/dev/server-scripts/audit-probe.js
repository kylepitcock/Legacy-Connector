// probe: top class names in modlauncher's TransformerAuditTrail
if (typeof G === "undefined") G = {};
function findFieldDeep(start, typeName) {
  for (var c = start.getClass(); c != null; c = c.getSuperclass()) {
    var fs = c.getDeclaredFields();
    for (var i = 0; i < fs.length; i++) if (fs[i].getType().getName() == typeName) return fs[i];
  }
  return null;
}
function unsafe() {
  var UC = Java.type("sun.misc.Unsafe");
  var f = UC.class.getDeclaredField("theUnsafe"); f.setAccessible(true); return f.get(null);
}
function getF(obj, f) {
  var u = unsafe();
  var off = u.objectFieldOffset(f);
  return u.getObject(obj, off);
}
if (tick > 0 && tick % 1800 == 0) {
  G.audited = true;
  try {
    var cl = Java.type("net.minecraft.world.level.Level").class.getClassLoader();
    var out = "loader=" + cl.getClass().getName();
    var ct = null;
    for (var l = cl; l != null && ct == null; l = l.getParent()) {
      var ff = findFieldDeep(l, "cpw.mods.modlauncher.ClassTransformer");
      if (ff != null) { ct = getF(l, ff); out += " found in " + l.getClass().getName(); }
    }
    if (ct == null) {
      // TransformingClassLoader may hold it through a lambda: try Launcher
      var L = Java.type("cpw.mods.modlauncher.Launcher");
      var inst = getF(null, L.class.getDeclaredField("INSTANCE"));
      out += " launcher=" + inst;
    } else {
      var af = findFieldDeep(ct, "cpw.mods.modlauncher.TransformerAuditTrail");
      var at = getF(ct, af);
      var mf = findFieldDeep(at, "java.util.Map");
      var map = getF(at, mf);
      var counts = [];
      var it = map.entrySet().iterator();
      var total = 0;
      while (it.hasNext()) { var e = it.next(); var n = e.getValue().size(); total += n; counts.push([e.getKey(), n]); }
      counts.sort(function (a, b) { return b[1] - a[1]; });
      out += " classes=" + counts.length + " activities=" + total + " top: " + counts.slice(0, 15).map(function (c) { return c[0] + "=" + c[1]; }).join(", ");
    }
    log.info("AUDITPROBE: " + out);
  } catch (e) { log.info("AUDITPROBE failed: " + e); }
}

