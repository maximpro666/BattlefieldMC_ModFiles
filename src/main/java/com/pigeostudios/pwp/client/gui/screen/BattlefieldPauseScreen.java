package com.pigeostudios.pwp.client.gui.screen;

import com.pigeostudios.pwp.client.gui.I18n;
import com.pigeostudios.pwp.client.gui.UITheme;
import com.pigeostudios.pwp.client.gui.component.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class BattlefieldPauseScreen extends Screen {

    private float fadeAlpha   = 0f;
    private float slideIn     = 0f;
    private float overlayAlpha = 0f;
    private long  openTime;

    public BattlefieldPauseScreen() {
        super(Component.literal(I18n.get("pwp.ui.paused")));
    }

    @Override
    protected void init() {
        openTime = System.currentTimeMillis();
        int panelW = 260;
        int panelH = Math.max(240, Math.min(300, height - 80));
        int panelY = Math.max(20, height / 2 - panelH / 2);
        int cx = width / 2;
        int startY = panelY + 36;
        int btnW = panelW - 40;
        int btnH = 26;
        int gap = 6;
        int bx = cx - btnW / 2;

        addRenderableWidget(new BButton(bx, startY, btnW, btnH,
            Component.literal("\u25B6 " + I18n.get("pwp.ui.return_to_battle")), btn -> onClose(),
            BButton.Variant.PRIMARY));

        addRenderableWidget(new BButton(bx, startY + (btnH + gap), btnW, btnH,
            Component.literal("\u2694 " + I18n.get("pwp.ui.team_selection")), btn ->
                Minecraft.getInstance().setScreen(new TeamSelectionScreen())));

        addRenderableWidget(new BButton(bx, startY + (btnH + gap) * 2, btnW, btnH,
            Component.literal("\u2699 " + I18n.get("pwp.ui.settings")), btn ->
                Minecraft.getInstance().setScreen(new SettingsMenuScreen())));

        addRenderableWidget(new BButton(bx, startY + (btnH + gap) * 3, btnW, btnH,
            Component.literal("\uD83D\uDCE2 Reports"), btn ->
                Minecraft.getInstance().setScreen(new ReportScreen())));

        addRenderableWidget(new BButton(bx, startY + (btnH + gap) * 4, btnW, btnH,
            Component.literal("\u2620 Redeploy"), btn -> {
                if (Minecraft.getInstance().player != null && Minecraft.getInstance().getConnection() != null)
                    Minecraft.getInstance().getConnection().sendCommand("redeploy");
                onClose();
            }, BButton.Variant.DANGER));

        addRenderableWidget(new BButton(bx, startY + (btnH + gap) * 5, btnW, btnH,
            Component.literal("\u21A9 " + I18n.get("pwp.ui.disconnect")), btn -> {
                if (Minecraft.getInstance().level != null)
                    Minecraft.getInstance().level.disconnect();
                Minecraft.getInstance().setScreen(new BattlefieldMainMenuScreen());
            }, BButton.Variant.DANGER));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        float elapsed = (System.currentTimeMillis() - openTime) / 250f;
        fadeAlpha   = Math.min(1f, elapsed);
        slideIn     = AnimationHelper.lerp(slideIn, 1f, 0.18f);
        overlayAlpha = AnimationHelper.lerp(overlayAlpha, 1f, 0.10f);

        int overlayColor = (int)(overlayAlpha * 0xDD) << 24;
        g.fill(0, 0, width, height, overlayColor);

        int panelW = 260;
        int panelH = Math.max(240, Math.min(300, height - 80));
        int panelX = width / 2 - panelW / 2;
        int panelY = Math.max(20, height / 2 - panelH / 2);
        float slideOffset = (1f - AnimationHelper.easeOutCubic(slideIn)) * 40f;

        g.pose().pushPose();
        g.pose().translate(0, slideOffset, 0);

        g.fill(panelX, panelY, panelX + panelW, panelY + panelH,
            AnimationHelper.withAlpha(UITheme.BG_PANEL, (int)(fadeAlpha * 0xEE)));
        g.fill(panelX, panelY, panelX + panelW, panelY + 3,
            AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fadeAlpha * 0xFF)));

        g.fill(panelX, panelY, panelX + panelW, panelY + 1,
            AnimationHelper.withAlpha(UITheme.BORDER, (int)(fadeAlpha * 0xAA)));
        g.fill(panelX, panelY + panelH - 1, panelX + panelW, panelY + panelH,
            AnimationHelper.withAlpha(UITheme.BORDER, (int)(fadeAlpha * 0xAA)));
        g.fill(panelX, panelY, panelX + 1, panelY + panelH,
            AnimationHelper.withAlpha(UITheme.BORDER, (int)(fadeAlpha * 0xAA)));
        g.fill(panelX + panelW - 1, panelY, panelX + panelW, panelY + panelH,
            AnimationHelper.withAlpha(UITheme.BORDER, (int)(fadeAlpha * 0xAA)));

        String paused = I18n.get("pwp.ui.paused_uppercase");
        int pw = font.width(paused);
        int alpha = (int)(fadeAlpha * 0xFF);
        g.drawString(font, paused, panelX + panelW / 2 - pw / 2, panelY + 16,
            AnimationHelper.withAlpha(UITheme.ACCENT, alpha));

        int lineW = 100;
        g.fill(panelX + panelW / 2 - lineW / 2, panelY + 46,
              panelX + panelW / 2 + lineW / 2, panelY + 48,
            AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fadeAlpha * 0x88)));

        g.pose().popPose();

        float btnAlpha = AnimationHelper.easeOutCubic(slideIn);
        g.setColor(1f, 1f, 1f, btnAlpha);
        super.render(g, mx, my, pt);
        g.setColor(1f, 1f, 1f, 1f);

        String esc = "ESC to resume";
        int ew = font.width(esc);
        g.drawString(font, esc, width / 2 - ew / 2, panelY + panelH - 16,
            AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int)(fadeAlpha * 150)));
    }

    @Override
    public boolean isPauseScreen() { return true; }
}
