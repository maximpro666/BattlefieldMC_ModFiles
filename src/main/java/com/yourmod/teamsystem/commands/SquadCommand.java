package com.yourmod.teamsystem.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.core.*;
import net.minecraft.commands.CommandSourceStack;
import java.util.List;
import java.util.UUID;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class SquadCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("squad")
            .then(Commands.literal("create")
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(ctx -> createSquad(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
            .then(Commands.literal("invite")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ctx -> invitePlayer(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
            .then(Commands.literal("join")
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(ctx -> joinSquad(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
            .then(Commands.literal("leave")
                .executes(ctx -> leaveSquad(ctx.getSource())))
            .then(Commands.literal("kick")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ctx -> kickPlayer(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
            .then(Commands.literal("list")
                .executes(ctx -> listSquads(ctx.getSource(), null)))
            .then(Commands.literal("info")
                .executes(ctx -> squadInfo(ctx.getSource())))
            .then(Commands.literal("disband")
                .executes(ctx -> disbandSquad(ctx.getSource())))
            .then(Commands.literal("promote")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ctx -> promoteLeader(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
            .then(Commands.literal("chat")
                .then(Commands.argument("message", StringArgumentType.greedyString())
                    .executes(ctx -> squadChat(ctx.getSource(), StringArgumentType.getString(ctx, "message"))))));
    }

    private static int createSquad(CommandSourceStack source, String name) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        SquadManager sm = TeamSystem.getSquadManager();
        TeamManager tm = TeamSystem.getTeamManager();
        Team playerTeam = tm.getOrCreatePlayerData(player.getUUID()).getTeam();

        if (!playerTeam.isPlayable()) {
            source.sendFailure(Component.literal("§cSpectators cannot create squads"));
            return 0;
        }

        Squad existing = sm.getPlayerSquad(player.getUUID());
        if (existing != null) {
            source.sendFailure(Component.literal("§cYou are already in a squad"));
            return 0;
        }

        Squad squad = sm.createSquad(name, playerTeam, player.getUUID());
        tm.assignSquad(player.getUUID(), squad.getSquadId());

        source.sendSuccess(() -> Component.literal("§aSquad created: §b" + name), true);
        return 1;
    }

    private static int invitePlayer(CommandSourceStack source, ServerPlayer target) {
        ServerPlayer sender = source.getPlayer();
        if (sender == null) return 0;

        SquadManager sm = TeamSystem.getSquadManager();
        Squad squad = sm.getPlayerSquad(sender.getUUID());

        if (squad == null) {
            source.sendFailure(Component.literal("§cYou are not in a squad"));
            return 0;
        }

        if (!squad.isLeader(sender.getUUID())) {
            source.sendFailure(Component.literal("§cOnly squad leader can invite"));
            return 0;
        }

        if (squad.isFull()) {
            source.sendFailure(Component.literal("§cSquad is full"));
            return 0;
        }

        sm.invitePlayer(target.getUUID(), squad);
        target.displayClientMessage(Component.literal(
            String.format("§6You were invited to squad §b%s§6. Use §a/squad join %s",
                squad.getName(), squad.getName())), true);

        source.sendSuccess(() -> Component.literal("§aInvited " + target.getName().getString()), true);
        return 1;
    }

    private static int joinSquad(CommandSourceStack source, String squadName) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        SquadManager sm = TeamSystem.getSquadManager();
        Squad existing = sm.getPlayerSquad(player.getUUID());

        if (existing != null) {
            source.sendFailure(Component.literal("§cLeave your current squad first"));
            return 0;
        }

        for (Squad squad : sm.getAllSquads()) {
            if (squad.getName().equalsIgnoreCase(squadName)) {
                SquadManager.Invitation inv = sm.getInvitation(player.getUUID(), squad.getSquadId());
                if (inv == null) {
                    source.sendFailure(Component.literal("§cNo invitation for this squad"));
                    return 0;
                }

                sm.acceptInvitation(player.getUUID(), squad.getSquadId());
                TeamSystem.getTeamManager().assignSquad(player.getUUID(), squad.getSquadId());

                source.sendSuccess(() -> Component.literal("§aJoined squad: §b" + squadName), true);
                return 1;
            }
        }

        source.sendFailure(Component.literal("§cSquad not found"));
        return 0;
    }

    private static int leaveSquad(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        SquadManager sm = TeamSystem.getSquadManager();
        Squad squad = sm.getPlayerSquad(player.getUUID());

        if (squad == null) {
            source.sendFailure(Component.literal("§cYou are not in a squad"));
            return 0;
        }

        String squadName = squad.getName();
        sm.leaveSquad(player.getUUID());
        TeamSystem.getTeamManager().assignSquad(player.getUUID(), -1);

        source.sendSuccess(() -> Component.literal("§aLeft squad: §b" + squadName), true);
        return 1;
    }

    private static int kickPlayer(CommandSourceStack source, ServerPlayer target) {
        ServerPlayer sender = source.getPlayer();
        if (sender == null) return 0;

        SquadManager sm = TeamSystem.getSquadManager();
        Squad squad = sm.getPlayerSquad(sender.getUUID());

        if (squad == null) {
            source.sendFailure(Component.literal("§cYou are not in a squad"));
            return 0;
        }

        if (!squad.isLeader(sender.getUUID())) {
            source.sendFailure(Component.literal("§cOnly leader can kick"));
            return 0;
        }

        sm.kickMember(sender.getUUID(), target.getUUID());
        TeamSystem.getTeamManager().assignSquad(target.getUUID(), -1);

        source.sendSuccess(() -> Component.literal("§aKicked " + target.getName().getString()), true);
        return 1;
    }

    private static int listSquads(CommandSourceStack source, Team team) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        SquadManager sm = TeamSystem.getSquadManager();
        TeamManager tm = TeamSystem.getTeamManager();
        Team playerTeam = tm.getOrCreatePlayerData(player.getUUID()).getTeam();

        source.sendSuccess(() -> Component.literal("§6Squads in " + playerTeam.getName() + ":"), false);
        for (Squad squad : sm.getTeamSquads(playerTeam)) {
            source.sendSuccess(() -> Component.literal(
                String.format("§b%s§6 (%d/%d) - Leader: §a%s",
                    squad.getName(), squad.getMemberCount(), 6,
                    tm.getOrCreatePlayerData(squad.getLeaderUUID()).getDisplayName())), false);
        }
        return 1;
    }

    private static int squadInfo(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        SquadManager sm = TeamSystem.getSquadManager();
        Squad squad = sm.getPlayerSquad(player.getUUID());

        if (squad == null) {
            source.sendFailure(Component.literal("§cYou are not in a squad"));
            return 0;
        }

        TeamManager tm = TeamSystem.getTeamManager();
        source.sendSuccess(() -> Component.literal("§6Squad: §b" + squad.getName()), false);
        source.sendSuccess(() -> Component.literal("§6Leader: §a" +
            tm.getOrCreatePlayerData(squad.getLeaderUUID()).getDisplayName()), false);

        List<UUID> memberList = squad.getMembers();
        for (int idx = 0; idx < memberList.size(); idx++) {
            final int i = idx;
            source.sendSuccess(() -> Component.literal("  §b" + (i+1) + ". §a" +
                tm.getOrCreatePlayerData(memberList.get(i)).getDisplayName()), false);
        }
        return 1;
    }

    private static int disbandSquad(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        SquadManager sm = TeamSystem.getSquadManager();
        Squad squad = sm.getPlayerSquad(player.getUUID());

        if (squad == null) {
            source.sendFailure(Component.literal("§cYou are not in a squad"));
            return 0;
        }

        if (!squad.isLeader(player.getUUID())) {
            source.sendFailure(Component.literal("§cOnly leader can disband"));
            return 0;
        }

        String squadName = squad.getName();
        sm.disbandSquad(squad.getSquadId());

        source.sendSuccess(() -> Component.literal("§aSquad disbanded: §b" + squadName), true);
        return 1;
    }

    private static int promoteLeader(CommandSourceStack source, ServerPlayer newLeader) {
        ServerPlayer sender = source.getPlayer();
        if (sender == null) return 0;

        SquadManager sm = TeamSystem.getSquadManager();
        Squad squad = sm.getPlayerSquad(sender.getUUID());

        if (squad == null) {
            source.sendFailure(Component.literal("§cYou are not in a squad"));
            return 0;
        }

        if (!squad.isLeader(sender.getUUID())) {
            source.sendFailure(Component.literal("§cOnly leader can promote"));
            return 0;
        }

        sm.promoteLeader(sender.getUUID(), newLeader.getUUID());
        source.sendSuccess(() -> Component.literal("§aPromoted " + newLeader.getName().getString()), true);
        return 1;
    }

    private static int squadChat(CommandSourceStack source, String message) {
        ServerPlayer sender = source.getPlayer();
        if (sender == null) return 0;

        SquadManager sm = TeamSystem.getSquadManager();
        Squad squad = sm.getPlayerSquad(sender.getUUID());

        if (squad == null) {
            source.sendFailure(Component.literal("§cYou are not in a squad"));
            return 0;
        }

        Component chatMsg = Component.literal(String.format("§6[Squad] §a%s§6: %s",
            sender.getName().getString(), message));

        for (ServerPlayer member : sm.getSquadMembersOnline(squad.getSquadId(), source.getServer())) {
            member.displayClientMessage(chatMsg, false);
        }

        return 1;
    }
}
