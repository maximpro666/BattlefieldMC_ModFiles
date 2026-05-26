package com.pigeostudios.pwp.client.gui.screen;

import com.pigeostudios.pwp.client.gui.UITheme;
import com.pigeostudios.pwp.network.PacketHandler;
import com.pigeostudios.pwp.network.TOSAcceptPacket;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class TOSAgreementScreen extends Screen {
    private static final int COLOR_ORANGE = UITheme.ACCENT;
    private static final int COLOR_MUTED = UITheme.TEXT_MUTED;
    private final String tosUrl;
    private final String privacyUrl;
    private Checkbox acceptCheckbox;
    private Button continueButton;

    public TOSAgreementScreen(String tosUrl, String privacyUrl) {
        super(Component.literal("Соглашение"));
        this.tosUrl = tosUrl;
        this.privacyUrl = privacyUrl;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int cy = height / 2;

        addRenderableWidget(Button.builder(
            Component.literal("• Условия использования"),
            btn -> Util.getPlatform().openUri(tosUrl)
        ).bounds(cx - 120, cy - 30, 240, 20).build());

        addRenderableWidget(Button.builder(
            Component.literal("• Политика конфиденциальности"),
            btn -> Util.getPlatform().openUri(privacyUrl)
        ).bounds(cx - 120, cy, 240, 20).build());

        acceptCheckbox = addRenderableWidget(new Checkbox(
            cx - 120, cy + 35, 240, 20,
            Component.literal("Я принимаю условия использования и политику конфиденциальности"),
            false
        ) {
            @Override
            public void onPress() {
                super.onPress();
                continueButton.active = selected();
            }
        });

        continueButton = addRenderableWidget(Button.builder(
            Component.literal("Продолжить"),
            btn -> {
                PacketHandler.CHANNEL.sendToServer(new TOSAcceptPacket());
                minecraft.setScreen(null);
            }
        ).bounds(cx - 75, cy + 70, 150, 30).build());
        continueButton.active = false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int cx = width / 2;
        int cy = height / 2 - 60;

        graphics.drawCenteredString(font, "Добро пожаловать на Project Warfare Pigeo", cx, cy - 40, COLOR_ORANGE);
        graphics.drawCenteredString(font, "Пожалуйста, ознакомьтесь с условиями:", cx, cy - 10, COLOR_MUTED);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
