package com.pigeostudios.pwp.client.gui.overlay;

import com.pigeostudios.pwp.client.ClientTeamData;
import com.pigeostudios.pwp.client.ClientVoiceHandler;
import com.pigeostudios.pwp.client.gui.UITheme;
import com.pigeostudios.pwp.client.gui.component.AnimationHelper;
import com.pigeostudios.pwp.client.gui.component.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class VoiceChatHudOverlay {

    private static final int LINE_H = 12;
    private static final int PADDING = 4;
    private static final int MARGIN = 6;
    private static final int MAX_VISIBLE = 5;
    private static final int HIDE_DELAY_MS = 2000;

    private static final int BAR_W = 3;
    private static final int BAR_GAP = 1;
    private static final int BAR_MAX_H = 8;
    private static final int BAR_X_OFF = 4;

    private final Map<UUID, Float> speakerFade = new HashMap<>();
    private long lastActivity = 0;
    private float panelAlpha = 0f;

    private static String channelTag(int ch, boolean russian) {
        return switch (ch) {
            case ClientVoiceHandler.CHANNEL_SQUAD -> russian ? "\u0421\u041A\u0412\u0410\u0414" : "SQUAD";
            case ClientVoiceHandler.CHANNEL_TEAM  -> russian ? "\u041A\u041E\u041C\u0410\u041D\u0414\u0410" : "TEAM";
            default -> russian ? "\u041B\u041E\u041A\u0410\u041B" : "LOCAL";
        };
    }

    private static int channelColor(int ch) {
        return switch (ch) {
            case ClientVoiceHandler.CHANNEL_SQUAD -> UITheme.VOICE_SQUAD;
            case ClientVoiceHandler.CHANNEL_TEAM  -> UITheme.VOICE_TEAM;
            default -> UITheme.VOICE_LOCAL;
        };
    }

    public void render(GuiGraphics g, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        if (font == null || mc.player == null) return;

        boolean russian = "ru".equals(ClientTeamData.language);
        List<ClientVoiceHandler.SpeakingEntry> speakers = ClientVoiceHandler.getActiveGroupSpeakers();
        boolean pttActive = ClientVoiceHandler.isPttActive();
        int channel = ClientVoiceHandler.getActiveChannelGroup();
        float anim = ClientVoiceHandler.getVoiceAnim();

        long now = System.currentTimeMillis();

        boolean hasContent = pttActive || !speakers.isEmpty();
        if (hasContent) {
            lastActivity = now;
        }

        long idleMs = now - lastActivity;
        float targetAlpha = (idleMs < HIDE_DELAY_MS) ? 1f : 0f;
        panelAlpha = AnimationHelper.lerp(panelAlpha, targetAlpha, 0.05f);

        if (panelAlpha < 0.01f) {
            speakerFade.clear();
            return;
        }

        for (ClientVoiceHandler.SpeakingEntry e : speakers) {
            float target = 1f;
            long age = now - e.timestamp();
            long fadeStart = ClientTeamData.SPEAKING_TIMEOUT_MS - 300;
            if (age > fadeStart) {
                target = Math.max(0f, (float)(ClientTeamData.SPEAKING_TIMEOUT_MS - age) / 300f);
            }
            speakerFade.merge(e.uuid(), target, (old, t) -> old + (t - old) * 0.12f);
        }
        speakerFade.entrySet().removeIf(e -> e.getValue() <= 0.01f);

        int count = 0;
        for (ClientVoiceHandler.SpeakingEntry e : speakers) {
            if (speakerFade.getOrDefault(e.uuid(), 0f) > 0.01f) count++;
        }
        count = Math.min(count, MAX_VISIBLE);
        int lines = count;
        if (pttActive) lines++;
        int panelH = lines * LINE_H + PADDING * 2;

        int maxW = 0;
        if (pttActive) {
            String tag = channelTag(channel, russian);
            maxW = Math.max(maxW, font.width("[" + tag + "]") + BAR_X_OFF + (BAR_W + BAR_GAP) * 3);
        }
        int rendered = 0;
        for (ClientVoiceHandler.SpeakingEntry e : speakers) {
            if (rendered >= count) break;
            if (speakerFade.getOrDefault(e.uuid(), 0f) <= 0.01f) continue;
            String tag = channelTag(e.channel(), russian);
            maxW = Math.max(maxW, font.width("[" + tag + "] " + e.name()) + BAR_X_OFF + (BAR_W + BAR_GAP) * 3);
            rendered++;
        }
        int panelW = maxW + PADDING * 2 + 6;

        int x = screenWidth - panelW - MARGIN;
        int y = screenHeight - panelH - MARGIN;

        int bgAlpha = (int)(panelAlpha * UITheme.ALPHA_HUD);
        RenderHelper.roundedRect(g, x, y, panelW, panelH, 4,
            AnimationHelper.withAlpha(UITheme.BG_HUD, bgAlpha));

        int cy = y + PADDING;

        if (pttActive) {
            String tag = channelTag(channel, russian);
            int chColor = channelColor(channel);
            int alpha = (int)(0xFF * panelAlpha * anim);
            g.drawString(font, "\u00BB [" + tag + "]", x + PADDING, cy,
                AnimationHelper.withAlpha(chColor, alpha));
            cy += LINE_H;
        }

        rendered = 0;
        for (ClientVoiceHandler.SpeakingEntry entry : speakers) {
            if (rendered >= count) break;
            float fade = speakerFade.getOrDefault(entry.uuid(), 0f);
            if (fade <= 0.01f) continue;
            rendered++;

            int ch = entry.channel();
            String tag = channelTag(ch, russian);
            int chColor = channelColor(ch);
            int alpha = (int)(0xFF * panelAlpha * fade);

            float timeSec = (now - entry.timestamp()) / 1000f;
            float speed = 4f + (1f - fade) * 4f;
            int barX = x + PADDING;
            int nameX = barX + BAR_X_OFF + (BAR_W + BAR_GAP) * 3;

            for (int b = 0; b < 3; b++) {
                float phase = (float)(b * Math.PI * 2 / 3);
                float raw = (float)Math.sin(timeSec * speed + phase + entry.uuid().hashCode() * 0.1);
                float height = (raw * 0.5f + 0.5f) * BAR_MAX_H * fade;
                int barH = Math.max(1, (int)height);
                int barTop = cy + LINE_H / 2 - barH;

                fillRect(g, barX + b * (BAR_W + BAR_GAP), barTop, BAR_W, barH,
                    AnimationHelper.withAlpha(chColor, alpha));
            }

            g.drawString(font, "[" + tag + "] " + entry.name(),
                nameX, cy, AnimationHelper.withAlpha(chColor, alpha));
            cy += LINE_H;
        }
    }

    private void fillRect(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + h, color);
    }
}
