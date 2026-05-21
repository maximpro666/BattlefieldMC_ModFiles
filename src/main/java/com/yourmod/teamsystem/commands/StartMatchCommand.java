package com.yourmod.teamsystem.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.yourmod.teamsystem.core.LifecycleNotifier;
import com.yourmod.teamsystem.network.PacketHandler;
import com.yourmod.teamsystem.network.TransferPacket;
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
    private static final String MATCH_ADDRESS = "127.0.0.1:25566";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("startmatch")
            .requires(src -> src.hasPermission(2))
            .executes(ctx -> {
                ctx.getSource().sendSuccess(() -> Component.literal("§aStarting match server..."), true);
                startMatchServerAsync(ctx.getSource().getServer());
                return 1;
            })
        );
    }

    private static void startMatchServerAsync(net.minecraft.server.MinecraftServer server) {
        Thread launcher = new Thread(() -> {
            try {
                Path launcherDir = Path.of(System.getProperty("user.dir")).resolve("../launcher").normalize();
                // Clean up stale flag from previous cycle
                try { Files.deleteIfExists(launcherDir.resolve("match_cycle_done.flag")); } catch (Exception ignored) {}
                // Broadcast: cleaning up old match server
                server.execute(() -> LifecycleNotifier.broadcastToAll(server, "match_cleaning"));
                // Kill any existing match server process and clean up temp dir
                try {
                    ProcessBuilder stopPb = new ProcessBuilder(
                        "powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass",
                        "-File", launcherDir.resolve("stop-match.ps1").toString()
                    );
                    stopPb.directory(launcherDir.toFile());
                    stopPb.redirectErrorStream(true);
                    Process stopProc = stopPb.start();
                    stopProc.waitFor(15, TimeUnit.SECONDS);
                } catch (Exception ignored) {}
                ProcessBuilder pb = new ProcessBuilder(
                    "powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass",
                    "-File", launcherDir.resolve("start-match.ps1").toString()
                );
                pb.directory(launcherDir.toFile());
                pb.redirectErrorStream(true);
                Process proc = pb.start();

                java.util.concurrent.atomic.AtomicBoolean doneSeen = new java.util.concurrent.atomic.AtomicBoolean(false);

                // Consume process output in a separate thread
                Thread outputReader = new Thread(() -> {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            com.yourmod.teamsystem.TeamSystem.LOGGER.info("[launcher] " + line);
                            if (line.contains("Game starts in")) {
                                doneSeen.set(true);
                            }
                        }
                    } catch (Exception e) {
                        // ignore
                    }
                }, "MatchOutputReader");
                outputReader.setDaemon(true);
                outputReader.start();

                // Broadcast: waiting for match server to start
                server.execute(() -> LifecycleNotifier.broadcastToAll(server, "match_waiting"));
                // Wait for port open AND "Done" message in output
                String[] parts = MATCH_ADDRESS.split(":");
                String host = parts[0];
                int port = Integer.parseInt(parts[1]);
                long deadline = System.currentTimeMillis() + 180_000;
                boolean portOpen = false;

                while (System.currentTimeMillis() < deadline) {
                    if (!portOpen) {
                        try (Socket s = new Socket()) {
                            s.connect(new InetSocketAddress(host, port), 5000);
                            portOpen = true;
                        } catch (Exception e) {
                            Thread.sleep(5000);
                            continue;
                        }
                    }
                    if (doneSeen.get()) break;
                    Thread.sleep(500);
                }

                if (portOpen && doneSeen.get()) {
                    server.execute(() -> LifecycleNotifier.broadcastNotification(server, "match_ready", 5000));
                    try { Thread.sleep(1000); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
                    server.execute(() -> {
                        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                            PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p),
                                new TransferPacket(MATCH_ADDRESS));
                        }
                        com.yourmod.teamsystem.TeamSystem.LOGGER.info("Transfer packet sent to all players");
                    });
                } else {
                    server.execute(() -> LifecycleNotifier.broadcastToAll(server, "match_failed"));
                    com.yourmod.teamsystem.TeamSystem.LOGGER.error("Match server did not start in time");
                }

                int exitCode = proc.waitFor();
                com.yourmod.teamsystem.TeamSystem.LOGGER.info("Launcher exited with code {}", exitCode);
            } catch (Exception e) {
                com.yourmod.teamsystem.TeamSystem.LOGGER.error("Failed to start match server", e);
            }
        }, "MatchLauncher");
        launcher.setDaemon(true);
        launcher.start();
    }
}
