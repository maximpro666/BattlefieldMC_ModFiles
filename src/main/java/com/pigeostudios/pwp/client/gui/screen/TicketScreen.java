package com.pigeostudios.pwp.client.gui.screen;

import com.pigeostudios.pwp.client.gui.UITheme;
import com.pigeostudios.pwp.client.gui.component.AnimationHelper;
import com.pigeostudios.pwp.client.gui.component.BButton;
import com.pigeostudios.pwp.network.PacketHandler;
import com.pigeostudios.pwp.network.TicketMessagePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class TicketScreen extends Screen {

    private static final int PW = 280;
    private static final int PH = 300;

    private float fadeAlpha = 0f, slideIn = 0f;
    private long openTime;
    private final int ticketId;

    public TicketScreen(int ticketId) {
        super(Component.literal("Ticket #" + ticketId));
        this.ticketId = ticketId;
    }

    @Override
    protected void init() {
        openTime = System.currentTimeMillis();
        int cx = width / 2, cy = height / 2;

        addRenderableWidget(new BButton(cx - 40, cy + PH / 2 - 24, 80, 20,
            Component.literal("BACK"), btn -> Minecraft.getInstance().setScreen(null), BButton.Variant.GHOST));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        float elapsed = (System.currentTimeMillis() - openTime) / 250f;
        fadeAlpha = Math.min(1f, elapsed);
        slideIn = AnimationHelper.lerp(slideIn, 1f, 0.18f);

        g.fill(0, 0, width, height, (int)(fadeAlpha * 0xDD) << 24 | 0x000000);

        int cx = width / 2, cy = height / 2;
        int px = cx - PW / 2, py = cy - PH / 2;

        float off = (1f - AnimationHelper.easeOutCubic(slideIn)) * 30f;
        g.pose().pushPose();
        g.pose().translate(0, off, 0);

        g.fill(px, py, px + PW, py + PH,
            AnimationHelper.withAlpha(UITheme.BG_PANEL, (int)(fadeAlpha * 0xEE)));
        g.fill(px, py, px + PW, py + 3,
            AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fadeAlpha * 255)));
        int ba = (int)(fadeAlpha * 0xAA);
        g.fill(px, py, px + PW, py + 1, AnimationHelper.withAlpha(UITheme.BORDER, ba));
        g.fill(px, py + PH - 1, px + PW, py + PH, AnimationHelper.withAlpha(UITheme.BORDER, ba));
        g.fill(px, py, px + 1, py + PH, AnimationHelper.withAlpha(UITheme.BORDER, ba));
        g.fill(px + PW - 1, py, px + PW, py + PH, AnimationHelper.withAlpha(UITheme.BORDER, ba));

        g.drawCenteredString(font, "\u00A76TICKET #" + ticketId, cx, py + 16,
            AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fadeAlpha * 255)));
        g.fill(cx - 40, py + 30, cx + 40, py + 32,
            AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fadeAlpha * 0x88)));

        g.drawCenteredString(font, "\u00A77Use /t reply " + ticketId + " \u00A77<msg>", cx, py + PH / 2,
            AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int)(fadeAlpha * 200)));

        g.pose().popPose();
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
