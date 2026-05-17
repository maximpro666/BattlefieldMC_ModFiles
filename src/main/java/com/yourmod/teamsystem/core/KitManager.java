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
    private static Method curiosGetStacks;
    private static Method curiosSetStack;
    private static Method curiosGetInventory;

    static {
        try {
            Class<?> apiClass = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            Method apiMethod = apiClass.getMethod("getCuriosInventory", Class.forName("net.minecraft.world.entity.LivingEntity"));
            Class<?> handlerClass = Class.forName("top.theillusivec4.curios.api.type.inventory.ICuriosItemHandler");
            curiosGetCurios = handlerClass.getMethod("getCurios");
            Class<?> invClass = Class.forName("top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler");
            curiosGetStacks = invClass.getMethod("getStackInSlot", int.class);
            curiosSetStack = invClass.getMethod("setStackInSlot", int.class, ItemStack.class);
            curiosGetInventory = apiMethod;
            curiosDetected = true;
            TeamSystem.LOGGER.info("Curios API detected for kit integration");
        } catch (Exception e) {
            curiosDetected = false;
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

    public KitManager() { loadKits(); }

    public static List<ItemStack> getCuriosSlots(ServerPlayer player) {
        List<ItemStack> result = new ArrayList<>();
        Map<String, ?> curios = getCuriosMap(player);
        for (Object slot : curios.values()) {
            Object stacks = slot; // ICurioType.getStacks() is the IDynamicStackHandler
            for (int i = 0; i < 100; i++) {
                try {
                    ItemStack stack = (ItemStack) curiosGetStacks.invoke(stacks, i);
                    if (stack == null || stack.isEmpty()) break;
                    result.add(stack);
                } catch (Exception e) { break; }
            }
        }
        return result;
    }

    public static void clearCuriosSlots(ServerPlayer player) {
        Map<String, ?> curios = getCuriosMap(player);
        for (Object slot : curios.values()) {
            Object stacks = slot;
            for (int i = 0; i < 100; i++) {
                try {
                    ItemStack s = (ItemStack) curiosGetStacks.invoke(stacks, i);
                    if (s == null || s.isEmpty()) break;
                    curiosSetStack.invoke(stacks, i, ItemStack.EMPTY);
                } catch (Exception e) { break; }
            }
        }
    }

    public static void setCuriosSlot(ServerPlayer player, int index, ItemStack stack) {
        Map<String, ?> curios = getCuriosMap(player);
        int slotIdx = 0;
        for (Object slot : curios.values()) {
            Object stacks = slot;
            for (int i = 0; i < 100; i++) {
                try {
                    ItemStack s = (ItemStack) curiosGetStacks.invoke(stacks, i);
                    if (s == null) break;
                    if (slotIdx == index) {
                        curiosSetStack.invoke(stacks, i, stack);
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

    public boolean claimKit(ServerPlayer player, String kitName, TeamManager teamManager) {
        Kit kit = getKit(kitName);
        if (kit == null) return false;
        if (!player.isAlive()) return false;
        Team playerTeam = teamManager.getOrCreatePlayerData(player.getUUID()).getTeam();
        int playerRank = teamManager.getOrCreatePlayerData(player.getUUID()).getRankOrdinal();
        if (kit.getTeam() != Team.SPECTATOR && kit.getTeam() != playerTeam) return false;
        if (playerRank < kit.getMinRankOrdinal()) return false;
        if (kit.getCooldownSeconds() > 0) {
            if (isOnCooldown(player.getUUID(), kitName)) return false;
            setCooldown(player.getUUID(), kitName, kit.getCooldownSeconds());
        }

        kit.applyToPlayer(player);
        return true;
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
