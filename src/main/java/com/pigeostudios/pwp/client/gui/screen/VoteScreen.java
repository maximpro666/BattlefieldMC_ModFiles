package com.pigeostudios.pwp.client.gui.screen;

import com.pigeostudios.pwp.client.gui.UITheme;

import com.pigeostudios.pwp.network.PacketHandler;
import com.pigeostudios.pwp.network.MapVotePacket;
import com.pigeostudios.pwp.client.gui.component.BButton;
import com.pigeostudios.pwp.client.gui.component.AnimationHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.List;

public class VoteScreen extends Screen {

    private static final int COLOR_BG       = UITheme.BG_SCREEN;
    private static final int COLOR_CARD     = UITheme.BG_PANEL;
    private static final int COLOR_ORANGE   = UITheme.ACCENT;
    private static final int COLOR_BORDER   = UITheme.BORDER;
    private static final int COLOR_TEXT     = UITheme.TEXT_PRIMARY;
    private static final int COLOR_SUBTEXT  = UITheme.TEXT_MUTED;
    private static final int COLOR_SELECTED = UITheme.ACCENT_GHOST;

    private static final List<String> MAPS = Arrays.asList(
        "Operation Sandstorm",
        "Arctic Outpost",
        "Urban Assault",
        "Desert Storm",
        "Eastern Front"
    );

    private String selectedMap = null;
    private float fadeAlpha    = 0f;
    private long openTime;
    private float[] hoverState = new float[MAPS.size()];

    private static final int CARD_W = 220;
    private static final int CARD_H = 44;
    private static final int GAP    = 8;

    public VoteScreen() {
        super(Component.literal("Map Vote"));
    }

    @Override
    protected void init() {
        openTime = System.currentTimeMillis();
        int totalH = MAPS.size() * (CARD_H + GAP) - GAP;
        int listY  = height / 2 - totalH / 2;

        addRenderableWidget(new BButton(
            width / 2 - 60, height / 2 + totalH / 2 + 20, 120, 20,
            Component.literal("Cast Vote"), btn -> castVote()
        ));
    }

    private void castVote() {
        if (selectedMap != null) {
            PacketHandler.CHANNEL.sendToServer(new MapVotePacket(selectedMap));
            onClose();
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        float elapsed = (System.currentTimeMillis() - openTime) / 250f;
        fadeAlpha = Math.min(1f, elapsed);

        g.fill(0, 0, width, height, AnimationHelper.withAlpha(COLOR_BG, (int)(fadeAlpha * 0xCC)));

        String title = "VOTE FOR NEXT MAP";
        int tw = font.width(title);
        g.drawString(font, title, width / 2 - tw / 2, 20, AnimationHelper.withAlpha(COLOR_ORANGE, (int)(fadeAlpha * 255)));

        int totalH = MAPS.size() * (CARD_H + GAP) - GAP;
        int listY  = height / 2 - totalH / 2;
        int listX  = width / 2 - CARD_W / 2;

        for (int i = 0; i < MAPS.size(); i++) {
            String map = MAPS.get(i);
            int cy = listY + i * (CARD_H + GAP);
            boolean hov = mx >= listX && mx <= listX + CARD_W && my >= cy && my <= cy + CARD_H;
            hoverState[i] = AnimationHelper.lerp(hoverState[i], hov ? 1f : 0f, 0.15f);
            boolean sel = map.equals(selectedMap);
            drawMapCard(g, listX, cy, map, sel, hoverState[i], fadeAlpha);
        }

        super.render(g, mx, my, pt);
    }

    private void drawMapCard(GuiGraphics g, int x, int y, String mapName,
                              boolean selected, float hover, float alpha) {
        int bg  = selected
            ? AnimationHelper.withAlpha(COLOR_SELECTED, (int)(alpha * 255))
            : AnimationHelper.withAlpha(COLOR_CARD, (int)(alpha * 0xDD));
        int brd = selected
            ? AnimationHelper.withAlpha(COLOR_ORANGE, (int)(alpha * 255))
            : AnimationHelper.withAlpha(COLOR_BORDER, (int)(alpha * (0x44 + hover * 0xBB)));

        g.fill(x, y, x + CARD_W, y + CARD_H, bg);
        g.fill(x, y, x + CARD_W, y + 1, brd);
        g.fill(x, y + CARD_H - 1, x + CARD_W, y + CARD_H, brd);
        g.fill(x, y, x + 1, y + CARD_H, brd);
        g.fill(x + CARD_W - 1, y, x + CARD_W, y + CARD_H, brd);

        if (selected) {
            g.fill(x, y, x + 3, y + CARD_H, AnimationHelper.withAlpha(COLOR_ORANGE, (int)(alpha * 255)));
        }

        int tw = font.width(mapName);
        g.drawString(font, mapName, x + CARD_W / 2 - tw / 2, y + CARD_H / 2 - 4,
            AnimationHelper.withAlpha(COLOR_TEXT, (int)(alpha * 255)));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int totalH = MAPS.size() * (CARD_H + GAP) - GAP;
        int listY  = height / 2 - totalH / 2;
        int listX  = width / 2 - CARD_W / 2;
        for (int i = 0; i < MAPS.size(); i++) {
            int cy = listY + i * (CARD_H + GAP);
            if (mx >= listX && mx <= listX + CARD_W && my >= cy && my <= cy + CARD_H) {
                selectedMap = MAPS.get(i);
                return true;
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
