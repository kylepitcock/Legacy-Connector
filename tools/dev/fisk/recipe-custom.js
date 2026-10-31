// Code recipes (IRecipe subclasses): tachyon device recharge with unstable quantum matter, emptying a loaded gun, katana/chokuto, display case, quiver.
var CraftingInput = Java.type("net.minecraft.world.item.crafting.CraftingInput");
var RecipeType = Java.type("net.minecraft.world.item.crafting.RecipeType");
var ModItems = Java.type("com.fiskmods.heroes.common.item.ModItems");
var ModBlocks = Java.type("com.fiskmods.heroes.common.block.ModBlocks");
var LItem = Java.type("legacyconnector.v1_7_10.net.minecraft.item.Item");
var JList = Java.type("java.util.ArrayList");
function mod(item, count, meta) { return LItemStack.mcc$toModern(new LItemStack(item, count, meta)); }
function craft(stacks, w, h) {
  var list = new JList(); stacks.forEach(function (s) { list.add(s); }); while (list.size() < w * h) list.add(Java.type("net.minecraft.world.item.ItemStack").EMPTY);
  var input = CraftingInput.of(w, h, list), it = server.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING).iterator(), found = [];
  while (it.hasNext()) { var hh = it.next(); if (String(hh.id()).indexOf("custom/") < 0) continue; var rc = hh.value();
    if (rc.matches(input, server.overworld())) found.push(String(hh.id()).replace("fiskheroes:", "") + " -> " + rc.assemble(input, server.registryAccess())); }
  return found.join("; ") || "no custom recipe matches";
}
var uqm = LItem.getItemFromBlock(ModBlocks.unstableQuantumMatter);
var o = [];
o.push("tachyon device + 2 tachyon QM (meta 1): " + craft([mod(ModItems.tachyonDevice, 1, 0), mod(uqm, 1, 1), mod(uqm, 1, 1)], 3, 3));
o.push("tachyon battery + 1 QM: " + craft([mod(ModItems.tachyonBattery, 1, 0), mod(uqm, 1, 1)], 3, 3));
o.push("tachyon prototype + 1 QM: " + craft([mod(ModItems.tachyonPrototype, 1, 0), mod(uqm, 1, 1)], 3, 3));
o.push("desert eagle alone (no ammo): " + craft([mod(ModItems.desertEagle, 1, 0)], 3, 3));
o.join("\n")
