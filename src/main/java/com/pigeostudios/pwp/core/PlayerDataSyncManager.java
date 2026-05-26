package com.pigeostudios.pwp.core;

/**
 * PlayerDataSyncManager is no longer needed.
 * All data is synced to CentralDatabase in real-time:
 *  - TeamManager syncs kills/deaths/stats on each change
 *  - VehicleManager syncs cooldowns to DB directly
 *  - GameManager uses CentralDatabase for map selection
 *  - PlayerEventHandler uses TeamManager.loadFromCentralDatabase()
 *
 * @deprecated Removed in favor of real-time CentralDatabase access.
 *             Class kept as empty placeholder to prevent compile errors
 *             until all references are cleaned up.
 */
@Deprecated
public class PlayerDataSyncManager {
}
