# Kit Customization Screen — Implementation Plan

## Overview
Replace the current `KitLoadoutScreen` with a full-featured `KitCustomizationScreen` that has a two-column layout (weapon preview + config panel), slot cycling, attachment selection, armor slots (incl. Curios), and auto-ammo.

## Phase 1: Data Model (KitConfig.java)

### 1.1 Add `KitArmor` class to `KitConfig`
```java
public static class KitArmor {
    public List<String> helmet = new ArrayList<>();
    public List<String> chestplate = new ArrayList<>();
    public List<String> backpack = new ArrayList<>();
    public List<String> shoulderpads = new ArrayList<>();
}
```

### 1.2 Add `armor` field to `KitDef`
```java
public KitArmor armor = new KitArmor();
```

### 1.3 Register new items
- `warbornexplosives:*` IDs for grenades (except `tm_62m`)
- `warborn:*` IDs for armor
- Update `ItemResolver.resolve()` to handle TACZ addon IDs (daffas_arsenal, maxstuff) — already works via GunId NBT fallback

## Phase 2: New Screen — KitCustomizationScreen.java

### Layout
```
┌─────────────────────────────────────────────────────────┐
│  Header: "KIT NAME" (localized)                        │
├──────────────────┬──────────────────────────────────────┤
│  Preview Panel   │  Config Panel (scrollable)          │
│  (200px fixed)   │                                      │
│                  │  ├ SlotCard: Primary  ───── [◄][►]  │
│  ┌────────────┐  │  ├ SlotCard: Secondary ─── [◄][►]  │
│  │ Weapon     │  │  ├ SlotCard: Special  ──── [◄][►]  │
│  │ Render     │  │  ├ SlotCard: Grenade  ──── [◄][►]  │
│  │ ◄    ►     │  │                                      │
│  │ M4A1       │  │  ├── ATTACHMENTS ────────            │
│  │ 5.56mm     │  │  │  Scope: [Red Dot] [Holo]          │
│  └────────────┘  │  │  Barrel: [Standard] [Supp]        │
│                  │  │                                      │
│                  │  ├── ARMOR ───────────────────         │
│                  │  │  Helmet ▓ [◄][►]                    │
│                  │  │  Chest  ▓ [◄][►]                    │
│                  │  │  Back   ▓ [◄][►]                    │
│                  │  │  Pads   ▓ [◄][►]                    │
├──────────────────┴──────────────────────────────────────┤
│  [← BACK]                    [✓ SAVE & DEPLOY]          │
└─────────────────────────────────────────────────────────┘
```

### 2.1 PreviewPanel
- `renderPreviewItem()`: renders selected weapon via `GuiGraphics.renderItem()` centered, ~3x scale using PoseStack
- `◄` / `►` buttons (20x20px) to cycle through weapons in the active slot
- Below: item display name + ammo info (auto-resolve from TACZ NBT or superb registry)

### 2.2 SlotCards
- 4 cards: Primary, Secondary, Special, Grenade
- Each card shows: slot icon/name, current selected weapon name (human-readable, not ID)
- `[◄]` / `[►]` buttons (18x18px) cycle through the List<String> for that slot
- Click on card body = activate that slot (preview switches to it)
- Active slot = highlighted border (ACCENT)

### 2.3 AttachmentSection (visible only when Primary slot active)
- Shows only if the selected weapon ID has an entry in `attachment_limits`
- Categories (scope, barrel, grip, magazine) as rows
- Each category = label + horizontal chip buttons (pill style, 24px height)
- Currently selected chip = ACCENT fill
- Selection stored in `Map<String, String> selectedAttachments`

### 2.4 ArmorSection (visible if kit config has armor lists)
- 4 rows: Helmet, Chestplate, Backpack, Shoulderpads
- Each row: icon + item name + [◄][►] for cycling
- Backpack goes to Curios `back` slot
- Shoulderpads go to Curios `body` slot

### 2.5 Footer
- `[← Back]` — returns to KitSelectionScreen
- `[✓ Save & Deploy]` — saves all selections, sends to server, closes screen

### 2.6 Data flow on save
Combine selections into a JSON:
```json
{
  "Primary": "tacz:m4a1",
  "Secondary": "tacz:glock_17",
  "Special": "superbwarfare:taser",
  "Grenade": "warbornexplosives:m67",
  "Helmet": "warborn:nato_helmet",
  "Chestplate": "warborn:nato_shturmovik_chestplate",
  "Backpack": "warborn:nato_sqad_leader_backpack",
  "Shoulderpads": "warborn:nato_shoulderpads",
  "Attachments": {
    "tacz:m4a1": {
      "scope": "red_dot",
      "barrel": "suppressor"
    }
  }
}
```

Send via existing `KitSavePacket` (just a string field) or new `KitCustomizationSavePacket`.

## Phase 3: Server-Side Apply (KitConfigServerHelper.java)

### 3.1 Parse extended loadout JSON
Add armor fields to `PlayerLoadout`:
```java
public static class LoadoutSlots {
    public WeaponSlot primary;
    public WeaponSlot secondary;
    public String special;
    public String grenade;
    public String helmet;
    public String chestplate;
    public String backpack;
    public String shoulderpads;
    public Map<String, Map<String, String>> attachments;
}
```

### 3.2 Apply armor
- Helmet → `player.getInventory().armor.set(3, stack)`
- Chestplate → `player.getInventory().armor.set(2, stack)`
- Backpack → Curios `back` slot (use existing `KitManager.setCurioSlot(player, "back", stack)`)
- Shoulderpads → Curios `body` slot

### 3.3 Auto-ammo
After giving weapons, check each and give ammo:
- TACZ guns already have magazines with ammo in NBT → no action needed
- Superb Warfare guns → give appropriate ammo box:
  - `superbwarfare:handgun_ammo_box` for handguns (glock, m1911, mp_443)
  - `superbwarfare:rifle_ammo_box` for rifles
  - `superbwarfare:sniper_ammo_box` for snipers
  - `superbwarfare:shotgun_ammo_box` for shotguns
  - `superbwarfare:heavy_ammo` for heavy weapons (rpg, javelin, igla)
- Ammo goes into hotbar slot 4-7 or inventory

### 3.4 Apply attachments to TACZ guns
For TACZ modern_kinetic_gun, attachment selections get written into the item's NBT:
```nbt
{
  GunId: "tacz:m4a1",
  Attachment: {
    scope: "tacz:scope_red_dot",
    barrel: "tacz:barrel_suppressor"
  }
}
```
This requires knowing the TACZ attachment item IDs for each attachment name — may need a mapping table.

## Phase 4: Navigation Updates

### 4.1 Update KitSelectionScreen
Change the "Customize" button to open `KitCustomizationScreen` instead of `KitLoadoutScreen`.

### 4.2 Remove/unregister KitLoadoutScreen
Once `KitCustomizationScreen` is working, the old `KitLoadoutScreen` can be kept or removed.

## Phase 5: Verification
1. Start server with `kits.json` containing all weapon types (TACZ, SBW, warborn, warbornexplosives)
2. Join game, select team → class → kit → see the new customization screen
3. Click through all slots, verify ◄► cycling works
4. Verify preview updates on hover/click
5. Save & Deploy → verify server applies weapons + armor + ammo correctly
6. Verify Curios slots get backpack/shoulderpads
7. Test with different language settings (ru/en) for bilingual names

## Key Files to Modify
| File | Change |
|------|--------|
| `data/KitConfig.java` | Add `KitArmor` class + `armor` field to `KitDef` |
| `client/gui/screen/KitCustomizationScreen.java` | **NEW** — full screen implementation |
| `network/KitSavePacket.java` | No changes needed (sends raw JSON string) |
| `data/KitConfigServerHelper.java` | Parse armor + attachments; apply armor + curios + ammo |
| `data/ItemResolver.java` | Minor — ensure warborn IDs resolve correctly |
| `core/KitManager.java` | Add `setCurioSlot(player, slot, stack)` method |
| `client/gui/screen/KitSelectionScreen.java` | Change `KitLoadoutScreen` → `KitCustomizationScreen` |
