# 1.12.2 coremod hook places, round 4

Status (2026-11-15): gated and run on a server (`run-cm4`: AppleCore, Charm, Quark, RandomThings): 103 hooks, 38 hook methods at 48 places, 48 places without equivalent, no hook failure, self tests unchanged. `tools/dev/coremod-sites-1122-probe.js` (`-PserverTickScript=`) loads every mixin target (all ten mixins apply) and drives the places a server reaches without a player. "applies" = the mixin applies and the game path ran without a hook failure; the mod's visible effect was not checked in play.
Run with `-PcoremodReport` to list every unmapped place in the log.

| Site (legacy method) | Hook | Place on 1.21.1 | State |
|---|---|---|---|
| EntityPlayer.addMovementStat, 5 x fmul | AppleCore fireExhaustingActionEvent | ServerPlayer.checkMovementStatistics causeFoodExhaustion (PlayerMovementExhaustionHookMixin) | applies; needs a moving player |
| EntityLivingBase.getItemInUseMaxCount | AppleCore getItemInUseMaxCount | LivingEntity.getTicksUsingItem, getUseDuration (LivingEntityUseDurationHookMixin) | applies; needs a player using an item |
| BlockCake.eatCake | AppleCore onBlockFoodEaten | CakeBlock.eat, FoodData.eat wrap (CakeEatHookMixin; needs the cake view to be IEdible, unverified) | applies; needs a player eating cake |
| EntityWither.canDestroyBlock | Charm canWitherDestroyBlock | WitherBoss.canDestroy (WitherBlockHookMixin) | applies, canDestroy ran |
| EntityBoat.attackEntityFrom / EntityMinecart.killMinecart | Quark boatDrops / minecartDrops | VehicleEntity.destroy(Item) tail | applies, boat and minecart destroyed |
| EntityAITarget.isSuitableTarget | Quark hasWitchHat | TargetingConditions.test head | applies, test ran |
| WorldServer.areAllPlayersAsleep | Quark isEveryoneAsleep (0 default, 1 no, 2 yes) | ServerLevel.tick SleepStatus wraps | applies; needs sleeping players |
| BlockDynamicLiquid.isBlocked x3 | Quark isBlockNotBrokenByWater | FlowingFluid.canHoldFluid return | applies; water did not flow in the probe (also on master), not exercised |
| TileEntityFurnace.smeltItem | Charm changeSmeltingResult | AbstractFurnaceBlockEntity.burn assemble wrap | applies, furnace smelted iron |
| RecipeAddPattern.matches | Quark shiftLayerCountDown | LoomMenu.slotsChanged List.size ordinal 1 (ordinal unverified) | applies; ordinal not checked in play |
| FoodStats.addStats(ItemFood,ItemStack) x2, addStats(int,float) | AppleCore onFoodStatsAdded etc. | none: the transformer rewrites data flow around the mod's own FoodValues object | no equivalent |
| EntityPlayer.onLivingUpdate peaceful regen x2 | AppleCore firePeaceful*RegenEvent | none: returns event objects whose fields replace constants | no equivalent |
| BlockPistonBase x3, TileEntityPiston x3, piston renderer | Quark piston hooks | none: Quark substitutes a BlockPistonStructureHelper subclass; modern resolver is not subclassable here | no equivalent |
| Enchantment/EnchantmentDamage.canApply, EnchantmentHelper.getEnchantments | Quark | none: enchantments are data driven, no NBT list | no equivalent |
| DamageSource.causePlayerDamage | Quark createPlayerDamage | none: answer is a legacy DamageSource subclass | no equivalent |
| ItemBanner tooltip const 6 | Quark shiftLayerCountUp | none: modern tooltip prints every layer | no equivalent |
| TileEntityBeacon, ItemStack.getTextComponent, structure pieces (Church, MonumentCoreRoom, StructureStart) | Charm/Quark/RandomThings | not attempted (potion/AABB mapping; chat; worldgen pieces) | open |
| RandomThings places (collision boxes, slip, redstone power, slime chunk, tree ground, inventory drop) | RT | not attempted (another worker owns RT) | open |
| Client render hooks (Quark/RT/Charm, about 20 places) | various | not attempted | open |
