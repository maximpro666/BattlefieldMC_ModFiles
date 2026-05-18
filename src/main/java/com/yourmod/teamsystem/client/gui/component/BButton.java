package com.yourmod.teamsystem.client.gui.component;

import com.yourmod.teamsystem.client.ClientSoundHandler;
import com.yourmod.teamsystem.core.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import java.util.function.Consumer;

public class BButton extends Button {
    private int borderColor = 0xFF2E2E2E;
    private int hoverBorderColor = 0xFFE07B00;
    private int fillColor = 0xAA0A0A0A;
    private int hoverFillColor = 0xAA141414;
    private int textColor = 0xFFEFEFEF;
    private int hoverTextColor = 0xFFFFFFFF;
    private int accentColor = 0xFFE07B00;
    private boolean drawAccent = true;
    private float hoverAnim = 0f;
    private boolean wasHovered = false;

    public BButton(int x, int y, int width, int height, Component text, OnPress onPress) {
        super(x, y, width, height, text, onPress, DEFAULT_NARRATION);
    }

    public BButton(int x, int y, int width, int height, Component text, OnPress onPress, boolean drawAccent) {
        super(x, y, width, height, text, onPress, DEFAULT_NARRATION);
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
        if (hovered && !wasHovered) {
            ClientSoundHandler.playGuiSound(ModSounds.GUI_BUTTON_HOVER);
        }
        wasHovered = hovered;
        hoverAnim = AnimationHelper.lerp(hoverAnim, hovered ? 1f : 0f, 0.15f);

        int border = AnimationHelper.blendColors(borderColor, hoverBorderColor, hoverAnim);
        int fill = AnimationHelper.blendColors(fillColor, hoverFillColor, hoverAnim);
        int txtCol = AnimationHelper.blendColors(textColor, hoverTextColor, hoverAnim);

        g.fill(getX(), getY(), getX() + width, getY() + height, fill);
        g.fill(getX(), getY(), getX() + width, getY() + 1, border);
        g.fill(getX(), getY() + height - 1, getX() + width, getY() + height, border);
        g.fill(getX(), getY(), getX() + 1, getY() + height, border);
        g.fill(getX() + width - 1, getY(), getX() + width, getY() + height, border);

        if (drawAccent) {
            g.fill(getX(), getY(), getX() + 2, getY() + height, AnimationHelper.withAlpha(accentColor, (int)(0xFF * (0.6f + hoverAnim * 0.4f))));
        }

        var font = Minecraft.getInstance().font;
        var txt = getMessage();
        int tx = getX() + (width - font.width(txt)) / 2;
        int ty = getY() + (height - font.lineHeight) / 2;
        g.drawString(font, txt, tx, ty, txtCol);
    }

    @Override
    public void onPress() {
        ClientSoundHandler.playGuiSound(ModSounds.GUI_BUTTON_CLICK);
        super.onPress();
    }

    @Override
    public void playDownSound(net.minecraft.client.sounds.SoundManager handler) {
    }

    public static void drawBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }
}
