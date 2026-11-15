# 1.12.2 coremod hook places, rounds 4 and 5

Status (2026-11-15, round 5 batch 3): gated, run on a server (`run-cm5`: AppleCore, AutoRegLib, Charm, Quark, RandomThings) and in a client (`runClientTest`, same mods in `run-clienttest/mods/1.12.2`): 103 hooks, 50 hook methods at 60 places, 36 places without equivalent, no hook failure, self tests unchanged (item placement 754 of 754, block self test 12 with problems, 16 ERROR lines as on master). `tools/dev/coremod-sites-1122-probe.js` (`-PserverTickScript=`) loads every mixin target and drives the places a server reaches without a player (fake player via FakePlayerFactory for collision/inventory). "applies" = the mixin applies and the game path ran without a hook failure; "works" = the mod's effect was asserted in the probe.
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
| TileEntityBeacon.addEffectsToPlayers (hook told world, area, effects, duration, amplifier) | Charm addBeaconEffect | BeaconBlockEntity.applyEffects head (BeaconEffectsHookMixin; area/duration/amplifier recomputed as the game does; vanilla effects mapped by registry name) | works: tamed wolf in the area gets regeneration under a regeneration beacon |
| ItemStack.getTextComponent (last thing) | Quark createStackComponent | ItemStack.getDisplayName return (ItemStackComponentHookMixin; component goes through 1.12 JSON, the item's own component is put back so the hover survives) | applies: the component becomes the hook's "" wrapper with the name as sibling; the chat icon is client side, not checked |
| LayerArmorBase.renderArmorLayer (per slot, after the stack was read) | Charm isArmorInvisible | HumanoidArmorLayer.renderArmorPiece (12 argument overload) head cancel (ArmorLayerHiddenHookMixin, client) | applies (merged handler found in the client); the leather-armour effect was not looked at |
| Render.renderEntityOnFire head-cancel | Quark renderFire | EntityRenderDispatcher.renderFlame head cancel (EntityFlameHookMixin, client; EntityFireHook1122 sets up the legacy render context at the camera-relative position) | applies; drawing not looked at (needs a burning entity on screen) |
| LayerEntityOnShoulder.renderEntityOnShoulder (texture after aload 6) | Quark replaceParrotTexture | ParrotRenderer.getVariantTexture return, with the shoulder tag kept by ParrotOnShoulderLayer.render head/return (ShoulderParrotTextureHookMixin + ParrotTextureHookMixin, client) | applies; texture swap not looked at |
| structure pieces (Church, MonumentCoreRoom, StructureStart) | Charm/RT | not yet | open |
| World.getStrongPower / getRedstonePower (first thing; nonzero answer returned) | RT getStrongPower / getRedstonePower | SignalGetter.getDirectSignal / getSignal head (SignalGetterHookMixin) | applies; no exception; RT interface block effect not driven |
| EntitySlime.getCanSpawnHere (slime chunk, 0/1/-1) | RT overrideSlimeChunk | Slime.checkSlimeSpawnRules head (SlimeChunkHookMixin) | works: with a slime cube in the chunk the rule is true 60 of 60, without 0 of 60 |
| EntityLivingBase.travel ground slipperiness (after fmul) | RT slipFix | LivingEntity.travel, wrap of BlockState.getFriction (LivingEntitySlipperinessHookMixin; value passed as friction*0.91, result divided again) | works: zombie on stone keeps 0.273 of 0.5, on super lubricent stone 0.5 |
| InventoryPlayer.dropAllItems (per stack) | RT shouldPlayerDrop | Inventory.dropAll, wrap of ItemStack.isEmpty (InventoryDropAllHookMixin) | works: a stack with spectreAnchor stays, a plain one drops |
| WorldGenAbstractTree.setDirtAt | RT protectGround | TrunkPlacer.setDirtAt head (TrunkPlacerGroundHookMixin) | works: oak on fertilized dirt keeps it, on grass makes dirt |
| Block.addCollisionBoxToList (first thing, true = handled) | RT addCollisionBoxesToList | BlockStateBase.getCollisionShape(BlockGetter,BlockPos,CollisionContext) head (BlockStateCollisionHookMixin): hook fills a box list (asked box = the block's cell), boxes become the shape, entity from the context | works: water shape empty bare, solid with water walking boots (fake player) |
| Client render hooks (Quark/RT/Charm, about 20 places) | various | not attempted | open |
