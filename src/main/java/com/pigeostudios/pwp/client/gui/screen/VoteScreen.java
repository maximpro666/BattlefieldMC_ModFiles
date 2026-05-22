package com.pigeostudios.pwp.client.gui.screen;

import com.pigeostudios.pwp.client.ClientTeamData;
import com.pigeostudios.pwp.client.gui.UITheme;
import com.pigeostudios.pwp.client.gui.component.AnimationHelper;
import com.pigeostudios.pwp.client.gui.component.RenderHelper;
import com.pigeostudios.pwp.network.MapVotePacket;
import com.pigeostudios.pwp.network.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class VoteScreen extends Screen {

    private static final int CARD_W = 170;
    private static final int CARD_H = 178;
    private static final int PREVIEW_H = 80;
    private static final int GAP = 10;
    private static final int COLS = 3;

    private static final int[] CARD_COLORS = {
        0xFF1C5FAD, 0xFFAD1C1C, 0xFF2E8B57, 0xFFB8860B,
        0xFF6A5ACD, 0xFFCD853F, 0xFF4682B4, 0xFF8B4513, 0xFF556B2F
    };

    private final List<String> mapNames;
    private final int[] voteCounts;
    private final int[] displayVotes;
    private final float[] barProgress;
    private final float[] hoverState;
    private final float[] enterState;
    private int remainingSeconds;
    private String selectedMap;
    private long openTime;
    private float fadeAlpha;
    private boolean dismissed;
    private int gridStartX, gridStartY, gridRows, gridCols, gridW, gridH;

    public static void open(List<String> mapNames, int[] voteCounts, int remainingSeconds) {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new VoteScreen(mapNames, voteCounts, remainingSeconds));
    }

    public VoteScreen(List<String> mapNames, int[] voteCounts, int remainingSeconds) {
        super(Component.literal("Map Vote"));
        this.mapNames = mapNames != null ? new ArrayList<>(mapNames) : new ArrayList<>();
        int n = this.mapNames.size();
        this.voteCounts = voteCounts != null && voteCounts.length == n ? voteCounts : new int[n];
        this.displayVotes = new int[n];
        this.barProgress = new float[n];
        this.hoverState = new float[n];
        this.enterState = new float[n];
        this.remainingSeconds = remainingSeconds;
        this.selectedMap = null;
        this.dismissed = false;
    }

    @Override
    protected void init() {
        if (mapNames.isEmpty()) {
            dismissed = true;
            onClose();
            return;
        }
        openTime = System.currentTimeMillis();
        gridCols = Math.min(COLS, mapNames.size());
        gridRows = (mapNames.size() + COLS - 1) / COLS;
        gridW = gridCols * CARD_W + (gridCols - 1) * GAP;
        gridH = gridRows * CARD_H + (gridRows - 1) * GAP;
        gridStartX = width / 2 - gridW / 2;
        gridStartY = height / 2 - gridH / 2 + 10;
    }

    @Override
    public void tick() {
        int phase = ClientTeamData.getGamePhase();
        if (phase != 1) {
            dismissed = true;
            onClose();
        }
    }

    public void updateVotes(int remainingSeconds, int[] voteCounts) {
        this.remainingSeconds = remainingSeconds;
        if (voteCounts != null && voteCounts.length == this.voteCounts.length) {
            System.arraycopy(voteCounts, 0, this.voteCounts, 0, voteCounts.length);
        }
    }

    private void castVote(String mapName) {
        PacketHandler.CHANNEL.sendToServer(new MapVotePacket(mapName));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        if (dismissed) return;

        float elapsed = (System.currentTimeMillis() - openTime) / 300f;
        fadeAlpha = Math.min(1f, elapsed);

        int alpha = (int) (fadeAlpha * 0xCC);
        g.fill(0, 0, width, height, AnimationHelper.withAlpha(UITheme.BG_SCREEN, alpha));

        renderTitle(g);
        renderTimer(g);

        int totalVotes = 0;
        for (int v : voteCounts) totalVotes += v;

        for (int i = 0; i < mapNames.size(); i++) {
            int col = i % COLS;
            int row = i / COLS;
            int cx = gridStartX + col * (CARD_W + GAP);
            int cy = gridStartY + row * (CARD_H + GAP);

            boolean hov = mx >= cx && mx <= cx + CARD_W && my >= cy && my <= cy + CARD_H;
            hoverState[i] = AnimationHelper.lerp(hoverState[i], hov ? 1f : 0f, 0.15f);
            enterState[i] = AnimationHelper.lerp(enterState[i], 1f, 0.15f);

            if (displayVotes[i] < voteCounts[i]) displayVotes[i]++;
            else if (displayVotes[i] > voteCounts[i]) displayVotes[i]--;

            float targetBar = totalVotes > 0 ? (float) voteCounts[i] / totalVotes : 0f;
            barProgress[i] = AnimationHelper.lerp(barProgress[i], targetBar, 0.08f);

            boolean sel = mapNames.get(i).equals(selectedMap);
            drawCard(g, cx, cy, i, sel, hoverState[i], enterState[i], fadeAlpha);
        }

        super.render(g, mx, my, pt);
    }

    private void renderTitle(GuiGraphics g) {
        int alpha = (int) (fadeAlpha * 255);
        String title = "\u2694 \u0412\u042b\u0411\u0415\u0420\u0418\u0422\u0415 \u041a\u0410\u0420\u0422\u0423 \u2694";
        int tw = font.width(title);

        int cx = width / 2;
        int ty = 28;

        RenderHelper.glow(g, cx - tw / 2 - 12, ty - 6, tw + 24, font.lineHeight + 12,
            UITheme.ACCENT, 8, fadeAlpha * 0.3f);
        g.drawString(font, title, cx - tw / 2, ty,
            AnimationHelper.withAlpha(UITheme.ACCENT, alpha));

        int lineY = ty + font.lineHeight + 8;
        int lineW = 200;
        int lineX1 = cx - lineW / 2;
        int lineX2 = cx + lineW / 2 - 40;
        g.fill(lineX1, lineY, lineX1 + 60, lineY + 1,
            AnimationHelper.withAlpha(UITheme.ACCENT_DIM, (int) (alpha * 0.6f)));
        g.fill(lineX1 + 70, lineY, lineX2, lineY + 1,
            AnimationHelper.withAlpha(UITheme.BORDER, (int) (alpha * 0.3f)));
        g.fill(lineX2 + 50, lineY, lineX2 + 110, lineY + 1,
            AnimationHelper.withAlpha(UITheme.ACCENT_DIM, (int) (alpha * 0.6f)));
    }

    private void renderTimer(GuiGraphics g) {
        int alpha = (int) (fadeAlpha * 255);
        int cx = width / 2;
        int ty = 60;

        boolean urgent = remainingSeconds <= 5;
        int timerColor = urgent ? UITheme.STATUS_DANGER : UITheme.TEXT_SECONDARY;

        if (urgent) {
            float pulse = (float) Math.sin((System.currentTimeMillis() - openTime) / 300.0 * Math.PI * 2) * 0.3f + 0.7f;
            RenderHelper.glow(g, cx - 60, ty - 4, 120, font.lineHeight + 8,
                UITheme.STATUS_DANGER, 6, fadeAlpha * pulse * 0.4f);
        }

        String timerText = "\u0413\u043e\u043b\u043e\u0441\u043e\u0432\u0430\u043d\u0438\u0435: " + remainingSeconds + "\u0441";
        int tw = font.width(timerText);
        g.drawString(font, timerText, cx - tw / 2, ty,
            AnimationHelper.withAlpha(timerColor, alpha));
    }

    private void drawCard(GuiGraphics g, int x, int y, int index, boolean selected,
                           float hover, float enter, float alpha) {
        String mapName = mapNames.get(index);
        int cardColor = CARD_COLORS[Math.abs(mapName.hashCode()) % CARD_COLORS.length];
        int darkColor = darken(cardColor, 0.5f);

        float enterScale = 0.9f + 0.1f * Math.min(1f, enter);
        float cardAlpha = alpha * Math.min(1f, enter * 1.5f);
        int a = (int) (cardAlpha * 255);

        int drawW = (int) (CARD_W * enterScale);
        int drawH = (int) (CARD_H * enterScale);
        int drawX = x + (CARD_W - drawW) / 2;
        int drawY = y + (CARD_H - drawH) / 2;

        // Shadow
        RenderHelper.dropShadow(g, drawX, drawY, drawW, drawH, 4, (int) (cardAlpha * 60));

        // Card background
        g.fill(drawX, drawY, drawX + drawW, drawY + drawH,
            AnimationHelper.withAlpha(UITheme.BG_PANEL, a));

        // Preview area with gradient
        RenderHelper.gradientRectV(g, drawX, drawY, drawW, PREVIEW_H,
            AnimationHelper.withAlpha(cardColor, a),
            AnimationHelper.withAlpha(darkColor, a));

        // Map initial letter — drawn with shadow for legibility
        String initial = mapName.isEmpty() ? "?" : mapName.substring(0, 1).toUpperCase();
        int initX = drawX + drawW / 2 - font.width(initial) / 2;
        int initY = drawY + PREVIEW_H / 2 - font.lineHeight / 2;
        float initGlow = selected ? 1f : 0.6f + hover * 0.4f;
        g.drawString(font, initial, initX + 1, initY + 1,
            AnimationHelper.withAlpha(0x40000000, (int) (a * initGlow)));
        g.drawString(font, initial, initX, initY,
            AnimationHelper.withAlpha(0xDDFFFFFF, (int) (a * initGlow)));

        // Map name
        int nameY = drawY + PREVIEW_H + 8;
        int nameColor = selected ? UITheme.ACCENT : (hover > 0.5f ? UITheme.TEXT_PRIMARY : UITheme.TEXT_SECONDARY);
        int tw = font.width(mapName);
        int maxNameW = drawW - 16;
        String displayName = font.width(mapName) > maxNameW
            ? font.plainSubstrByWidth(mapName, maxNameW - 6) + "..."
            : mapName;
        g.drawString(font, displayName, drawX + drawW / 2 - font.width(displayName) / 2, nameY,
            AnimationHelper.withAlpha(nameColor, a));

        // Vote bar background
        int barY = nameY + 16;
        int barH = 6;
        int barW = drawW - 20;
        int barX = drawX + 10;
        g.fill(barX, barY, barX + barW, barY + barH,
            AnimationHelper.withAlpha(UITheme.BG_SLOT, a));

        // Vote bar fill (animated)
        int fillW = (int) (barW * barProgress[index]);
        if (fillW > 0) {
            int barColor = selected ? UITheme.ACCENT : cardColor;
            g.fill(barX, barY, barX + fillW, barY + barH,
                AnimationHelper.withAlpha(barColor, a));
            RenderHelper.glowBar(g, barX, barY, fillW, barH, barColor, 3);
        }

        // Vote count text
        int voteY = barY + barH + 3;
        String voteText = displayVotes[index] + "";
        int vw = font.width(voteText);
        g.drawString(font, voteText, drawX + drawW / 2 - vw / 2, voteY,
            AnimationHelper.withAlpha(selected ? UITheme.ACCENT : UITheme.TEXT_MUTED,
                (int) (a * (selected ? 1f : 0.8f))));

        // Selection / hover border
        if (selected) {
            RenderHelper.glow(g, drawX, drawY, drawW, drawH, UITheme.ACCENT, 4, cardAlpha * 0.5f);
            g.fill(drawX, drawY, drawX + drawW, drawY + 2,
                AnimationHelper.withAlpha(UITheme.ACCENT, a));
            g.fill(drawX, drawY + drawH - 2, drawX + drawW, drawY + drawH,
                AnimationHelper.withAlpha(UITheme.ACCENT, a));
            g.fill(drawX, drawY, drawX + 2, drawY + drawH,
                AnimationHelper.withAlpha(UITheme.ACCENT, a));
            g.fill(drawX + drawW - 2, drawY, drawX + drawW, drawY + drawH,
                AnimationHelper.withAlpha(UITheme.ACCENT, a));

            // Checkmark
            String check = "\u2714";
            int ckX = drawX + drawW - font.width(check) - 6;
            int ckY = drawY + PREVIEW_H - font.lineHeight - 4;
            g.drawString(font, check, ckX, ckY,
                AnimationHelper.withAlpha(UITheme.ACCENT, a));
        } else if (hover > 0.1f) {
            int brdA = (int) (a * hover * 0.6f);
            int brdColor = AnimationHelper.withAlpha(cardColor, brdA);
            g.fill(drawX, drawY, drawX + drawW, drawY + 1, brdColor);
            g.fill(drawX, drawY + drawH - 1, drawX + drawW, drawY + drawH, brdColor);
            g.fill(drawX, drawY, drawX + 1, drawY + drawH, brdColor);
            g.fill(drawX + drawW - 1, drawY, drawX + drawW, drawY + drawH, brdColor);
        }
    }

    private static int darken(int color, float factor) {
        int r = (int) (((color >> 16) & 0xFF) * factor);
        int g = (int) (((color >> 8) & 0xFF) * factor);
        int b = (int) ((color & 0xFF) * factor);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        for (int i = 0; i < mapNames.size(); i++) {
            int col = i % COLS;
            int row = i / COLS;
            int cx = gridStartX + col * (CARD_W + GAP);
            int cy = gridStartY + row * (CARD_H + GAP);

            if (mx >= cx && mx <= cx + CARD_W && my >= cy && my <= cy + CARD_H) {
                selectedMap = mapNames.get(i);
                castVote(selectedMap);
                return true;
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        dismissed = true;
        onClose();
        return true;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public boolean shouldCloseOnEsc() { return true; }
}
