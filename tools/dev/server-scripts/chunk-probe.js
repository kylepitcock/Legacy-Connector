// Dev probe (prepend after lib.js, e.g. LIBS="lib chunk-probe"): every 3000 ticks logs how the chunk holders of the overworld are distributed
// (ticket level, latest status), the distance manager's tickets and the sizes of the chunk map's containers (leaks of unloaded chunks, #161).
if (typeof G === "undefined") G = {};
function mccUnsafe() { var UC = Java.type("sun.misc.Unsafe"); var f = UC.class.getDeclaredField("theUnsafe"); f.setAccessible(true); return f.get(null); }
function mccField(obj, name) {
  var u = mccUnsafe();
  for (var c = obj.getClass(); c != null; c = c.getSuperclass()) {
    var fs = c.getDeclaredFields();
    for (var i = 0; i < fs.length; i++) if (fs[i].getName() == name) return u.getObject(obj, u.objectFieldOffset(fs[i]));
  }
  return null;
}
function mccSizes(obj, label) {
  var u = mccUnsafe(); var out = label + ":";
  for (var c = obj.getClass(); c != null; c = c.getSuperclass()) {
    var fs = c.getDeclaredFields();
    for (var i = 0; i < fs.length; i++) {
      var f = fs[i];
      if (java.lang.reflect.Modifier.isStatic(f.getModifiers()) || f.getType().isPrimitive()) continue;
      try {
        var v = u.getObject(obj, u.objectFieldOffset(f));
        if (v == null) continue;
        var n = (v instanceof Java.type("java.util.Map") || v instanceof Java.type("java.util.Collection")) ? v.size() : -1;
        if (n > 0) out += " " + f.getName() + "=" + n;
      } catch (e) {}
    }
  }
  return out;
}
if (tick > 0 && tick % 3000 == 0) {
  try {
    var lvl = server.overworld(), cs = lvl.getChunkSource(), cm = cs.chunkMap;
    var byLevel = {}, byStatus = {};
    var it = mccField(cm, "updatingChunkMap").values().iterator();
    while (it.hasNext()) {
      var h = it.next();
      var l = "" + h.getTicketLevel(); byLevel[l] = (byLevel[l] || 0) + 1;
      var st = "" + h.getLatestStatus(); byStatus[st] = (byStatus[st] || 0) + 1;
    }
    log.info("CHUNKPROBE t" + tick + " levels " + JSON.stringify(byLevel) + " status " + JSON.stringify(byStatus));
    log.info("CHUNKPROBE " + mccSizes(cm, "ChunkMap") + " | " + mccSizes(mccField(cs, "distanceManager"), "DistanceManager") + " | " + mccSizes(cs, "ServerChunkCache"));
  } catch (e) { log.info("CHUNKPROBE failed: " + e); }
}
