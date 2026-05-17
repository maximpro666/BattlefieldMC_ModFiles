package com.yourmod.teamsystem.client.gui.component;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

@FunctionalInterface
interface OnPress {
    void onPress(BButton button);
}

public class BButton extends AbstractWidget {
    private static final ResourceLocation TEXTURE = new ResourceLocation("teamsystem", "textures/gui/button.png");
    private final OnPress onPress;
    private boolean toggled;

    public BButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message);
        this.onPress = onPress;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        RenderSystem.setShaderTexture(0, TEXTURE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
        int v = isHoveredOrFocused() ? height : 0;
        graphics.blit(TEXTURE, getX(), getY(), 0, v, width, height, width, height * 2);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int textColor = isHoveredOrFocused() ? 0xFFFFAA00 : 0xFFFFFFFF;
        graphics.drawCenteredString(Minecraft.getInstance().font, this.getMessage(), getX() + width / 2, getY() + (height - 8) / 2, textColor);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        this.onPress.onPress(this);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
        this.defaultButtonNarrationText(narration);
    }

    public boolean isToggled() {
        return toggled;
    }

    public void setToggled(boolean toggled) {
        this.toggled = toggled;
    }
}
