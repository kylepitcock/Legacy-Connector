/**
 * Public API of Legacy Connector for addon authors: NeoForge 1.21.1 mods that add modern-Minecraft support to legacy
 * mods (1.7.10, 1.12.2) running on Legacy Connector.
 *
 * <h2>Stability</h2>
 * Everything in {@code org.kylepitcock.legacyconnector.api} and {@code org.kylepitcock.legacyconnector.api.event} is public API.
 * It follows semantic versioning from 1.0.0 on; while the mod is 0.x, only the PATCH number is guaranteed compatible (0.3.x addons
 * keep working on 0.3.y), and a MINOR bump may change signatures (the changelog says so). Classes in {@code api.internal}
 * are implementation details: do not use them.
 *
 * <h2>Rules that keep the API stable</h2>
 * <ul>
 *   <li>Signatures use only JDK, Minecraft/NeoForge 1.21.1 and API types. Legacy objects (a 1.7.10 {@code ItemStack}, a
 *       {@code TileEntity}, a {@code World}) are passed as {@code Object}: the shim classes behind them are internal and
 *       change freely. Reach into them with {@link org.kylepitcock.legacyconnector.api.LegacyAccess} (reflection with cached
 *       method handles), using the names of the original legacy API, exactly as the legacy mod itself would call them.</li>
 *   <li>Everything here must be called on the main thread (the server thread, or the client thread for client-only
 *       objects) unless a method says otherwise.</li>
 * </ul>
 *
 * <h2>Entry points</h2>
 * {@link org.kylepitcock.legacyconnector.api.LegacyApi} (query mods and phases),
 * {@link org.kylepitcock.legacyconnector.api.LegacyAddon} (lifecycle callbacks), the events in {@code api.event},
 * {@link org.kylepitcock.legacyconnector.api.LegacyItems}, {@link org.kylepitcock.legacyconnector.api.LegacyBlocks} and
 * {@link org.kylepitcock.legacyconnector.api.LegacyWorlds} (conversions),
 * {@link org.kylepitcock.legacyconnector.api.LegacyCapabilities} (expose legacy tile entities as NeoForge capabilities) and
 * {@link org.kylepitcock.legacyconnector.api.LegacyContentHooks} (resolve modern ids to the keys legacy mods expect).
 */
package org.kylepitcock.legacyconnector.api;
