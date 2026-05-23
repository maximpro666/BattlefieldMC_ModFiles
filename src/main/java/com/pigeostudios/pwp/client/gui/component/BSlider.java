package com.pigeostudios.pwp.client.gui.component;

import com.pigeostudios.pwp.client.gui.UITheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import java.util.function.Consumer;

public class BSlider extends AbstractWidget {

    private static final int COLOR_BORDER   = 0xFF2E2E2E;
    private static final int COLOR_TRACK    = 0xFF3A3A3A;
    private static final int COLOR_FILL     = 0xFFE07B00;
    private static final int COLOR_HANDLE   = 0xFFFFFFFF;
    private static final int COLOR_HANDLE_HOVER = 0xFFFFE0B0;
    private static final int COLOR_TEXT     = 0xFFB0B0B0;
    private static final int COLOR_VALUE    = 0xFFEFEFEF;

    private float value;
    private final Consumer<Float> onChange;
    private float hoverAnim = 0f;
    private boolean wasHovered = false;

    public BSlider(int x, int y, int width, int height, Component label, float initial, Consumer<Float> onChange) {
        super(x, y, width, height, label);
        this.value = initial;
        this.onChange = onChange;
    }

    @Override
    public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
        boolean hovered = isHoveredOrFocused();
        wasHovered = hovered;
        hoverAnim = AnimationHelper.lerp(hoverAnim, hovered ? 1f : 0f, 0.12f);

        int sx = getX();
        int sy = getY();
        int sw = width;
        int sh = height;

        // Background
        int bgColor = AnimationHelper.blendColors(0xDD141414, 0xDD1E1E1E, hoverAnim);
        g.fill(sx + 2, sy, sx + sw - 2, sy + sh, bgColor);
        g.fill(sx, sy + 2, sx + 2, sy + sh - 2, bgColor);
        g.fill(sx + sw - 2, sy + 2, sx + sw, sy + sh - 2, bgColor);

        // Borders
        int borderColor = AnimationHelper.blendColors(COLOR_BORDER, UITheme.ACCENT_DIM, hoverAnim);
        g.fill(sx, sy, sx + sw, sy + 1, borderColor);
        g.fill(sx, sy + sh - 1, sx + sw, sy + sh, borderColor);
        g.fill(sx, sy, sx + 1, sy + sh, borderColor);
        g.fill(sx + sw - 1, sy, sx + sw, sy + sh, borderColor);

        // Orange accent bar on left
        int accentAlpha = (int)(150 + hoverAnim * 105);
        g.fill(sx + 1, sy + 2, sx + 3, sy + sh - 2, AnimationHelper.withAlpha(UITheme.ACCENT, accentAlpha));

        // Track
        int trackX = sx + 12;
        int trackW = sw - 28;
        int trackY = sy + sh / 2 - 1;
        int trackH = 3;

        g.fill(trackX, trackY, trackX + trackW, trackY + trackH, COLOR_TRACK);

        // Track fill
        int fillW = (int)(value * trackW);
        if (fillW > 0) {
            g.fill(trackX, trackY, trackX + fillW, trackY + trackH, COLOR_FILL);
        }

        // Handle (circle)
        int handleRadius = 5 + (int)(hoverAnim * 1);
        int handleX = trackX + fillW;
        int handleY = trackY + trackH / 2;
        int handleColor = AnimationHelper.blendColors(COLOR_HANDLE, COLOR_HANDLE_HOVER, hoverAnim);

        g.fill(handleX - handleRadius, handleY - handleRadius,
               handleX + handleRadius, handleY + handleRadius, handleColor);
        if (hoverAnim > 0.01f) {
            int glowAlpha = (int)(hoverAnim * 60);
            int glowColor = AnimationHelper.withAlpha(UITheme.ACCENT, glowAlpha);
            g.fill(handleX - handleRadius - 2, handleY - handleRadius - 2,
                   handleX + handleRadius + 2, handleY + handleRadius + 2, glowColor);
        }

        // Label
        var font = Minecraft.getInstance().font;
        String label = getMessage().getString();
        int colonIdx = label.indexOf(": ");
        String labelText = colonIdx >= 0 ? label.substring(0, colonIdx) : label;
        String valueText = colonIdx >= 0 ? label.substring(colonIdx + 2) : "";

        g.drawString(font, labelText, sx + 8, sy + (sh - font.lineHeight) / 2, COLOR_TEXT);

        if (!valueText.isEmpty()) {
            int vw = font.width(valueText);
            g.drawString(font, valueText, sx + sw - 8 - vw, sy + (sh - font.lineHeight) / 2, COLOR_VALUE);
        }
    }

    @Override
    public void onClick(double mx, double my) {
        updateFromMouse(mx);
    }

    @Override
    public void onDrag(double mx, double my, double dx, double dy) {
        updateFromMouse(mx);
    }

    private void updateFromMouse(double mx) {
        int trackX = getX() + 12;
        int trackW = width - 28;
        float v = (float)((mx - trackX) / trackW);
        v = Math.max(0, Math.min(1, v));
        if (Math.abs(v - value) > 0.005f) {
            value = v;
            onChange.accept(v);
        }
    }

    public float getValue() { return value; }
    public void setValue(float v) { this.value = Math.max(0, Math.min(1, v)); }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput out) {
    }

    @Override
    public void playDownSound(net.minecraft.client.sounds.SoundManager handler) {
    }
}
