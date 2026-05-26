package com.pigeostudios.pwp.client.gui.screen;

import com.pigeostudios.pwp.client.gui.I18n;
import com.pigeostudios.pwp.client.gui.UITheme;
import com.pigeostudios.pwp.client.gui.component.*;
import com.pigeostudios.pwp.data.KitConfig;
import com.pigeostudios.pwp.network.PacketHandler;
import com.pigeostudios.pwp.network.KitSavePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.*;

public class KitLoadoutScreen extends Screen {

    private final String classId;
    private final String kitId;

    private final List<SlotRow> weaponRows = new ArrayList<>();
    private final List<SlotRow> armorRows  = new ArrayList<>();
    private final Map<String, String> selections = new LinkedHashMap<>();
    private final Map<String, String> armorSelections = new LinkedHashMap<>();

    private float fadeAlpha  = 0f;
    private long  openTime;
    private boolean saved    = false;
    private int    savedTimer = 0;
    private TopBar topBar;

    public KitLoadoutScreen(String classId, String kitId) {
        super(Component.literal("Kit Loadout"));
        this.classId = classId;
        this.kitId   = kitId;
    }

    @Override
    protected void init() {
        openTime = System.currentTimeMillis();
        topBar   = new TopBar();
        loadKitData();
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private void loadKitData() {
        weaponRows.clear(); armorRows.clear();

        KitConfig cfg = KitConfig.get();
        if (cfg == null) return;
        KitConfig.ClassConfig cl = cfg.classes.get(classId);
        if (cl == null) return;
        KitConfig.KitDef kit = cl.kits.get(kitId);
        if (kit == null) return;

        String[] icons = {"\uD83D\uDD2B", "\uD83D\uDD27", "\uD83D\uDC8A", "\uD83D\uDCA3"};
        String[][] weaponSlots = {
            {"primary", I18n.get("pwp.ui.kit_loadout.slot_primary")},
            {"secondary", I18n.get("pwp.ui.kit_loadout.slot_secondary")},
            {"special", I18n.get("pwp.ui.kit_loadout.slot_special")},
            {"grenade", I18n.get("pwp.ui.kit_loadout.slot_grenade")},
        };

        if (kit.weapons != null) {
            for (int si = 0; si < weaponSlots.length; si++) {
                String key = weaponSlots[si][0];
                String label = weaponSlots[si][1];
                List<String> opts = getWeaponList(kit.weapons, key);
                selections.put(key, opts.isEmpty() ? "" : opts.get(0));

                SlotRow row = new SlotRow(label, si < icons.length ? icons[si] : "\u2022", opts);
                String finalKey = key;
                row.setOnChanged(() -> selections.put(finalKey, row.getSelectedValue()));
                weaponRows.add(row);
            }
        }
    }

    private static List<String> getWeaponList(com.pigeostudios.pwp.data.KitConfig.KitWeapons w, String key) {
        return switch (key) {
            case "primary"   -> w.primary   != null ? w.primary   : List.of();
            case "secondary" -> w.secondary != null ? w.secondary : List.of();
            case "special"   -> w.special   != null ? w.special   : List.of();
            case "grenade"   -> w.grenade   != null ? w.grenade   : List.of();
            default          -> List.of();
        };
    }

    @Override
    public void tick() {
        if (saved && ++savedTimer > 30) { onClose(); }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        fadeAlpha = Math.min(1f, (System.currentTimeMillis() - openTime) / 250f);
        int alpha = (int)(fadeAlpha * 0xFF);

        g.fill(0, 0, width, height,
            AnimationHelper.withAlpha(UITheme.BG_SCREEN, (int)(fadeAlpha * 0xCC)));

        topBar.render(g, width, I18n.get("pwp.ui.kit_loadout.title"),
            com.pigeostudios.pwp.client.ClientTeamData.localPlayerWC,
            com.pigeostudios.pwp.client.ClientTeamData.localPlayerBC,
            com.pigeostudios.pwp.client.ClientTeamData.localPlayerRank);

        BreadcrumbNav.render(g, width, TopBar.TOP_H,
            List.of(I18n.get("pwp.ui.kit_loadout.breadcrumb_spawn"), I18n.get("pwp.ui.kit_loadout.breadcrumb_classes"), I18n.get("pwp.ui.kit_loadout.breadcrumb_kits"), I18n.get("pwp.ui.kit_loadout.breadcrumb_loadout")), alpha);

        KitConfig cfg = KitConfig.get();
        KitConfig.KitDef currentKit = cfg != null && cfg.classes != null && cfg.classes.containsKey(classId)
            ? cfg.classes.get(classId).kits.get(kitId) : null;
        String kitName = currentKit != null && currentKit.display_name != null
            ? I18n.localize(currentKit.display_name).toUpperCase() : kitId.toUpperCase();
        String kitDesc = currentKit != null && currentKit.description != null
            ? I18n.localize(currentKit.description) : "";

        int topH = TopBar.TOP_H + BreadcrumbNav.BREADCRUMB_H;
        int contentY = topH + 12;
        int slotW = Math.min(500, width - 80);
        int slotX = (width - slotW) / 2;

        g.drawString(font, kitName, slotX, contentY,
            AnimationHelper.withAlpha(UITheme.ACCENT, alpha));
        if (!kitDesc.isEmpty()) {
            g.drawString(font, kitDesc, slotX, contentY + 12,
                AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int)(fadeAlpha * 200)));
        }
        contentY += 28;
        com.pigeostudios.pwp.client.gui.component.AccentLine.draw(g, slotX, contentY, Math.min(slotW, 200), fadeAlpha);
        contentY += 16;

        for (SlotRow row : weaponRows) {
            row.render(g, slotX, contentY, slotW, true, mx, my, fadeAlpha);
            contentY += SlotRow.SLOT_H + 6;
        }

        contentY += 16;
        int btnW = Math.min(250, slotW);
        int btnX = (width - btnW) / 2;

        boolean btnHov = mx >= btnX && mx <= btnX + btnW && my >= contentY && my <= contentY + 32;

        if (saved) {
            g.fill(btnX, contentY, btnX + btnW, contentY + 32,
                AnimationHelper.withAlpha(UITheme.STATUS_OK, (int)(fadeAlpha * 0xDD)));
            String svTxt = I18n.get("pwp.ui.kit_loadout.saved");
            g.drawString(font, svTxt, btnX + btnW / 2 - font.width(svTxt) / 2, contentY + 11, 0xFFFFFFFF);
        } else {
            int btnBg = AnimationHelper.blendColors(UITheme.ACCENT, 0xFFFF8C0A, btnHov ? 1f : 0f);
            g.fill(btnX, contentY, btnX + btnW, contentY + 32,
                AnimationHelper.withAlpha(btnBg, alpha));
            g.fill(btnX, contentY, btnX + 2, contentY + 32,
                AnimationHelper.withAlpha(0x33000000, alpha));
            String svTxt = I18n.get("pwp.ui.kit_loadout.save_btn");
            g.drawString(font, svTxt, btnX + btnW / 2 - font.width(svTxt) / 2, contentY + 11, 0xFFFFFFFF);
        }

        super.render(g, mx, my, pt);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn != 0) return super.mouseClicked(mx, my, btn);

        int topH = TopBar.TOP_H + BreadcrumbNav.BREADCRUMB_H;
        int contentY = topH + 12 + 28 + 16 + 16;
        int slotW = Math.min(500, width - 80);
        int slotX = (width - slotW) / 2;

        for (SlotRow row : weaponRows) {
            if (my >= contentY && my <= contentY + SlotRow.SLOT_H) {
                row.handleClick(mx, slotX, contentY, slotW);
                selections.put(row.getSlotKey(), row.getSelectedValue());
                return true;
            }
            contentY += SlotRow.SLOT_H + 6;
        }

        contentY += 16;
        int btnW = Math.min(250, slotW);
        int btnX = (width - btnW) / 2;
        if (mx >= btnX && mx <= btnX + btnW && my >= contentY && my <= contentY + 32) {
            saveLoadout();
            return true;
        }

        return super.mouseClicked(mx, my, btn);
    }

    private void saveLoadout() {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (var e : selections.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(escapeJson(e.getKey())).append("\":\"").append(escapeJson(e.getValue())).append("\"");
        }
        for (var e : armorSelections.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"armor_").append(escapeJson(e.getKey())).append("\":\"").append(escapeJson(e.getValue())).append("\"");
        }
        sb.append("}");
        PacketHandler.CHANNEL.sendToServer(new KitSavePacket(classId + ":" + kitId, sb.toString()));
        saved = true;
        savedTimer = 0;
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @Override
    public void onClose() { SpawnScreenHelper.reopen(); }
}
