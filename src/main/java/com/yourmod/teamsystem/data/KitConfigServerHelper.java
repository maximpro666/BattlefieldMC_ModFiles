package com.yourmod.teamsystem.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.core.KitManager;
import com.yourmod.teamsystem.core.PlayerCombatData;
import com.yourmod.teamsystem.core.Team;
import com.yourmod.teamsystem.core.TeamManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public class KitConfigServerHelper {

    /**
     * Apply a KitConfig-based kit to a player.
     * @param player target player
     * @param classId class key from KitConfig.classes
     * @param kitId kit key from ClassConfig.kits
     * @return error message or null on success
     */
    public static String applyKit(ServerPlayer player, String classId, String kitId) {
        KitConfig cfg = KitConfig.get();
        if (cfg == null) return "Kit config not loaded";

        KitConfig.ClassConfig clazz = cfg.classes.get(classId);
        if (clazz == null) return "Class not found: " + classId;

        KitConfig.KitDef kit = clazz.kits.get(kitId);
        if (kit == null) return "Kit not found: " + kitId;

        TeamManager teamManager = TeamSystem.getTeamManager();
        PlayerCombatData data = teamManager.getOrCreatePlayerData(player.getUUID());

        // Check requirements
        if (kit.requirements != null) {
            if (data.getRankOrdinal() < kit.requirements.rank)
                return "Rank too low, need " + kit.requirements.rank;
            if (kit.requirements.team != null && !kit.requirements.team.equalsIgnoreCase(data.getTeam().name()))
                return "Wrong team for this kit";
        }

        if (!player.isAlive()) return "You are dead";

        // Read player's loadout config
        PlayerLoadout loadout = parseLoadout(data.getLoadoutConfig(), kit);
        loadout.player_uuid = player.getUUID().toString();
        loadout.classId = classId;
        loadout.kitId = kitId;

        // Build items from loadout
        List<ItemStack> hotbarItems = new ArrayList<>();
        ItemStack primary = resolveFirst(kit.weapons.primary, loadout.loadout.primary != null ? loadout.loadout.primary.id : null);
        ItemStack secondary = resolveFirst(kit.weapons.secondary, loadout.loadout.secondary != null ? loadout.loadout.secondary.id : null);
        ItemStack special = resolveFirst(kit.weapons.special, loadout.loadout.special);
        ItemStack grenade = resolveFirst(kit.weapons.grenade, loadout.loadout.grenade);

        if (!primary.isEmpty()) hotbarItems.add(primary);
        if (!secondary.isEmpty()) hotbarItems.add(secondary);
        if (!special.isEmpty()) hotbarItems.add(special);
        if (!grenade.isEmpty()) hotbarItems.add(grenade);

        // Clear inventory and apply
        player.getInventory().clearContent();
        player.getInventory().armor.set(0, ItemStack.EMPTY);
        player.getInventory().armor.set(1, ItemStack.EMPTY);
        player.getInventory().armor.set(2, ItemStack.EMPTY);
        player.getInventory().armor.set(3, ItemStack.EMPTY);
        player.getInventory().offhand.set(0, ItemStack.EMPTY);
        KitManager.clearCuriosSlots(player);

        // Place items in hotbar slots 0-3 (primary, secondary, special, grenade)
        for (int i = 0; i < hotbarItems.size() && i < 9; i++) {
            player.getInventory().setItem(i, hotbarItems.get(i));
        }

        player.inventoryMenu.broadcastChanges();
        return null; // success
    }

    private static ItemStack resolveFirst(List<String> options, String preferred) {
        if (options == null || options.isEmpty()) return ItemStack.EMPTY;

        // Try preferred weapon first
        if (preferred != null && !preferred.isEmpty() && options.contains(preferred)) {
            ItemStack stack = ItemResolver.resolve(preferred);
            if (!stack.isEmpty()) return stack;
        }

        // Fallback to first available option
        for (String opt : options) {
            ItemStack stack = ItemResolver.resolve(opt);
            if (!stack.isEmpty()) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static PlayerLoadout parseLoadout(String json, KitConfig.KitDef kit) {
        PlayerLoadout loadout = new PlayerLoadout();
        loadout.loadout = new PlayerLoadout.LoadoutSlots();

        if (json == null || json.isEmpty()) return loadout;

        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();

            String primaryStr = obj.has("Primary") ? obj.get("Primary").getAsString() : "";
            String secondaryStr = obj.has("Secondary") ? obj.get("Secondary").getAsString() : "";
            String specialStr = obj.has("Special") ? obj.get("Special").getAsString() : "";
            String grenadeStr = obj.has("Grenade") ? obj.get("Grenade").getAsString() : "";

            // Validate choices against kit options
            if (!primaryStr.isEmpty() && kit.weapons.primary.contains(primaryStr)) {
                loadout.loadout.primary = new PlayerLoadout.WeaponSlot(primaryStr);
            }
            if (!secondaryStr.isEmpty() && kit.weapons.secondary.contains(secondaryStr)) {
                loadout.loadout.secondary = new PlayerLoadout.WeaponSlot(secondaryStr);
            }
            if (!specialStr.isEmpty() && kit.weapons.special.contains(specialStr)) {
                loadout.loadout.special = specialStr;
            }
            if (!grenadeStr.isEmpty() && kit.weapons.grenade.contains(grenadeStr)) {
                loadout.loadout.grenade = grenadeStr;
            }
        } catch (Exception e) {
            TeamSystem.LOGGER.warn("Failed to parse loadout JSON: {}", e.getMessage());
        }

        return loadout;
    }

    /**
     * Get available classes for a player (filtered by team).
     */
    public static Map<String, KitConfig.ClassConfig> getClassesForPlayer(ServerPlayer player) {
        KitConfig cfg = KitConfig.get();
        if (cfg == null) return Map.of();

        Team team = TeamSystem.getTeamManager().getOrCreatePlayerData(player.getUUID()).getTeam();
        int rank = TeamSystem.getTeamManager().getOrCreatePlayerData(player.getUUID()).getRankOrdinal();

        Map<String, KitConfig.ClassConfig> result = new LinkedHashMap<>();
        for (Map.Entry<String, KitConfig.ClassConfig> entry : cfg.classes.entrySet()) {
            KitConfig.ClassConfig clazz = entry.getValue();
            boolean classAvailable = false;

            for (KitConfig.KitDef kit : clazz.kits.values()) {
                if (kit.requirements == null) {
                    classAvailable = true;
                    break;
                }
                boolean teamOk = kit.requirements.team == null || kit.requirements.team.equalsIgnoreCase(team.name());
                boolean rankOk = rank >= kit.requirements.rank;
                if (teamOk && rankOk) {
                    classAvailable = true;
                    break;
                }
            }

            if (classAvailable) {
                result.put(entry.getKey(), clazz);
            }
        }
        return result;
    }
}
