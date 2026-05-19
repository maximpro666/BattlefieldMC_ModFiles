package com.yourmod.teamsystem.client.gui.screen;

import com.yourmod.teamsystem.client.gui.UITheme;

import com.yourmod.teamsystem.data.KitConfig;
import com.yourmod.teamsystem.network.PacketHandler;
import com.yourmod.teamsystem.network.KitSavePacket;
import com.yourmod.teamsystem.client.gui.component.BButton;
import com.yourmod.teamsystem.client.gui.component.AnimationHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.*;

public class KitLoadoutScreen extends Screen {

    private static final int COLOR_BG      = 0xFF1A1A2E;
    private static final int COLOR_PANEL   = 0xFF16213E;
    private static final int COLOR_ORANGE  = 0xFFE94560;
    private static final int COLOR_SLOT_BG = 0xFF0F3460;
    private static final int COLOR_BORDER  = 0xFF533483;
    private static final int COLOR_TEXT    = 0xFFFFFFFF;
    private static final int COLOR_SUBTEXT = 0xFF888888;

    private static final String[] SLOT_KEYS = {
        "Primary", "Secondary", "Special", "Grenade"
    };

    private final String classId;
    private final String kitId;
    private final int SLOT_W = 180;
    private final int SLOT_H = 20;
    private final int GAP = 2;

    private final Map<String, List<String>> weaponOptions = new LinkedHashMap<>();
    private final Map<String, String> selections = new LinkedHashMap<>();
    private final Map<String, Integer> selectionIndex = new LinkedHashMap<>();

    private float fadeAlpha = 0f;
    private long openTime;
    private int hoveredSlot = -1;
    private int scrollOffset = 0;

    public KitLoadoutScreen(String classId, String kitId) {
        super(Component.literal("Kit Loadout"));
        this.classId = classId;
        this.kitId = kitId;
    }

    @Override
    protected void init() {
        openTime = System.currentTimeMillis();
        loadWeaponOptions();

        int panelW = SLOT_W + 40;
        int panelX = width / 2 - panelW / 2;
        int totalH = weaponOptions.size() * (SLOT_H + GAP) + 60;
        int panelY = height / 2 - totalH / 2;
        if (panelY < 20) panelY = 20;

        addRenderableWidget(new BButton(
            panelX + panelW / 2 - 60, panelY + totalH - 25, 120, 20,
            Component.literal("Save Loadout"), btn -> saveLoadout()
        ));
    }

    private void loadWeaponOptions() {
        weaponOptions.clear();
        selections.clear();
        selectionIndex.clear();

        KitConfig cfg = KitConfig.get();
        if (cfg == null) return;

        KitConfig.ClassConfig cl = cfg.classes.get(classId);
        if (cl == null) return;

        KitConfig.KitDef kit = cl.kits.get(kitId);
        if (kit == null) return;

        if (kit.weapons.primary != null && !kit.weapons.primary.isEmpty()) {
            weaponOptions.put("Primary", kit.weapons.primary);
            selectionIndex.put("Primary", 0);
        }
        if (kit.weapons.secondary != null && !kit.weapons.secondary.isEmpty()) {
            weaponOptions.put("Secondary", kit.weapons.secondary);
            selectionIndex.put("Secondary", 0);
        }
        if (kit.weapons.special != null && !kit.weapons.special.isEmpty()) {
            weaponOptions.put("Special", kit.weapons.special);
            selectionIndex.put("Special", 0);
        }
        if (kit.weapons.grenade != null && !kit.weapons.grenade.isEmpty()) {
            weaponOptions.put("Grenade", kit.weapons.grenade);
            selectionIndex.put("Grenade", 0);
        }

        for (Map.Entry<String, List<String>> entry : weaponOptions.entrySet()) {
            String first = entry.getValue().isEmpty() ? "" : entry.getValue().get(0);
            selections.put(entry.getKey(), first);
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        scrollOffset = (int) Math.max(0, Math.min(scrollOffset - delta * 20, Math.max(0, weaponOptions.size() * (SLOT_H + GAP) - height + 100)));
        return true;
    }

    private void saveLoadout() {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : selections.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\"");
        }
        sb.append("}");
        PacketHandler.CHANNEL.sendToServer(new KitSavePacket(classId + ":" + kitId, sb.toString()));
        onClose();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        fadeAlpha = Math.min(1f, (System.currentTimeMillis() - openTime) / 250f);
        g.fill(0, 0, width, height, AnimationHelper.withAlpha(COLOR_BG, (int)(fadeAlpha * 0xCC)));

        int panelW = SLOT_W + 40;
        int panelX = width / 2 - panelW / 2;
        int listH = weaponOptions.size() * (SLOT_H + GAP);
        int panelH = listH + 60;
        int panelY = height / 2 - panelH / 2;
        if (panelY < 20) panelY = 20;

        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, AnimationHelper.withAlpha(COLOR_PANEL, (int)(fadeAlpha * 0xDD)));
        g.fill(panelX, panelY, panelX + panelW, panelY + 3, AnimationHelper.withAlpha(COLOR_ORANGE, (int)(fadeAlpha * 255)));

        String title = kitId.toUpperCase() + " LOADOUT";
        int tw = font.width(title);
        g.drawString(font, title, width / 2 - tw / 2, panelY + 8, AnimationHelper.withAlpha(COLOR_ORANGE, (int)(fadeAlpha * 255)));

        hoveredSlot = -1;
        int i = 0;
        for (String slotKey : weaponOptions.keySet()) {
            int sy = panelY + 25 + i * (SLOT_H + GAP);
            int sx = panelX + 10;
            boolean hov = mx >= sx && mx <= sx + SLOT_W && my >= sy && my <= sy + SLOT_H;
            if (hov) hoveredSlot = i;
            drawSlot(g, sx, sy, slotKey, hov, fadeAlpha);
            i++;
        }

        super.render(g, mx, my, pt);
    }

    private void drawSlot(GuiGraphics g, int x, int y, String slotKey, boolean hov, float alpha) {
        int bg = hov ? AnimationHelper.withAlpha(0xFF1A3A6E, (int)(alpha * 255))
                     : AnimationHelper.withAlpha(COLOR_SLOT_BG, (int)(alpha * 255));
        int brd = hov ? AnimationHelper.withAlpha(COLOR_ORANGE, (int)(alpha * 200))
                      : AnimationHelper.withAlpha(COLOR_BORDER, (int)(alpha * 150));

        g.fill(x, y, x + SLOT_W, y + SLOT_H, bg);
        g.fill(x, y, x + SLOT_W, y + 1, brd);
        g.fill(x, y + SLOT_H - 1, x + SLOT_W, y + SLOT_H, brd);

        String sel = selections.getOrDefault(slotKey, "\u2014");
        String shortId = sel.contains(":") ? sel.substring(sel.indexOf(':') + 1) : sel;
        String label = slotKey + ": " + shortId;

        g.drawString(font, label, x + 4, y + 5, AnimationHelper.withAlpha(COLOR_TEXT, (int)(alpha * 255)));

        if (hov) {
            g.drawString(font, "< >", x + SLOT_W - 20, y + 5, AnimationHelper.withAlpha(COLOR_ORANGE, (int)(alpha * 255)));
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (weaponOptions.isEmpty()) return super.mouseClicked(mx, my, btn);

        int panelW = SLOT_W + 40;
        int panelX = width / 2 - panelW / 2;
        int listH = weaponOptions.size() * (SLOT_H + GAP);
        int panelH = listH + 60;
        int panelY = height / 2 - panelH / 2;
        if (panelY < 20) panelY = 20;

        int i = 0;
        for (String slotKey : weaponOptions.keySet()) {
            int sy = panelY + 25 + i * (SLOT_H + GAP);
            int sx = panelX + 10;
            if (mx >= sx && mx <= sx + SLOT_W && my >= sy && my <= sy + SLOT_H) {
                List<String> options = weaponOptions.get(slotKey);
                if (options == null || options.isEmpty()) return true;
                int idx = selectionIndex.getOrDefault(slotKey, 0);
                idx = (idx + 1) % options.size();
                selectionIndex.put(slotKey, idx);
                selections.put(slotKey, options.get(idx));
                return true;
            }
            i++;
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
