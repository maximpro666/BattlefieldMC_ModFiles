package com.yourmod.teamsystem;

import com.mojang.logging.LogUtils;
import com.yourmod.teamsystem.commands.GameCommand;
import com.yourmod.teamsystem.commands.LobbyCommand;
import com.yourmod.teamsystem.commands.MapCommand;
import com.yourmod.teamsystem.commands.TeamCommand;
import com.yourmod.teamsystem.core.GameManager;
import com.yourmod.teamsystem.core.MapPoolManager;
import com.yourmod.teamsystem.core.TeamManager;
import com.yourmod.teamsystem.events.CombatEventHandler;
import com.yourmod.teamsystem.events.PlayerEventHandler;
import com.yourmod.teamsystem.network.PacketHandler;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
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
    private static MapPoolManager mapPoolManager;
    private static GameManager gameManager;

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
        ResourceKey<Level> overworldKey = ResourceKey.create(Registries.DIMENSION, new ResourceLocation("minecraft:overworld"));
        teamManager = event.getServer().getLevel(overworldKey)
            .getDataStorage()
            .computeIfAbsent(
                nbt -> TeamManager.load(event.getServer(), nbt),
                () -> new TeamManager(event.getServer()),
                "teamsystem_data"
            );

        mapPoolManager = event.getServer().getLevel(overworldKey)
            .getDataStorage()
            .computeIfAbsent(
                nbt -> MapPoolManager.load(event.getServer(), nbt),
                () -> new MapPoolManager(event.getServer()),
                "teamsystem_mappool"
            );
        mapPoolManager.loadConfig();

        gameManager = new GameManager(event.getServer());
        MinecraftForge.EVENT_BUS.register(gameManager);

        MinecraftForge.EVENT_BUS.register(new PlayerEventHandler(teamManager));
        MinecraftForge.EVENT_BUS.register(new CombatEventHandler(teamManager));

        LOGGER.info("Team System initialized");
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        MinecraftForge.EVENT_BUS.unregister(gameManager);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        TeamCommand.register(event.getDispatcher());
        MapCommand.register(event.getDispatcher());
        GameCommand.register(event.getDispatcher());
        LobbyCommand.register(event.getDispatcher());
    }

    public static TeamManager getTeamManager() {
        return teamManager;
    }

    public static MapPoolManager getMapPoolManager() {
        return mapPoolManager;
    }

    public static GameManager getGameManager() {
        return gameManager;
    }
}
