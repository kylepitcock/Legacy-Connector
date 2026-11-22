// Thermal Expansion 1.12.2 machine tables built at load: do the vanilla smelting recipes reach the Redstone Furnace, and does the Sawmill take logs?
// usage: LIBS=lib tools/dev/run-server-script.sh thermal-recipes-1122 400 150 (Thermal jars in run-subtest/mods/1.12.2)
if (typeof TR === "undefined") TR = { done: false };
if (!TR.done && tick >= 2) {
  TR.done = true;
  var B = Java.type("legacyconnector.v1_12_2.net.minecraft.init.Blocks");
  var I = Java.type("legacyconnector.v1_12_2.net.minecraft.init.Items");
  var IS = Java.type("legacyconnector.v1_12_2.net.minecraft.item.ItemStack");
  var out = "";
  function ask(label, cls, stack) {
    try {
      var m = Java.type(cls);
      out += "\n   " + label + ": " + (cls == "cofh.thermalexpansion.util.managers.machine.FurnaceManager" ? m.recipeExists(stack, false) : m.recipeExists(stack));
    } catch (e) { out += "\n   " + label + ": EXC " + e; }
  }
  var FM = "cofh.thermalexpansion.util.managers.machine.FurnaceManager", SM = "cofh.thermalexpansion.util.managers.machine.SawmillManager";
  ask("furnace iron ore", FM, new IS(B.IRON_ORE, 1, 0));
  ask("furnace sand", FM, new IS(B.SAND, 1, 0));
  ask("furnace beef", FM, new IS(I.BEEF, 1, 0));
  ask("furnace oak log (charcoal)", FM, new IS(B.LOG, 1, 0));
  ask("sawmill oak log", SM, new IS(B.LOG, 1, 0));
  ask("sawmill spruce log", SM, new IS(B.LOG, 1, 1));
  ask("sawmill acacia log", SM, new IS(B.LOG2, 1, 0));
  ask("sawmill dark oak log", SM, new IS(B.LOG2, 1, 1));
  log.info("SERVERSCRIPT: THERMAL RECIPES" + out);
}
