# BattlefieldMC Full GUI System — Prompt for Claude

## Context

Forge 1.20.1 mod "TeamSystem" (`com.yourmod.teamsystem`). Already implemented:

### Existing Screens
- `TeamSelectionScreen` — pick NATO/RUSSIA
- `BattlefieldPauseScreen` — ESC menu with return/team/settings/disconnect
- `SettingsMenuScreen` — volume/language/scale/opacity
- `VoteScreen` — map voting
- `KitSelectionScreen` — grid of kits (old version, uses `KitEntry`/`KitSelectPacket`)
- `KitLoadoutScreen` — slot-based loadout editing (old version, uses `KitSavePacket`)
- `VehicleSelectionScreen` — vehicle at spawn
- `SquadScreen` — squad management
- `BattlefieldMainMenuScreen` — main menu

### Existing Widgets (in `client.gui.component`)
- `BCard` — simple rendering card (slide animation, colored border/accent)
- `BButton` — custom button with hover glow + accent bar
- `BProgressBar` — animated progress bar
- `BScrollPanel` — scroll container
- `BSlider` — slider control

### NEW data models already generated (in `data/`)
- **`KitConfig.java`** — server-side config: `Map<String, ClassConfig>` → `KitDef` → `KitWeapons`/`AttachmentLimit`/`KitRequirements`. Saved to `world/teamsystem/kits.json`.
- **`PlayerLoadout.java`** — per-player: `classId`/`kitId`/`LoadoutSlots`(primary/secondary WeaponSlot + special/grenade). Saved to `world/teamsystem/playerloadouts/<uuid>.json`.
- **`LockState.java`** — enum: `AVAILABLE`, `LOCKED_RANK`, `LOCKED_KIT`, `LOCKED_TEAM`, `LOCKED_MAP`, `INCOMPATIBLE` with tooltip helpers.
- **`LockChecker.java`** — static methods: `checkKit()`, `checkAttachment()`, `checkVehicle()`.

### NEW client screens already generated (in `client.gui.widget` / `client.gui.screen`)
- **`widget/BCard.java`** — interactive `AbstractWidget` card (title/subtitle/lock overlay/stagger fade-in/hover scale). Extends `net.minecraft.client.gui.components.AbstractWidget`.
- **`widget/BWidgets.java`** — contains: `BToggle` (animated switch), `BDropdown` (scrollable dropdown), `BNumberInput` (+/−), `BTag` (colored tag), `BLockOverlay` (dim+lock icon), `BScrollPanel` (scrollbar with drag).
- **`screen/ClassSelectionScreen.java`** — 5 class cards grid → opens `KitSelectionScreen`
- **`screen/LoadoutScreen.java`** — PRIMARY/SECONDARY/SPECIAL/GRENADE slots with inline attachment tags, `[CHANGE]` → `AttachmentPickerScreen`
- *(Note: `AttachmentPickerScreen` not yet generated — needs to be created)*

### NEW server-side data already generated (in `data/`)
- `KitConfig` with full JSON save/load + default config
- `PlayerLoadout` with JSON load/save per UUID
- `LockChecker` with player context (rank/team/map/class/kit)

We need to build the complete Battlefield-style menu system:
- **Kit menu** (class → kit → loadout editing) — screens done, wire up to `KitConfig`/`PlayerLoadout`
- **Attachment customization** — `AttachmentPickerScreen` still needs to be implemented
- **Vehicle selection** — `VehicleSelectionScreen` exists, needs `LockChecker` integration
- **Admin panel** — match control, kit config, map mgmt, permissions — new screens needed
- **Match Results Screen** — new
- **Spawn Screen** — new
- **Admin Permission System** — new

## UI Theme (from `UITheme.java`)

```java
BG_SCREEN     = 0xCC0A0A0A  // dark overlay
BG_PANEL      = 0xDD141414  // panel bg
BG_SURFACE    = 0xDD1C1C1C  // card bg
BG_SLOT       = 0xFF242424  // item slot
BG_HUD        = 0xAA0F0F0F  // HUD bg
BG_BLACK      = 0xFF000000
TEXT_PRIMARY   = 0xFFEFEFEF
TEXT_SECONDARY = 0xFF909090
TEXT_MUTED     = 0xFF606060
ACCENT         = 0xFFE07B00  // orange
ACCENT_DIM     = 0x80E07B00
ACCENT_GHOST   = 0x26E07B00
BORDER         = 0xFF2E2E2E
BORDER_ALT     = 0x0AFFFFFF
STATUS_OK      = 0xFF50B050  // green
STATUS_WARN    = 0xFFCCA030  // yellow
STATUS_DANGER  = 0xFFCC3030  // red
```

Existing components: `BButton`, `BProgressBar`, `AnimationHelper` (lerp, withAlpha, blendColors, easeOutCubic).

## Part 1: Player Menu System

### 1.1 TeamSelectionScreen (already exists)
- Shows NATO / RUSSIA / SPECTATOR buttons in a sliding panel
- Must persist (keep reference) — player returns here if they leave a team

### 1.2 ClassSelectionScreen (NEW)
Opens after team selection or from ESC menu "Change Class".
```
┌──────────────────────────────┐
│        SELECT CLASS          │  title with orange underline
├──────────────────────────────┤
│ ┌──────┐ ┌──────┐ ┌──────┐  │  3–5 class cards in a row
│ │图标   │ │图标   │ │图标   │  │  each with:
│ │Assault│ │Medic │ │Enginer│  │   - class icon (ItemStack or colored box)
│ │AR/SG │ │SMG   │ │Shotgun│  │   - class name
│ │       │ │      │ │       │  │   - weapon type label
│ └──────┘ └──────┘ └──────┘  │   - locked overlay if unavailable
│ ┌──────┐ ┌──────┐           │
│ │图标   │ │图标   │           │
│ │Support│ │Recon │           │
│ │LMG    │ │Sniper│           │
│ └──────┘ └──────┘           │
├──────────────────────────────┤
│   [BACK TO TEAM SELECT]     │  BButton bottom-left
└──────────────────────────────┘
```
- Each class card: `~120×100px`, rounded corners (just fill), hover glow
- Locked state: grayed out with `TEXT_MUTED`, lock icon (unicode `\uD83D\uDD12` or `[X]`), tooltip on hover
- Scrollable if more than 5 classes
- Animation: cards fade in with stagger delay

### 1.3 KitSelectionScreen (NEW)
After picking a class, shows kit variants for that class.
```
┌──────────────────────────────────┐
│   ASSAULT → SELECT KIT          │  breadcrumb: "Class → Kit"
├──────────────────────────────────┤
│ ┌────┐ ┌────┐ ┌────┐            │  horizontal scrollable row
│ │Rifle│ │Gren│ │Breach│          │  each kit card ~160×140px
│ │man  │ │adier│ │er    │          │
│ │     │ │     │ │      │          │
│ │M4A1 │ │M4A1 │ │M4A1  │          │  - kit name
│ │+M203│ │     │ │      │          │  - primary weapon preview
│ └────┘ └────┘ └────┘            │  - special ability label
│                                   │
│ [Kit description text here]      │  bottom panel shows selected kit info
├──────────────────────────────────┤
│ [CUSTOMIZE LOADOUT]  [SELECT]   │  2 buttons
└──────────────────────────────────┘
```
- Each kit card shows: name, primary weapon, special ability
- Hover enlarges card slightly (scale 1.05)
- "Customize Loadout" opens LoadoutScreen
- "Select" confirms kit and closes

### 1.4 LoadoutScreen (NEW)
```
┌──────────────────────────────────────────┐
│    CUSTOMIZE: RIFLEMAN                   │  title
├──────────────────────────────────────────┤
│ ┌────────────────────────────────────┐   │
│ │ PRIMARY: M4A1       [CHANGE]       │   │  weapon slots with preview
│ │  Scope: Red Dot                    │   │
│ │  Barrel: Compensator               │   │  inline compact attachment list
│ │  Grip: Vertical                    │   │
│ │  Magazine: 30rnd                   │   │
│ ├────────────────────────────────────┤   │
│ │ SECONDARY: G17       [CHANGE]     │   │
│ │  [no attachments]                  │   │
│ ├────────────────────────────────────┤   │
│ │ SPECIAL: M67 Frag     [CHANGE]    │   │  equipment slot
│ ├────────────────────────────────────┤   │
│ │ GRENADE: M67 Frag     [CHANGE]    │   │  throwable slot
│ └────────────────────────────────────┘   │
│ [BACK]         [SAVE & CLOSE]           │
└──────────────────────────────────────────┘
```
- Each slot row is a `BButton`-styled card (full width, 40px height)
- Clicking "[CHANGE]" opens a picker popup/dropdown
- Attachments shown inline as compact tags (colored by category)
- Scroll if too many slots

### 1.5 AttachmentPicker (NEW — sub-screen or popup)
```
┌──────────────────────────────┐
│  M4A1 → SCOPE               │
├──────────────────────────────┤
│ ○ Iron Sights  (default)    │  radio list
│ ○ Red Dot Sight  Lv.1       │
│ ○ Holographic     Lv.2      │  grayed if locked by rank/kit
│ ○ ×4 Scope        Lv.5🔒   │  🔒 = locked, tooltip shows requirement
│ ○ ×8 Scope         ─        │  ─ = unavailable for this gun
├──────────────────────────────┤
│ [APPLY]     [CANCEL]        │
└──────────────────────────────┘
```
- Shows only attachments compatible with current weapon
- Locked items show requirement text on hover: "Requires Rank: Sergeant"
- Categories: Scope, Barrel, Grip, Magazine, Ammo, Muzzle, Underbarrel
- Scrollable list

### 1.6 VehicleSelectionScreen (NEW)
Opens at spawn if vehicles available + player has enough score/rank.
```
┌──────────────────────────────────┐
│     SELECT VEHICLE              │
├──────────────────────────────────┤
│ ┌──────┐ ┌──────┐ ┌──────┐     │  grid of vehicle cards
│ │  🚁  │ │  🚁  │ │  🚁  │     │
│ │Transport│ │Attack │ │Scout │     │
│ │         │ │Heli   │ │Heli  │     │  - vehicle name
│ │ 2 seats │ │ 1 seat│ │ 1    │     │  - seat count
│ ├─────────┤ ├───────┤ ├──────┤     │  - cost/requirement
│ │Req:Crew │ │   3★  │ │  5★  │     │
│ └──────┘ └──────┘ └──────┘     │
│ ┌──────┐ ┌──────┐              │
│ │  🚗  │ │  🚚  │              │
│ │  Jeep │ │  Truck│             │
│ │  3★   │ │  1★  │             │
│ └──────┘ └──────┘              │
├──────────────────────────────────┤
│ [SPAWN WITHOUT VEHICLE]        │
└──────────────────────────────────┘
```
- Grid layout, responsive (3 cols on wide, 2 on narrow)
- Unavailable vehicles: grayed + lock + reason
- Cost shown as stars or rank level
- Scrollable

## Part 2: Admin Panel

### 2.1 AdminPanel Main Screen
```
┌──────────────────────────────────────────────┐
│                   ADMIN PANEL                │  header with orange underline
│              ⚡ Quick Actions                │  toolbar row
├──────────────────────────────────────────────┤
│ [START MATCH] [RESTART] [STOP] [FORCE VOTE] │  BButton row, green/red styled
├──────────────────────────────────────────────┤
│ CURRENT: PostSovietStreets | NATO 85-72 RUS  │  status bar
│ Phase: Playing | Timer: 14:32 | Players: 12  │
├──────┬───────────────────────────────────────┤
│ NAV  │  (CONTENT PANEL)                      │
│      │                                       │
│ 📋   │  Changes based on selected tab        │
│ Match│                                       │
│      │                                       │
│ 🗺️   │  Each tab has its own sub-layout      │
│ Maps │                                       │
│      │                                       │
│ ⚔️   │                                       │
│ Kits │                                       │
│      │                                       │
│ 👥   │                                       │
│ Teams│                                       │
│      │                                       │
│ ⚙️   │                                       │
│ Conf │                                       │
└──────┴───────────────────────────────────────┘
```
- Left nav bar: tab icons + labels (fixed width ~50px, expands on hover to ~160px with text)
- Content panel fills remaining space
- Each tab is a separate sub-screen (loaded on click)

### 2.2 Admin — Match Tab
```
┌──────────────────────────────────────────────┐
│              MATCH CONTROL                   │
├──────────────────────────────────────────────┤
│ ┌────────────────────────────────────────┐   │
│ │ Status: RUNNING                        │   │  large status card
│ │ Map: PostSovietStreets                 │   │
│ │ Phase: Playing (14:32 elapsed)         │   │
│ │ NATO: 85 tickets | RUSSIA: 72 tickets  │   │
│ └────────────────────────────────────────┘   │
│                                              │
│ Actions:                                      │
│ [FORCE START]  [FORCE END]  [RESET MATCH]    │  colored action buttons
│ [NEXT MAP → VOTE]  [SKIP TO LOBBY]           │
│                                              │
│ Timer:                                        │
│ ┌──────────────────────────────┐             │
│ │ Set match time: [ 15:00 ]   │             │  editable text field
│ │ [APPLY TIMER]               │             │
│ └──────────────────────────────┘             │
└──────────────────────────────────────────────┘
```

### 2.3 Admin — Maps Tab
```
┌──────────────────────────────────────────────┐
│              MAP MANAGEMENT                  │
├──────────────────────────────────────────────┤
│ Map Pool:                                    │
│ ┌────────────────┐ ┌──────────┐ ┌──────────┐│
│ │ PostSovietStr. │ │ Балашов  │ │ Лесецк   ││
│ │ ✓ Active       │ │ ✗ Missing│ │ ✗ Missing││
│ ├────────────────┤ ├──────────┤ ├──────────┤│
│ │ [REMOVE]       │ │ [FIX PATH]│ │ [FIX]   ││
│ └────────────────┘ └──────────┘ └──────────┘│
│                                              │
│ [ADD MAP] → opens file dialog / path input   │
│                                              │
│ ┌────────────────────────────────────────┐   │
│ │ Vote Duration: [ 30 ] seconds         │   │
│ │ Maps per vote: [ 4 ]                  │   │
│ │ [SAVE SETTINGS]                       │   │
│ └────────────────────────────────────────┘   │
└──────────────────────────────────────────────┘
```

### 2.4 Admin — Kits Tab
This is the most complex tab. Full kit/class configuration.
```
┌──────────────────────────────────────────────┐
│              KIT EDITOR                      │
├──────────┬───────────────────────────────────┤
│ CLASS    │  KIT DETAILS                      │
│ SELECT   │                                   │
│          │  Name: [ Rifleman           ]     │  editable text
│ [ASSAULT]│  Class: Assault (locked)           │  dropdown
│ [MEDIC]  │  ─────────────────────────────     │
│ [ENGINEER]│ Available Weapons:                 │
│ [SUPPORT]│  ┌─────────────────────────────┐   │
│ [RECON]  │  │ ✓ M4A1    [EDIT ATTACHMENTS]│   │  per-weapon rows
│          │  │ ✓ M16A4   [EDIT ATTACHMENTS]│   │
│          │  │ ✗ SCAR-H  [+ADD]           │   │
│          │  │ ✗ HK417                    │   │
│          │  └─────────────────────────────┘   │
│          │  ─────────────────────────────     │
│          │  Available Secondaries:             │
│          │  [G17 ✓] [M9 ✓] [G18 ✗] […]       │  tag-style toggles
│          │  Available Equipment:               │
│          │  [M67 ✓] [Flash ✓] [Smoke ✓]       │
│          │  ─────────────────────────────     │
│          │  Requirements:                      │
│          │  Rank: [ ≥ 3 ★          ]          │  dropdown/number
│          │  ─────────────────────────────     │
│          │  [COPY KIT] [RESET DEFAULT]       │
│          │  [SAVE KIT]                       │
└──────────┴───────────────────────────────────┘
```
- Left: class list (highlighted = selected)
- Right: full kit editor for selected class's selected kit
- Per-weapon attachment limits: what scopes/barrels/grips/etc are allowed
- "EDIT ATTACHMENTS" opens an attachment permission matrix:
```
┌──────────────────────────────────────┐
│  M4A1 — Allowed Attachments          │
├──────────────────────────────────────┤
│ Scope: [IronSights] [RedDot] [Holo]  │  allowed = highlighted
│        [×4Scope] [×8Scope]           │  dimmed = disallowed
│ Barrel: [Standard] [Long] [Heavy]   │
│ Grip:   [None] [Vertical] [Angled]   │  toggle per item
│ Magazine: [30rnd] [45rnd] [Drum]    │
│ Ammo:   [Standard] [HP] [AP]        │
├──────────────────────────────────────┤
│ [SAVE] [CANCEL]                      │
└──────────────────────────────────────┘
```

### 2.5 Admin — Teams Tab
```
┌──────────────────────────────────────────────┐
│              TEAM SETTINGS                   │
├──────────────────────────────────────────────┤
│ Team Size:                                    │
│ NATO:   [ 32 ]  |  RUSSIA: [ 32 ]            │  number inputs
│ Auto-balance: [ENABLED]                       │  toggle
│ Balance threshold: [ 2 ]                      │  auto-bal when diff ≥ this
│                                              │
│ Spawn Settings:                               │
│ Spawn protection: [ 5 ] seconds              │
│ Allow squad spawn: [YES]                      │
│                                              │
│ ┌────────────────────────────────────────┐   │
│ │ Current teams:                        │   │
│ │ NATO (12): Player1, Player2, ...      │   │  read-only list
│ │ RUSSIA (8): Player3, Player4, ...     │   │
│ │ [SWAP PLAYER] [FORCE BALANCE]         │   │
│ └────────────────────────────────────────┘   │
└──────────────────────────────────────────────┘
```

### 2.6 Admin — Config Tab
```
┌──────────────────────────────────────────────┐
│              SERVER CONFIG                   │
├──────────────────────────────────────────────┤
│ ┌──────────────┐ ┌──────────────┐            │
│ │ Game Rules   │ │ Map Settings │            │  category cards
│ │ • FF: ON     │ │ • Rotate: ON │            │
│ │ • Respawn: 5s│ │ • Vote: ON   │            │
│ │ • Tickets:200│ │              │            │
│ └──────────────┘ └──────────────┘            │
│ ┌──────────────┐ ┌──────────────┐            │
│ │ Kit Rules    │ │ Economy      │            │
│ │ • Limit: ON  │ │ • Score: ON  │            │
│ │ • 1 per class│ │ • XP rate: 1│            │
│ └──────────────┘ └──────────────┘            │
│                                              │
│ All values editable inline.                  │
│ [SAVE ALL] [RESET TO DEFAULTS]              │
└──────────────────────────────────────────────┘
```

## Part 3: Availability / Lock System

### 3.1 Lock States
Every selectable item (class, kit, weapon, attachment, vehicle) must have one of:

| State | Visual | Behavior |
|-------|--------|----------|
| `AVAILABLE` | Full color, selectable | Click to select |
| `LOCKED_RANK` | Grayed + rank icon (`★`) + "Req: Rank 3" | Tooltip on hover, click disabled |
| `LOCKED_KIT` | Grayed + kit icon + "Req: Assault Kit" | Tooltip |
| `LOCKED_TEAM` | Grayed + team icon + "NATO only" | Tooltip |
| `LOCKED_MAP` | Grayed + map icon + "Not on this map" | Tooltip |
| `INCOMPATIBLE` | Dimmed + `—` + "Not compatible with M4A1" | Tooltip, not selectable |

### 3.2 Lock Render Mixin
- Each lockable widget draws a semi-transparent dark overlay + icon + text
- Alternatively: row/card just sets `alpha = 0.35 * baseAlpha` and removes click handler
- Lock icon: unicode `\uD83D\uDD12` (🔒) or drawn as small padlock

### 3.3 Auto-filter
- When selecting a weapon, only show attachments compatible with that weapon
- When selecting a kit, only show weapons assigned to that kit
- When selecting a class, only show kits available for that class
- All filtered by: player rank, current team, current map, server config

## Part 4: Data Persistence

### 4.1 Loadout Save Format
```json
{
  "player_uuid": "...",
  "class": "assault",
  "kit": "rifleman",
  "loadout": {
    "primary": { "id": "m4a1", "attachments": { "scope": "red_dot", "barrel": "compensator", "grip": "vertical", "magazine": "30rnd" } },
    "secondary": { "id": "g17", "attachments": {} },
    "special": "m67_frag",
    "grenade": "m67_frag"
  },
  "vehicle": null
}
```
- Saved to `world/teamsystem/playerloadouts/<uuid>.json`
- Auto-save on confirm (no auto-save every frame — only on explicit confirm)
- Load on login, apply on spawn

### 4.2 Kit Config Save Format (admin)
```json
{
  "classes": {
    "assault": {
      "display_name": "Assault",
      "icon": "tacz:assault_icon",
      "kits": {
        "rifleman": {
          "display_name": "Rifleman",
          "description": "Standard assault rifleman",
          "weapons": {
            "primary": ["tacz:m4a1", "tacz:m16a4"],
            "secondary": ["tacz:g17"],
            "special": ["tacz:m67"],
            "grenade": ["tacz:m67"]
          },
          "attachment_limits": {
            "tacz:m4a1": { "scope": ["ironsights", "red_dot", "holographic"], "barrel": ["standard", "compensator"] }
          },
          "requirements": { "rank": 1, "team": null }
        }
      }
    }
  }
}
```
- Saved to `world/teamsystem/kits.json`
- Editable via admin panel Kit Editor tab
- Reloaded on save (no restart needed)

## Part 5: Screen Navigation Flow

```
Login
  └→ TeamSelectionScreen (always shown first)
       ├→ NATO/RUSSIA → ClassSelectionScreen
       │    └→ KitSelectionScreen
       │         ├→ "Select" → close, spawn with loadout
       │         └→ "Customize" → LoadoutScreen
       │              └→ AttachmentPicker (sub-screen per attachment slot)
       └→ SPECTATOR → close, freecam

ESC (in-game)
  └→ BattlefieldPauseScreen
       ├→ Return to Battle
       ├→ Change Class → ClassSelectionScreen (same flow as above)
       ├→ Change Vehicle → VehicleSelectionScreen
       ├→ Settings → SettingsMenuScreen
       └→ Disconnect

Admin (/admin command or button)
  └→ AdminPanel
       ├→ Match Tab
       ├→ Maps Tab
       ├→ Kits Tab
       │    └→ Attachment Permission Matrix (sub-screen per weapon)
       ├→ Teams Tab
       └→ Config Tab
```

## Part 6: Screen Design Specifications

### General Screen Conventions
- All screens: dark background (`BG_SCREEN = 0xCC0A0A0A`), panel (`BG_PANEL = 0xDD141414`)
- Orange accent: 3px top border on panels (`ACCENT = 0xFFE07B00`)
- Title text: orange, uppercase, centered
- Fade-in animation on open (0.25s)
- Smooth slide-in for panels (easeOutCubic)
- All BButton instances: 180×22px default, dark fill, orange border on hover
- Scrollable content: custom scrollbar (thin, 4px, dark with orange thumb), or use `AbstractSelectionList` / `ScrollPanel`
- Locked items: alpha 0.35, lock icon, tooltip with reason
- Back navigation: always a "Back" BButton in bottom-left or top-left
- Responsive: min 854×480, design for 1280×720 default

### Widgets to implement (if not existing):
- **BCard**: A clickable card with icon, title, description, optional lock state. `120×100px` default. Hover glow (lerp border color). Used for class/gun/vehicle selection.
- **BToggle**: Yes/No toggle switch. Animated. `ACCENT` when on, `BORDER` when off.
- **BDropdown**: Dropdown select with scrollable option list. Dark themed.
- **BNumberInput**: Number field with +/- buttons. Dark themed.
- **BTag**: Compact colored tag for attachment display. Small, rounded bg, colored by category.
- **BLockOverlay**: Overlay widget that renders lock state over another widget (dim + icon + tooltip)
- **BScrollPanel**: Scrollable container with custom scrollbar.

## Part 6.b: Custom Tab Overlay (Scoreboard)

Current file: `BattlefieldTabOverlay.java` — rendered when holding Tab key.

### Player Name Format

Replace default Minecraft username with a styled compound name:

```
┌──────────────────────────────────────────────┐
│ [★★ GENERAL]  [ShadowNinja]  [♥]  [NATO]    │  style: rank + callsign + donor + team flag
│                                               │
│  Format in game:                              │
│  ★★ GENERAL | ShadowNinja ♥                  │  compact one-line version
│                                               │
│  Components:                                  │
│  1. RANK PREFIX                               │  colored by rank tier (bronze/silver/gold/diamond)
│     ★ Private                                 │  stars = number of rank
│     ★★ Corporal                              │  or custom icon per rank level
│     ★★★ Sergeant                            │  gradient color option for high ranks
│     ★★★★ Lieutenant                         │
│     ★★★★★ Captain                           │
│     ★★★★★★ Major                           │
│     ☆ General (special)                       │  animated star for highest rank
│                                               │
│  2. CALLSIGN (calltag)                        │  custom name set via admin or player profile
│     "ShadowNinja"                             │  NOT Minecraft username
│     Can include colors/codes if donor         │  e.g., "&cShadow&6Ninja" = red-to-gold gradient
│                                               │
│  3. DONOR BADGE                               │  appears after callsign, animated
│     ♥  (Donor) ── gentle pulse (alpha 0.7→1.0)│
│     ♦  (Premium) ── slow rotate/glow effect   │
│     ✦  (Sponsor) ── shimmer sweep every 3s    │
│     ⭐ (Creator) ── full gold, always bright  │
│     ─── No badge for non-donors ───           │
│                                               │
│  4. TEAM COLOR TINT                           │  entire name row tinted team color:
│     NATO:   slight blue tint (TEAM_NATO)      │  or just left border + squad dot
│     RUSSIA: slight red tint (TEAM_RUSSIA)     │  or full name colored
│     SPECTATOR: gray, italic                   │
└──────────────────────────────────────────────┘
```

### Donation Badge Animation

```java
// Example: gentle pulse for ♥ badge
float pulse = 0.7f + 0.3f * (float)(Math.sin(System.currentTimeMillis() * 0.003) * 0.5 + 0.5);
int badgeAlpha = (int)(pulse * 255);
// draw badge with badgeAlpha at callsign end

// Example: shimmer sweep for ✦ badge (every 3s)
long cycle = System.currentTimeMillis() % 3000;
float shimmerX = (float)cycle / 3000f;  // 0..1 sweep
// draw a highlight gradient that moves across the badge
```

### Full Scoreboard Layout

```
┌──────────────────────────────────────────────┐
│              SCOREBOARD                       │  title centered (fades in/out with alpha)
├──────────────────────────────────────────────┤
│                                              │
│ ┌── NATO ───────────────────── 336 ─ 215 ─┐  │  team header: name + tickets + bar
│ │ ★ Player1 ♥       2.50  128hr   34ms   │  │  columns: RANK | NAME | KDR | TIME | PING
│ │ ★ Shadow ♥        1.60  85hr    42ms   │  │
│ │ ─── Squad Alpha ───                     │  │  squad separator line
│ │ ⭐ Viper           2.50  1khr    18ms   │  │  ⭐ = squad leader
│ │   Noob             0.25  3h      55ms   │  │
│ │ ★ Frost ♥         1.00  45hr    22ms   │  │
│ └──────────────────────────────────────────┘  │
│                                              │
│ ┌── RUSSIA ────────────────── 215 ─ 336 ─┐  │
│ │ ★★★ Bear ♥          7.50  500hr   12ms   │  │  high rank = multiple stars
│ │ ★★ Wolf             1.50  120hr   28ms   │  │
│ │ ─── Squad Bravo ───                      │  │
│ │ ★★ Hawk ♥          1.40  60hr    33ms   │  │
│ │   Fish              0.11  1h      89ms   │  │
│ └──────────────────────────────────────────┘  │
│                                              │
│ ┌── SPECTATORS ──────────────────────────┐  │
│ │ ★ General | Ghost, ★★ Cpl | Pixel ... │  │  muted, smaller text
│ └──────────────────────────────────────────┘  │
│                                              │
│                [TAB to close]                 │  hint text at very bottom
└──────────────────────────────────────────────┘
```

### Design Specs

**Team Header Row:**
- Team name + team-colored left accent bar (`TEAM_NATO` blue / `TEAM_RUSSIA` red)
- Ticket count displayed on right side
- Mini ticket bar (thin, 4px, 100px wide) showing ratio visually
- Background: `BG_PANEL` with team-color tint at 10% alpha

**Player Row:**
- Height: 18px (more room for icons)
- Alternating row bg (very subtle `BORDER_ALT = 0x0AFFFFFF`)
- Squad grouping: rows from same squad have a 2px left border in squad-unique color (cycling palette)
- Current player: orange 2px left border + slightly brighter background
- Columns:
  - **RANK ICON**: 22px — small icon/text representing rank tier (e.g., chevron `^` / star `★` / eagle `🦅` depending on rank level). Drawn as colored character or small texture. Color gradient by rank.
  - **NAME**: 220px — callsign (14px, white or donor-colored) + donor badge (12px, animated)
  - **KDR**: 50px right-aligned, white — calculated as `kills / Math.max(1, deaths)`, formatted as `2.50` or `0.33`. Colored green if >1.0, yellow if >0.5, red if ≤0.5.
  - **PLAYTIME**: 40px right-aligned, `TEXT_SECONDARY` — just text like `10hr`, `5h`, `1khr` (no icon). Hours rounded, short format. If ≥1000 → `1khr`, if ≥100 → `128hr`, if ≥10 → `45hr`, if <10 → `5h`.
  - **PING**: 50px right-aligned, green if <50 / yellow if 50-100 / red if >100 (value in ms)

**Squad Separator:**
- Horizontal line across full width
- Squad name in middle (e.g., "─── Squad Alpha ───")
- Small icon: ⭐ for squad leader
- Text: `TEXT_SECONDARY`, smaller font (10px if available, else 12px)

**Spectator Section:**
- Smaller text (maybe 10px), `TEXT_MUTED` color
- `─── SPECTATORS (3) ───` header
- One line: comma-separated names with rank prefix (no donor badge)
- If >8 spectators: "Player1, Player2, ..., +5 more"
- Max height: 3 lines, then "..."

**Your Stats Highlight:**
- Current player's row gets:
  - Orange left border (2px, `ACCENT`)
  - Slightly brighter bg (BG_PANEL instead of BG_SURFACE)
  - "YOU" tag after name (small, orange bg, 2px padding)

**Animations:**
- Tab opens: fade-in (lerp alpha 0→1 over 300ms)
- Tab closes: fade-out (lerp alpha 1→0 over 200ms)
- Donor badges: continuous gentle animation (pulse/glow/shimmer based on tier)
- No per-row animation (keep it performant)

**Data Source:**
- `ClientTeamData.playerDataMap` — `Map<UUID, PlayerListEntry>` with fields:
  - `callsign()` — custom display name
  - `rank()` — ordinal → `Rank.fromOrdinal()`
  - `squad()` — squad name
  - `teamOrdinal()` — NATO/RUSSIA/SPECTATOR
  - `kills()`, `deaths()` — K/D stats
  - Plus: `donorTier()` (0=none, 1=donor, 2=premium, 3=sponsor, 4=creator)
  - Plus: `score()` — match score
  - Plus: `ping()` — latency ms

## Part 7: Admin Panel Detailed UX

### Navigation
- Left sidebar: fixed ~50px, icons only (expands to 160px with text on hover)
- Icons (unicode or text):
  - ⚡ Match (lightning bolt or play icon)
  - 🗺️ Maps
  - ⚔️ Kits (crossed swords)
  - 👥 Teams (silhouettes)
  - ⚙️ Config (gear)
- Active tab: orange left border indicator
- Hover: slide-out text animation

### Tab Persistence
- Switching tabs does NOT reset sub-selections
- Kit tab remembers which class+kit was selected
- Maps tab remembers scroll position

### Quick Action Bar
- Horizontal bar below header
- Big buttons with icons:
  - [▶ START] green `STATUS_OK` bg
  - [⟳ RESTART] yellow `STATUS_WARN` bg
  - [■ STOP] red `STATUS_DANGER` bg
  - [🗳 FORCE VOTE] orange `ACCENT` bg
- Confirmation dialog (BConfirmPopup) for destructive actions

## Part 8: Match Results Screen

Shown to all players when a match ends (winner decided).

```
┌──────────────────────────────────────────────┐
│                                              │
│              MATCH OVER                      │  big title, orange, uppercase
│                                              │
│       ┌──────────────────────────┐           │
│       │    ★  NATO WINS ★       │           │  winning team name + color
│       │     NATO 200 — 156 RUS   │           │  final ticket count
│       │      Duration: 14:32     │           │
│       └──────────────────────────┘           │
│                                              │
│       ─── TOP PLAYERS ───                    │
│                                              │
│  ┌────────────────────────────────────────┐  │
│  │  1  ★★★ Captain | Bear ♥   32-5  6400 │  │  MVP row (gold)
│  │  2  ★★★★ General | Wolf    25-8  5000 │  │  2nd (silver tint)
│  │  3  ★★ Corporal | Shadow ♥ 20-10 3800 │  │  3rd (bronze tint)
│  │  4  ★★★ Sergeant | Viper   18-6  3200 │  │
│  │  5  ★ Private | Noob        5-20  800 │  │
│  └────────────────────────────────────────┘  │
│                                              │
│       Next map: Лесецк                        │
│       Starting in: 45 seconds                 │  timer counting down
│                                              │
│       [CONTINUE]                             │  big button, exits to spawn/lobby
│       [SPECTATE]                             │  smaller, enters spectator mode
│                                              │
└──────────────────────────────────────────────┘
```

### Design Specs
- **Winner card**: centered, slightly elevated (larger panel, bright team-colored accent). NATO = blue border `TEAM_NATO`, RUSSIA = red border `TEAM_RUSSIA`.
- **MVP medal icons** next to rank:
  - 🥇 Gold (MVP) — row has subtle gold glow/shine
  - 🥈 Silver (2nd) — slight silver tint to text
  - 🥉 Bronze (3rd) — slight bronze tint to text
- **Player rows**: same format as Tab (rank + callsign + donor badge + KDR + playtime + ping), but only top 10-15 players by score
- **Your row highlighted** if you're in top 10
- **Next map + timer**: orange text, countdown updates every second
- **Animations**:
  - Screen slides in from bottom (easeOutCubic, 500ms)
  - Winning team card has a slow pulsing glow (alpha 0.8→1.0)
  - MVP rows fade in with stagger delay (100ms each)
  - Timer ticks smoothly
- **Auto-close**: when timer reaches 0, auto-transitions to next match (or lobby). "CONTINUE" button also works.
- **Background**: slightly blurred/darker than normal overlay

## Part 9: Spawn Screen

Shown when player is dead and ready to respawn. Replaces the vanilla death/respawn screen.

```
┌──────────────────────────────────────────────┐
│         YOU ARE DEAD                         │  large text, red tint
│         Respawn in: 5 seconds                │  countdown (changes to "SPAWN" when ready)
├──────────────────────────────────────────────┤
│                                              │
│   ┌── SPAWN POINTS ──────────────────────┐   │  highlighted option (current best)
│   │ ● TEAM BASE               [2.0s]    │   │  green dot, shortest timer
│   │   Squad Alpha Spawn        [7.0s]    │   │  available if squad alive & leader alive
│   │   Vehicle: Transport Jeep  [5.0s]    │   │  available if vehicle has free seat
│   │   Squad Bravo Spawn        [—]       │   │  grayed, no squad leader alive
│   └──────────────────────────────────────┘   │
│                                              │
│   ┌── QUICK KIT ─────────────────────────┐   │
│   │ ASSAULT      [RIFLEMAN]    [CHANGE]  │   │  current kit shown, click to change
│   │ Primary: M4A1 + Red Dot              │   │  compact loadout preview
│   │ Secondary: G17                       │   │
│   └──────────────────────────────────────┘   │
│                                              │
│   ┌── VEHICLES AVAILABLE ────────────────┐   │  shown only if vehicles unlocked
│   │  🚁 Transport (2 seats)  [SPAWN]     │   │
│   │  🚗 Jeep (1 seat)        [SPAWN]     │   │
│   └──────────────────────────────────────┘   │
│                                              │
│   [SPAWN]                                     │  big orange button, activates when timer=0
└──────────────────────────────────────────────┘
```

### Spawn Point States
| State | Visual | Tooltip |
|-------|--------|---------|
| `AVAILABLE` | Green dot, timer shown | Click to select |
| `COOLDOWN` | Orange dot, timer counting | "Available in Xs" |
| `NO_LEADER` | Red dot, `—` | "No squad leader alive" |
| `FULL` | Red dot, `—` | "Vehicle full" |
| `LOCKED` | Gray dot, `—` | "Requires rank 3" |

### Design Specs
- **Respawn timer**: large countdown in center-top, orange when >3s, green when ≤3s, flashing "SPAWN" when ready
- **Spawn point list**: radio-list style, click to select, highlighted with orange left border
- **Timer per spawn point**: calculated as `base_respawn_time + spawn_point_delay` — displayed in brackets
- **Quick kit section**: current class+kit shown, "CHANGE" opens ClassSelectionScreen/KitSelectionScreen (compact mode)
- **Vehicles section**: only if player has vehicles available (rank check + team has vehicles)
- **Default selection**: auto-selects fastest available spawn point
- **SPAWN button**: disabled (gray) while timer > 0, enables and pulses when ready
- **Animation**: screen fades in from red tint (death feel) → normal dark overlay over ~1s
- **ESC**: opens BattlefieldPauseScreen (but you're dead, only return/team/spectate options)

## Part 10: Admin Panel — Permission System

### Admin Ranks

| Level | Name | Access |
|-------|------|--------|
| 0 | NONE | No access |
| 1 | MOD | Can kick/ban players, force start/stop match, basic info |
| 2 | ADMIN | Everything MOD + kit editor, map management, team balance |
| 3 | OWNER | Full access, config editing, permission management |

### Permission Nodes (granular)
```
teamsystem.admin.view          — view admin panel (read-only)
teamsystem.admin.match.start   — force start match
teamsystem.admin.match.stop    — force stop/end match
teamsystem.admin.match.restart — restart match
teamsystem.admin.match.timer   — change match timer
teamsystem.admin.map.add       — add maps to pool
teamsystem.admin.map.remove    — remove maps
teamsystem.admin.kit.view      — view kit config (read-only)
teamsystem.admin.kit.edit      — edit kits, weapons, attachments
teamsystem.admin.team.balance  — force team balance, swap players
teamsystem.admin.team.size     — change team max size
teamsystem.admin.config.edit   — edit server config (game rules)
teamsystem.admin.permissions   — manage admin permissions (OWNER only)
teamsystem.admin.kick          — kick players
teamsystem.admin.ban           — ban players
```

### Admin Panel — Permissions Tab

New tab in admin panel (only visible to OWNER level):

```
┌──────────────────────────────────────────────┐
│            PERMISSIONS                       │
├──────────┬───────────────────────────────────┤
│ ADMINS   │  PLAYER: ShadowNinja              │
│ LIST     │  ─────────────────────             │
│          │  Level: [ ADMIN ▼     ]           │  dropdown: MOD / ADMIN / OWNER
│ [MOD]    │                                    │
│  Shadow  │  Permissions:                      │
│          │  ☑ Match Control                   │  grouped checkboxes
│ [ADMIN]  │  ☑ Map Management                  │  expandable categories
│  Bear    │  ☐ Kit Editor                      │
│  Wolf    │  ☐ Team Settings                   │
│ [ADMIN]  │  ☐ Server Config                   │
│  Hawk    │  ☐ Permissions                     │  only OWNER can toggle this
│          │                                    │
│ [ADD]    │  [SAVE] [REVOKE ACCESS]           │
└──────────┴───────────────────────────────────┘
```

### Design Specs
- **Left column**: player list grouped by admin level (MOD / ADMIN / OWNER), highlighted = selected
- **Right column**: selected player's permission editor
- **Level dropdown**: MOD / ADMIN / OWNER — OWNER can only be set by another OWNER
- **Permission checkboxes**: grouped by category, expandable sections with arrow `▶` / `▼`
- **"ADD" button**: opens player search popup (text input, auto-complete from online player list)
- **"REVOKE ACCESS"**: removes admin, confirmation dialog required
- **Client-side check**: if player's level changes, panel refreshes immediately
- **Server-side enforcement**: all commands check permission on server, not just hiding UI

### Data Storage
```json
// Saved to world/teamsystem/admin_permissions.json
{
  "admins": [
    {
      "uuid": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
      "name": "ShadowNinja",
      "level": "ADMIN",
      "permissions": [
        "teamsystem.admin.match.*",
        "teamsystem.admin.map.view",
        "teamsystem.admin.kit.view",
        "teamsystem.admin.team.balance",
        "teamsystem.admin.kick"
      ],
      "granted_by": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
      "granted_at": "2026-05-18T07:00:00Z"
    }
  ]
}
```
- Loaded on server start
- Reloaded on save (no restart)
- All admin commands check against this file using player UUID

Provide:
1. **Full Java class structure** — for each screen, list:
   - Class name, extends what
   - Key fields
   - `init()` layout (widget positions as formulas using `width/height`)
   - `render()` additional drawing (background, title, animations)
   - Any inner classes needed
2. **Widget specs** — BCard, BToggle, BDropdown, BNumberInput, BTag, BLockOverlay, BScrollPanel
3. **Data model classes** — `PlayerLoadout`, `KitConfig`, `ClassConfig`, `AttachmentLimit`
4. **Save/load logic** — Gson serialization, file paths
5. **Lock system** — How to determine lock state, where to check
6. **Navigation flow** — Which screen opens which, how data passes between them
7. **Suggested file paths** — Where each new `.java` file should go in the package

For each screen, provide the **exact layout code** (x, y, width, height as functions of screen `width`/`height`), the button actions, and any animation logic.

Use existing `UITheme` colors, `BButton`, `AnimationHelper`. Keep the dark semi-transparent Battlefield aesthetic. ***NOT*** Minecraft vanilla style — this is Battlefield-style UI: sharp, dark, orange accents, no rounded everything, professional mil-sim look.
