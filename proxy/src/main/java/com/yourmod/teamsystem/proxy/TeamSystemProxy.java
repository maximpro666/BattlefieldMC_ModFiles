package com.yourmod.teamsystem.proxy;

import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TeamSystemProxy extends Plugin implements Listener {

    private static final String CHANNEL = "bfmc:proxy";
    private String lobbyServerName = "lobby";
    private String matchServerName = "match";

    @Override
    public void onEnable() {
        getProxy().registerChannel(CHANNEL);
        getProxy().getPluginManager().registerListener(this, this);
        loadConfig();
        getLogger().info("TeamSystem Proxy initialized");
    }

    private void loadConfig() {
        try {
            Path cfg = getDataFolder().toPath().resolve("config.json");
            if (Files.exists(cfg)) {
                String json = Files.readString(cfg);
                if (json.contains("\"lobby\"")) lobbyServerName = "lobby";
                if (json.contains("\"match\"")) matchServerName = "match";
            } else {
                String defaultCfg = """
                    {"lobby": "lobby", "match": "match"}
                    """;
                Files.createDirectories(cfg.getParent());
                Files.writeString(cfg, defaultCfg);
            }
        } catch (IOException e) {
            getLogger().warning("Could not load config, using defaults");
        }
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getTag().equals(CHANNEL)) return;
        event.setCancelled(true);

        byte[] data = event.getData();
        ByteBuffer buf = ByteBuffer.wrap(data);
        int length = buf.getInt();
        byte[] bytes = new byte[length];
        buf.get(bytes);
        String message = new String(bytes, StandardCharsets.UTF_8);

        getLogger().info("Received proxy message: " + message);

        switch (message) {
            case "start_match" -> onStartMatch();
            case "end_match" -> onEndMatch();
            default -> getLogger().warning("Unknown proxy message: " + message);
        }
    }

    private void onStartMatch() {
        getLogger().info("Starting match server...");
        getProxy().getScheduler().runAsync(this, () -> {
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
                        getLogger().info("[launcher] " + line);
                    }
                }
                int exitCode = proc.waitFor();

                if (exitCode == 0) {
                    getLogger().info("Match server started, moving players...");
                    ServerInfo match = getProxy().getServerInfo(matchServerName);
                    if (match != null) {
                        for (ProxiedPlayer p : getProxy().getPlayers()) {
                            p.connect(match);
                        }
                    }
                } else {
                    getLogger().severe("Launcher failed with exit code " + exitCode);
                }
            } catch (Exception e) {
                getLogger().severe("Failed to start match server");
                e.printStackTrace();
            }
        });
    }

    private void onEndMatch() {
        getLogger().info("Match ended, moving players back to lobby...");
        ServerInfo lobby = getProxy().getServerInfo(lobbyServerName);
        ServerInfo match = getProxy().getServerInfo(matchServerName);

        if (lobby != null && match != null) {
            List<ProxiedPlayer> matchPlayers = List.copyOf(match.getPlayers());
            for (ProxiedPlayer player : matchPlayers) {
                player.connect(lobby);
            }
            getLogger().info("Moved " + matchPlayers.size() + " players back to lobby");
        }

        getProxy().getScheduler().schedule(this, () -> stopMatchServer(), 3, TimeUnit.SECONDS);
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
                    getLogger().info("[launcher:stop] " + line);
                }
            }
            int exitCode = proc.waitFor();
            getLogger().info("Match server cleaned up (exit " + exitCode + ")");
        } catch (Exception e) {
            getLogger().severe("Failed to clean up match server");
            e.printStackTrace();
        }
    }
}
