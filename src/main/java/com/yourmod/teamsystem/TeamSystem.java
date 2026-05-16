package com.yourmod.teamsystem;

import com.yourmod.teamsystem.commands.TeamCommand;
import com.yourmod.teamsystem.core.TeamManager;
import com.yourmod.teamsystem.events.CombatEventHandler;
import com.yourmod.teamsystem.events.PlayerEventHandler;
import com.yourmod.teamsystem.network.PacketHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(TeamSystem.MODID)
public class TeamSystem {
    public static final String MODID = "teamsystem";
    public static final Logger LOGGER = LoggerFactory.getLogger(TeamSystem.class);

    private static TeamManager teamManager;

    public TeamSystem() {
        LOGGER.info("Initializing Team System");

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(PacketHandler::register);

        MinecraftForge.EVENT_BUS.addListener(this::onServerStarted);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStopping);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);

        MinecraftForge.EVENT_BUS.register(new PlayerEventHandler());
        MinecraftForge.EVENT_BUS.register(new CombatEventHandler());
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            LOGGER.info("Common setup complete");
        });
    }

    private void onServerStarted(ServerStartedEvent event) {
        teamManager = new TeamManager(event.getServer());
        teamManager.load();
        LOGGER.info("TeamManager initialized and data loaded");
    }

    private void onServerStopping(ServerStoppingEvent event) {
        if (teamManager != null) {
            teamManager.save();
            LOGGER.info("TeamManager data saved");
        }
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        TeamCommand.register(event.getDispatcher());
        LOGGER.info("Commands registered");
    }

    public static TeamManager getTeamManager() {
        return teamManager;
    }
}
