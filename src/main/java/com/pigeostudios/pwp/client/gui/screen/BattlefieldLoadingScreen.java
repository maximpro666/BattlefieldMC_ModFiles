package com.pigeostudios.pwp.client.gui.screen;

import com.pigeostudios.pwp.client.gui.UITheme;
import com.pigeostudios.pwp.client.gui.component.AnimationHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.util.Optional;
import java.util.Random;

public class BattlefieldLoadingScreen extends Overlay {

    private static final ResourceLocation[] BACKGROUNDS = {
        new ResourceLocation("pwp", "textures/gui/loading/bg.png"),
    };
    private static final ResourceLocation LEFT_PANEL =
        new ResourceLocation("pwp", "textures/gui/loading/left.png");
    private static final ResourceLocation RIGHT_PANEL =
        new ResourceLocation("pwp", "textures/gui/loading/right.png");
    private static final float COMPLETE_PROGRESS = 1f;

    private final int bgIndex = new Random().nextInt(BACKGROUNDS.length);

    private float logoAlpha       = 0f;
    private float accentLineW     = 0f;
    private float barPulse        = 0f;
    private float progress        = 0f;
    private float displayProgress = 0f;
    private float indeterminate   = 0f;
    private String loadingText    = "CONNECTING";
    private long   openTime;
    private boolean dismissed     = false;
    private int     connectedStage = 0;
    private int     configStage    = 0;
    private int     teamStage      = 0;
    private boolean hasLeftTex, hasRightTex;

    private static BattlefieldLoadingScreen INSTANCE;

    public BattlefieldLoadingScreen() {
        openTime = System.currentTimeMillis();
        INSTANCE = this;
        checkTextures();
    }

    private void checkTextures() {
        var rm = Minecraft.getInstance().getResourceManager();
        hasLeftTex  = rm.getResource(LEFT_PANEL).isPresent();
        hasRightTex = rm.getResource(RIGHT_PANEL).isPresent();
    }

    public static BattlefieldLoadingScreen getInstance() { return INSTANCE; }

    public long getOpenTime() { return openTime; }

    public static void show() {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.getOverlay() instanceof BattlefieldLoadingScreen)) {
            mc.setOverlay(new BattlefieldLoadingScreen());
        }
    }

    public static void updateProgress(int connStage, int cfgStage, int teamStg) {
        BattlefieldLoadingScreen inst = INSTANCE;
        if (inst == null || inst.dismissed) return;
        inst.connectedStage = connStage;
        inst.configStage = cfgStage;
        inst.teamStage = teamStg;
        inst.recalcProgress();
        if (inst.progress >= COMPLETE_PROGRESS) {
            inst.dismiss();
        }
    }

    private void recalcProgress() {
        if (dismissed) return;
        float p = 0f;
        if (connectedStage >= 2) p += 0.33f;
        else if (connectedStage >= 1) p += 0.15f;
        if (configStage >= 2) p += 0.34f;
        else if (configStage >= 1) p += 0.15f;
        if (teamStage >= 2) p += 0.33f;
        else if (teamStage >= 1) p += 0.15f;
        progress = Math.max(0f, Math.min(1f, p));

        if (connectedStage < 2) loadingText = "CONNECTING";
        else if (configStage < 2) loadingText = "RECEIVING CONFIG";
        else if (teamStage < 2) loadingText = "ASSIGNING TEAM";
        else loadingText = "READY";
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        int w = g.guiWidth();
        int h = g.guiHeight();
        var font = Minecraft.getInstance().font;

        g.fill(0, 0, w, h, UITheme.BG_BLACK);
        if (bgIndex < BACKGROUNDS.length) {
            g.setColor(1f, 1f, 1f, 0.25f);
            g.blit(BACKGROUNDS[bgIndex], 0, 0, 0, 0, w, h, w, h);
            g.setColor(1f, 1f, 1f, 1f);
        }

        // Side panel textures (if files exist in assets)
        int panelW = w / 5;
        if (hasLeftTex) {
            g.blit(LEFT_PANEL, 0, 0, 0, 0, panelW, h, panelW, h);
        }
        if (hasRightTex) {
            g.blit(RIGHT_PANEL, w - panelW, 0, 0, 0, panelW, h, panelW, h);
        }

        // Dark overlay
        g.fill(0, 0, w, h,
            AnimationHelper.withAlpha(UITheme.BG_BLACK, 0xAA));

        logoAlpha   = Math.min(1f, logoAlpha   + 0.02f);
        accentLineW = Math.min(1f, accentLineW + 0.03f);
        barPulse   += 0.06f;
        indeterminate = (indeterminate + 0.004f) % 1f;

        int cx = w / 2;
        int cy = h / 2;
        int alpha = (int)(logoAlpha * 0xFF);

        // ── Title ──
        g.drawString(font, "BATTLEFIELD", cx - font.width("BATTLEFIELD") / 2, cy - 74,
            AnimationHelper.withAlpha(UITheme.ACCENT, alpha));

        g.drawString(font, loadingText, cx - font.width(loadingText) / 2, cy - 54,
            AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int)(logoAlpha * 200)));

        // Accent line
        int lineW = (int)(120 * accentLineW);
        g.fill(cx - lineW / 2, cy - 40, cx + lineW / 2, cy - 38,
            AnimationHelper.withAlpha(UITheme.ACCENT, alpha));

        // ── Progress bar ──
        int barW = 260;
        int barH = 4;
        int barX = cx - barW / 2;
        int barY = cy + 14;

        float target = progress < 0.95f ? progress + 0.05f : progress;
        displayProgress = AnimationHelper.lerp(displayProgress, target, 0.06f);
        if (progress >= COMPLETE_PROGRESS) displayProgress = 1f;

        // Bar background
        g.fill(barX, barY, barX + barW, barY + barH,
            AnimationHelper.withAlpha(0xFF1A1A1A, alpha));

        // Bar fill (smooth)
        int fillW = (int)(barW * displayProgress);
        if (fillW > 0) {
            float pulse = 0.6f + 0.4f * (float)Math.sin(barPulse);
            g.fill(barX, barY, barX + fillW, barY + barH,
                AnimationHelper.withAlpha(UITheme.ACCENT, (int)(alpha * pulse)));
        }

        // Indeterminate shimmer
        if (progress < 0.01f) {
            int shX = (int)(indeterminate * (barW + 60)) - 30;
            g.fill(barX + shX, barY, barX + shX + 30, barY + barH,
                AnimationHelper.withAlpha(0x44FFFFFF, (int)(alpha * 0.3f)));
        }

        // Border
        g.fill(barX - 1, barY - 1, barX + barW + 1, barY,
            AnimationHelper.withAlpha(UITheme.BORDER, alpha));
        g.fill(barX - 1, barY + barH, barX + barW + 1, barY + barH + 1,
            AnimationHelper.withAlpha(UITheme.BORDER, alpha));
        g.fill(barX - 1, barY, barX, barY + barH,
            AnimationHelper.withAlpha(UITheme.BORDER, alpha));
        g.fill(barX + barW, barY, barX + barW + 1, barY + barH,
            AnimationHelper.withAlpha(UITheme.BORDER, alpha));

        // Percentage
        String pctText = String.format("%.0f%%", displayProgress * 100);
        g.drawString(font, pctText, cx - font.width(pctText) / 2, cy + 26,
            AnimationHelper.withAlpha(UITheme.TEXT_SECONDARY, (int)(logoAlpha * 200)));

        // Hint
        String hint = "Loading combat systems...";
        g.drawString(font, hint, cx - font.width(hint) / 2, cy + 48,
            AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int)(logoAlpha * 100)));

        // Version
        String ver = "PWP v2.0 \u00B7 MC 1.20.1 Forge";
        g.drawString(font, ver, cx - font.width(ver) / 2, h - 16,
            AnimationHelper.withAlpha(UITheme.TEXT_MUTED, 0x55));

        // Corner decorations
        int corner = 24;
        int cd = 12;
        g.fill(corner, corner, corner + cd, corner + 1,
            AnimationHelper.withAlpha(UITheme.ACCENT, (int)(alpha * 0.6f)));
        g.fill(corner, corner, corner + 1, corner + cd,
            AnimationHelper.withAlpha(UITheme.ACCENT, (int)(alpha * 0.6f)));
        g.fill(w - corner - cd, corner, w - corner, corner + 1,
            AnimationHelper.withAlpha(UITheme.ACCENT, (int)(alpha * 0.6f)));
        g.fill(w - corner - 1, corner, w - corner, corner + cd,
            AnimationHelper.withAlpha(UITheme.ACCENT, (int)(alpha * 0.6f)));
        g.fill(corner, h - corner - 1, corner + cd, h - corner,
            AnimationHelper.withAlpha(UITheme.ACCENT, (int)(alpha * 0.6f)));
        g.fill(corner, h - corner - cd, corner + 1, h - corner,
            AnimationHelper.withAlpha(UITheme.ACCENT, (int)(alpha * 0.6f)));
        g.fill(w - corner - cd, h - corner - 1, w - corner, h - corner,
            AnimationHelper.withAlpha(UITheme.ACCENT, (int)(alpha * 0.6f)));
        g.fill(w - corner - 1, h - corner - cd, w - corner, h - corner,
            AnimationHelper.withAlpha(UITheme.ACCENT, (int)(alpha * 0.6f)));
    }

    public void dismiss() {
        dismissed = true;
        Minecraft.getInstance().setOverlay(null);
        INSTANCE = null;
    }
}
