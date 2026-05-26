package com.pigeostudios.pwp.client;

import club.minnced.discord.rpc.DiscordEventHandlers;
import club.minnced.discord.rpc.DiscordRPC;
import club.minnced.discord.rpc.DiscordRichPresence;
import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.core.Team;
import net.minecraft.client.Minecraft;

public class DiscordRPCHandler {
    private static final DiscordRPC LIB = DiscordRPC.INSTANCE;
    private static DiscordRichPresence presence = new DiscordRichPresence();
    private static Thread callbackThread;
    private static volatile boolean running = false;
    private static String applicationId = "";
    private static boolean enabled = false;
    private static long startTimestamp = 0;

    public static void initOnClient() {
        DiscordConfig config = DiscordConfig.load();
        enabled = config.isEnabled();
        applicationId = config.getApplicationId();

        if (!enabled || applicationId.isEmpty() || "YOUR_DISCORD_APPLICATION_ID".equals(applicationId)) {
            PWP.LOGGER.info("Discord RPC disabled or not configured (set applicationId in config/pwp/discord.json)");
            return;
        }

        DiscordEventHandlers handlers = new DiscordEventHandlers();
        handlers.ready = user -> PWP.LOGGER.info("Discord RPC ready for user {}", user.username);
        handlers.errored = (errorCode, message) -> PWP.LOGGER.error("Discord RPC error [{}]: {}", errorCode, message);
        handlers.disconnected = (errorCode, message) -> PWP.LOGGER.warn("Discord RPC disconnected [{}]: {}", errorCode, message);

        LIB.Discord_Initialize(applicationId, handlers, true, null);

        startTimestamp = System.currentTimeMillis() / 1000;
        running = true;

        callbackThread = new Thread(() -> {
            while (running && !Thread.currentThread().isInterrupted()) {
                updateNow();
                LIB.Discord_RunCallbacks();
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }, "DiscordRPC-Callbacks");
        callbackThread.setDaemon(true);
        callbackThread.start();

        Runtime.getRuntime().addShutdownHook(new Thread(DiscordRPCHandler::shutdown, "DiscordRPC-Shutdown"));

        PWP.LOGGER.info("Discord RPC initialized with App ID: {}", applicationId);
    }

    public static void shutdown() {
        running = false;
        if (callbackThread != null) {
            callbackThread.interrupt();
            callbackThread = null;
        }
        LIB.Discord_Shutdown();
        PWP.LOGGER.info("Discord RPC shut down");
    }

    private static void updateNow() {
        if (!running || !enabled) return;

        Minecraft mc = Minecraft.getInstance();
        boolean hasWorld = mc.level != null && mc.player != null;

        String state;
        String details;
        String largeImageKey = "logo";
        String largeImageText = "PROJECTWARFAREPIGEO";
        String smallImageKey = null;
        String smallImageText = null;

        if (!hasWorld) {
            state = "В главном меню";
            details = "Minecraft 1.20.1 | t.me/PROJECTWARFAREPIGEO";
        } else {
            int phase = ClientTeamData.getGamePhase();
            String mapName = ClientTeamData.getCurrentMapName();
            String teamName = getTeamDisplayName(ClientTeamData.getLocalPlayerTeam());

            switch (phase) {
                case 0:
                    state = "Ожидание в лобби";
                    details = teamName != null ? "Команда: " + teamName : "Лобби";
                    break;
                case 1:
                    state = "Голосование за карту";
                    details = teamName != null ? "Команда: " + teamName : "Лобби";
                    break;
                case 2:
                    state = "Бой на карте " + (mapName.isEmpty() ? "?" : mapName);
                    details = teamName != null ? "Команда: " + teamName : "В игре";
                    smallImageKey = getTeamSmallImage(ClientTeamData.getLocalPlayerTeam());
                    smallImageText = teamName;
                    break;
                case 3:
                    state = "Матч завершается";
                    details = mapName.isEmpty() ? "Подведение итогов" : "Карта: " + mapName;
                    break;
                default:
                    state = "В игре";
                    details = "";
            }
        }

        presence.state = state;
        presence.details = details;
        presence.largeImageKey = largeImageKey;
        presence.largeImageText = largeImageText;

        if (smallImageKey != null) {
            presence.smallImageKey = smallImageKey;
            presence.smallImageText = smallImageText;
        } else {
            presence.smallImageKey = null;
            presence.smallImageText = null;
        }

        presence.startTimestamp = startTimestamp;
        presence.endTimestamp = 0;

        if (hasWorld && ClientTeamData.getGamePhase() == 2) {
            presence.partyId = "pwp_match";
            presence.partySize = mc.getConnection() != null ? mc.getConnection().getOnlinePlayers().size() : 0;
            presence.partyMax = 64;
        } else {
            presence.partyId = null;
            presence.partySize = 0;
            presence.partyMax = 0;
        }

        LIB.Discord_UpdatePresence(presence);
    }

    private static String getTeamDisplayName(Team team) {
        if (team == null) return null;
        switch (team) {
            case NATO: return "НАТО";
            case RUSSIA: return "Россия";
            case SPECTATOR: return "Наблюдатель";
            default: return null;
        }
    }

    private static String getTeamSmallImage(Team team) {
        if (team == null) return null;
        switch (team) {
            case NATO: return "nato";
            case RUSSIA: return "russia";
            default: return null;
        }
    }
}
