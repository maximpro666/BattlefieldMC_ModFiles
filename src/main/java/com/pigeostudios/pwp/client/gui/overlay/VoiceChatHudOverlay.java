package com.pigeostudios.pwp.client.gui.overlay;

import com.pigeostudios.pwp.client.ClientTeamData;
import com.pigeostudios.pwp.client.ClientVoiceHandler;
import com.pigeostudios.pwp.client.gui.UITheme;
import com.pigeostudios.pwp.client.gui.component.AnimationHelper;
import com.pigeostudios.pwp.client.gui.component.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

public class VoiceChatHudOverlay {

    private static final int LINE_H = 16;
    private static final int PADDING = 6;
    private static final int MARGIN = 10;
    private static final int MAX_VISIBLE = 6;

    private float panelAlpha = 0f;

    public void render(GuiGraphics g, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        if (font == null || mc.player == null) return;

        boolean russian = "ru".equals(ClientTeamData.language);
        List<ClientVoiceHandler.SpeakingEntry> speakers = ClientVoiceHandler.getActiveGroupSpeakers();
        boolean pttActive = ClientVoiceHandler.isPttActive();
        int channel = ClientVoiceHandler.getActiveChannelGroup();
        float anim = ClientVoiceHandler.getVoiceAnim();

        boolean hasContent = pttActive || !speakers.isEmpty();
        if (!hasContent) {
            panelAlpha = AnimationHelper.lerp(panelAlpha, 0f, 0.1f);
            return;
        }
        panelAlpha = AnimationHelper.lerp(panelAlpha, 1f, 0.15f);
        if (panelAlpha < 0.01f) return;

        int count = Math.min(speakers.size(), MAX_VISIBLE);
        int lines = count;
        if (pttActive) lines++;
        int panelH = lines * LINE_H + PADDING * 2;

        int maxW = 0;
        if (pttActive) {
            String tag = channel == ClientVoiceHandler.CHANNEL_SQUAD
                ? (russian ? "\u0421\u041A\u0412\u0410\u0414" : "SQUAD")
                : (russian ? "\u041A\u041E\u041C\u0410\u041D\u0414\u0410" : "TEAM");
            maxW = Math.max(maxW, font.width("\u25B6 [" + tag + "]"));
        }
        for (int i = 0; i < count; i++) {
            String tag = speakers.get(i).channel() == ClientVoiceHandler.CHANNEL_SQUAD
                ? (russian ? "\u0421\u041A\u0412\u0410\u0414" : "SQUAD")
                : (russian ? "\u041A\u041E\u041C\u0410\u041D\u0414\u0410" : "TEAM");
            maxW = Math.max(maxW, font.width("\u266B [" + tag + "] " + speakers.get(i).name()));
        }
        int panelW = maxW + PADDING * 2 + 6;

        int x = screenWidth - panelW - MARGIN;
        int y = screenHeight - panelH - MARGIN;

        int bgAlpha = (int)(panelAlpha * UITheme.ALPHA_HUD);
        RenderHelper.roundedRect(g, x, y, panelW, panelH, 4,
            AnimationHelper.withAlpha(UITheme.BG_HUD, bgAlpha));

        int cy = y + PADDING;

        if (pttActive) {
            String tag = channel == ClientVoiceHandler.CHANNEL_SQUAD
                ? (russian ? "\u0421\u041A\u0412\u0410\u0414" : "SQUAD")
                : (russian ? "\u041A\u041E\u041C\u0410\u041D\u0414\u0410" : "TEAM");
            int chColor = channel == ClientVoiceHandler.CHANNEL_SQUAD
                ? UITheme.VOICE_SQUAD : UITheme.VOICE_TEAM;
            int alpha = (int)(0xFF * panelAlpha * anim);
            g.drawString(font, "\u25B6 [" + tag + "]", x + PADDING, cy,
                AnimationHelper.withAlpha(chColor, alpha));
            cy += LINE_H;
        }

        for (int i = 0; i < count; i++) {
            ClientVoiceHandler.SpeakingEntry entry = speakers.get(i);
            int ch = entry.channel();
            String tag = ch == ClientVoiceHandler.CHANNEL_SQUAD
                ? (russian ? "\u0421\u041A\u0412\u0410\u0414" : "SQUAD")
                : (russian ? "\u041A\u041E\u041C\u0410\u041D\u0414\u0410" : "TEAM");
            int chColor = ch == ClientVoiceHandler.CHANNEL_SQUAD
                ? UITheme.VOICE_SQUAD : UITheme.VOICE_TEAM;
            int alpha = (int)(0xFF * panelAlpha);
            g.drawString(font, "\u266B [" + tag + "] " + entry.name(),
                x + PADDING, cy, AnimationHelper.withAlpha(chColor, alpha));
            cy += LINE_H;
        }
    }
}
