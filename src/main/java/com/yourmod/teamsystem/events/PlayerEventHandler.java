package com.yourmod.teamsystem.events;

import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.core.*;
import com.yourmod.teamsystem.network.OpenTeamSelectionScreenPacket;
import com.yourmod.teamsystem.network.PacketHandler;
import com.yourmod.teamsystem.proxy.ProxyMessenger;
import net.minecraft.ChatFormatting;
import net.minecraftforge.network.PacketDistributor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;

public class PlayerEventHandler {
    private final TeamManager teamManager;

    private static boolean curiosDetected = false;
    private static Method curiosGetInventory;
    private static Method curiosGetCurios;
    private static Method curiosGetStacks;
    private static Method curiosGetStackInSlot;
    private static Method curiosSetStackInSlot;
    private static Method curiosGetSlots;
    private static Item dogTagItem;

    static {
        try {
            Class<?> apiClass = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            curiosGetInventory = apiClass.getMethod("getCuriosInventory", LivingEntity.class);
            Class<?> handlerClass = Class.forName("top.theillusivec4.curios.api.type.capability.ICuriosItemHandler");
            curiosGetCurios = handlerClass.getMethod("getCurios");
            Class<?> stacksHandlerClass = Class.forName("top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler");
            curiosGetStacks = stacksHandlerClass.getMethod("getStacks");
            Class<?> invClass = Class.forName("top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler");
            curiosGetStackInSlot = invClass.getMethod("getStackInSlot", int.class);
            curiosSetStackInSlot = invClass.getMethod("setStackInSlot", int.class, ItemStack.class);
            curiosGetSlots = invClass.getMethod("getSlots");
            curiosDetected = true;
            TeamSystem.LOGGER.info("DogTag: Curios API detected");
        } catch (Exception e) {
            curiosDetected = false;
            TeamSystem.LOGGER.warn("DogTag: Curios API not available");
        }
        try {
            dogTagItem = BuiltInRegistries.ITEM.get(new ResourceLocation("superbwarfare:dog_tag"));
            if (dogTagItem == Items.AIR) dogTagItem = null;
        } catch (Exception e) {
            dogTagItem = null;
        }
    }

    public PlayerEventHandler(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            teamManager.fullSyncPlayer(player);
            GameManager game = TeamSystem.getGameManager();
            if (game != null) {
                game.syncPhaseToPlayer(player);
                if (game.isPlaying() && game.isMapReady()) {
                    Team team = teamManager.getOrCreatePlayerData(player.getUUID()).getTeam();
                    MapConfig map = game.getCurrentMap();
                    if (team != null && team.isPlayable() && map != null) {
                        game.teleportPlayerToMapAtTeamSpawn(player, team);
                        game.setMapRespawn(player, team);
                    } else {
                        game.teleportPlayerToLobby(player);
                        game.setLobbyRespawn(player);
                    }
                } else if (game.isPlaying()) {
                    game.teleportPlayerToLobby(player);
                    game.setLobbyRespawn(player);
                    player.sendSystemMessage(Component.literal("§eMap is loading, please wait...")
                        .withStyle(ChatFormatting.YELLOW));
                } else {
                    game.teleportPlayerToLobby(player);
                    game.setLobbyRespawn(player);
                }

                if (ProxyMessenger.isMatchServer()) {
                    PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenTeamSelectionScreenPacket());
                }

                if (game.isPlaying() || game.isVoting()) {
                    player.sendSystemMessage(Component.literal("§6=== Game in progress ===")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                    player.sendSystemMessage(Component.literal("§eUse §a/team nato§e or §a/team russia§e to join a team!")
                        .withStyle(ChatFormatting.YELLOW));
                    player.sendSystemMessage(Component.literal("§7After choosing a team, use §a/squad create <name>§7 or §a/squad join <name>§7 to squad up.")
                        .withStyle(ChatFormatting.GRAY));
                }
            }
            MarkerManager mm = TeamSystem.getMarkerManager();
            if (mm != null) {
                mm.syncToPlayer(player);
            }
            CapturePointManager cp = TeamSystem.getCapturePointManager();
            if (cp != null) {
                cp.syncToPlayer(player);
            }
            var pcd = teamManager.getOrCreatePlayerData(player.getUUID());
            if (pcd.getDisplayName().isEmpty() && pcd.getCallsign().isEmpty()) {
                player.sendSystemMessage(Component.literal("§6=== Добро пожаловать! ===")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                player.sendSystemMessage(Component.literal("§eУстановите свой позывной командой: §a/callsign <ник>")
                    .withStyle(ChatFormatting.YELLOW));
                ensureDogTag(player);
            }
            TeamSystem.LOGGER.info("Synced team data for player: {}", player.getName().getString());
            checkAndAutoStartCycle(player.server);
            // On lobby server: auto-transfer late joiners if match server is running
            if (!ProxyMessenger.isMatchServer()) {
                try (Socket s = new Socket()) {
                    s.connect(new InetSocketAddress("127.0.0.1", 25566), 300);
                    com.yourmod.teamsystem.network.PacketHandler.CHANNEL.send(
                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                        new com.yourmod.teamsystem.network.TransferPacket("127.0.0.1:25566"));
                    TeamSystem.LOGGER.info("Late joiner {} transferred to match server", player.getName().getString());
                } catch (Exception e) {
                    // No match server running, stay in lobby
                }
            }
        }
    }

    private static void checkAndAutoStartCycle(MinecraftServer srv) {
        try {
            Path flagFile = Path.of(System.getProperty("user.dir")).resolve("../launcher/match_cycle_done.flag").normalize();
            if (Files.exists(flagFile)) {
                Files.delete(flagFile);
                TeamSystem.LOGGER.info("Match cycle flag detected, auto-starting next match in 5s...");
                new Thread(() -> {
                    try { Thread.sleep(5000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    srv.execute(() -> {
                        srv.getCommands().performPrefixedCommand(srv.createCommandSourceStack(), "startmatch");
                    });
                }, "AutoMatchStarter").start();
            }
        } catch (Exception e) {
            TeamSystem.LOGGER.error("Failed to check cycle flag", e);
        }
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            net.minecraft.core.BlockPos pos = event.getPos();
            if (player.level().getBlockState(pos).getBlock() == TeamSystem.RESPAWN_BEACON_BLOCK.get()) {
                // Check for player-placed beacon (has block entity with owner)
                if (player.level().getBlockEntity(pos) instanceof com.yourmod.teamsystem.blockentity.RespawnBeaconBlockEntity beacon
                        && beacon.getOwnerUUID() != null) {
                    TeamSystem.getRespawnManager().onBeaconBroken(pos, beacon);
                    String msg = "§c[BCN] " + beacon.getName() + " destroyed!";
                    player.serverLevel().players().forEach(p ->
                        p.displayClientMessage(net.minecraft.network.chat.Component.literal(msg), false));
                }
                // Check for FOB — remove from manager on block break
                String dim = player.serverLevel().dimension().location().toString();
                com.yourmod.teamsystem.core.FOBManager.SavedFOB fob = TeamSystem.getFOBManager().getFOBAt(pos, dim);
                if (fob != null) {
                    TeamSystem.getFOBManager().removeFOB(fob.fobId);
                    String msg = "§c[FOB] " + fob.name + " destroyed!";
                    player.serverLevel().players().forEach(p ->
                        p.displayClientMessage(net.minecraft.network.chat.Component.literal(msg), false));
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.setHealth(player.getMaxHealth());
            GameManager game = TeamSystem.getGameManager();

            if (game == null) {
                teamManager.fullSyncPlayer(player);
                return;
            }

            if (game.isPlaying()) {
                MapConfig map = TeamSystem.getMapPoolManager().getCurrentMap().orElse(null);
                if (map != null && !map.hasRespawn()) {
                    teamManager.setPlayerTeam(player, Team.SPECTATOR);
                    game.teleportPlayerToLobby(player);
                    game.setLobbyRespawn(player);
                    player.sendSystemMessage(Component.literal("Respawn is disabled on this map. You are now spectating.")
                        .withStyle(ChatFormatting.RED));
                    return;
                }
                // Teleport back to map at team spawn
                Team team = teamManager.getOrCreatePlayerData(player.getUUID()).getTeam();
                if (team != null && team.isPlayable()) {
                    game.teleportPlayerToMapAtTeamSpawn(player, team);
                    game.setMapRespawn(player, team);
                    PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                        new com.yourmod.teamsystem.network.SoundPacket(
                            com.yourmod.teamsystem.core.ModSounds.GUI_RESPAWN.get().getLocation().toString()));
                } else {
                    game.teleportPlayerToLobby(player);
                    game.setLobbyRespawn(player);
                }
                teamManager.fullSyncPlayer(player);
                return;
            }

            if (game.isLobby()) {
                game.teleportPlayerToLobby(player);
                game.setLobbyRespawn(player);
                return;
            }

            teamManager.fullSyncPlayer(player);
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID uuid = player.getUUID();
            RateLimiter.removePlayer(uuid);
            EconomyManager econ = TeamSystem.getEconomyManager();
            if (econ != null) econ.clearPlayer(uuid);
            KitManager km = TeamSystem.getKitManager();
            if (km != null) km.clearPlayerCooldowns(uuid);
            SquadManager sm = TeamSystem.getSquadManager();
            if (sm != null) sm.removePlayer(uuid);
            MarkerManager mm = TeamSystem.getMarkerManager();
            if (mm != null) mm.removePlayerMarkers(uuid);
            VehicleManager vm = TeamSystem.getVehicleManager();
            if (vm != null) vm.unregisterPlayerVehicles(uuid);
            FOBManager fm = TeamSystem.getFOBManager();
            if (fm != null) fm.clearPlayerCooldown(uuid);
            AttachmentEventHandler.clearPlayerAttachments(player);
            TeamSystem.LOGGER.info("Cleaned up player data: {}", player.getName().getString());
        }
    }

    private Optional<?> getCuriosHandler(ServerPlayer player) {
        if (!curiosDetected) return Optional.empty();
        try {
            Object result = curiosGetInventory.invoke(null, player);
            if (result instanceof Optional<?> o) return o;
            if (result != null) {
                Method resolve = result.getClass().getMethod("resolve");
                Object resolved = resolve.invoke(result);
                if (resolved instanceof Optional<?> o) return o;
            }
        } catch (Exception ignored) {}
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    private boolean ensureDogTag(ServerPlayer player) {
        return setDogTagName(player, player.getName().getString());
    }

    @SuppressWarnings("unchecked")
    public boolean setDogTagName(ServerPlayer player, String name) {
        if (dogTagItem == null || !curiosDetected) return false;
        try {
            Optional<?> opt = getCuriosHandler(player);
            if (opt.isEmpty()) return false;
            Object handler = opt.get();
            Map<String, ?> curiosMap = (Map<String, ?>) curiosGetCurios.invoke(handler);
            for (Map.Entry<String, ?> entry : curiosMap.entrySet()) {
                Object stacksHandler = entry.getValue();
                if (stacksHandler == null) continue;
                Object stacks = curiosGetStacks.invoke(stacksHandler);
                int size = (int) curiosGetSlots.invoke(stacks);
                for (int i = 0; i < size; i++) {
                    ItemStack existing = (ItemStack) curiosGetStackInSlot.invoke(stacks, i);
                    if (!existing.isEmpty() && existing.getItem() == dogTagItem) {
                        existing.setHoverName(Component.literal(name));
                        TeamSystem.LOGGER.info("Renamed dog tag for {} to {}", player.getName().getString(), name);
                        return true;
                    }
                }
            }
            for (Map.Entry<String, ?> entry : curiosMap.entrySet()) {
                Object stacksHandler = entry.getValue();
                if (stacksHandler == null) continue;
                Object stacks = curiosGetStacks.invoke(stacksHandler);
                int size = (int) curiosGetSlots.invoke(stacks);
                for (int i = 0; i < size; i++) {
                    ItemStack existing = (ItemStack) curiosGetStackInSlot.invoke(stacks, i);
                    if (existing.isEmpty()) {
                        ItemStack tag = new ItemStack(dogTagItem);
                        tag.setHoverName(Component.literal(name));
                        tag.enchant(Enchantments.BINDING_CURSE, 1);
                        curiosSetStackInSlot.invoke(stacks, i, tag);
                        TeamSystem.LOGGER.info("Gave dog tag to {} with name {}", player.getName().getString(), name);
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            TeamSystem.LOGGER.warn("Failed to set dog tag name: {}", e.getMessage());
        }
        return false;
    }
}
