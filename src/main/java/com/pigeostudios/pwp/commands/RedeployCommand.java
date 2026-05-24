package com.pigeostudios.pwp.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.core.GameManager;
import com.pigeostudios.pwp.core.TeamManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RedeployCommand {
    private static final long COOLDOWN_MS = 5000;
    private static final Map<UUID, Long> lastRedeployTime = new ConcurrentHashMap<>();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("redeploy")
            .executes(ctx -> {
                if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                    GameManager game = PWP.getGameManager();
                    if (game == null || !game.isPlaying()) {
                        player.sendSystemMessage(Component.literal("\u00a7cYou can only redeploy during a match"));
                        return 0;
                    }
                    long now = System.currentTimeMillis();
                    Long last = lastRedeployTime.get(player.getUUID());
                    if (last != null && now - last < COOLDOWN_MS) {
                        int remaining = (int)((COOLDOWN_MS - (now - last)) / 1000) + 1;
                        player.sendSystemMessage(Component.literal("\u00a7cWait " + remaining + "s before redeploying again"));
                        return 0;
                    }
                    lastRedeployTime.put(player.getUUID(), now);
                    player.getInventory().clearContent();
                    TeamManager tm = PWP.getTeamManager();
                    if (tm != null) {
                        tm.incrementDeaths(player.getUUID());
                    }
                    player.getPersistentData().putBoolean("pwp:instant_death", true);
                    player.setHealth(0f);
                    player.die(player.damageSources().fellOutOfWorld());
                }
                return Command.SINGLE_SUCCESS;
            }));
    }
}
