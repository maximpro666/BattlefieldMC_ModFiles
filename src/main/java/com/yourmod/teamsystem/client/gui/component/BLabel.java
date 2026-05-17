package com.yourmod.teamsystem.client.gui.component;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class BLabel implements BComponent {
    protected int x;
    protected int y;
    protected Component text;
    protected int color;

    public BLabel(int x, int y, Component text, int color) {
        this.x = x;
        this.y = y;
        this.text = text;
        this.color = color;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.drawString(Minecraft.getInstance().font, text, x, y, color);
    }

    public void setText(Component text) {
        this.text = text;
    }

    public void setColor(int color) {
        this.color = color;
    }
}
