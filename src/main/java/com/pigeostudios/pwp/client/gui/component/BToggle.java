package com.pigeostudios.pwp.client.gui.component;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class BToggle extends AbstractWidget {

    private static final int COLOR_BORDER    = 0xFF2E2E2E;
    private static final int COLOR_BG        = 0xDD141414;
    private static final int COLOR_BG_HOVER  = 0xDD1E1E1E;
    private static final int COLOR_TOGGLE_OFF = 0xFF3A3A3A;
    private static final int COLOR_TOGGLE_ON  = 0xFFE07B00;
    private static final int COLOR_HANDLE    = 0xFFFFFFFF;
    private static final int COLOR_TEXT      = 0xFFB0B0B0;
    private static final int COLOR_TEXT_ON   = 0xFFEFEFEF;

    private boolean state;
    private final Consumer<Boolean> onChange;
    private float hoverAnim = 0f;

    public BToggle(int x, int y, int width, int height, Component label, boolean initialState, Consumer<Boolean> onChange) {
        super(x, y, width, height, label);
        this.state = initialState;
        this.onChange = onChange;
    }

    public boolean getState() { return state; }
    public void setState(boolean s) { this.state = s; }

    @Override
    public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
        boolean hovered = isHoveredOrFocused();
        hoverAnim = AnimationHelper.lerp(hoverAnim, hovered ? 1f : 0f, 0.12f);

        int sx = getX();
        int sy = getY();
        int sw = width;
        int sh = height;

        // Background
        int bgColor = AnimationHelper.blendColors(COLOR_BG, COLOR_BG_HOVER, hoverAnim);
        g.fill(sx + 2, sy, sx + sw - 2, sy + sh, bgColor);
        g.fill(sx, sy + 2, sx + 2, sy + sh - 2, bgColor);
        g.fill(sx + sw - 2, sy + 2, sx + sw, sy + sh - 2, bgColor);

        // Borders
        int borderColor = state
            ? AnimationHelper.blendColors(COLOR_BORDER, 0xFFE07B00, hoverAnim * 0.5f)
            : AnimationHelper.blendColors(COLOR_BORDER, 0xFFE07B00, hoverAnim * 0.3f);
        g.fill(sx, sy, sx + sw, sy + 1, borderColor);
        g.fill(sx, sy + sh - 1, sx + sw, sy + sh, borderColor);
        g.fill(sx, sy, sx + 1, sy + sh, borderColor);
        g.fill(sx + sw - 1, sy, sx + sw, sy + sh, borderColor);

        // Accent bar on left when on
        if (state) {
            g.fill(sx + 1, sy + 2, sx + 3, sy + sh - 2, AnimationHelper.withAlpha(COLOR_TOGGLE_ON, 200));
        }

        int accentAlpha = (int)(150 + hoverAnim * 105);
        g.fill(sx + 1, sy + 2, sx + 3, sy + sh - 2, AnimationHelper.withAlpha(COLOR_TOGGLE_ON, accentAlpha));

        // Label
        var font = Minecraft.getInstance().font;
        int labelColor = state ? COLOR_TEXT_ON : COLOR_TEXT;
        g.drawString(font, getMessage(), sx + 8, sy + (sh - font.lineHeight) / 2, labelColor);

        // Toggle switch
        int toggleX = sx + sw - 40;
        int toggleY = sy + (sh - 14) / 2;
        int toggleW = 32;
        int toggleH = 14;

        int bgToggle = state ? COLOR_TOGGLE_ON : COLOR_TOGGLE_OFF;
        g.fill(toggleX, toggleY, toggleX + toggleW, toggleY + toggleH, bgToggle);

        int handleX = state ? toggleX + toggleW - 12 : toggleX + 2;
        int handleY = toggleY + 2;
        int handleS = 10;
        g.fill(handleX, handleY, handleX + handleS, handleY + handleS, COLOR_HANDLE);
    }

    @Override
    public void onClick(double mx, double my) {
        state = !state;
        if (onChange != null) onChange.accept(state);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput out) {
    }

    @Override
    public void playDownSound(net.minecraft.client.sounds.SoundManager handler) {
    }
}
