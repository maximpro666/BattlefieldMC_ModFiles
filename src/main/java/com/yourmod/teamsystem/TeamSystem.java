package com.yourmod.teamsystem;

import com.mojang.logging.LogUtils;
import com.yourmod.teamsystem.blockentity.RespawnBeaconBlockEntity;
import com.yourmod.teamsystem.commands.*;
import com.yourmod.teamsystem.core.*;
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
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
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

    public TeamSystem() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        TABS.register(modEventBus);
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

        mapPoolManager = new MapPoolManager(event.getServer());
        mapPoolManager.loadConfig();
        MinecraftForge.EVENT_BUS.register(mapPoolManager);

        MapDimensionGenerator.generateDimensionDatapacks(event.getServer());

        gameManager = new GameManager(event.getServer());
        MinecraftForge.EVENT_BUS.register(gameManager);

        markerManager = new MarkerManager();
        respawnManager = new RespawnManager(event.getServer());

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
        MarkerCommand.register(event.getDispatcher());
        RespawnCommand.register(event.getDispatcher());
    }

    public static TeamManager getTeamManager() { return teamManager; }
    public static MapPoolManager getMapPoolManager() { return mapPoolManager; }
    public static GameManager getGameManager() { return gameManager; }
    public static MarkerManager getMarkerManager() { return markerManager; }
    public static RespawnManager getRespawnManager() { return respawnManager; }
}
