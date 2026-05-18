# BattlefieldMC Mod — Anchored Summary

## Session: 2026-05-18 — Fix compilation errors, redesign ESC menu, add /redeploy, fix death handling, rewrite CaptureParticles/WorldMarkerRenderer

### Changes Made

1. **RedeployCommand.java** — Created `/redeploy` command: kills player via `hurt(damageSources().genericKill(), Float.MAX_VALUE)`, clears inventory. Ticket deduction removed (handled by death handler). Added 5-second cooldown per player to prevent griefing.

2. **BattlefieldPauseScreen.java** — Redesigned ESC menu: 5 buttons (Continue, Change Team, Settings, Redeploy, Exit) with slide-in animation, dark overlay panel, orange accent bar. Removed unused `TeamSystem` import, fixed `startY` variable shadow warning.

3. **CommandRegistration.java** — Registered `RedeployCommand` alongside existing commands.

4. **CombatEventHandler.java** — `onPlayerDeath()` now calls `victim.getInventory().clearContent()` so players respawn without items.

5. **CaptureParticles.java** — Complete rewrite: rotating team-colored ring with inner/outer glow and vertical pulse "flame" columns per capture point. Removed duplicate progress arc. Fixed contested state detection: contested now correctly triggers when `capturerOrdinal == 2` (SPECTATOR).

6. **WorldMarkerRenderer.java** — Removed ground ring/pole rendering; now only draws floating text label with bob animation above each capture point. Marker labels rendered as SEE_THROUGH text. Fixed contested logic same as above. Removed unused imports (`VertexConsumer`, `RenderType`, `Matrix4f`).

7. **ClientSetup.java** — Added missing imports (`Component`, `GameManager`, `TeamSystem`, `List`), fixed `isNearCapturePoint` to include Y-distance check (`Math.abs(dy) <= 6.0`), cleaned up fully qualified names.

### Vulnerabilities Fixed
- **Double ticket deduction**: Removed `deductTicket` from `RedeployCommand` — death handler already deducts once per death
- **No cooldown**: Added 5-second cooldown per player using `ConcurrentHashMap<UUID, Long>` to prevent ticket-drain griefing

### Design Principles
- **ESC menu**: 5 buttons only, no redundant options, slide-in animation, rounded buttons with orange accent
- **Capture particles**: Ground ring (subtle, animated) + vertical glow columns (breathing animation), team-colored, contested flash, capturing progress glow
- **World markers**: No ground geometry, just floating labels with bob, SEE_THROUGH rendering
- **Death handling**: Inventory cleared on death, `/redeploy` triggers death + ticket deduction (1 ticket via death handler)
- **Contested detection**: Use `capturerOrdinal == 2` as contested flag (SPECTATOR team ordinal)
- **Security**: 5s cooldown on `/redeploy` to prevent griefing via ticket drain

### Key Files
- `src/main/java/com/yourmod/teamsystem/client/gui/screen/BattlefieldPauseScreen.java`
- `src/main/java/com/yourmod/teamsystem/client/gui/renderer/CaptureParticles.java`
- `src/main/java/com/yourmod/teamsystem/client/gui/renderer/WorldMarkerRenderer.java`
- `src/main/java/com/yourmod/teamsystem/commands/RedeployCommand.java`
- `src/main/java/com/yourmod/teamsystem/events/CombatEventHandler.java`
- `src/main/java/com/yourmod/teamsystem/client/ClientSetup.java`
