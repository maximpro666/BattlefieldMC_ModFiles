package com.yourmod.teamsystem;

import com.mojang.logging.LogUtils;
import com.yourmod.teamsystem.commands.TeamCommand;
import com.yourmod.teamsystem.core.TeamManager;
import com.yourmod.teamsystem.events.CombatEventHandler;
import com.yourmod.teamsystem.events.PlayerEventHandler;
import com.yourmod.teamsystem.network.PacketHandler;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod("teamsystem")
public class TeamSystem {
    public static final String MODID = "teamsystem";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static TeamManager teamManager;

    public TeamSystem() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(PacketHandler::register);
        LOGGER.info("Team System packets registered");
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        teamManager = event.getServer().getLevel(Level.OVERWORLD)
            .getDataStorage()
            .computeIfAbsent(
                nbt -> TeamManager.load(event.getServer(), nbt),
                () -> new TeamManager(event.getServer()),
                "teamsystem_data"
            );

        MinecraftForge.EVENT_BUS.register(new PlayerEventHandler(teamManager));
        MinecraftForge.EVENT_BUS.register(new CombatEventHandler(teamManager));

        LOGGER.info("Team System initialized");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        if (teamManager != null) {
            TeamCommand.register(event.getDispatcher(), teamManager);
        }
    }

    public static TeamManager getTeamManager() {
        return teamManager;
    }
}
