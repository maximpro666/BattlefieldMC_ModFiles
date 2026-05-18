package com.yourmod.teamsystem.client.gui.screen;

import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.client.gui.component.BButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class SettingsMenuScreen extends Screen {
    private final Screen parent;
    private String[] languages = {"ru", "en"};
    private int langIndex;

    public SettingsMenuScreen(Screen parent) {
        super(Component.literal("Settings"));
        this.parent = parent;
        this.langIndex = ClientTeamData.language.equals("ru") ? 0 : 1;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int cy = height / 2 - 40;

        addRenderableWidget(new BButton(cx - 80, cy, 160, 24, "Language: " + (langIndex == 0 ? "RU" : "EN"), btn -> {
            langIndex = (langIndex + 1) % languages.length;
            ClientTeamData.language = languages[langIndex];
            btn.setMessage(Component.literal("Language: " + (langIndex == 0 ? "RU" : "EN")));
        }));
        addRenderableWidget(new BButton(cx - 80, cy + 30, 160, 24, "Back", btn -> {
            minecraft.setScreen(parent);
        }));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        g.fill(0, 0, width, height, 0xCC111111);
        String title = "SETTINGS";
        int titleW = font.width(title);
        g.drawString(font, title, (width - titleW) / 2, height / 2 - 80, 0xFF00AAFF);
        super.render(g, mouseX, mouseY, partialTick);
    }
}
