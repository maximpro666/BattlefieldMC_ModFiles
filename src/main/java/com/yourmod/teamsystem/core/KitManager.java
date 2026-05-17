package com.yourmod.teamsystem.core;

import com.google.gson.*;
import com.yourmod.teamsystem.TeamSystem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class KitManager {
    private static final String CONFIG_DIR = "config/teamsystem";
    private static final String KITS_FILE = CONFIG_DIR + "/kits.json";

    private Map<String, Kit> kits = new HashMap<>();
    private Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    private static boolean curiosDetected = false;
    private static Method curiosGetCurios;
    private static Method curiosGetStacksFromType;
    private static Method curiosGetStackInSlot;
    private static Method curiosSetStackInSlot;
    private static Method curiosGetInventory;

    static {
        try {
            Class<?> apiClass = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            curiosGetInventory = apiClass.getMethod("getCuriosInventory", Class.forName("net.minecraft.world.entity.LivingEntity"));
            Class<?> handlerClass = Class.forName("top.theillusivec4.curios.api.type.inventory.ICuriosItemHandler");
            curiosGetCurios = handlerClass.getMethod("getCurios");
            Class<?> curioTypeClass = Class.forName("top.theillusivec4.curios.api.type.ICurioType");
            curiosGetStacksFromType = curioTypeClass.getMethod("getStacks");
            Class<?> invClass = Class.forName("top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler");
            curiosGetStackInSlot = invClass.getMethod("getStackInSlot", int.class);
            curiosSetStackInSlot = invClass.getMethod("setStackInSlot", int.class, ItemStack.class);
            curiosDetected = true;
            TeamSystem.LOGGER.info("Curios API detected for kit integration");
        } catch (Exception e) {
            curiosDetected = false;
            TeamSystem.LOGGER.warn("Curios API not available: {}", e.getMessage());
        }
    }

    private static Optional<?> getCuriosHandler(ServerPlayer player) {
        if (!curiosDetected) return Optional.empty();
        try {
            Object result = curiosGetInventory.invoke(null, player);
            if (result instanceof Optional<?> o) return o;
        } catch (Exception e) {
            TeamSystem.LOGGER.warn("Failed to get curios handler: {}", e.getMessage());
        }
        return Optional.empty();
    }

    private static Map<String, ?> getCuriosMap(ServerPlayer player) {
        if (!curiosDetected) return Collections.emptyMap();
        try {
            Optional<?> opt = getCuriosHandler(player);
            if (opt.isPresent()) {
                Object handler = opt.get();
                Object result = curiosGetCurios.invoke(handler);
                if (result instanceof Map<?, ?> m) return (Map<String, ?>) m;
            }
        } catch (Exception e) {
            TeamSystem.LOGGER.warn("Failed to get curios map: {}", e.getMessage());
        }
        return Collections.emptyMap();
    }

    private static List<Object> getAllCuriosStacks(ServerPlayer player) {
        List<Object> stackHandlers = new ArrayList<>();
        if (!curiosDetected) return stackHandlers;
        Map<String, ?> curios = getCuriosMap(player);
        for (Object curioType : curios.values()) {
            try {
                Object stacks = curiosGetStacksFromType.invoke(curioType);
                if (stacks != null) stackHandlers.add(stacks);
            } catch (Exception e) {
                TeamSystem.LOGGER.warn("Failed to get stacks from curio type: {}", e.getMessage());
            }
        }
        return stackHandlers;
    }

    public KitManager() { loadKits(); }

    public static List<ItemStack> getCuriosSlots(ServerPlayer player) {
        List<ItemStack> result = new ArrayList<>();
        for (Object stacks : getAllCuriosStacks(player)) {
            for (int i = 0; i < 100; i++) {
                try {
                    ItemStack stack = (ItemStack) curiosGetStackInSlot.invoke(stacks, i);
                    if (stack == null || stack.isEmpty()) break;
                    result.add(stack);
                } catch (Exception e) { break; }
            }
        }
        return result;
    }

    public static void clearCuriosSlots(ServerPlayer player) {
        for (Object stacks : getAllCuriosStacks(player)) {
            for (int i = 0; i < 100; i++) {
                try {
                    ItemStack s = (ItemStack) curiosGetStackInSlot.invoke(stacks, i);
                    if (s == null || s.isEmpty()) break;
                    curiosSetStackInSlot.invoke(stacks, i, ItemStack.EMPTY);
                } catch (Exception e) { break; }
            }
        }
    }

    public static void setCuriosSlot(ServerPlayer player, int index, ItemStack stack) {
        int slotIdx = 0;
        for (Object stacks : getAllCuriosStacks(player)) {
            for (int i = 0; i < 100; i++) {
                try {
                    ItemStack s = (ItemStack) curiosGetStackInSlot.invoke(stacks, i);
                    if (s == null) break;
                    if (slotIdx == index) {
                        curiosSetStackInSlot.invoke(stacks, i, stack);
                        return;
                    }
                    slotIdx++;
                } catch (Exception e) { break; }
            }
        }
    }

    public void loadKits() {
        try {
            Path configPath = Paths.get(KITS_FILE);
            if (Files.exists(configPath)) {
                String content = Files.readString(configPath);
                JsonArray arr = JsonParser.parseString(content).getAsJsonArray();
                for (JsonElement elem : arr) {
                    Kit kit = Kit.fromJson(elem.getAsJsonObject());
                    kits.put(kit.getName(), kit);
                }
                TeamSystem.LOGGER.info("Loaded {} kits", kits.size());
            }
        } catch (Exception e) { TeamSystem.LOGGER.warn("Failed to load kits: {}", e.getMessage()); }
    }

    public void saveKits() {
        try {
            Path configPath = Paths.get(CONFIG_DIR);
            Files.createDirectories(configPath);
            JsonArray arr = new JsonArray();
            for (Kit kit : kits.values()) { arr.add(kit.toJson()); }
            Files.writeString(Paths.get(KITS_FILE), new GsonBuilder().setPrettyPrinting().create().toJson(arr));
            TeamSystem.LOGGER.info("Saved {} kits", kits.size());
        } catch (Exception e) { TeamSystem.LOGGER.warn("Failed to save kits: {}", e.getMessage()); }
    }

    public void createKit(String name, String displayName, Team team, int minRankOrdinal, List<Kit.IndexedItem> items) {
        Kit kit = new Kit(name, displayName, team, minRankOrdinal, 0, items);
        kits.put(name, kit);
        saveKits();
    }

    public void deleteKit(String name) { if (kits.remove(name) != null) saveKits(); }
    public Kit getKit(String name) { return kits.get(name); }

    public List<Kit> getAvailableKits(ServerPlayer player, TeamManager teamManager) {
        List<Kit> available = new ArrayList<>();
        Team playerTeam = teamManager.getOrCreatePlayerData(player.getUUID()).getTeam();
        int playerRank = teamManager.getOrCreatePlayerData(player.getUUID()).getRankOrdinal();
        for (Kit kit : kits.values()) {
            if ((kit.getTeam() == Team.SPECTATOR || kit.getTeam() == playerTeam) &&
                playerRank >= kit.getMinRankOrdinal()) {
                available.add(kit);
            }
        }
        return available;
    }

    public String claimKit(ServerPlayer player, String kitName, TeamManager teamManager) {
        Kit kit = getKit(kitName);
        if (kit == null) return "§cКит не найден: " + kitName;
        if (!player.isAlive()) return "§cВы мертвы";
        Team playerTeam = teamManager.getOrCreatePlayerData(player.getUUID()).getTeam();
        int playerRank = teamManager.getOrCreatePlayerData(player.getUUID()).getRankOrdinal();
        if (kit.getTeam() != Team.SPECTATOR && kit.getTeam() != playerTeam) return "§cКит недоступен для вашей команды";
        if (playerRank < kit.getMinRankOrdinal()) return "§cТребуется ранг " + kit.getMinRankOrdinal();
        if (kit.getCooldownSeconds() > 0) {
            if (isOnCooldown(player.getUUID(), kitName)) {
                long remaining = (cooldowns.getOrDefault(player.getUUID(), Collections.emptyMap()).getOrDefault(kitName, 0L) - System.currentTimeMillis()) / 1000;
                return "§cКд " + remaining + " сек";
            }
            setCooldown(player.getUUID(), kitName, kit.getCooldownSeconds());
        }

        kit.applyToPlayer(player);
        return null; // success
    }

    private boolean isOnCooldown(UUID playerId, String kitName) {
        Map<String, Long> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null) return false;
        Long expiry = playerCooldowns.get(kitName);
        if (expiry == null) return false;
        if (System.currentTimeMillis() >= expiry) {
            playerCooldowns.remove(kitName);
            return false;
        }
        return true;
    }

    private void setCooldown(UUID playerId, String kitName, int cooldownSeconds) {
        cooldowns.computeIfAbsent(playerId, k -> new HashMap<>())
            .put(kitName, System.currentTimeMillis() + cooldownSeconds * 1000L);
    }

    public Map<String, Kit> getKits() { return kits; }
}
