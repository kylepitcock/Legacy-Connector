# Changing legacy recipes

Back to [Home](Home.md); see also [Bridges](Bridges.md).

Legacy recipes are ordinary modern recipes in a data pack (`legacyconnector_legacy_recipes`) placed below world data packs, so
they are changed like any mod's recipes:

- **Data packs:** each recipe has a stable id, `<mod>:<kind>/<output item>` (`<kind>` is `shaped`, `shapeless`, `smelting` or
  `custom`; the output's namespace is added for another mod's item, `_2`, `_3`, ... for more recipes of one output), e.g.
  `ic2:shaped/macerator` or `legacytest:shaped/minecraft/diamond`. 1.12.2 recipes keep their registry name
  (`<mod>:<name>_code`); 1.12.2 smelting is `legacyconnector:smelting_1122/<output item>`. A file at the same path in a world
  data pack replaces the recipe; `{"neoforge:conditions":[{"type":"neoforge:false"}]}` removes it.
- **KubeJS:** `event.remove({id: 'ic2:shaped/macerator'})`, `event.remove({mod: 'ic2'})`, `event.replaceInput(...)` and so on
  work as for modern mods.
- **MineTweaker 3 (1.7.10):** the legacy jar loads and runs its `scripts/*.zs` against the legacy recipe lists;
  `recipes.addShaped/addShapeless/remove` and `furnace.addRecipe/remove` become modern recipes. MineTweaker runs its scripts when
  the server starts (after data packs loaded), so the data packs are reloaded once when a script changed the recipes;
  `/minetweaker reload` is picked up the same way within a few seconds. For 1.12.2, recipes removed from the legacy recipe
  registry or smelting list are left out the same way (CraftTweaker 1.12 itself is not tested yet).

KubeJS needs nothing else: what it (or a data pack) does to a legacy recipe also applies to the legacy crafting and smelting
lookups that mod crafting tables, autocrafters and furnaces use (`CraftingManager.findMatchingRecipe`,
`FurnaceRecipes.getSmeltingResult`): a removed recipe no longer crafts there, a changed one crafts as changed. Not yet:
MineTweaker removing vanilla recipes (they are not in the legacy lists; remove them with a data pack or KubeJS), input filters
and `replaceInput` on custom legacy recipe classes (`legacyconnector:legacy_recipe`: KubeJS sees their output, not their
inputs; remove them by id or output and add a new recipe), and machine recipes kept in a mod's own API (IC2 macerator, Thermal
pulverizer), which are not modern recipes.
