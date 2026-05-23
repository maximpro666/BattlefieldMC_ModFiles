package com.pigeostudios.pwp.client;

import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.network.PacketHandler;
import com.pigeostudios.pwp.network.VoiceChannelSwitchPacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ClientVoiceHandler {

    public record SpeakingEntry(UUID uuid, String name, int channel, long timestamp) {}

    public static final int CHANNEL_LOCAL = 0;
    public static final int CHANNEL_SQUAD = 1;
    public static final int CHANNEL_TEAM  = 2;

    private static boolean voiceChatDetected = false;
    private static int activeChannel = 0;
    private static boolean squadPttDown = false;
    private static boolean teamPttDown = false;
    private static final Map<UUID, SpeakingEntry> speakingPlayers = new ConcurrentHashMap<>();

    private static int releaseTimer = 0;
    private static boolean fadingOut = false;
    private static int fadeChannel = CHANNEL_LOCAL;
    private static float voiceAnim = 0f;
    private static final int RELEASE_DELAY_TICKS = 10;

    private static Object cachedPttHandler;
    private static Field cachedPttField;
    private static boolean reflectionFailed = false;

    static {
        try {
            Class.forName("de.maxhenkel.voicechat.api.VoicechatApi");
            voiceChatDetected = true;
            PWP.LOGGER.info("Simple Voice Chat detected");
        } catch (ClassNotFoundException e) {
            voiceChatDetected = false;
        }
    }

    private static KeyMapping svPttKeyMapping;
    private static boolean svPttKeyChecked = false;

    static void setSVPtt(boolean pressed) {
        if (!voiceChatDetected || reflectionFailed) return;
        if (!pressed && isPhysicalPttDown()) return;
        try {
            if (cachedPttHandler == null) {
                Class<?> cm = Class.forName("de.maxhenkel.voicechat.voice.client.ClientManager");
                Method getHandler = cm.getMethod("getPttKeyHandler");
                Object handler = getHandler.invoke(null);
                if (handler == null) return;
                cachedPttHandler = handler;
                cachedPttField = cachedPttHandler.getClass().getDeclaredField("pttKeyDown");
                cachedPttField.setAccessible(true);
                PWP.LOGGER.info("[Voice] PTT reflection initialized");
            }
            cachedPttField.setBoolean(cachedPttHandler, pressed);
        } catch (Exception e) {
            reflectionFailed = true;
            PWP.LOGGER.error("[Voice] PTT reflection failed", e);
        }
    }

    private static boolean isPhysicalPttDown() {
        try {
            if (!svPttKeyChecked || svPttKeyMapping == null) {
                svPttKeyMapping = null;
                for (KeyMapping km : Minecraft.getInstance().options.keyMappings) {
                    if ("key.pushToTalk".equals(km.getName())) {
                        svPttKeyMapping = km;
                        break;
                    }
                }
                svPttKeyChecked = true;
            }
            if (svPttKeyMapping != null && svPttKeyMapping.getKey().getValue() != -1) {
                long window = Minecraft.getInstance().getWindow().getWindow();
                return org.lwjgl.glfw.GLFW.glfwGetKey(window, svPttKeyMapping.getKey().getValue()) == 1;
            }
        } catch (Exception ignored) {}
        return false;
    }

    public static boolean isVoiceChatDetected() {
        return voiceChatDetected;
    }

    public static int getActiveChannel() {
        return activeChannel;
    }

    public static float getVoiceAnim() {
        return voiceAnim;
    }

    public static boolean isFadingOut() {
        return fadingOut;
    }

    public static void reinforcePtt() {
        setSVPtt(squadPttDown || teamPttDown || fadingOut);
    }

    public static void setSquadPtt(boolean pressed) {
        if (squadPttDown == pressed) return;
        squadPttDown = pressed;
        updateChannel();
    }

    public static void setTeamPtt(boolean pressed) {
        if (teamPttDown == pressed) return;
        teamPttDown = pressed;
        updateChannel();
    }

    private static void sendChannelSwitch(int channel) {
        if (Minecraft.getInstance().getConnection() != null) {
            PacketHandler.CHANNEL.sendToServer(new VoiceChannelSwitchPacket(channel));
        }
    }

    private static void updateChannel() {
        boolean pttActive = squadPttDown || teamPttDown;

        if (pttActive) {
            int newChannel = teamPttDown ? CHANNEL_TEAM : CHANNEL_LOCAL;

            if (newChannel != activeChannel || releaseTimer > 0 || fadingOut) {
                activeChannel = newChannel;
                releaseTimer = 0;
                fadingOut = false;
                fadeChannel = CHANNEL_LOCAL;
                voiceAnim = 1f;
                sendChannelSwitch(activeChannel);
            }
            setSVPtt(true);
            return;
        }

        if (activeChannel != CHANNEL_LOCAL) {
            if (!fadingOut) {
                fadeChannel = activeChannel;
                releaseTimer = RELEASE_DELAY_TICKS;
                fadingOut = true;
            }
            activeChannel = CHANNEL_LOCAL;
            sendChannelSwitch(CHANNEL_LOCAL);
        }
        setSVPtt(false);
    }

    public static void onPlayerSpeaking(UUID playerUUID, String playerName, int channel) {
        if (Minecraft.getInstance().level == null) return;
        long now = System.currentTimeMillis();
        speakingPlayers.put(playerUUID, new SpeakingEntry(playerUUID, playerName, channel, now));
        cleanupExpired();
    }

    public static List<SpeakingEntry> getActiveGroupSpeakers() {
        long now = System.currentTimeMillis();
        return speakingPlayers.values().stream()
            .filter(e -> now - e.timestamp < ClientTeamData.SPEAKING_TIMEOUT_MS)
            .filter(e -> e.channel() != CHANNEL_LOCAL)
            .sorted(Comparator.comparingLong(e -> e.timestamp))
            .collect(Collectors.toList());
    }

    public static boolean isPttActive() {
        return squadPttDown || teamPttDown || fadingOut;
    }

    public static int getActiveChannelGroup() {
        if (teamPttDown || (fadingOut && fadeChannel == CHANNEL_TEAM)) return CHANNEL_TEAM;
        return CHANNEL_LOCAL;
    }

    public static void tick() {
        if (fadingOut) {
            releaseTimer--;
            voiceAnim = Math.max(0f, (float) releaseTimer / RELEASE_DELAY_TICKS);

            if (ClientSetup.SQUAD_VOICE_KEY.isDown() || ClientSetup.TEAM_VOICE_KEY.isDown()) {
                releaseTimer = 0;
                fadingOut = false;
                fadeChannel = CHANNEL_LOCAL;
                voiceAnim = 1f;
            } else if (releaseTimer <= 0) {
                fadingOut = false;
                fadeChannel = CHANNEL_LOCAL;
                voiceAnim = 0f;
            }
        } else if (activeChannel == CHANNEL_LOCAL && voiceAnim > 0f) {
            voiceAnim = Math.max(0f, voiceAnim - 0.05f);
        }

        cleanupExpired();
        if (Minecraft.getInstance().player != null && Minecraft.getInstance().level != null) {
            boolean squadDown = ClientSetup.SQUAD_VOICE_KEY.isDown();
            boolean teamDown = ClientSetup.TEAM_VOICE_KEY.isDown();
            if (squadPttDown != squadDown || teamPttDown != teamDown) {
                squadPttDown = squadDown;
                teamPttDown = teamDown;
                updateChannel();
            }
        }
        setSVPtt(squadPttDown || teamPttDown);
    }

    private static void cleanupExpired() {
        long now = System.currentTimeMillis();
        speakingPlayers.values().removeIf(e -> now - e.timestamp >= ClientTeamData.SPEAKING_TIMEOUT_MS);
    }
}
