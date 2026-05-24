package com.pigeostudios.pwp.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class RulesCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rules")
            .executes(ctx -> {
                ctx.getSource().sendSuccess(() -> Component.literal("§6§l=== P R A V I L A ==="), false);
                ctx.getSource().sendSuccess(() -> Component.literal(""), false);
                ctx.getSource().sendSuccess(() -> Component.literal("§6§lGAME"), false);
                ctx.getSource().sendSuccess(() -> Component.literal(" §71.1 §cTeam Kill §7- убийство союзника"), false);
                ctx.getSource().sendSuccess(() -> Component.literal(" §71.2 §cFriendly Fire §7- уничтожение своей техники/ресурсов"), false);
                ctx.getSource().sendSuccess(() -> Component.literal(" §71.3 §cGriefing §7- блокировка союзников, стройка в спавне"), false);
                ctx.getSource().sendSuccess(() -> Component.literal(" §71.4 §cCheating §7- использование читов, xray, аимбот"), false);
                ctx.getSource().sendSuccess(() -> Component.literal(" §71.5 §cExploits §7- использование багов игры/мода"), false);
                ctx.getSource().sendSuccess(() -> Component.literal(" §71.6 §cTeam Stacking §7- намеренный дисбаланс команд"), false);
                ctx.getSource().sendSuccess(() -> Component.literal(" §71.7 §cAFK §7- бездействие во время матча"), false);
                ctx.getSource().sendSuccess(() -> Component.literal(" §71.8 §cLeave mid-match §7- выход с матча"), false);
                ctx.getSource().sendSuccess(() -> Component.literal(" §71.9 §cCoalition §7- союз сторон, сговор между командами"), false);
                ctx.getSource().sendSuccess(() -> Component.literal(""), false);
                ctx.getSource().sendSuccess(() -> Component.literal("§6§lCHAT"), false);
                ctx.getSource().sendSuccess(() -> Component.literal(" §72.1 §cToxicity §7- оскорбления, унижения"), false);
                ctx.getSource().sendSuccess(() -> Component.literal(" §72.2 §cSpam §7- повторяющиеся сообщения, флуд"), false);
                ctx.getSource().sendSuccess(() -> Component.literal(" §72.3 §cDiscrimination §7- расизм, сексизм, иное"), false);
                ctx.getSource().sendSuccess(() -> Component.literal(" §72.4 §cAdvertising §7- реклама других серверов"), false);
                ctx.getSource().sendSuccess(() -> Component.literal(" §72.5 §cNSFW §7- неприемлемый контент"), false);
                ctx.getSource().sendSuccess(() -> Component.literal(" §72.6 §cCaps §7- чрезмерное использование капса"), false);
                ctx.getSource().sendSuccess(() -> Component.literal(""), false);
                ctx.getSource().sendSuccess(() -> Component.literal("§6§lVOICE"), false);
                ctx.getSource().sendSuccess(() -> Component.literal(" §73.1 §cScreaming §7- крик в микрофон"), false);
                ctx.getSource().sendSuccess(() -> Component.literal(" §73.2 §cMusic §7- воспроизведение музыки"), false);
                ctx.getSource().sendSuccess(() -> Component.literal(" §73.3 §cVoice Abuse §7- оскорбления в голосовом чате"), false);
                ctx.getSource().sendSuccess(() -> Component.literal(" §73.4 §cSoundboard §7- использование звуковых досок"), false);
                ctx.getSource().sendSuccess(() -> Component.literal(""), false);
                ctx.getSource().sendSuccess(() -> Component.literal("§6§lGENERAL"), false);
                ctx.getSource().sendSuccess(() -> Component.literal(" §74.1 §cInappropriate Name §7- неприемлемый никнейм"), false);
                ctx.getSource().sendSuccess(() -> Component.literal(" §74.2 §cInappropriate Skin §7- неприемлемый скин"), false);
                ctx.getSource().sendSuccess(() -> Component.literal(" §74.3 §cImpersonation §7- имитация администрации"), false);
                ctx.getSource().sendSuccess(() -> Component.literal(" §74.4 §cBan Evasion §7- обход бана (альт-аккаунт)"), false);
                ctx.getSource().sendSuccess(() -> Component.literal(" §74.5 §cFalse Report §7- ложные жалобы"), false);
                ctx.getSource().sendSuccess(() -> Component.literal(""), false);
                ctx.getSource().sendSuccess(() -> Component.literal("§7/help для списка команд"), false);
                return Command.SINGLE_SUCCESS;
            }));
    }
}
