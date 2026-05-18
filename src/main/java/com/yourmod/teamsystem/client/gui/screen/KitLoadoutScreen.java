package com.yourmod.teamsystem.client.gui.screen;

import com.yourmod.teamsystem.client.gui.UITheme;

import com.yourmod.teamsystem.network.PacketHandler;
import com.yourmod.teamsystem.network.KitSavePacket;
import com.yourmod.teamsystem.client.gui.component.BButton;
import com.yourmod.teamsystem.client.gui.component.AnimationHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;

public class KitLoadoutScreen extends Screen {

    private static final int COLOR_BG      = UITheme.BG_SCREEN;
    private static final int COLOR_PANEL   = UITheme.BG_PANEL;
    private static final int COLOR_ORANGE  = UITheme.ACCENT;
    private static final int COLOR_SLOT_BG = UITheme.BG_SLOT;
    private static final int COLOR_BORDER  = UITheme.NAMETAG_SPECTATOR;
    private static final int COLOR_TEXT    = UITheme.TEXT_PRIMARY;
    private static final int COLOR_SUBTEXT = UITheme.TEXT_MUTED;

    private static final String[] SLOT_NAMES = {
        "Primary", "Secondary", "Melee", "Gadget 1", "Gadget 2", "Grenade"
    };
    private static final int SLOT_W = 160;
    private static final int SLOT_H = 36;
    private static final int GAP    = 6;

    private final String kitId;
    private final Map<Integer, String> slotSelections = new HashMap<>();

    private float fadeAlpha = 0f;
    private long openTime;
    private int hoveredSlot = -1;

    public KitLoadoutScreen(String kitId) {
        super(Component.literal("Kit Loadout: " + kitId));
        this.kitId = kitId;
    }

    @Override
    protected void init() {
        openTime = System.currentTimeMillis();
        int panelW = SLOT_W + 40;
        int panelX = width / 2 - panelW / 2;
        int panelH = SLOT_NAMES.length * (SLOT_H + GAP) + 50;
        int panelY = height / 2 - panelH / 2;

        addRenderableWidget(new BButton(
            panelX + panelW / 2 - 60, panelY + panelH + 10, 120, 20,
            Component.literal("Save Loadout"), btn -> saveLoadout()
        ));
    }

    private void saveLoadout() {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < SLOT_NAMES.length; i++) {
            String val = slotSelections.getOrDefault(i, "");
            sb.append("\"").append(SLOT_NAMES[i]).append("\":\"").append(val).append("\"");
            if (i < SLOT_NAMES.length - 1) sb.append(",");
        }
        sb.append("}");
        PacketHandler.CHANNEL.sendToServer(new KitSavePacket(kitId, sb.toString()));
        onClose();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        float elapsed = (System.currentTimeMillis() - openTime) / 250f;
        fadeAlpha = Math.min(1f, elapsed);

        g.fill(0, 0, width, height, AnimationHelper.withAlpha(COLOR_BG, (int)(fadeAlpha * 0xCC)));

        int panelW = SLOT_W + 40;
        int panelX = width / 2 - panelW / 2;
        int panelH = SLOT_NAMES.length * (SLOT_H + GAP) + 50;
        int panelY = height / 2 - panelH / 2;

        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, AnimationHelper.withAlpha(COLOR_PANEL, (int)(fadeAlpha * 0xDD)));
        g.fill(panelX, panelY, panelX + panelW, panelY + 3, AnimationHelper.withAlpha(COLOR_ORANGE, (int)(fadeAlpha * 255)));

        String title = "LOADOUT: " + kitId.toUpperCase();
        int tw = font.width(title);
        g.drawString(font, title, width / 2 - tw / 2, panelY + 10, AnimationHelper.withAlpha(COLOR_ORANGE, (int)(fadeAlpha * 255)));

        hoveredSlot = -1;
        for (int i = 0; i < SLOT_NAMES.length; i++) {
            int sy = panelY + 28 + i * (SLOT_H + GAP);
            int sx = panelX + 10;
            boolean hov = mx >= sx && mx <= sx + SLOT_W && my >= sy && my <= sy + SLOT_H;
            if (hov) hoveredSlot = i;
            drawSlot(g, sx, sy, i, hov, fadeAlpha);
        }

        super.render(g, mx, my, pt);
    }

    private void drawSlot(GuiGraphics g, int x, int y, int idx, boolean hov, float alpha) {
        int bg  = hov ? AnimationHelper.withAlpha(UITheme.BG_SURFACE, (int)(alpha * 255))
                      : AnimationHelper.withAlpha(COLOR_SLOT_BG, (int)(alpha * 255));
        int brd = hov ? AnimationHelper.withAlpha(COLOR_ORANGE, (int)(alpha * 200))
                      : AnimationHelper.withAlpha(COLOR_BORDER, (int)(alpha * 150));

        g.fill(x, y, x + SLOT_W, y + SLOT_H, bg);
        g.fill(x, y, x + SLOT_W, y + 1, brd);
        g.fill(x, y + SLOT_H - 1, x + SLOT_W, y + SLOT_H, brd);
        g.fill(x, y, x + 1, y + SLOT_H, brd);
        g.fill(x + SLOT_W - 1, y, x + SLOT_W, y + SLOT_H, brd);

        g.drawString(font, SLOT_NAMES[idx], x + 6, y + 4, AnimationHelper.withAlpha(COLOR_SUBTEXT, (int)(alpha * 200)));

        String sel = slotSelections.getOrDefault(idx, "\u2014 Empty \u2014");
        g.drawString(font, sel, x + 6, y + 16, AnimationHelper.withAlpha(COLOR_TEXT, (int)(alpha * 255)));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        for (int i = 0; i < SLOT_NAMES.length; i++) {
            int sy = height / 2 - (SLOT_NAMES.length * (SLOT_H + GAP) + 50) / 2 + 28 + i * (SLOT_H + GAP);
            int sx = width / 2 - (SLOT_W + 40) / 2 + 10;
            if (mx >= sx && mx <= sx + SLOT_W && my >= sy && my <= sy + SLOT_H) {
                String cur = slotSelections.getOrDefault(i, "");
                slotSelections.put(i, cur.isEmpty() ? "default_" + SLOT_NAMES[i].toLowerCase() : "");
                return true;
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
