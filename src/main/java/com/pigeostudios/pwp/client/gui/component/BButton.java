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

    public BButton variant(Variant v) { this.variant = v; return this; }
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

        float scale = 1f - pressAnim * 0.03f;

        int borderColor;
        int fillColor;
        int textColor;
        boolean drawAccent;

        switch (variant) {
            case PRIMARY:
                fillColor = AnimationHelper.blendColors(UITheme.ACCENT, 0xFFFF8C0A, hoverAnim);
                borderColor = fillColor;
                textColor = 0xFFFFFFFF;
                drawAccent = true;
                break;
            case GHOST:
                fillColor = AnimationHelper.withAlpha(UITheme.ACCENT_GHOST, (int)(0x26 * hoverAnim));
                borderColor = 0x00000000;
                textColor = AnimationHelper.blendColors(UITheme.TEXT_SECONDARY, UITheme.ACCENT, hoverAnim);
                drawAccent = true;
                break;
            case DANGER:
                fillColor = AnimationHelper.blendColors(0x00000000, 0x223A1010, (int)(hoverAnim * 255));
                borderColor = AnimationHelper.blendColors(UITheme.BORDER, UITheme.STATUS_DANGER, hoverAnim);
                textColor = AnimationHelper.blendColors(UITheme.TEXT_SECONDARY, UITheme.STATUS_DANGER, hoverAnim);
                drawAccent = true;
                break;
            default:
                fillColor = AnimationHelper.blendColors(0x00000000, UITheme.BG_SURFACE, (int)(hoverAnim * 0xDD));
                borderColor = AnimationHelper.blendColors(UITheme.BORDER, UITheme.ACCENT_DIM, hoverAnim);
                textColor = AnimationHelper.blendColors(UITheme.TEXT_PRIMARY, UITheme.ACCENT, hoverAnim);
                drawAccent = true;
                break;
        }

        int bw = width;
        int bh = height;
        int sw = (int)(bw * scale);
        int sh = (int)(bh * scale);
        int sx = getX() + (bw - sw) / 2;
        int sy = getY() + (bh - sh) / 2;

        if ((fillColor >> 24 & 0xFF) > 0) {
            g.fill(sx, sy, sx + sw, sy + sh, fillColor);
        }

        g.fill(sx, sy, sx + sw, sy + 1, borderColor);
        g.fill(sx, sy + sh - 1, sx + sw, sy + sh, borderColor);
        g.fill(sx + sw - 1, sy, sx + sw, sy + sh, borderColor);

        if (drawAccent) {
            int accentAlpha = (int)(0xFF * (0.4f + hoverAnim * 0.6f));
            g.fill(sx, sy, sx + 2, sy + sh, AnimationHelper.withAlpha(UITheme.ACCENT, accentAlpha));
        }

        var font = Minecraft.getInstance().font;
        var txt = getMessage();
        int tx = sx + (sw - font.width(txt)) / 2;
        int ty = sy + (sh - font.lineHeight) / 2;
        g.drawString(font, txt, tx, ty, textColor);
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
