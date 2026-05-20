package com.yourmod.teamsystem.client.gui.component;

import com.yourmod.teamsystem.client.ClientSoundHandler;
import com.yourmod.teamsystem.client.gui.UITheme;
import com.yourmod.teamsystem.core.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class BButton extends Button {
    public enum Variant {
        DEFAULT, PRIMARY, GHOST, DANGER
    }

    private Variant variant = Variant.DEFAULT;
    private float hoverAnim = 0f;
    private float pressAnim = 0f;
    private boolean wasHovered = false;
    private boolean wasPressed = false;
    private boolean small = false;

    public BButton(int x, int y, int width, int height, Component text, OnPress onPress) {
        super(x, y, width, height, text, onPress, DEFAULT_NARRATION);
    }

    public BButton(int x, int y, int width, int height, Component text, OnPress onPress, Variant variant) {
        super(x, y, width, height, text, onPress, DEFAULT_NARRATION);
        this.variant = variant;
    }

    public BButton small(boolean v) { this.small = v; return this; }

    public void setVariant(Variant v) { this.variant = v; }

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        boolean hovered = isHoveredOrFocused();
        boolean pressed = hovered && Minecraft.getInstance().mouseHandler.isLeftPressed();

        if (hovered && !wasHovered) {
            ClientSoundHandler.playGuiSound(ModSounds.GUI_BUTTON_HOVER);
        }
        wasHovered = hovered;

        if (pressed && !wasPressed) {
            ClientSoundHandler.playGuiSound(ModSounds.GUI_BUTTON_CLICK);
        }
        wasPressed = pressed;

        hoverAnim = AnimationHelper.lerp(hoverAnim, hovered ? 1f : 0f, 0.12f);
        pressAnim = AnimationHelper.lerp(pressAnim, pressed ? 1f : 0f, 0.15f);

        int pressOffset = pressed ? 1 : 0;

        int borderColor;
        int fillColor;
        int textColor;
        boolean drawAccent;

        switch (variant) {
            case PRIMARY:
                fillColor = AnimationHelper.blendColors(UITheme.ACCENT, 0xFFFF8C0A, hoverAnim);
                borderColor = fillColor;
                textColor = 0xFF000000;
                drawAccent = false;
                break;
            case GHOST:
                fillColor = AnimationHelper.withAlpha(UITheme.ACCENT_GHOST, (int)(0x26 * hoverAnim));
                borderColor = 0x00000000;
                textColor = AnimationHelper.blendColors(UITheme.TEXT_SECONDARY, UITheme.ACCENT, hoverAnim);
                drawAccent = false;
                break;
            case DANGER:
                fillColor = AnimationHelper.blendColors(0x00000000, 0x223A1010, (int)(hoverAnim * 255));
                borderColor = AnimationHelper.blendColors(UITheme.BORDER, UITheme.STATUS_DANGER, hoverAnim);
                textColor = AnimationHelper.blendColors(UITheme.TEXT_SECONDARY, UITheme.STATUS_DANGER, hoverAnim);
                drawAccent = false;
                break;
            default: // DEFAULT
                fillColor = AnimationHelper.blendColors(0x00000000, UITheme.BG_SURFACE, (int)(hoverAnim * 0xDD));
                borderColor = AnimationHelper.blendColors(UITheme.BORDER, UITheme.ACCENT_DIM, hoverAnim);
                textColor = AnimationHelper.blendColors(UITheme.TEXT_PRIMARY, UITheme.ACCENT, hoverAnim);
                drawAccent = true;
                break;
        }

        int x = getX();
        int y = getY() + pressOffset;
        int w = width;
        int h = height;

        // Fill
        if ((fillColor >> 24 & 0xFF) > 0) {
            g.fill(x, y, x + w, y + h, fillColor);
        }

        // Border (top, bottom, right only — left is accent)
        g.fill(x, y, x + w, y + 1, borderColor);
        g.fill(x, y + h - 1, x + w, y + h, borderColor);
        g.fill(x + w - 1, y, x + w, y + h, borderColor);

        // Left accent bar
        if (drawAccent) {
            int accentAlpha = (int)(0xFF * (0.4f + hoverAnim * 0.6f));
            g.fill(x, y, x + 2, y + h, AnimationHelper.withAlpha(UITheme.ACCENT, accentAlpha));
        }

        // Text
        var font = Minecraft.getInstance().font;
        var txt = getMessage();
        int tx = x + (w - font.width(txt)) / 2;
        int ty = y + (h - font.lineHeight) / 2;
        g.drawString(font, txt, tx, ty, textColor);
    }

    @Override
    public void onPress() {
        super.onPress();
    }

    @Override
    public void playDownSound(net.minecraft.client.sounds.SoundManager handler) {
    }
}
