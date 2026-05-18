package com.yourmod.teamsystem.client.gui.component;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import java.util.function.Consumer;

public class BButton extends Button {
    private int borderColor = 0xFF555555;
    private int hoverBorderColor = 0xFFAAAAAA;
    private int fillColor = 0xAA222222;
    private int hoverFillColor = 0xAA444444;
    private int textColor = 0xFFCCCCCC;
    private int hoverTextColor = 0xFFFFFFFF;
    private int accentColor = 0xFF00AAFF;
    private boolean drawAccent = true;

    public BButton(int x, int y, int width, int height, String text, Button.OnPress onPress) {
        super(x, y, width, height, Component.literal(text), onPress, DEFAULT_NARRATION);
    }

    public BButton(int x, int y, int width, int height, String text, Button.OnPress onPress, boolean drawAccent) {
        super(x, y, width, height, Component.literal(text), onPress, DEFAULT_NARRATION);
        this.drawAccent = drawAccent;
    }

    public void setColors(int border, int hoverBorder, int fill, int hoverFill, int text, int hoverText, int accent) {
        this.borderColor = border;
        this.hoverBorderColor = hoverBorder;
        this.fillColor = fill;
        this.hoverFillColor = hoverFill;
        this.textColor = text;
        this.hoverTextColor = hoverText;
        this.accentColor = accent;
    }

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        boolean hovered = isHoveredOrFocused();
        int border = hovered ? hoverBorderColor : borderColor;
        int fill = hovered ? hoverFillColor : fillColor;
        int txtCol = hovered ? hoverTextColor : textColor;

        g.fill(getX(), getY(), getX() + width, getY() + height, fill);
        g.fill(getX(), getY(), getX() + width, getY() + 1, border);
        g.fill(getX(), getY() + height - 1, getX() + width, getY() + height, border);
        g.fill(getX(), getY(), getX() + 1, getY() + height, border);
        g.fill(getX() + width - 1, getY(), getX() + width, getY() + height, border);

        if (drawAccent) {
            g.fill(getX(), getY(), getX() + 2, getY() + height, accentColor);
        }

        var font = Minecraft.getInstance().font;
        var txt = getMessage();
        int tx = getX() + (width - font.width(txt)) / 2;
        int ty = getY() + (height - font.lineHeight) / 2;
        g.drawString(font, txt, tx, ty, txtCol);
    }

    public static void drawBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }
}
