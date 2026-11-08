// Registry census of the running game: one JSON object {namespace: {blocks:[..], items:[..], entities:[..], biomes:N, dims:N}} (non-vanilla namespaces only).
var BR = Java.type("net.minecraft.core.registries.BuiltInRegistries");
var out = {};
function slot(ns) { if (!out[ns]) out[ns] = {blocks: [], items: [], entities: [], biomes: 0, dims: 0}; return out[ns]; }
function scan(reg, field) {
  var it = reg.keySet().iterator();
  while (it.hasNext()) {
    var k = it.next(), ns = String(k.getNamespace());
    if (ns === "minecraft" || ns === "neoforge" || ns === "c") continue;
    slot(ns)[field].push(String(k.getPath()));
  }
}
scan(BR.BLOCK, "blocks");
scan(BR.ITEM, "items");
scan(BR.ENTITY_TYPE, "entities");
try {
  var ra = server.registryAccess();
  var RK = Java.type("net.minecraft.core.registries.Registries");
  var bi = ra.registryOrThrow(RK.BIOME).keySet().iterator();
  while (bi.hasNext()) { var k = bi.next(); if (String(k.getNamespace()) !== "minecraft") slot(String(k.getNamespace())).biomes++; }
  var di = ra.registryOrThrow(RK.DIMENSION_TYPE).keySet().iterator();
  while (di.hasNext()) { var k2 = di.next(); if (String(k2.getNamespace()) !== "minecraft") slot(String(k2.getNamespace())).dims++; }
} catch (e) { }
JSON.stringify(out);
