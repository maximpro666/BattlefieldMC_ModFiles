# Project Warfare Pigeo (PWP) Mod — Anchored Summary

## Session: 2026-05-24 — Kit weapon specs from ECONOMY_BALANCE.md, vehicle definition discovery, vehicle availability pipeline, fix WorldMarkerRenderer crash, cleanup balance doc

### Changes Made

1. **KitConfig.java** — `buildDefault()` rewritten: each kit uses exact weapons from ECONOMY_BALANCE.md §8 (e.g., Rifleman → M4A1/SCAR-L/G36K only, not the full pool). Pistols added to sniper (secondary slot, DMR→tertiary) and heavy armor (alongside shotgun) kits. Unused pool constants kept.

2. **ConfigManager.java** — `extractDefaults()`: replaced 4 hardcoded sample filenames with dynamic `.json` discovery from the resource directory via Java NIO FileSystem API, works in JAR and dev filesystem.

3. **WorldMarkerRenderer.java** — Fixed pre-existing `cannot find symbol cfg` at line 360: replaced `cfg.showDistance` with `VisualsConfig.get().base.showDistance`.

4. **Vehicle availability pipeline** — Cross-package changes:
   - `VehicleManager.getAvailableVehicles()` sends real `requiredAccessLevel` from vehicle definitions
   - `TeamManager.syncVehicles()` passes real player rank
   - `VehicleSyncPacket` includes `available` boolean field
   - `VehicleEntry` (client) records `available` from packet
   - `VehicleSelectionScreen` dims locked vehicles, shows `LockBadge` (reused from kit system), prevents click-through

5. **ECONOMY_BALANCE.md** — Removed bug tables (§4.4, 5.4, 6.6, 8.7, 9.2). Removed sections 11–16 (commander/events, weather, anti-abuse, discrepancies, roadmap). Removed `Привязка ранга к спавну техники` note (§3.1) — feature now implemented. Kept functional specs: economy core, tickets, progression, BC, VC, vehicles, ammo, kits (8), FOB, medicine (10).

### Key Files
- `src/main/java/com/pigeostudios/pwp/data/KitConfig.java`
- `src/main/java/com/pigeostudios/pwp/config/ConfigManager.java`
- `src/main/java/com/pigeostudios/pwp/client/gui/renderer/WorldMarkerRenderer.java`
- `src/main/java/com/pigeostudios/pwp/core/VehicleManager.java`
- `src/main/java/com/pigeostudios/pwp/core/TeamManager.java`
- `src/main/java/com/pigeostudios/pwp/network/VehicleSyncPacket.java`
- `src/main/java/com/pigeostudios/pwp/client/VehicleEntry.java`
- `src/main/java/com/pigeostudios/pwp/client/gui/screen/VehicleSelectionScreen.java`
- `ECONOMY_BALANCE.md`

### Design Principles
- **Vehicle availability**: serverside rank check → network `available` flag → client UI lock state (same pattern as kit system)
- **Kit weapons**: source of truth is ECONOMY_BALANCE.md, not ad‑hoc pools; pistol required on every kit
- **Config extraction**: dynamic resource discovery prevents missing vehicle definitions at runtime
- **Doc cleanup**: balance doc should contain only functional specs, not bug tracking or roadmap

---

## Session: 2026-05-18 — Fix compilation errors, redesign ESC menu, add /redeploy, fix death handling, rewrite CaptureParticles/WorldMarkerRenderer, design spawn system

### Design Decisions (NEW)
1. **Xaero's Minimap** — используется для мини-карты и полной карты (Entity Radar + waypoint'ы `[TS]`). Кастомный рендер карты НЕ пишем.
2. **SpawnSelectionScreen** — экран респавна для мёртвых игроков (левая панель: точки респавна, правая: команда+кит). Открывается при смерти или `/redeploy`.
3. **Cooldown 20s** — сквадмейт под огнём (урон) + FOB под атакой (враг в 15 блоках). КД на спавн.
4. **Пакеты**: `RespawnAtPointPacket` (C2S), `RespawnBeaconSyncPacket`, `SquadmateStatusSyncPacket` (ServerTickEvent.END), `OpenSpawnSelectionScreenPacket`, `CloseSpawnScreenPacket` (S2C).
5. **XaeroIntegration** — через рефлексию, soft dependency, префикс `[TS]`.

### Key Files
- `DESIGN_Map_Minimap_Xaero.md` — полный дизайн-документ

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
- `src/main/java/com/pigeostudios/pwp/client/gui/screen/BattlefieldPauseScreen.java`
- `src/main/java/com/pigeostudios/pwp/client/gui/renderer/CaptureParticles.java`
- `src/main/java/com/pigeostudios/pwp/client/gui/renderer/WorldMarkerRenderer.java`
- `src/main/java/com/pigeostudios/pwp/commands/RedeployCommand.java`
- `src/main/java/com/pigeostudios/pwp/events/CombatEventHandler.java`
- `src/main/java/com/pigeostudios/pwp/client/ClientSetup.java`

---

## Локализация (Language System) — ВАЖНО

**Принцип:** весь текст мода (чат, гуи, уведомления) должен проходить через локализацию. Хардкод русского текста в Java-файлах запрещён.

### 1. Единый файл русского текста
Весь русский текст — в одном файле:  
`src/main/resources/assets/pwp/lang/ru_ru.json`  
Английские оригиналы — там же: `en_us.json`

### 2. Два механизма рендера

| Где | Механизм | Пример |
|-----|----------|--------|
| **Сервер → Клиент** (чат, команды, события) | `Component.translatable("key", args...)` | `player.sendSystemMessage(Component.translatable("pwp.chat.kit.not_found", name))` |
| **Клиентский GUI** (экраны, виджеты) | `I18n.get("key", args...)` | `I18n.get("pwp.ui.buy_for")` |
| **Билингвальные строки из конфигов** (kit/class display_name) | `I18n.localize("EN // RU")` | `I18n.localize(kit.display_name)` |

### 3. Правила добавления нового текста
1. **Всегда** добавляй ключ в `en_us.json` (англ.) и `ru_ru.json` (рус.)
2. На сервере используй `Component.translatable("key", arg1, arg2)`
3. В GUI используй `I18n.get("key", arg1, arg2)` (import: `com.pigeostudios.pwp.client.gui.I18n`)
4. Цветовые коды (`§c`, `§a`, `§e`, `§6`, `§7`, `§f`) пиши прямо в значениях JSON
5. Методы, возвращающие ошибки (`buyVehicle`, `claimKit`, `applyKit`), должны возвращать `Component` (не `String`), с `null` при успехе

### 4. Переключение языка
`ClientTeamData.language` — `"ru"` для русского, любое другое — английский.
`I18n.get()` проверяет это поле. `Component.translatable()` автоматически использует язык клиента Minecraft.

### 5. Чего НЕ делать
- ❌ Не писать `Component.literal("§cРусский текст")` в Java-файлах
- ❌ Не добавлять русский текст вне `ru_ru.json`
- ❌ Не использовать `I18n` на сервере (он клиентский!)
- ❌ Не возвращать `String` с ошибкой из серверных методов — возвращай `Component`
