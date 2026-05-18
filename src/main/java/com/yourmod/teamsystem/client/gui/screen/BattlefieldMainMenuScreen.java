package com.yourmod.teamsystem.client.gui.screen;

import com.yourmod.teamsystem.client.gui.UITheme;

import com.yourmod.teamsystem.client.gui.component.BButton;
import com.yourmod.teamsystem.client.gui.component.AnimationHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class BattlefieldMainMenuScreen extends Screen {

    private static final int COLOR_ORANGE  = UITheme.ACCENT;
    private static final int COLOR_BG      = UITheme.BG_BLACK;
    private static final int COLOR_TEXT    = UITheme.TEXT_PRIMARY;
    private static final int COLOR_SUBTEXT = UITheme.TEXT_MUTED;

    private static final ResourceLocation[] BACKGROUNDS = {
        new ResourceLocation("teamsystem", "textures/gui/bg_0.png"),
        new ResourceLocation("teamsystem", "textures/gui/bg_1.png"),
        new ResourceLocation("teamsystem", "textures/gui/bg_2.png"),
    };

    private int    bgIndex     = 0;
    private float  bgAlpha     = 1f;
    private float  nextAlpha   = 0f;
    private boolean crossfading = false;
    private long   lastSwitch  = System.currentTimeMillis();
    private static final long SWITCH_INTERVAL = 5000L;
    private static final float FADE_SPEED     = 0.025f;

    private float fadeAlpha = 0f;
    private long openTime;
    private float logoFloat = 0f;

    public BattlefieldMainMenuScreen() {
        super(Component.literal("Battlefield - Main Menu"));
    }

    @Override
    protected void init() {
        openTime = System.currentTimeMillis();
        int cx = width / 2;
        int startY = height / 2 + 10;
        int btnW = 200;
        int btnH = 22;
        int gap  = 8;

        addRenderableWidget(new BButton(cx - btnW / 2, startY, btnW, btnH,
            Component.literal("Play"), btn -> {
            }));

        addRenderableWidget(new BButton(cx - btnW / 2, startY + btnH + gap, btnW, btnH,
            Component.literal("Settings"), btn ->
                Minecraft.getInstance().setScreen(new SettingsMenuScreen())));

        addRenderableWidget(new BButton(cx - btnW / 2, startY + (btnH + gap) * 2, btnW, btnH,
            Component.literal("Quit"), btn -> Minecraft.getInstance().stop()));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        long now = System.currentTimeMillis();
        float elapsed = (now - openTime) / 300f;
        fadeAlpha = Math.min(1f, elapsed);
        logoFloat = (float)(Math.sin(now / 1800.0) * 3.0);

        if (!crossfading && now - lastSwitch > SWITCH_INTERVAL) {
            crossfading = true;
        }
        if (crossfading) {
            nextAlpha = Math.min(1f, nextAlpha + FADE_SPEED);
            bgAlpha   = Math.max(0f, bgAlpha   - FADE_SPEED);
            if (nextAlpha >= 1f) {
                bgIndex     = (bgIndex + 1) % BACKGROUNDS.length;
                bgAlpha     = 1f;
                nextAlpha   = 0f;
                crossfading = false;
                lastSwitch  = now;
            }
        }

        g.fill(0, 0, width, height, COLOR_BG);
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

        g.fill(0, 0, width, height, AnimationHelper.withAlpha(UITheme.BG_BLACK, (int)(fadeAlpha * 0x88)));

        int logoY = height / 2 - 80 + (int)logoFloat;
        String line1 = "BATTLEFIELD";
        String line2 = "PvP SYSTEM";
        int w1 = font.width(line1) * 2;
        int w2 = font.width(line2) * 2;
        g.drawString(font, line1, width / 2 - w1 / 2 + 2, logoY + 2,
            AnimationHelper.withAlpha(UITheme.BG_BLACK, (int)(fadeAlpha * 200)));
        g.drawString(font, line1, width / 2 - w1 / 2, logoY,
            AnimationHelper.withAlpha(COLOR_ORANGE, (int)(fadeAlpha * 255)));
        g.drawString(font, line2, width / 2 - w2 / 2, logoY + 16,
            AnimationHelper.withAlpha(COLOR_TEXT, (int)(fadeAlpha * 220)));

        String ver = "v1.20.1 \u2022 teamsystem mod";
        int vw = font.width(ver);
        g.drawString(font, ver, width / 2 - vw / 2, height - 16,
            AnimationHelper.withAlpha(COLOR_SUBTEXT, (int)(fadeAlpha * 160)));

        super.render(g, mx, my, pt);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
