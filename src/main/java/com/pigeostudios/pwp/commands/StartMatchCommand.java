package com.pigeostudios.pwp.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.core.GameManager;
import com.pigeostudios.pwp.core.LifecycleNotifier;
import com.pigeostudios.pwp.network.PacketHandler;
import com.pigeostudios.pwp.network.TransferPacket;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public class StartMatchCommand {
    private static final int TIMEOUT_SECONDS = 300;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("startmatch")
            .requires(src -> src.hasPermission(2))
            .executes(ctx -> {
                GameManager game = PWP.getGameManager();
                if (game == null) {
                    ctx.getSource().sendFailure(Component.literal("§cGame manager not available!"));
                    return 0;
                }
                if (game.isVoting()) {
                    ctx.getSource().sendFailure(Component.literal("§cVoting is already in progress!"));
                    return 0;
                }
                if (game.isPlaying()) {
                    ctx.getSource().sendFailure(Component.literal("§cA match is already in progress!"));
                    return 0;
                }
                ctx.getSource().sendSuccess(() -> Component.literal("§aНачинается голосование за карту..."), true);
                game.startVotingWithAutoStart();
                return 1;
            })
        );
    }

    public static Path findLauncherDir() {
        // Try relative to user.dir (servar/lobby directory)
        Path fromUserDir = Path.of(System.getProperty("user.dir")).resolve("../launcher").normalize();
        if (Files.exists(fromUserDir.resolve("start-match.ps1"))) {
            return fromUserDir;
        }
        // Try relative to class location (development environment)
        try {
            Path fromClass = Path.of(StartMatchCommand.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                .getParent().resolve("../launcher").normalize();
            if (Files.exists(fromClass.resolve("start-match.ps1"))) {
                return fromClass;
            }
        } catch (Exception ignored) {}
        // Try relative to the project root (IDE / gradle run)
        Path fromProject = Path.of(System.getProperty("user.dir")).resolve("launcher"); // already in project/launcher
        if (Files.exists(fromProject.resolve("start-match.ps1"))) {
            return fromProject;
        }
        return fromUserDir;
    }

    public static void startMatchServerAsync(net.minecraft.server.MinecraftServer server) {
        Thread launcher = new Thread(() -> {
            Path launcherDir = null;
            Process proc = null;
            try {
                launcherDir = findLauncherDir();
                com.pigeostudios.pwp.PWP.LOGGER.info("Using launcher directory: {}", launcherDir);
                if (!Files.exists(launcherDir.resolve("start-match.ps1"))) {
                    com.pigeostudios.pwp.PWP.LOGGER.error("start-match.ps1 not found in {}", launcherDir);
                    server.execute(() -> LifecycleNotifier.broadcastToAll(server, "match_failed"));
                    return;
                }
                // Clean up stale flags from previous cycle
                try { Files.deleteIfExists(launcherDir.resolve("match_cycle_done.flag")); } catch (Exception ignored) {}
                try { Files.deleteIfExists(launcherDir.resolve("match_active.flag")); } catch (Exception ignored) {}
                // Broadcast: cleaning up old match server
                server.execute(() -> {
                    GameManager gm = PWP.getGameManager();
                    if (gm != null) gm.setLobbyStatus("cleaning");
                    LifecycleNotifier.broadcastToAll(server, "match_cleaning");
                });
                // Kill any existing match server process and clean up temp dir
                try {
                    ProcessBuilder stopPb = new ProcessBuilder(
                        "powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass",
                        "-File", launcherDir.resolve("stop-match.ps1").toString()
                    );
                    stopPb.directory(launcherDir.toFile());
                    stopPb.redirectErrorStream(true);
                    Process stopProc = stopPb.start();
                    stopProc.waitFor(5, TimeUnit.SECONDS);
                    // Wait a bit for port to be free
                    String matchAddr = com.pigeostudios.pwp.PWP.getConfig().getMatchAddress();
                    String[] parts = matchAddr.split(":");
                    String host = parts[0];
                    int port = Integer.parseInt(parts[1]);
                    for (int i = 0; i < 15; i++) {
                        try (Socket s = new Socket()) {
                            s.connect(new InetSocketAddress(host, port), 1000);
                            Thread.sleep(1000);
                        } catch (Exception e) {
                            break;
                        }
                    }
                } catch (Exception e) {
                    com.pigeostudios.pwp.PWP.LOGGER.warn("Failed to stop previous match server: {}", e.getMessage());
                }

                // Broadcast: waiting for match server to start
                server.execute(() -> {
                    GameManager gm = PWP.getGameManager();
                    if (gm != null) gm.setLobbyStatus("waiting");
                    LifecycleNotifier.broadcastToAll(server, "match_waiting");
                });

                ProcessBuilder pb = new ProcessBuilder(
                    "powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass",
                    "-File", launcherDir.resolve("start-match.ps1").toString()
                );
                pb.directory(launcherDir.toFile());
                pb.redirectErrorStream(true);
                Process startedProc = pb.start();
                proc = startedProc;
                final java.util.concurrent.atomic.AtomicBoolean doneSeen = new java.util.concurrent.atomic.AtomicBoolean(false);

                // Consume process output in a separate thread
                final Process readerProc = startedProc;
                Thread outputReader = new Thread(() -> {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(readerProc.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            com.pigeostudios.pwp.PWP.LOGGER.info("[launcher] " + line);
                            if (line.contains("Match server ready")) {
                                doneSeen.set(true);
                            }
                        }
                    } catch (Exception e) {
                        // ignore
                    }
                }, "MatchOutputReader");
                outputReader.setDaemon(true);
                outputReader.start();

                // Wait for script to report "Match server ready" in output
                String[] parts = com.pigeostudios.pwp.PWP.getConfig().getMatchAddress().split(":");
                String host = parts[0];
                int port = Integer.parseInt(parts[1]);
                long deadline = System.currentTimeMillis() + (TIMEOUT_SECONDS * 1000L);
                boolean portOpen = false;
                boolean readySeen = false;

                while (System.currentTimeMillis() < deadline) {
                    if (doneSeen.get() && portOpen) {
                        readySeen = true;
                        break;
                    }
                    if (!portOpen) {
                        try (Socket s = new Socket()) {
                            s.connect(new InetSocketAddress(host, port), 5000);
                            portOpen = true;
                            com.pigeostudios.pwp.PWP.LOGGER.info("Match server port {} is open", port);
                        } catch (Exception e) {
                            Thread.sleep(5000);
                            continue;
                        }
                    }
                    if (doneSeen.get() && portOpen) {
                        readySeen = true;
                        break;
                    }
                    // Check if the script process has exited
                    if (!startedProc.isAlive()) {
                        try { Thread.sleep(500); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
                        if (doneSeen.get() && portOpen) {
                            readySeen = true;
                            break;
                        }
                    }
                    Thread.sleep(500);
                }

                if (readySeen) {
                    // Write match_active.flag so late joiners can be auto-transferred
                    try {
                        Path flag = launcherDir.resolve("match_active.flag");
                        Files.writeString(flag, com.pigeostudios.pwp.PWP.getConfig().getMatchAddress());
                        com.pigeostudios.pwp.PWP.LOGGER.info("Written match_active.flag");
                    } catch (Exception e) {
                        com.pigeostudios.pwp.PWP.LOGGER.warn("Failed to write match_active.flag: {}", e.getMessage());
                    }
                    server.execute(() -> {
                        GameManager gm = PWP.getGameManager();
                        if (gm != null) gm.setLobbyStatus("ready");
                        LifecycleNotifier.broadcastNotification(server, "match_ready", 5000);
                    });
                    try { Thread.sleep(4000); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
                    server.execute(() -> {
                        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                            PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p),
                                new TransferPacket(com.pigeostudios.pwp.PWP.getConfig().getMatchAddress()));
                        }
                        com.pigeostudios.pwp.PWP.LOGGER.info("Transfer packet sent to all players");
                    });
                } else {
                    server.execute(() -> {
                        GameManager gm = PWP.getGameManager();
                        if (gm != null) gm.setLobbyStatus("failed");
                        LifecycleNotifier.broadcastToAll(server, "match_failed");
                    });
                    String reason = !portOpen ? "port never opened" : "match server not ready after port open";
                    com.pigeostudios.pwp.PWP.LOGGER.error("Match server did not start in time: {}", reason);
                }

                int exitCode = proc.waitFor();
                com.pigeostudios.pwp.PWP.LOGGER.info("Launcher exited with code {}", exitCode);
            } catch (Exception e) {
                com.pigeostudios.pwp.PWP.LOGGER.error("Failed to start match server", e);
                if (server != null) {
                    server.execute(() -> {
                        GameManager gm = PWP.getGameManager();
                        if (gm != null) gm.setLobbyStatus("failed");
                        LifecycleNotifier.broadcastToAll(server, "match_failed");
                    });
                }
            } finally {
                // If process is still running, kill it on failure
                if (proc != null && proc.isAlive()) {
                    com.pigeostudios.pwp.PWP.LOGGER.warn("Killing orphaned match server process");
                    proc.destroyForcibly();
                    try { proc.waitFor(5, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
                }
            }
        }, "MatchLauncher");
        launcher.setDaemon(true);
        launcher.start();
    }
}
