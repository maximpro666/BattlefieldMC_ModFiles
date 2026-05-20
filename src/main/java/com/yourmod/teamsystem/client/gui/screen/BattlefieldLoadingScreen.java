package com.yourmod.teamsystem.client.gui.screen;

import com.yourmod.teamsystem.client.gui.UITheme;
import com.yourmod.teamsystem.client.gui.component.AnimationHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.resources.ResourceLocation;

import java.util.Random;

public class BattlefieldLoadingScreen extends Overlay {

    private static final ResourceLocation[] BACKGROUNDS = {
        new ResourceLocation("teamsystem", "textures/gui/bg_0.png"),
        new ResourceLocation("teamsystem", "textures/gui/bg_1.png"),
        new ResourceLocation("teamsystem", "textures/gui/bg_2.png"),
    };
    private static final float COMPLETE_PROGRESS = 1f;

    private final int bgIndex = new Random().nextInt(BACKGROUNDS.length);

    private float logoAlpha     = 0f;
    private float accentLineW   = 0f;
    private float barPulse      = 0f;
    private float progress      = 0f;
    private String loadingText  = "CONNECTING";
    private long   openTime;
    private boolean dismissed   = false;
    private int     connectedStage   = 0;
    private int     configStage       = 0;
    private int     teamStage         = 0;

    private static BattlefieldLoadingScreen INSTANCE;

    public BattlefieldLoadingScreen() {
        openTime = System.currentTimeMillis();
        INSTANCE = this;
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

    public void setProgress(float p) { this.progress = Math.max(0f, Math.min(1f, p)); }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        int w = g.guiWidth();
        int h = g.guiHeight();
        var font = Minecraft.getInstance().font;

        g.fill(0, 0, w, h, UITheme.BG_BLACK);
        if (bgIndex < BACKGROUNDS.length) {
            g.setColor(1f, 1f, 1f, 0.3f);
            g.blit(BACKGROUNDS[bgIndex], 0, 0, 0, 0, w, h, w, h);
            g.setColor(1f, 1f, 1f, 1f);
        }

        g.fill(0, 0, w, h,
            AnimationHelper.withAlpha(UITheme.BG_BLACK, 0xBB));

        logoAlpha   = Math.min(1f, logoAlpha   + 0.03f);
        accentLineW = Math.min(1f, accentLineW + 0.04f);
        barPulse   += 0.08f;

        int cx = w / 2;
        int cy = h / 2;
        int alpha = (int)(logoAlpha * 0xFF);

        g.drawString(font, "BATTLEFIELD", cx - font.width("BATTLEFIELD") / 2, cy - 70,
            AnimationHelper.withAlpha(UITheme.ACCENT, alpha));

        g.drawString(font, loadingText, cx - font.width(loadingText) / 2, cy - 50,
            AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int)(logoAlpha * 200)));

        int lineW = (int)(160 * accentLineW);
        g.fill(cx - lineW / 2, cy - 36, cx + lineW / 2, cy - 34,
            AnimationHelper.withAlpha(UITheme.ACCENT, alpha));

        int barW = 280;
        int barH = 6;
        int barX = cx - barW / 2;
        int barY = cy - 10;

        g.fill(barX, barY, barX + barW, barY + barH,
            AnimationHelper.withAlpha(UITheme.BG_SLOT, alpha));

        int fillW = (int)(barW * progress);
        if (fillW > 0) {
            float pulseAlpha = 0.7f + 0.3f * (float)Math.sin(barPulse);
            g.fill(barX, barY, barX + fillW, barY + barH,
                AnimationHelper.withAlpha(UITheme.ACCENT, (int)(alpha * pulseAlpha)));
            if (fillW > 3) {
                g.fill(barX + fillW - 3, barY, barX + fillW, barY + barH,
                    AnimationHelper.withAlpha(UITheme.ACCENT, 0xBB));
            }
        }

        g.fill(barX - 1, barY - 1, barX + barW + 1, barY,
            AnimationHelper.withAlpha(UITheme.BORDER, alpha));
        g.fill(barX - 1, barY + barH, barX + barW + 1, barY + barH + 1,
            AnimationHelper.withAlpha(UITheme.BORDER, alpha));
        g.fill(barX - 1, barY, barX, barY + barH,
            AnimationHelper.withAlpha(UITheme.BORDER, alpha));
        g.fill(barX + barW, barY, barX + barW + 1, barY + barH,
            AnimationHelper.withAlpha(UITheme.BORDER, alpha));

        String pctText = String.format("%.0f%%", progress * 100);
        g.drawString(font, pctText, cx - font.width(pctText) / 2, cy + 16,
            AnimationHelper.withAlpha(UITheme.TEXT_SECONDARY, (int)(logoAlpha * 200)));

        String hint = "Initializing combat systems...";
        g.drawString(font, hint, cx - font.width(hint) / 2, cy + 40,
            AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int)(logoAlpha * 150)));

        String ver = "BattlefieldMC v2.0 \u00B7 MC 1.20.1 Forge";
        int vw = font.width(ver);
        g.drawString(font, ver, cx - vw / 2, h - 16,
            AnimationHelper.withAlpha(UITheme.TEXT_MUTED, 0x66));
    }

    public void setLoadingText(String text) { this.loadingText = text; }

    public void dismiss() {
        dismissed = true;
        Minecraft.getInstance().setOverlay(null);
        INSTANCE = null;
    }
}