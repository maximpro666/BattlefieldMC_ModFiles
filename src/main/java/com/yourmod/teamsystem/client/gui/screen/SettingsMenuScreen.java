package com.yourmod.teamsystem.client.gui.screen;

import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.client.ClientSoundHandler;
import com.yourmod.teamsystem.client.gui.I18n;
import com.yourmod.teamsystem.client.gui.UITheme;

import com.yourmod.teamsystem.client.gui.component.BButton;
import com.yourmod.teamsystem.client.gui.component.BSlider;
import com.yourmod.teamsystem.client.gui.component.AnimationHelper;
import com.yourmod.teamsystem.core.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class SettingsMenuScreen extends Screen {

    private static final int COLOR_BG     = UITheme.BG_SCREEN;
    private static final int COLOR_PANEL  = UITheme.BG_PANEL;
    private static final int COLOR_ORANGE = UITheme.ACCENT;
    private static final int COLOR_BORDER = UITheme.BORDER;
    private static final int COLOR_TEXT   = UITheme.TEXT_PRIMARY;

    private float fadeAlpha  = 0f;
    private long openTime;

    public SettingsMenuScreen() {
        super(Component.literal(I18n.get("teamsystem.ui.settings")));
    }

    @Override
    protected void init() {
        openTime = System.currentTimeMillis();
        int cx = width / 2;
        int btnW = 200;
        int btnH = 22;
        int gap = 6;
        int startY = 40;

        BSlider[] volumeSlider = {null};
        volumeSlider[0] = new BSlider(cx - btnW / 2, startY, btnW, btnH,
            Component.literal(I18n.get("teamsystem.ui.volume", (int)(ClientTeamData.guiVolume * 100))),
            ClientTeamData.guiVolume,
            v -> {
                ClientTeamData.guiVolume = v;
                volumeSlider[0].setMessage(Component.literal(I18n.get("teamsystem.ui.volume", (int)(v * 100))));
            });
        addRenderableWidget(volumeSlider[0]);

        addRenderableWidget(new BButton(cx - btnW / 2, startY + (btnH + gap), btnW, btnH,
            Component.literal(I18n.get("teamsystem.ui.language", ClientTeamData.language.toUpperCase())),
            btn -> {
                String lang = ClientTeamData.language.equals("ru") ? "en" : "ru";
                ClientTeamData.language = lang;
                btn.setMessage(Component.literal(I18n.get("teamsystem.ui.language", lang.toUpperCase())));
            }));

        BSlider[] scaleSlider = {null};
        scaleSlider[0] = new BSlider(cx - btnW / 2, startY + (btnH + gap) * 2, btnW, btnH,
            Component.literal(I18n.get("teamsystem.ui.gui_scale", (int)(ClientTeamData.guiScale * 100))),
            (ClientTeamData.guiScale - 0.5f) / 1.0f,
            v -> {
                ClientTeamData.guiScale = 0.5f + v * 1.0f;
                scaleSlider[0].setMessage(Component.literal(I18n.get("teamsystem.ui.gui_scale", (int)(ClientTeamData.guiScale * 100))));
            });
        addRenderableWidget(scaleSlider[0]);

        BSlider[] opacitySlider = {null};
        opacitySlider[0] = new BSlider(cx - btnW / 2, startY + (btnH + gap) * 3, btnW, btnH,
            Component.literal(I18n.get("teamsystem.ui.opacity", (int)(ClientTeamData.guiOpacity * 100))),
            (ClientTeamData.guiOpacity - 0.3f) / 0.7f,
            v -> {
                ClientTeamData.guiOpacity = 0.3f + v * 0.7f;
                opacitySlider[0].setMessage(Component.literal(I18n.get("teamsystem.ui.opacity", (int)(ClientTeamData.guiOpacity * 100))));
            });
        addRenderableWidget(opacitySlider[0]);

        addRenderableWidget(new BButton(cx - btnW / 2, startY + (btnH + gap) * 4, btnW, btnH,
            Component.literal(I18n.get("teamsystem.ui.back")), btn -> onClose()));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        float elapsed = (System.currentTimeMillis() - openTime) / 250f;
        fadeAlpha = Math.min(1f, elapsed);

        g.fill(0, 0, width, height, AnimationHelper.withAlpha(COLOR_BG, (int)(fadeAlpha * 0xCC)));

        int panelW = 240;
        int panelH = 190;
        int panelX = width / 2 - panelW / 2;
        int panelY = 30;

        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, AnimationHelper.withAlpha(COLOR_PANEL, (int)(fadeAlpha * 0xDD)));
        g.fill(panelX, panelY, panelX + panelW, panelY + 3, AnimationHelper.withAlpha(COLOR_ORANGE, (int)(fadeAlpha * 255)));

        String title = I18n.get("teamsystem.ui.settings_uppercase");
        int tw = font.width(title);
        g.drawString(font, title, width / 2 - tw / 2, panelY + 10, AnimationHelper.withAlpha(COLOR_ORANGE, (int)(fadeAlpha * 255)));

        super.render(g, mx, my, pt);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
