package com.pigeostudios.pwp.client.gui.overlay;

import com.pigeostudios.pwp.client.ClientTeamData;
import com.pigeostudios.pwp.client.gui.I18n;
import com.pigeostudios.pwp.client.gui.UITheme;
import com.pigeostudios.pwp.client.gui.component.AnimationHelper;
import com.pigeostudios.pwp.client.gui.component.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

public class BleedingOverlay {
    private static final int BAR_W = 200;
    private static final int BAR_H = 8;
    private static final int BAR_RADIUS = 4;
    private static final int VIGNETTE_COLOR = 0xCC220000;

    private float smoothBleedout = 0f;
    private float smoothRevive = 0f;

    public void render(GuiGraphics g, int screenWidth, int screenHeight) {
        if (!ClientTeamData.isBleeding) return;

        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int cx = screenWidth / 2;

        animateBars();

        renderVignette(g, screenWidth, screenHeight);

        boolean isRu = "ru".equals(ClientTeamData.language);
        String title = isRu ? "\u0418\u0421\u0422\u0415\u041a\u0410\u0415\u0422 \u041a\u0420\u041e\u0412\u042c\u042e" : I18n.get("pwp.ui.hud.bleeding");
        int titleCol = AnimationHelper.withAlpha(0xFFFF4444, 200 + (int)(55f * (float)Math.sin(System.currentTimeMillis() / 300d)));
        g.drawString(font, title, cx - font.width(title) / 2, screenHeight / 2 - 50, titleCol);

        renderBleedoutBar(g, cx, screenHeight);

        if (ClientTeamData.reviverUUID != null) {
            renderReviveProgress(g, cx, screenHeight, isRu, font);
        }
    }

    private void animateBars() {
        int maxBleedTicks = Math.max(ClientTeamData.bleedTimeRemaining, 1);
        float bleedFrac = ClientTeamData.bleedTimeRemaining / (float) (30 * 20);
        smoothBleedout = AnimationHelper.lerp(smoothBleedout, bleedFrac, 0.08f);

        float reviveFrac = ClientTeamData.reviveProgress / 100f;
        smoothRevive = AnimationHelper.lerp(smoothRevive, reviveFrac, 0.12f);
    }

    private void renderVignette(GuiGraphics g, int w, int h) {
        g.fill(0, 0, w, h, VIGNETTE_COLOR);

        int edgeAlpha = 120;
        g.fill(0, 0, w, h / 6, AnimationHelper.withAlpha(0xFF220000, edgeAlpha));
        g.fill(0, h - h / 6, w, h, AnimationHelper.withAlpha(0xFF220000, edgeAlpha));
        g.fill(0, 0, w / 6, h, AnimationHelper.withAlpha(0xFF220000, edgeAlpha));
        g.fill(w - w / 6, 0, w, h, AnimationHelper.withAlpha(0xFF220000, edgeAlpha));
    }

    private void renderBleedoutBar(GuiGraphics g, int cx, int screenHeight) {
        int bx = cx - BAR_W / 2;
        int by = screenHeight / 2 - 30;

        RenderHelper.roundedRect(g, bx, by, BAR_W, BAR_H, BAR_RADIUS, AnimationHelper.withAlpha(0x44000000, 180));

        int fillW = (int) (BAR_W * (1f - smoothBleedout));
        if (fillW > 0) {
            float pulse = 0.8f + 0.2f * (float) Math.sin(System.currentTimeMillis() / 200d);
            int barColor = AnimationHelper.withAlpha(0xFFCC3030, (int)(180 * pulse));
            RenderHelper.gradientRectH(g, bx, by, fillW, BAR_H, barColor, AnimationHelper.withAlpha(0xFF882222, 160));
        }

        String pct = Math.round((1f - smoothBleedout) * 100) + "%";
        g.drawString(Minecraft.getInstance().font, pct, cx - Minecraft.getInstance().font.width(pct) / 2, by + 1, 0xFFFFFFFF);
    }

    private void renderReviveProgress(GuiGraphics g, int cx, int screenHeight, boolean isRu, Font font) {
        int by = screenHeight / 2 - 18;

        RenderHelper.roundedRect(g, cx - BAR_W / 2, by, BAR_W, BAR_H, BAR_RADIUS, AnimationHelper.withAlpha(0x44000000, 180));

        int fillW = (int) (BAR_W * smoothRevive);
        if (fillW > 0) {
            RenderHelper.gradientRectH(g, cx - BAR_W / 2, by, fillW, BAR_H,
                AnimationHelper.withAlpha(0xFF50B050, 200), AnimationHelper.withAlpha(0xFF307030, 160));
        }

        String label = isRu ? "\u0421\u041f\u0410\u0421\u0410\u042e\u0422..." : I18n.get("pwp.ui.hud.revived");
        g.drawString(font, label, cx - font.width(label) / 2, by - 12,
            AnimationHelper.withAlpha(0xFF50E050, 220));
    }

}
