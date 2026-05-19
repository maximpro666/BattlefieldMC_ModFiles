package com.yourmod.teamsystem.proxy;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Plugin(id = "teamsystem-proxy", name = "TeamSystem Proxy", version = "1.0.0")
public class TeamSystemProxy {

    private static final MinecraftChannelIdentifier CHANNEL =
        MinecraftChannelIdentifier.create("bfmc", "proxy");

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDir;
    private String lobbyServerName = "lobby";
    private String matchServerName = "match";

    @Inject
    public TeamSystemProxy(ProxyServer proxy, Logger logger, @DataDirectory Path dataDir) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDir = dataDir;
    }

    @Subscribe
    public void onInit(ProxyInitializeEvent event) {
        proxy.getChannelRegistrar().register(CHANNEL);
        proxy.getEventManager().register(this, PluginMessageEvent.class, this::onPluginMessage);
        loadConfig();
        logger.info("TeamSystem Proxy initialized");
    }

    private void loadConfig() {
        try {
            Path cfg = dataDir.resolve("config.json");
            if (Files.exists(cfg)) {
                String json = Files.readString(cfg);
                if (json.contains("\"lobby\"")) lobbyServerName = "lobby";
                if (json.contains("\"match\"")) matchServerName = "match";
            } else {
                String defaultCfg = """
                    {"lobby": "lobby", "match": "match"}
                    """;
                Files.createDirectories(dataDir);
                Files.writeString(cfg, defaultCfg);
            }
        } catch (IOException e) {
            logger.warn("Could not load config, using defaults", e);
        }
    }

    private void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().getId().equals(CHANNEL.getId())) return;
        event.setResult(PluginMessageEvent.ForwardResult.handled());

        byte[] data = event.getData();
        ByteBuffer buf = ByteBuffer.wrap(data);
        int length = buf.getInt();
        byte[] bytes = new byte[length];
        buf.get(bytes);
        String message = new String(bytes, StandardCharsets.UTF_8);

        logger.info("Received proxy message: {}", message);

        switch (message) {
            case "start_match" -> onStartMatch();
            case "end_match" -> onEndMatch();
            default -> logger.warn("Unknown proxy message: {}", message);
        }
    }

    private void onStartMatch() {
        logger.info("Starting match server...");
        CompletableFuture.runAsync(() -> {
            try {
                Path launcherDir = getLauncherDir();

                ProcessBuilder pb = new ProcessBuilder(
                    "powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass",
                    "-File", launcherDir.resolve("start-match.ps1").toString()
                );
                pb.directory(launcherDir.toFile());
                pb.redirectErrorStream(true);

                Process proc = pb.start();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        logger.info("[launcher] {}", line);
                    }
                }
                int exitCode = proc.waitFor();

                if (exitCode == 0) {
                    logger.info("Match server started, moving players...");
                    proxy.getAllPlayers().forEach(p -> {
                        RegisteredServer match = proxy.getServer(matchServerName).orElse(null);
                        if (match != null) {
                            p.createConnectionRequest(match).fireAndForget();
                        }
                    });
                } else {
                    logger.error("Launcher failed with exit code {}", exitCode);
                }
            } catch (Exception e) {
                logger.error("Failed to start match server", e);
            }
        });
    }

    private void onEndMatch() {
        logger.info("Match ended, moving players back to lobby...");
        RegisteredServer lobby = proxy.getServer(lobbyServerName).orElse(null);
        RegisteredServer match = proxy.getServer(matchServerName).orElse(null);

        if (lobby != null && match != null) {
            List<Player> matchPlayers = List.copyOf(match.getPlayersConnected());
            for (Player player : matchPlayers) {
                player.createConnectionRequest(lobby).fireAndForget();
            }
            logger.info("Moved {} players back to lobby", matchPlayers.size());
        }

        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            stopMatchServer();
        });
    }

    private Path getLauncherDir() {
        return Path.of(System.getProperty("user.dir")).resolve("../launcher").normalize();
    }

    private void stopMatchServer() {
        try {
            Path launcherDir = getLauncherDir();

            ProcessBuilder pb = new ProcessBuilder(
                "powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass",
                "-File", launcherDir.resolve("stop-match.ps1").toString()
            );
            pb.directory(launcherDir.toFile());
            pb.redirectErrorStream(true);

            Process proc = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.info("[launcher:stop] {}", line);
                }
            }
            int exitCode = proc.waitFor();
            logger.info("Match server cleaned up (exit {})", exitCode);
        } catch (Exception e) {
            logger.error("Failed to clean up match server", e);
        }
    }
}
