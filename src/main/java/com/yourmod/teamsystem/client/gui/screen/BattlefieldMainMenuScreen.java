package com.yourmod.teamsystem.client.gui.screen;

import com.yourmod.teamsystem.client.gui.UITheme;
import com.yourmod.teamsystem.client.gui.component.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class BattlefieldMainMenuScreen extends Screen {

    private static final ResourceLocation[] BACKGROUNDS = {
        new ResourceLocation("teamsystem", "textures/gui/bg_0.png"),
        new ResourceLocation("teamsystem", "textures/gui/bg_1.png"),
        new ResourceLocation("teamsystem", "textures/gui/bg_2.png"),
    };

    private int    bgIndex      = 0;
    private float  bgAlpha      = 1f;
    private float  nextAlpha    = 0f;
    private boolean crossfading = false;
    private long   lastSwitch   = System.currentTimeMillis();
    private static final long SWITCH_INTERVAL = 8000L;

    private float fadeAlpha   = 0f;
    private float logoFloat   = 0f;
    private float buttonsY    = 0f;
    private float accentLineW = 0f;
    private long  openTime;

    public BattlefieldMainMenuScreen() {
        super(Component.literal("Battlefield - Main Menu"));
    }

    @Override
    protected void init() {
        openTime = System.currentTimeMillis();
        int cx = width / 2;
        int startY = Math.max(height / 2 + 40, height - 220);
        int btnW = 220;
        int btnH = 28;
        int gap  = 8;

        addRenderableWidget(new BButton(cx - btnW / 2, startY, btnW, btnH,
            Component.literal("Play"), btn ->
                Minecraft.getInstance().setScreen(new JoinMultiplayerScreen(this)),
            BButton.Variant.PRIMARY));

        addRenderableWidget(new BButton(cx - btnW / 2, startY + btnH + gap, btnW, btnH,
            Component.literal("Settings"), btn ->
                Minecraft.getInstance().setScreen(new SettingsMenuScreen())));

        addRenderableWidget(new BButton(cx - btnW / 2, startY + (btnH + gap) * 2, btnW, btnH,
            Component.literal("Quit"), btn -> Minecraft.getInstance().stop(),
            BButton.Variant.DANGER));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        long now = System.currentTimeMillis();
        float elapsed = (now - openTime) / 400f;
        fadeAlpha = Math.min(1f, elapsed);
        logoFloat = (float)(Math.sin(now / 1800.0) * 2.0);
        buttonsY  = Math.min(1f, buttonsY + 0.035f);
        accentLineW = Math.min(1f, accentLineW + 0.05f);

        if (!crossfading && now - lastSwitch > SWITCH_INTERVAL) crossfading = true;
        if (crossfading) {
            nextAlpha = Math.min(1f, nextAlpha + 0.02f);
            bgAlpha   = Math.max(0f, bgAlpha   - 0.02f);
            if (nextAlpha >= 1f) {
                bgIndex     = (bgIndex + 1) % BACKGROUNDS.length;
                bgAlpha     = 1f; nextAlpha = 0f;
                crossfading = false; lastSwitch = now;
            }
        }

        g.fill(0, 0, width, height, UITheme.BG_BLACK);
        if (bgAlpha > 0.01f) {
            g.setColor(1f, 1f, 1f, bgAlpha * fadeAlpha);
            g.blit(BACKGROUNDS[bgIndex], 0, 0, 0, 0, width, height, width, height);
            g.setColor(1f, 1f, 1f, 1f);
        }
        if (crossfading && nextAlpha > 0.01f) {
            int nextIdx = (bgIndex + 1) % BACKGROUNDS.length;
            g.setColor(1f, 1f, 1f, nextAlpha * fadeAlpha);
            g.blit(BACKGROUNDS[nextIdx], 0, 0, 0, 0, width, height, width, height);
            g.setColor(1f, 1f, 1f, 1f);
        }

        g.fill(0, 0, width, height,
            AnimationHelper.withAlpha(UITheme.BG_BLACK, (int)(fadeAlpha * 0x88)));

        int logoY = Math.max(60, height / 2 - 100) + (int)logoFloat;
        String line1 = "BATTLEFIELD";
        String sub   = "TACTICAL COMBAT";
        int w1 = font.width(line1) * 2;
        int w2 = font.width(sub);
        int cx = width / 2;
        int alpha = (int)(fadeAlpha * 0xFF);

        g.drawString(font, line1, cx - w1 / 2 + 2, logoY + 2,
            AnimationHelper.withAlpha(UITheme.BG_BLACK, (int)(fadeAlpha * 200)));
        g.drawString(font, line1, cx - w1 / 2, logoY,
            AnimationHelper.withAlpha(UITheme.ACCENT, alpha));
        g.drawString(font, sub, cx - w2 / 2, logoY + 18,
            AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int)(fadeAlpha * 180)));

        int lineW = (int)(120 * accentLineW);
        g.fill(cx - lineW / 2, logoY + 34, cx + lineW / 2, logoY + 36,
            AnimationHelper.withAlpha(UITheme.ACCENT, alpha));

        float btnAlpha = buttonsY;
        g.pose().pushPose();
        g.pose().translate(0, (1f - AnimationHelper.easeOutCubic(buttonsY)) * 20f, 0);

        g.setColor(1f, 1f, 1f, btnAlpha);
        super.render(g, mx, my, pt);
        g.setColor(1f, 1f, 1f, 1f);

        g.pose().popPose();

        String ver = "BattlefieldMC v2.0 \u00B7 MC 1.20.1 Forge";
        int vw = font.width(ver);
        g.drawString(font, ver, cx - vw / 2, height - 14,
            AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int)(fadeAlpha * 120)));
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
