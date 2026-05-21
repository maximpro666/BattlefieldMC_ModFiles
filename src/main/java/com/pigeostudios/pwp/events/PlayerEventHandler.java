package com.pigeostudios.pwp.events;

import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.core.*;
import com.pigeostudios.pwp.core.TeamVoicePlugin;
import com.pigeostudios.pwp.network.OpenTeamSelectionScreenPacket;
import com.pigeostudios.pwp.network.PacketHandler;
import com.pigeostudios.pwp.proxy.ProxyMessenger;
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
import net.minecraftforge.event.TickEvent;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
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

    // Cache whether match server port is reachable, refreshed every 10s
    private static final ConcurrentHashMap<Integer, Boolean> matchPortCache = new ConcurrentHashMap<>();
    private static final AtomicLong lastPortCheck = new AtomicLong(0);
    private static final long PORT_CACHE_TTL_MS = 10_000L;

    private static boolean isMatchPortReachable(int port) {
        long now = System.currentTimeMillis();
        if (now - lastPortCheck.get() < PORT_CACHE_TTL_MS) {
            Boolean cached = matchPortCache.get(port);
            if (cached != null) return cached;
        }
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress("127.0.0.1", port), 500);
            matchPortCache.put(port, true);
            lastPortCheck.set(now);
            return true;
        } catch (Exception e) {
            matchPortCache.put(port, false);
            lastPortCheck.set(now);
            return false;
        }
    }

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
            PWP.LOGGER.info("DogTag: Curios API detected");
        } catch (Exception e) {
            curiosDetected = false;
            PWP.LOGGER.warn("DogTag: Curios API not available");
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
            GameManager game = PWP.getGameManager();
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
            MarkerManager mm = PWP.getMarkerManager();
            if (mm != null) {
                mm.syncToPlayer(player);
            }
            CapturePointManager cp = PWP.getCapturePointManager();
            if (cp != null) {
                cp.syncToPlayer(player);
            }
            var pcd = teamManager.getOrCreatePlayerData(player.getUUID());
            if (pcd.getDisplayName().isEmpty() && pcd.getCallsign().isEmpty()) {
                player.sendSystemMessage(Component.translatable("pwp.chat.welcome.title"));
                player.sendSystemMessage(Component.translatable("pwp.chat.welcome.set_callsign"));
                ensureDogTag(player);
            }
            PWP.LOGGER.info("Synced team data for player: {}", player.getName().getString());
            // Check cycle flag BEFORE checkAndAutoStartCycle deletes it
            boolean cycleInProgress = false;
            if (!ProxyMessenger.isMatchServer()) {
                Path cycleFlag = Path.of(System.getProperty("user.dir")).resolve("../launcher/match_cycle_done.flag").normalize();
                cycleInProgress = Files.exists(cycleFlag);
            }
            checkAndAutoStartCycle(player.server);
            // On lobby server: auto-transfer late joiners if match server is running
            if (!ProxyMessenger.isMatchServer() && !cycleInProgress) {
                int matchPort = 25566;
                if (player.server.getPort() != matchPort && isMatchPortReachable(matchPort)) {
                    com.pigeostudios.pwp.network.PacketHandler.CHANNEL.send(
                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                        new com.pigeostudios.pwp.network.TransferPacket("127.0.0.1:" + matchPort));
                    PWP.LOGGER.info("Late joiner {} transferred to match server", player.getName().getString());
                }
            } else if (cycleInProgress) {
                PWP.LOGGER.info("Match cycle in progress, skipping auto-transfer for {}", player.getName().getString());
            }
        }
    }

    private static void checkAndAutoStartCycle(MinecraftServer srv) {
        try {
            Path flagFile = Path.of(System.getProperty("user.dir")).resolve("../launcher/match_cycle_done.flag").normalize();
            if (Files.exists(flagFile)) {
                Files.delete(flagFile);
                LifecycleNotifier.broadcastNotification(srv, "cycle_detected", 4000);
                PWP.LOGGER.info("Match cycle flag detected, importing sync data...");
                // Import all player data + vehicle cooldowns from the finished match
                srv.execute(() -> PlayerDataSyncManager.importMatchData(srv));
                PWP.LOGGER.info("Auto-starting next match in 5s...");
                new Thread(() -> {
                    try { Thread.sleep(4000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    srv.execute(() -> {
                        LifecycleNotifier.broadcastNotification(srv, "auto_start", 4000);
                    });
                    try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    srv.execute(() -> {
                        srv.getCommands().performPrefixedCommand(srv.createCommandSourceStack(), "startmatch");
                    });
                }, "AutoMatchStarter").start();
            }
        } catch (Exception e) {
            PWP.LOGGER.error("Failed to check cycle flag", e);
        }
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            net.minecraft.core.BlockPos pos = event.getPos();
            if (player.level().getBlockState(pos).getBlock() == PWP.RESPAWN_BEACON_BLOCK.get()) {
                // Check for player-placed beacon (has block entity with owner)
                if (player.level().getBlockEntity(pos) instanceof com.pigeostudios.pwp.blockentity.RespawnBeaconBlockEntity beacon
                        && beacon.getOwnerUUID() != null) {
                    PWP.getRespawnManager().onBeaconBroken(pos, beacon);
                    com.pigeostudios.pwp.network.NotificationPacket bcnPkt = new com.pigeostudios.pwp.network.NotificationPacket(
                        "\u2699 \u041c\u0430\u044f\u043a " + beacon.getName() + " \u0440\u0430\u0437\u0440\u0443\u0448\u0435\u043d!", "error", 4000, "");
                    com.pigeostudios.pwp.network.PacketHandler.CHANNEL.send(
                        net.minecraftforge.network.PacketDistributor.ALL.noArg(), bcnPkt);
                }
                // Check for FOB — remove from manager on block break
                String dim = player.serverLevel().dimension().location().toString();
                com.pigeostudios.pwp.core.FOBManager.SavedFOB fob = PWP.getFOBManager().getFOBAt(pos, dim);
                if (fob != null) {
                    PWP.getFOBManager().removeFOB(fob.fobId);
                    com.pigeostudios.pwp.network.NotificationPacket fobPkt = new com.pigeostudios.pwp.network.NotificationPacket(
                        "\u2699 FOB " + fob.name + " \u0440\u0430\u0437\u0440\u0443\u0448\u0435\u043d\u0430!", "error", 4000, "");
                    com.pigeostudios.pwp.network.PacketHandler.CHANNEL.send(
                        net.minecraftforge.network.PacketDistributor.ALL.noArg(), fobPkt);
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.setHealth(player.getMaxHealth());
            GameManager game = PWP.getGameManager();

            if (game == null) {
                teamManager.fullSyncPlayer(player);
                return;
            }

            if (game.isPlaying()) {
                MapConfig map = PWP.getMapPoolManager().getCurrentMap().orElse(null);
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
                        new com.pigeostudios.pwp.network.SoundPacket(
                            com.pigeostudios.pwp.core.ModSounds.GUI_RESPAWN.get().getLocation().toString()));
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

    private int noHungerTick;

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
            noHungerTick++;
            if (noHungerTick >= 100) {
                noHungerTick = 0;
                player.getFoodData().setFoodLevel(20);
                player.getFoodData().setSaturation(5.0f);
            }

            GameManager gm = PWP.getGameManager();
            if (gm != null && gm.isPlaying()) {
                BattlefieldRuntime runtime = BattlefieldRuntime.getInstance();
                if (player.zza != 0 || player.xxa != 0) {
                    double dx = player.getX() - player.xOld;
                    double dz = player.getZ() - player.zOld;
                    if (dx * dx + dz * dz > 0.001) {
                        runtime.addActivity(player.getUUID(), BattlefieldRuntime.SCORE_MOVE);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID uuid = player.getUUID();
            RateLimiter.removePlayer(uuid);
            BattlefieldRuntime.getInstance().resetForPlayer(uuid);
            KitManager km = PWP.getKitManager();
            if (km != null) km.clearPlayerCooldowns(uuid);
            SquadManager sm = PWP.getSquadManager();
            if (sm != null) sm.removePlayer(uuid);
            MarkerManager mm = PWP.getMarkerManager();
            if (mm != null) mm.removePlayerMarkers(uuid);
            VehicleManager vm = PWP.getVehicleManager();
            if (vm != null) vm.unregisterPlayerVehicles(uuid);
            FOBManager fm = PWP.getFOBManager();
            if (fm != null) fm.clearPlayerCooldown(uuid);
            var gm = TeamVoicePlugin.getGroupManager();
            if (gm != null) gm.leaveChannel(uuid);
            AttachmentEventHandler.clearPlayerAttachments(player);
            PWP.LOGGER.info("Cleaned up player data: {}", player.getName().getString());
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
                        PWP.LOGGER.info("Renamed dog tag for {} to {}", player.getName().getString(), name);
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
                        PWP.LOGGER.info("Gave dog tag to {} with name {}", player.getName().getString(), name);
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            PWP.LOGGER.warn("Failed to set dog tag name: {}", e.getMessage());
        }
        return false;
    }
}
