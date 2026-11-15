# 1.12.2 coremod hook places, round 4

Status: all rows marked todo are written but NOT gated and NOT verified in a server run (session closed first). Baseline: 103 hooks, 28 hook methods at 33 places, 59 places without equivalent.
Run with `-PcoremodReport` (added to build.gradle) to list every unmapped place in the log.

| Site (legacy method) | Hook | Place on 1.21.1 | State |
|---|---|---|---|
| EntityPlayer.addMovementStat, 5 x fmul | AppleCore fireExhaustingActionEvent | ServerPlayer.checkMovementStatistics causeFoodExhaustion (PlayerMovementExhaustionHookMixin) | todo |
| EntityLivingBase.getItemInUseMaxCount | AppleCore getItemInUseMaxCount | LivingEntity.getTicksUsingItem, getUseDuration (LivingEntityUseDurationHookMixin) | todo |
| BlockCake.eatCake | AppleCore onBlockFoodEaten | CakeBlock.eat, FoodData.eat wrap (CakeEatHookMixin; needs the cake view to be IEdible, unverified) | todo |
| EntityWither.canDestroyBlock | Charm canWitherDestroyBlock | WitherBoss.canDestroy (WitherBlockHookMixin) | todo |
| EntityBoat.attackEntityFrom / EntityMinecart.killMinecart | Quark boatDrops / minecartDrops | VehicleEntity.destroy(Item) tail | todo |
| EntityAITarget.isSuitableTarget | Quark hasWitchHat | TargetingConditions.test head | todo |
| WorldServer.areAllPlayersAsleep | Quark isEveryoneAsleep (0 default, 1 no, 2 yes) | ServerLevel.tick SleepStatus wraps | todo |
| BlockDynamicLiquid.isBlocked x3 | Quark isBlockNotBrokenByWater | FlowingFluid.canHoldFluid return | todo |
| TileEntityFurnace.smeltItem | Charm changeSmeltingResult | AbstractFurnaceBlockEntity.burn assemble wrap | todo |
| RecipeAddPattern.matches | Quark shiftLayerCountDown | LoomMenu.slotsChanged List.size ordinal 1 (ordinal unverified) | todo |
| FoodStats.addStats(ItemFood,ItemStack) x2, addStats(int,float) | AppleCore onFoodStatsAdded etc. | none: the transformer rewrites data flow around the mod's own FoodValues object | no equivalent |
| EntityPlayer.onLivingUpdate peaceful regen x2 | AppleCore firePeaceful*RegenEvent | none: returns event objects whose fields replace constants | no equivalent |
| BlockPistonBase x3, TileEntityPiston x3, piston renderer | Quark piston hooks | none: Quark substitutes a BlockPistonStructureHelper subclass; modern resolver is not subclassable here | no equivalent |
| Enchantment/EnchantmentDamage.canApply, EnchantmentHelper.getEnchantments | Quark | none: enchantments are data driven, no NBT list | no equivalent |
| DamageSource.causePlayerDamage | Quark createPlayerDamage | none: answer is a legacy DamageSource subclass | no equivalent |
| ItemBanner tooltip const 6 | Quark shiftLayerCountUp | none: modern tooltip prints every layer | no equivalent |
| TileEntityBeacon, ItemStack.getTextComponent, structure pieces (Church, MonumentCoreRoom, StructureStart) | Charm/Quark/RandomThings | not attempted (potion/AABB mapping; chat; worldgen pieces) | open |
| RandomThings places (collision boxes, slip, redstone power, slime chunk, tree ground, inventory drop) | RT | not attempted (another worker owns RT) | open |
| Client render hooks (Quark/RT/Charm, about 20 places) | various | not attempted | open |
