/*
 * Project Warfare Pigeo (PWP) — tactical PvP mod for Minecraft Forge 1.20.1
 * Studio: Pigeo Studios
 * 
 * Squad-based combat with capture points, vehicles, ranks, FOBs,
 * ticket system, voice chat integration, custom HUD, and match lifecycle.
 * 
 * https://github.com/anomalyco/opencode
 */

package com.pigeostudios.pwp;

import com.mojang.logging.LogUtils;
import com.pigeostudios.pwp.blockentity.RespawnBeaconBlockEntity;
import com.pigeostudios.pwp.commands.*;
import com.pigeostudios.pwp.commands.FOBCommand;
import com.pigeostudios.pwp.commands.DeployCommand;
import com.pigeostudios.pwp.commands.KitSelectCommand;
import com.pigeostudios.pwp.core.*;
import com.pigeostudios.pwp.core.ModSounds;
import com.pigeostudios.pwp.data.CentralDatabase;
import com.pigeostudios.pwp.data.KitConfig;
import com.pigeostudios.pwp.events.CombatEventHandler;
import com.pigeostudios.pwp.events.PlayerEventHandler;
import com.pigeostudios.pwp.network.PacketHandler;
import com.pigeostudios.pwp.system.RoleSystem;
import com.pigeostudios.pwp.proxy.ProxyMessenger;
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
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Mod("pwp")
public class PWP {
    public static final String MODID = "pwp";
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
            .title(Component.translatable("itemGroup.pwp"))
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
    private static CapturePointManager capturePointManager;
    private static TicketManager ticketManager;

    private static PlayerEventHandler playerEventHandler;
    private static ContributionManager contributionManager;
    private static FOBManager fobManager;
    private static PWPConfig config;
    private static CaptureParticleManager captureParticleManager;
    private static RoleSystem roleSystem;

    private static BattlefieldRuntime battlefieldRuntime;

    public PWP() {
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
    public void onServerAboutToStart(ServerAboutToStartEvent event) {
        MapDimensionGenerator.generateDimensionDatapacks(event.getServer());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        CombatEventHandler.setServer(event.getServer());
        MapPoolManager temp = new MapPoolManager(event.getServer());
        temp.loadConfig();
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        // Kill any orphaned match server from previous run (only on lobby server,
        // match server must NOT kill itself via stop-match.ps1)
        if (!ProxyMessenger.isMatchServer()) {
            try {
                Path launcherDir = Path.of(System.getProperty("user.dir")).resolve("../launcher").normalize();
                ProcessBuilder stopPb = new ProcessBuilder(
                    "powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass",
                    "-File", launcherDir.resolve("stop-match.ps1").toString()
                );
                stopPb.directory(launcherDir.toFile());
                stopPb.redirectErrorStream(true);
                Process stopProc = stopPb.start();
                stopProc.waitFor(10, TimeUnit.SECONDS);
            } catch (Exception ignored) {}
        }

        ResourceKey<Level> overworldKey = ResourceKey.create(Registries.DIMENSION, new ResourceLocation("minecraft:overworld"));
        teamManager = event.getServer().getLevel(overworldKey)
            .getDataStorage()
            .computeIfAbsent(
                nbt -> TeamManager.load(event.getServer(), nbt),
                () -> new TeamManager(event.getServer()),
                "pwp_data"
            );

        CentralDatabase.init();
        teamManager.loadFromCentralDatabase();

        config = PWPConfig.load(event.getServer());

        KitConfig.loadOrCreate(event.getServer().getWorldPath(LevelResource.ROOT));

        roleSystem = new RoleSystem();
        roleSystem.reload(KitConfig.get());

        mapPoolManager = new MapPoolManager(event.getServer());
        mapPoolManager.loadConfig();
        MinecraftForge.EVENT_BUS.register(mapPoolManager);

        ProxyMessenger.init(event.getServer());

        gameManager = new GameManager(event.getServer());
        MinecraftForge.EVENT_BUS.register(gameManager);
        // Match server: pick a map and start directly; Lobby server waits for /startmatch
        if (ProxyMessenger.isMatchServer()) {
            gameManager.startMatchServerGame();
        }

        markerManager = new MarkerManager();
        respawnManager = new RespawnManager(event.getServer());

        kitManager = teamManager.getKitManager();
        squadManager = teamManager.getSquadManager();
        vehicleManager = teamManager.getVehicleManager();

        playerEventHandler = new PlayerEventHandler(teamManager);
        MinecraftForge.EVENT_BUS.register(playerEventHandler);
        MinecraftForge.EVENT_BUS.register(new CombatEventHandler(teamManager));
        MinecraftForge.EVENT_BUS.register(new com.pigeostudios.pwp.events.AttachmentEventHandler());

        battlefieldRuntime = BattlefieldRuntime.getInstance();
        battlefieldRuntime.loadFromTeamManager();
        MinecraftForge.EVENT_BUS.register(battlefieldRuntime);

        battlefieldRuntime.getVehicleDefRegistry().loadAll();

        battlefieldRuntime.getVehicleAdapterRegistry().register(
            new com.pigeostudios.pwp.vehicle.adapter.SuperbVehicleAdapter());
        battlefieldRuntime.getVehicleAdapterRegistry().register(
            new com.pigeostudios.pwp.vehicle.adapter.AshVehicleAdapter());

        MinecraftForge.EVENT_BUS.register(
            new com.pigeostudios.pwp.events.VehicleAccessControl());

        capturePointManager = new CapturePointManager();
        ticketManager = new TicketManager();
        contributionManager = new ContributionManager();
        fobManager = new FOBManager();
        captureParticleManager = new CaptureParticleManager();

        // Write ready flag ONLY after all initialization is complete
        if (ProxyMessenger.isMatchServer()) {
            try {
                Files.write(Path.of("match_ready.flag"), "ready".getBytes());
            } catch (Exception ignored) {}
        }

        LOGGER.info("Team System initialized");
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        MinecraftForge.EVENT_BUS.unregister(gameManager);
        if (teamManager != null) {
            teamManager.syncToDatabase();
        }
        var vgm = TeamVoicePlugin.getGroupManager();
        if (vgm != null) vgm.cleanup();
        CentralDatabase.close();
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
        com.pigeostudios.pwp.commands.CapturePointCommand.register(event.getDispatcher());
        EconomyCommand.register(event.getDispatcher());
        CallsignCommand.register(event.getDispatcher());
        FOBCommand.register(event.getDispatcher());
        DeployCommand.register(event.getDispatcher());
        KitSelectCommand.register(event.getDispatcher());
        com.pigeostudios.pwp.commands.AdminNotifyCommand.register(event.getDispatcher());
        com.pigeostudios.pwp.commands.AdminCommand.register(event.getDispatcher());
        com.pigeostudios.pwp.commands.KitAdminCommand.register(event.getDispatcher());
        com.pigeostudios.pwp.commands.RedeployCommand.register(event.getDispatcher());
        com.pigeostudios.pwp.commands.StartMatchCommand.register(event.getDispatcher());
        com.pigeostudios.pwp.commands.GiveCoinsCommand.register(event.getDispatcher());
        com.pigeostudios.pwp.commands.FlySpeedCommand.register(event.getDispatcher());
        com.pigeostudios.pwp.commands.DonatorCommand.register(event.getDispatcher());

    }

    public static TeamManager getTeamManager() { return teamManager; }
    public static MapPoolManager getMapPoolManager() { return mapPoolManager; }
    public static GameManager getGameManager() { return gameManager; }
    public static MarkerManager getMarkerManager() { return markerManager; }
    public static RespawnManager getRespawnManager() { return respawnManager; }
    public static KitManager getKitManager() { return kitManager; }
    public static SquadManager getSquadManager() { return squadManager; }
    public static VehicleManager getVehicleManager() { return vehicleManager; }
    public static CapturePointManager getCapturePointManager() { return capturePointManager; }
    public static TicketManager getTicketManager() { return ticketManager; }
    public static ContributionManager getContributionManager() { return contributionManager; }
    public static FOBManager getFOBManager() { return fobManager; }
    public static PlayerEventHandler getPlayerEventHandler() { return playerEventHandler; }

    public static PWPConfig getConfig() { return config; }
    public static CaptureParticleManager getCaptureParticleManager() { return captureParticleManager; }
    public static RoleSystem getRoleSystem() { return roleSystem; }
}
