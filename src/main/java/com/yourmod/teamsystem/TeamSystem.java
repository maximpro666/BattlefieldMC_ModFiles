package com.yourmod.teamsystem;

import com.mojang.logging.LogUtils;
import com.yourmod.teamsystem.blockentity.RespawnBeaconBlockEntity;
import com.yourmod.teamsystem.commands.*;
import com.yourmod.teamsystem.commands.FOBCommand;
import com.yourmod.teamsystem.commands.DeployCommand;
import com.yourmod.teamsystem.commands.KitSelectCommand;
import com.yourmod.teamsystem.core.*;
import com.yourmod.teamsystem.core.ModSounds;
import com.yourmod.teamsystem.events.CombatEventHandler;
import com.yourmod.teamsystem.events.PlayerEventHandler;
import com.yourmod.teamsystem.network.PacketHandler;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

@Mod("teamsystem")
public class TeamSystem {
    public static final String MODID = "teamsystem";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final RegistryObject<Block> RESPAWN_BEACON_BLOCK = BLOCKS.register("respawn_beacon",
        () -> new RespawnBeaconBlock(Block.Properties.copy(Blocks.BEACON).noOcclusion()));

    public static final RegistryObject<Item> RESPAWN_BEACON_ITEM = ITEMS.register("respawn_beacon",
        () -> new BlockItem(RESPAWN_BEACON_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<CreativeModeTab> TEAM_TAB = TABS.register("team_tab",
        () -> CreativeModeTab.builder()
            .icon(() -> new ItemStack(RESPAWN_BEACON_ITEM.get()))
            .title(Component.translatable("itemGroup.teamsystem"))
            .displayItems((params, output) -> {
                output.accept(RESPAWN_BEACON_ITEM.get());
            })
            .build());

    public static final RegistryObject<BlockEntityType<RespawnBeaconBlockEntity>> RESPAWN_BEACON_BLOCK_ENTITY =
        BLOCK_ENTITIES.register("respawn_beacon",
            () -> BlockEntityType.Builder.of(RespawnBeaconBlockEntity::new, RESPAWN_BEACON_BLOCK.get()).build(null));

    private static TeamManager teamManager;
    private static MapPoolManager mapPoolManager;
    private static GameManager gameManager;
    private static MarkerManager markerManager;
    private static RespawnManager respawnManager;
    private static KitManager kitManager;
    private static SquadManager squadManager;
    private static VehicleManager vehicleManager;
    private static EconomyManager economyManager;
    private static CapturePointManager capturePointManager;
    private static TicketManager ticketManager;

    private static PlayerEventHandler playerEventHandler;
    private static ContributionManager contributionManager;
    private static FOBManager fobManager;
    private static TeamSystemConfig config;
    private static CaptureParticleManager captureParticleManager;

    public TeamSystem() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        TABS.register(modEventBus);
        ModSounds.SOUNDS.register(modEventBus);
        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(PacketHandler::register);
        LOGGER.info("Team System packets registered");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        MapPoolManager temp = new MapPoolManager(event.getServer());
        temp.loadConfig();
        java.util.List<MapConfig> maps = temp.getMaps();
        MinecraftServer server = event.getServer();
        Thread preloader = new Thread(() -> {
            try {
                MapOffsetManager.preloadAllMaps(server, maps);
            } catch (Exception e) {
                LOGGER.error("Map preload failed: {}", e.getMessage());
            }
        }, "MapPreloader");
        preloader.setDaemon(true);
        preloader.start();
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

        config = TeamSystemConfig.load(event.getServer());

        mapPoolManager = new MapPoolManager(event.getServer());
        mapPoolManager.loadConfig();
        MinecraftForge.EVENT_BUS.register(mapPoolManager);

        MapDimensionGenerator.generateDimensionDatapacks(event.getServer());

        gameManager = new GameManager(event.getServer());
        MinecraftForge.EVENT_BUS.register(gameManager);
        gameManager.startInitialCountdown();

        markerManager = new MarkerManager();
        respawnManager = new RespawnManager(event.getServer());

        kitManager = teamManager.getKitManager();
        squadManager = teamManager.getSquadManager();
        vehicleManager = teamManager.getVehicleManager();

        playerEventHandler = new PlayerEventHandler(teamManager);
        MinecraftForge.EVENT_BUS.register(playerEventHandler);
        MinecraftForge.EVENT_BUS.register(new CombatEventHandler(teamManager));
        MinecraftForge.EVENT_BUS.register(new com.yourmod.teamsystem.events.AttachmentEventHandler());

        economyManager = new EconomyManager();
        economyManager.loadFromTeamManager();
        capturePointManager = new CapturePointManager();
        ticketManager = new TicketManager();
        contributionManager = new ContributionManager();
        fobManager = new FOBManager();
        captureParticleManager = new CaptureParticleManager();

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
        MarkerCommand.register(event.getDispatcher());
        RespawnCommand.register(event.getDispatcher());
        RankCommand.register(event.getDispatcher());
        KitCommand.register(event.getDispatcher());
        SquadCommand.register(event.getDispatcher());
        VehicleCommand.register(event.getDispatcher());
        PingCommand.register(event.getDispatcher());
        com.yourmod.teamsystem.commands.CapturePointCommand.register(event.getDispatcher());
        EconomyCommand.register(event.getDispatcher());
        CallsignCommand.register(event.getDispatcher());
        FOBCommand.register(event.getDispatcher());
        DeployCommand.register(event.getDispatcher());
        KitSelectCommand.register(event.getDispatcher());
        com.yourmod.teamsystem.commands.AdminNotifyCommand.register(event.getDispatcher());
        com.yourmod.teamsystem.commands.AdminCommand.register(event.getDispatcher());
        com.yourmod.teamsystem.commands.KitAdminCommand.register(event.getDispatcher());
        com.yourmod.teamsystem.commands.RedeployCommand.register(event.getDispatcher());

    }

    public static TeamManager getTeamManager() { return teamManager; }
    public static MapPoolManager getMapPoolManager() { return mapPoolManager; }
    public static GameManager getGameManager() { return gameManager; }
    public static MarkerManager getMarkerManager() { return markerManager; }
    public static RespawnManager getRespawnManager() { return respawnManager; }
    public static KitManager getKitManager() { return kitManager; }
    public static SquadManager getSquadManager() { return squadManager; }
    public static VehicleManager getVehicleManager() { return vehicleManager; }
    public static EconomyManager getEconomyManager() { return economyManager; }
    public static CapturePointManager getCapturePointManager() { return capturePointManager; }
    public static TicketManager getTicketManager() { return ticketManager; }
    public static ContributionManager getContributionManager() { return contributionManager; }
    public static FOBManager getFOBManager() { return fobManager; }
    public static PlayerEventHandler getPlayerEventHandler() { return playerEventHandler; }

    public static TeamSystemConfig getConfig() { return config; }
    public static CaptureParticleManager getCaptureParticleManager() { return captureParticleManager; }
}
