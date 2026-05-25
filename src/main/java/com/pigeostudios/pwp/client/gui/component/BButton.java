package com.pigeostudios.pwp.client.gui.component;

import com.pigeostudios.pwp.client.ClientSoundHandler;
import com.pigeostudios.pwp.client.gui.UITheme;
import com.pigeostudios.pwp.core.ModSounds;
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
    private boolean wasHovered = false;
    private float pressAnim = 0f;

    public BButton(int x, int y, int width, int height, Component text, OnPress onPress) {
        super(x, y, width, height, text, onPress, DEFAULT_NARRATION);
    }

    public BButton(int x, int y, int width, int height, Component text, OnPress onPress, Variant variant) {
        super(x, y, width, height, text, onPress, DEFAULT_NARRATION);
        this.variant = variant;
    }

    public BButton setVariant(Variant v) { this.variant = v; return this; }

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        boolean hovered = isHoveredOrFocused();

        if (hovered && !wasHovered) {
            ClientSoundHandler.playGuiSound(ModSounds.GUI_BUTTON_HOVER);
        }
        wasHovered = hovered;

        hoverAnim = AnimationHelper.lerp(hoverAnim, hovered ? 1f : 0f, 0.12f);
        pressAnim = AnimationHelper.lerp(pressAnim, 0f, 0.10f);

        int borderColor;
        int fillColor;
        int textColor;

        switch (variant) {
            case PRIMARY:
                fillColor = AnimationHelper.blendColors(UITheme.ACCENT, 0xFFFF8C0A, hoverAnim);
                borderColor = fillColor;
                textColor = 0xFFFFFFFF;
                break;
            case GHOST:
                fillColor = AnimationHelper.withAlpha(UITheme.ACCENT_GHOST, (int)(0x26 * hoverAnim));
                borderColor = 0x00000000;
                textColor = AnimationHelper.blendColors(UITheme.TEXT_SECONDARY, UITheme.ACCENT, hoverAnim);
                break;
            case DANGER:
                fillColor = AnimationHelper.blendColors(0x00000000, 0x223A1010, (int)(hoverAnim * 255));
                borderColor = AnimationHelper.blendColors(UITheme.BORDER, UITheme.STATUS_DANGER, hoverAnim);
                textColor = AnimationHelper.blendColors(UITheme.TEXT_SECONDARY, UITheme.STATUS_DANGER, hoverAnim);
                break;
            default:
                fillColor = AnimationHelper.blendColors(0xDD141414, 0xDD1E1E1E, hoverAnim);
                borderColor = AnimationHelper.blendColors(UITheme.BORDER, UITheme.ACCENT_DIM, hoverAnim);
                textColor = AnimationHelper.blendColors(0xFFB0B0B0, UITheme.ACCENT, hoverAnim * 0.8f);
                break;
        }

        int sx = getX();
        int sy = getY();
        int sw = width;
        int sh = height;

        // Rounded corners via corner fills
        g.fill(sx + 2, sy, sx + sw - 2, sy + sh, fillColor);
        g.fill(sx, sy + 2, sx + 2, sy + sh - 2, fillColor);
        g.fill(sx + sw - 2, sy + 2, sx + sw, sy + sh - 2, fillColor);

        // Borders
        g.fill(sx, sy, sx + sw, sy + 1, borderColor);
        g.fill(sx, sy + sh - 1, sx + sw, sy + sh, borderColor);
        g.fill(sx, sy, sx + 1, sy + sh, borderColor);
        g.fill(sx + sw - 1, sy, sx + sw, sy + sh, borderColor);

        if (variant == Variant.DEFAULT) {
            int accentAlpha = (int)(150 + hoverAnim * 105);
            g.fill(sx + 1, sy + 2, sx + 3, sy + sh - 2, AnimationHelper.withAlpha(UITheme.ACCENT, accentAlpha));
        }

        var font = Minecraft.getInstance().font;
        var txt = getMessage();
        int textColorFinal = textColor;

        int tx = sx + (sw - font.width(txt)) / 2;
        int ty = sy + (sh - font.lineHeight) / 2;
        g.drawString(font, txt, tx, ty, textColorFinal);
    }

    @Override
    public void onPress() {
        pressAnim = 1f;
        ClientSoundHandler.playGuiSound(ModSounds.GUI_BUTTON_CLICK);
        super.onPress();
    }

    @Override
    public void playDownSound(net.minecraft.client.sounds.SoundManager handler) {
    }
}
