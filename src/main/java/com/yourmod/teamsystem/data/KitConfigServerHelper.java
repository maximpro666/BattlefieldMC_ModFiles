package com.yourmod.teamsystem.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.core.KitManager;
import com.yourmod.teamsystem.core.PlayerCombatData;
import com.yourmod.teamsystem.core.Team;
import com.yourmod.teamsystem.core.TeamManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public class KitConfigServerHelper {

    public static String applyKit(ServerPlayer player, String classId, String kitId) {
        KitConfig cfg = KitConfig.get();
        if (cfg == null) return "Kit config not loaded";

        KitConfig.ClassConfig clazz = cfg.classes.get(classId);
        if (clazz == null) return "Class not found: " + classId;

        KitConfig.KitDef kit = clazz.kits.get(kitId);
        if (kit == null) return "Kit not found: " + kitId;

        TeamManager teamManager = TeamSystem.getTeamManager();
        PlayerCombatData data = teamManager.getOrCreatePlayerData(player.getUUID());

        if (kit.requirements != null) {
            if (data.getRankOrdinal() < kit.requirements.rank)
                return "Rank too low, need " + kit.requirements.rank;
            if (kit.requirements.team != null && !kit.requirements.team.equalsIgnoreCase(data.getTeam().name()))
                return "Wrong team for this kit";
        }

        if (!player.isAlive()) return "You are dead";

        // Parse loadout
        PlayerLoadout loadout = parseLoadout(data.getLoadoutConfig(), kit);
        loadout.player_uuid = player.getUUID().toString();
        loadout.classId = classId;
        loadout.kitId = kitId;

        // Resolve weapon items
        List<ItemStack> hotbarItems = new ArrayList<>();
        ItemStack primary = resolveFirst(kit.weapons.primary, loadout.loadout.primary != null ? loadout.loadout.primary.id : null);
        ItemStack secondary = resolveFirst(kit.weapons.secondary, loadout.loadout.secondary != null ? loadout.loadout.secondary.id : null);
        ItemStack special = resolveFirst(kit.weapons.special, loadout.loadout.special);
        ItemStack grenade = resolveFirst(kit.weapons.grenade, loadout.loadout.grenade);

        if (!primary.isEmpty()) hotbarItems.add(primary);
        if (!secondary.isEmpty()) hotbarItems.add(secondary);
        if (!special.isEmpty()) hotbarItems.add(special);
        if (!grenade.isEmpty()) hotbarItems.add(grenade);

        // Resolve armor items
        ItemStack helmet = resolveFirst(kit.armor.helmet, loadout.loadout.helmet);
        ItemStack chestplate = resolveFirst(kit.armor.chestplate, loadout.loadout.chestplate);
        ItemStack backpack = resolveFirst(kit.armor.backpack, loadout.loadout.backpack);
        ItemStack shoulderpads = resolveFirst(kit.armor.shoulderpads, loadout.loadout.shoulderpads);

        // Clear everything
        player.getInventory().clearContent();
        player.getInventory().armor.set(0, ItemStack.EMPTY);
        player.getInventory().armor.set(1, ItemStack.EMPTY);
        player.getInventory().armor.set(2, ItemStack.EMPTY);
        player.getInventory().armor.set(3, ItemStack.EMPTY);
        player.getInventory().offhand.set(0, ItemStack.EMPTY);
        KitManager.clearCuriosSlots(player);

        // Place weapons in hotbar slots 0-3
        for (int i = 0; i < hotbarItems.size() && i < 9; i++) {
            player.getInventory().setItem(i, hotbarItems.get(i));
        }

        // Apply armor
        if (!helmet.isEmpty())   player.getInventory().armor.set(3, helmet);
        if (!chestplate.isEmpty()) player.getInventory().armor.set(2, chestplate);
        if (!backpack.isEmpty()) KitManager.setCurioSlotByType(player, "back", backpack);
        if (!shoulderpads.isEmpty()) KitManager.setCurioSlotByType(player, "body", shoulderpads);

        // Auto-ammo for Superb Warfare weapons (TACZ guns carry their own mags)
        int ammoSlot = 4;
        for (ItemStack stack : hotbarItems) {
            ItemStack ammo = getAmmoFor(stack);
            if (!ammo.isEmpty() && ammoSlot < 9) {
                player.getInventory().setItem(ammoSlot, ammo);
                ammoSlot++;
            }
        }

        player.inventoryMenu.broadcastChanges();
        return null;
    }

    private static final Map<String, String> TACZ_CALIBER_MAP = Map.ofEntries(
        // Assault Rifles
        Map.entry("tacz:m4a1", "tacz:556x45"),
        Map.entry("tacz:hk416a5", "tacz:556x45"),
        Map.entry("tacz:hk416d", "tacz:556x45"),
        Map.entry("tacz:scar_l", "tacz:556x45"),
        Map.entry("tacz:m16a4", "tacz:556x45"),
        Map.entry("tacz:m16a1", "tacz:556x45"),
        Map.entry("tacz:aug", "tacz:556x45"),
        Map.entry("tacz:g36k", "tacz:556x45"),
        Map.entry("tacz:ak47", "tacz:762x39"),
        Map.entry("tacz:rpk", "tacz:762x39"),
        Map.entry("tacz:type_81", "tacz:762x39"),
        Map.entry("tacz:sks_tactical", "tacz:762x39"),
        Map.entry("tacz:qbz_95", "tacz:58x42"),
        Map.entry("tacz:qbz_191", "tacz:58x42"),
        // DMRs / Battle Rifles
        Map.entry("tacz:mk14", "tacz:308"),
        Map.entry("tacz:scar_h", "tacz:308"),
        Map.entry("tacz:hk_g3", "tacz:308"),
        Map.entry("tacz:fn_fal", "tacz:308"),
        Map.entry("tacz:fn_evolys", "tacz:308"),
        // Snipers
        Map.entry("tacz:ai_awp", "tacz:338"),
        Map.entry("tacz:m107", "tacz:50bmg"),
        Map.entry("tacz:m95", "tacz:50bmg"),
        Map.entry("tacz:m700", "tacz:30_06"),
        Map.entry("tacz:lonetrail", "tacz:30_06"),
        Map.entry("tacz:kar98", "tacz:792x57"),
        Map.entry("tacz:spr15hb", "tacz:556x45"),
        // SMGs
        Map.entry("tacz:hk_mp5a5", "tacz:9mm"),
        Map.entry("tacz:uzi", "tacz:9mm"),
        Map.entry("tacz:b93r", "tacz:9mm"),
        Map.entry("tacz:vector45", "tacz:45acp"),
        Map.entry("tacz:ump45", "tacz:45acp"),
        Map.entry("tacz:p90", "tacz:57x28"),
        // Pistols
        Map.entry("tacz:glock_17", "tacz:9mm"),
        Map.entry("tacz:cz75", "tacz:9mm"),
        Map.entry("tacz:m9a4", "tacz:9mm"),
        Map.entry("tacz:p320", "tacz:45acp"),
        Map.entry("tacz:m1911", "tacz:45acp"),
        Map.entry("tacz:hk_mk23", "tacz:45acp"),
        Map.entry("tacz:deagle", "tacz:50ae"),
        Map.entry("tacz:timeless50", "tacz:50ae"),
        Map.entry("tacz:rhino357", "tacz:357mag"),
        Map.entry("tacz:taurus500", "tacz:500mag"),
        Map.entry("tacz:taurus943", "tacz:22wmr"),
        Map.entry("tacz:springfield1873", "tacz:45_70"),
        // Shotguns
        Map.entry("tacz:aa12", "tacz:12g"),
        Map.entry("tacz:m1014", "tacz:12g"),
        Map.entry("tacz:spas_12", "tacz:12g"),
        Map.entry("tacz:m870", "tacz:12g"),
        Map.entry("tacz:db_long", "tacz:12g"),
        Map.entry("tacz:db_short", "tacz:12g"),
        // LMGs
        Map.entry("tacz:m249", "tacz:556x45"),
        Map.entry("tacz:minigun", "tacz:308"),
        // Launchers
        Map.entry("tacz:rpg7", "tacz:rpg_rocket"),
        Map.entry("tacz:m320", "tacz:40mm")
    );

    private static ItemStack getAmmoFor(ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();

        // TACZ guns use caliber-based ammo
        if (id.equals("tacz:modern_kinetic_gun")) {
            CompoundTag tag = stack.getTag();
            if (tag != null && tag.contains("GunId")) {
                String gunId = tag.getString("GunId");
                String caliber = TACZ_CALIBER_MAP.get(gunId);
                if (caliber != null) {
                    return resolveCaliberAmmo(caliber);
                }
            }
            return ItemStack.EMPTY;
        }

        // Superb Warfare ammo mapping
        if (!id.startsWith("superbwarfare:")) return ItemStack.EMPTY;

        String path = id.substring("superbwarfare:".length());

        // Throwables & tools that don't need ammo
        if (path.equals("hand_grenade") || path.equals("rgo_grenade")
            || path.equals("c4_bomb") || path.equals("taser")
            || path.equals("repair_tool") || path.equals("smoke_grenade")
            || path.equals("claymore") || path.equals("anti_tank_mine")) {
            return ItemStack.EMPTY;
        }

        // RPG / launchers
        if (path.equals("rpg") || path.equals("javelin") || path.equals("igla_9k38")
            || path.equals("m_79")) {
            return resolveDirect("superbwarfare:heavy_ammo");
        }

        // Handguns
        if (path.contains("glock") || path.contains("m_1911") || path.contains("mp_443")
            || path.contains("deagle") || path.contains("cz75")) {
            return resolveDirect("superbwarfare:handgun_ammo_box");
        }

        // SMGs (use handgun ammo - both are pistol caliber)
        if (path.contains("mp_5") || path.contains("vector")) {
            return resolveDirect("superbwarfare:handgun_ammo_box");
        }

        // Shotguns
        if (path.contains("m_870") || path.contains("aa_12")) {
            return resolveDirect("superbwarfare:shotgun_ammo_box");
        }

        // Snipers
        if (path.contains("awm") || path.contains("mosin") || path.contains("ntw")
            || path.contains("m_98b") || path.contains("svd") || path.contains("sks")
            || path.contains("bocek") || path.contains("k_98") || path.contains("mk_14")
            || path.contains("marlin") || path.contains("hunting_rifle")) {
            return resolveDirect("superbwarfare:sniper_ammo_box");
        }

        // Rifles / LMGs
        if (path.contains("m_4") || path.contains("hk_416") || path.contains("ak_47")
            || path.contains("ak_12") || path.contains("qbz") || path.contains("rpk")
            || path.contains("m_60") || path.contains("m_2_hb") || path.contains("minigun")) {
            return resolveDirect("superbwarfare:rifle_ammo_box");
        }

        // Warborn Explosives grenades (no ammo needed - consumable)
        if (id.startsWith("warbornexplosives:")) {
            return ItemStack.EMPTY;
        }

        // Warborn armor (no ammo needed)
        if (id.startsWith("warborn:")) {
            return ItemStack.EMPTY;
        }

        return ItemStack.EMPTY;
    }

    private static ItemStack resolveCaliberAmmo(String caliberId) {
        try {
            ResourceLocation rl = new ResourceLocation(caliberId);
            net.minecraft.world.item.Item item = BuiltInRegistries.ITEM.get(rl);
            if (item != null && item != net.minecraft.world.item.Items.AIR) {
                return new ItemStack(item);
            }
        } catch (Exception e) {
            // ignore
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack resolveDirect(String id) {
        try {
            ResourceLocation rl = new ResourceLocation(id);
            net.minecraft.world.item.Item item = BuiltInRegistries.ITEM.get(rl);
            if (item != null && item != net.minecraft.world.item.Items.AIR) {
                return new ItemStack(item);
            }
        } catch (Exception e) {
            // ignore
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack resolveFirst(List<String> options, String preferred) {
        if (options == null || options.isEmpty()) return ItemStack.EMPTY;
        if (preferred != null && !preferred.isEmpty() && options.contains(preferred)) {
            ItemStack stack = ItemResolver.resolve(preferred);
            if (!stack.isEmpty()) return stack;
        }
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

            // Parse armor
            if (obj.has("Helmet") && kit.armor.helmet.contains(obj.get("Helmet").getAsString())) {
                loadout.loadout.helmet = obj.get("Helmet").getAsString();
            }
            if (obj.has("Chestplate") && kit.armor.chestplate.contains(obj.get("Chestplate").getAsString())) {
                loadout.loadout.chestplate = obj.get("Chestplate").getAsString();
            }
            if (obj.has("Backpack") && kit.armor.backpack.contains(obj.get("Backpack").getAsString())) {
                loadout.loadout.backpack = obj.get("Backpack").getAsString();
            }
            if (obj.has("Shoulderpads") && kit.armor.shoulderpads.contains(obj.get("Shoulderpads").getAsString())) {
                loadout.loadout.shoulderpads = obj.get("Shoulderpads").getAsString();
            }

            // Parse attachments
            if (obj.has("Attachments") && obj.get("Attachments").isJsonObject()) {
                JsonObject attObj = obj.getAsJsonObject("Attachments");
                for (Map.Entry<String, JsonElement> entry : attObj.entrySet()) {
                    loadout.loadout.attachments.put(entry.getKey(), entry.getValue().getAsString());
                }
            }
        } catch (Exception e) {
            TeamSystem.LOGGER.warn("Failed to parse loadout JSON: {}", e.getMessage());
        }

        return loadout;
    }

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
