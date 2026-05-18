package com.yourmod.teamsystem.client.gui.component;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import java.util.function.Consumer;

public class BSlider extends AbstractWidget {

    private static final int COLOR_BG       = 0xAA0A0A0A;
    private static final int COLOR_BORDER   = 0xFF2E2E2E;
    private static final int COLOR_TRACK    = 0xFF555555;
    private static final int COLOR_FILL     = 0xFFE07B00;
    private static final int COLOR_HANDLE   = 0xFFFFFFFF;
    private static final int COLOR_TEXT     = 0xFFEFEFEF;

    private float value;
    private final Consumer<Float> onChange;

    public BSlider(int x, int y, int width, int height, Component label, float initial, Consumer<Float> onChange) {
        super(x, y, width, height, label);
        this.value = initial;
        this.onChange = onChange;
    }

    @Override
    public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
        int trackY = getY() + height / 2 - 2;
        int trackH = 4;

        g.fill(getX(), getY(), getX() + width, getY() + height, COLOR_BG);
        g.fill(getX(), getY(), getX() + width, getY() + 1, COLOR_BORDER);
        g.fill(getX(), getY() + height - 1, getX() + width, getY() + height, COLOR_BORDER);

        int fillW = (int)(value * (width - 10));
        g.fill(getX() + 5, trackY, getX() + 5 + fillW, trackY + trackH, COLOR_FILL);
        g.fill(getX() + 5 + fillW, trackY, getX() + width - 5, trackY + trackH, COLOR_TRACK);

        int hx = getX() + 5 + fillW;
        g.fill(hx - 3, trackY - 2, hx + 3, trackY + trackH + 2, COLOR_HANDLE);

        var font = Minecraft.getInstance().font;
        int tw = font.width(getMessage());
        g.drawString(font, getMessage(), getX() + 4, getY() + (height - font.lineHeight) / 2, COLOR_TEXT);
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
        float v = (float)((mx - getX() - 5) / (width - 10));
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
