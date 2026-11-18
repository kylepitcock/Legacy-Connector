// Village template pools of every namespace (data packs and code), for -PserverDevScript (copy this file into an empty directory first:
// the script is renamed when it has run). Logs "VILLAGE POOLS: <namespace>: N pools, M elements, K without a structure template (e.g. ...)"
// and the number of elements of other namespaces inside vanilla's own village pools. An element without a template has size 0 x 0 x 0
// (the structure file is missing: converted data that lost it, or a wrong folder).
var Registries = Java.type("net.minecraft.core.registries.Registries");
var RandomSource = Java.type("net.minecraft.util.RandomSource");
var Rotation = Java.type("net.minecraft.world.level.block.Rotation");
var pools = server.registryAccess().registryOrThrow(Registries.TEMPLATE_POOL);
var manager = server.getStructureManager();
var per = {}, broken = {}, distinct = {}, foreign = 0, total = 0;
var it = pools.entrySet().iterator();
while (it.hasNext()) {
  var e = it.next();
  var loc = e.getKey().location();
  var ns = "" + loc.getNamespace(), path = "" + loc.getPath();
  if (path.indexOf("village") < 0) continue;
  if (!per[ns]) per[ns] = [0, 0, 0];
  per[ns][0]++;
  var list = e.getValue().getShuffledTemplates(RandomSource.create(1));
  for (var i = 0; i < list.size(); i++) {
    var el = list.get(i);
    var text = "" + el.toString();
    per[ns][1]++;
    total++;
    var m = /([a-z0-9_.-]+:[a-z0-9_.\/-]+)/.exec(text);
    var template = m ? m[1] : null;
    if (ns == "minecraft" && template && template.indexOf("minecraft:") != 0) foreign++;
    if (!template || text.indexOf("Single") != 0) continue;
    var size = null;
    try { size = el.getSize(manager, Rotation.NONE); } catch (x) { size = null; }
    if (size == null || (size.getX() == 0 && size.getY() == 0 && size.getZ() == 0)) {
      per[ns][2]++;
      if (!broken[ns]) broken[ns] = [];
      broken[ns].push(path + " -> " + template);
      if (!distinct[ns]) distinct[ns] = {};
      distinct[ns][template] = true;
    }
  }
}
for (var ns in per) {
  log.info("VILLAGE POOLS: " + ns + ": " + per[ns][0] + " pools, " + per[ns][1] + " elements, " + per[ns][2] + " without a structure template (" + (distinct[ns] ? Object.keys(distinct[ns]).length : 0) + " distinct templates)" + (broken[ns] ? " (e.g. " + broken[ns].slice(0, 3).join("; ") + ")" : ""));
}
log.info("VILLAGE POOLS: vanilla village pools hold " + foreign + " elements of other namespaces; " + total + " village elements in all");
"done";
