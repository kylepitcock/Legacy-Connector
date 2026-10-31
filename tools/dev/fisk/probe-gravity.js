// Gravity: a mob without AI falls in the overworld and on the Moon (legacyconnector:dim_2595, Fisk's gravity factor 0.2) and in the Quantum Realm (dim_2594, 0.25).
var ResourceKey = Java.type("net.minecraft.resources.ResourceKey");
var Registries = Java.type("net.minecraft.core.registries.Registries");
function dimLevel(id) { return server.getLevel(ResourceKey.create(Registries.DIMENSION, RL.parse(id))); }
function fallTest(level, label) {
  level.getChunk(0, 0);
  var t = BuiltInRegistries.ENTITY_TYPE.get(RL.parse("minecraft:zombie"));
  var z = t.create(level); z.moveTo(0.5, 200, 0.5, 0, 0); z.setNoAi(true); level.addFreshEntity(z);
  return { label: label, z: z };
}
var GT = [fallTest(server.overworld(), "overworld"), fallTest(dimLevel("legacyconnector:dim_2595"), "moon"), fallTest(dimLevel("legacyconnector:dim_2594"), "quantum")];
var gstart = tick;
scenario([function () { return 8; }, function () {
  GT.forEach(function (g) { out("gravity " + g.label + ": dy after 8 ticks " + g.z.getDeltaMovement().y.toFixed(4) + " y=" + g.z.getY().toFixed(2) + " gravity()=" + g.z.getGravity().toFixed(4)); g.z.discard(); });
}]);
"gravity probe started"
