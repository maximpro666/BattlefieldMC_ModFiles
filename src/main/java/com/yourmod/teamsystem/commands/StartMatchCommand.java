package com.yourmod.teamsystem.commands;

import com.mojang.brigadier.CommandDispatcher;
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
import java.nio.file.Path;

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
                ProcessBuilder pb = new ProcessBuilder(
                    "powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass",
                    "-File", launcherDir.resolve("start-match.ps1").toString()
                );
                pb.directory(launcherDir.toFile());
                pb.redirectErrorStream(true);
                Process proc = pb.start();

                // Read output for logging
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        com.yourmod.teamsystem.TeamSystem.LOGGER.info("[launcher] " + line);
                    }
                }
                int exitCode = proc.waitFor();
                com.yourmod.teamsystem.TeamSystem.LOGGER.info("Launcher exited with code {}", exitCode);

                // Poll for server readiness
                String[] parts = MATCH_ADDRESS.split(":");
                String host = parts[0];
                int port = Integer.parseInt(parts[1]);
                long deadline = System.currentTimeMillis() + 120_000;
                boolean ready = false;

                while (System.currentTimeMillis() < deadline) {
                    try (Socket s = new Socket()) {
                        s.connect(new InetSocketAddress(host, port), 2000);
                        ready = true;
                        break;
                    } catch (Exception e) {
                        Thread.sleep(5000);
                    }
                }

                if (ready) {
                    server.execute(() -> {
                        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                            PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p),
                                new TransferPacket(MATCH_ADDRESS));
                        }
                        com.yourmod.teamsystem.TeamSystem.LOGGER.info("Transfer packet sent to all players");
                    });
                } else {
                    com.yourmod.teamsystem.TeamSystem.LOGGER.error("Match server did not start in time");
                }
            } catch (Exception e) {
                com.yourmod.teamsystem.TeamSystem.LOGGER.error("Failed to start match server", e);
            }
        }, "MatchLauncher");
        launcher.setDaemon(true);
        launcher.start();
    }
}
