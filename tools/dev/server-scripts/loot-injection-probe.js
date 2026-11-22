// A mod's LootTableLoadEvent pool (ChestGenHooks / loot injection) must show in the modern table of the chest it targets.
// usage: set TABLES = "minecraft:chests/simple_dungeon,..." before this script (defaults below), mods in run-subtest/mods/1.12.2
var res = "";
if (typeof DONE === "undefined") {
  DONE = 1;
  var RL = T("net.minecraft.resources.ResourceLocation");
  var RK = T("net.minecraft.resources.ResourceKey");
  var Regs = T("net.minecraft.core.registries.Registries");
  var JO = T("com.mojang.serialization.JsonOps");
  var RO = T("net.minecraft.resources.RegistryOps");
  var LT = T("net.minecraft.world.level.storage.loot.LootTable");
  var ops = RO.create(JO.INSTANCE, server.registryAccess());
  var names = (typeof TABLES === "undefined" ? "minecraft:chests/simple_dungeon,minecraft:chests/desert_pyramid,minecraft:chests/jungle_temple,minecraft:chests/nether_bridge,minecraft:chests/abandoned_mineshaft" : TABLES).split(",");
  for (var i = 0; i < names.length; i++) {
    var table = server.reloadableRegistries().getLootTable(RK.create(Regs.LOOT_TABLE, RL.parse(names[i])));
    // a pool injected by a legacy LootTableLoadEvent holds a LegacyEntry, which the data codec cannot write: that is how it shows here
    try {
      var json = "" + LT.DIRECT_CODEC.encodeStart(ops, table).result().orElse("?");
      res += names[i] + ": plain table, " + json.length + " chars, no legacy entry\n";
    } catch (e) {
      res += names[i] + ": has a legacy loot entry (" + ("" + e).indexOf("LegacyEntry") + ")\n";
    }
  }
}
res;
