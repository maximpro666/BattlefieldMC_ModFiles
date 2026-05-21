package com.yourmod.teamsystem.client.gui.overlay;

import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.client.ClientVoiceHandler;
import com.yourmod.teamsystem.client.gui.UITheme;
import com.yourmod.teamsystem.client.gui.component.AnimationHelper;
import com.yourmod.teamsystem.client.gui.component.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

public class VoiceChatHudOverlay {

    private static final int LINE_H = 18;
    private static final int PADDING = 8;
    private static final int MAX_VISIBLE = 4;

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
            panelAlpha = AnimationHelper.lerp(panelAlpha, 0f, 0.15f);
            return;
        }
        panelAlpha = AnimationHelper.lerp(panelAlpha, 1f, 0.15f);
        if (panelAlpha < 0.01f) return;

        int count = Math.min(speakers.size(), MAX_VISIBLE);
        int baseLines = count;
        if (pttActive) baseLines = Math.max(baseLines, 1);
        int panelH = baseLines * LINE_H + PADDING * 2;

        int maxW = 0;
        String pttLine = null;
        if (pttActive) {
            String pttPrefix = channel == ClientVoiceHandler.CHANNEL_SQUAD
                ? (russian ? "\u0421\u041A\u0412\u0410\u0414" : "SQUAD")
                : (russian ? "\u041A\u041E\u041C\u0410\u041D\u0414\u0410" : "TEAM");
            pttLine = "\u25B6 [" + pttPrefix + "]";
            maxW = Math.max(maxW, font.width(pttLine));
        }
        for (int i = 0; i < count; i++) {
            int ch = speakers.get(i).channel();
            String prefix = ch == ClientVoiceHandler.CHANNEL_SQUAD
                ? (russian ? "\u0421\u041A\u0412\u0410\u0414" : "SQUAD")
                : (russian ? "\u041A\u041E\u041C\u0410\u041D\u0414\u0410" : "TEAM");
            String line = "\u266B [" + prefix + "] " + speakers.get(i).name();
            maxW = Math.max(maxW, font.width(line));
        }
        int panelW = maxW + PADDING * 2 + 8;

        int x = screenWidth / 2 - panelW / 2;
        int y = (int)(screenHeight * 0.38f) - panelH / 2;

        int bgAlpha = (int)(panelAlpha * 180);
        RenderHelper.dropShadow(g, x, y, panelW, panelH, 3, (int)(80 * panelAlpha));
        RenderHelper.roundedRect(g, x, y, panelW, panelH, 4,
            AnimationHelper.withAlpha(UITheme.BG_HUD, bgAlpha));

        int cy = y + PADDING;

        if (pttActive) {
            int chColor = channel == ClientVoiceHandler.CHANNEL_SQUAD
                ? UITheme.VOICE_SQUAD
                : UITheme.VOICE_TEAM;
            int textAlpha = (int)(0xFF * panelAlpha * anim);
            int col = AnimationHelper.withAlpha(chColor, textAlpha);
            g.drawString(font, pttLine, x + PADDING, cy, col);
            cy += LINE_H;
        }

        for (int i = 0; i < count; i++) {
            ClientVoiceHandler.SpeakingEntry entry = speakers.get(i);
            int ch = entry.channel();
            int chColor = ch == ClientVoiceHandler.CHANNEL_SQUAD
                ? UITheme.VOICE_SQUAD
                : UITheme.VOICE_TEAM;
            String prefix = ch == ClientVoiceHandler.CHANNEL_SQUAD
                ? (russian ? "\u0421\u041A\u0412\u0410\u0414" : "SQUAD")
                : (russian ? "\u041A\u041E\u041C\u0410\u041D\u0414\u0410" : "TEAM");
            String channelTag = "[" + prefix + "]";
            String display = "\u266B " + channelTag + " " + entry.name();

            int textAlpha = (int)(0xFF * panelAlpha);
            int col = AnimationHelper.withAlpha(chColor, textAlpha);
            g.drawString(font, display, x + PADDING, cy, col);
            cy += LINE_H;
        }
    }
}
