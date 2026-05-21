package com.yourmod.teamsystem.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.core.BattlefieldRuntime;
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

        if (!player.isAlive()) {
            // Kit selection already saved in packet handler — will apply on respawn
            return "You are dead — kit will apply on respawn";
        }

        BattlefieldRuntime rt = BattlefieldRuntime.getInstance();
        if (kit.requirements != null) {
            if (data.getRankOrdinal() < kit.requirements.rank)
                return "Rank too low, need " + kit.requirements.rank;
            if (kit.requirements.team != null && !kit.requirements.team.equalsIgnoreCase(data.getTeam().name()))
                return "Wrong team for this kit";
            if (kit.requirements.bc_cost > 0) {
                if (rt.getBC(player.getUUID()) < kit.requirements.bc_cost)
                    return "Not enough BC, need " + kit.requirements.bc_cost;
                rt.deductBC(player.getUUID(), kit.requirements.bc_cost);
            }
        }

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
        String firstCaliber = null;
        for (ItemStack stack : hotbarItems) {
            ItemStack ammo = getAmmoFor(stack);
            if (!ammo.isEmpty() && ammoSlot < 9) {
                player.getInventory().setItem(ammoSlot, ammo);
                ammoSlot++;
            }
            // Track first TACZ caliber for ammo box filling
            if (firstCaliber == null) {
                String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                if (id.equals("tacz:modern_kinetic_gun") && stack.getTag() != null) {
                    String gunId = stack.getTag().getString("GunId");
                    if (!gunId.isEmpty()) firstCaliber = TACZ_CALIBER_MAP.get(gunId);
                }
            }
        }

        // Fill or give tacz ammo box with matching caliber
        if (firstCaliber != null) {
            ItemStack ammoBox = null;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack s = player.getInventory().getItem(i);
                if (!s.isEmpty() && BuiltInRegistries.ITEM.getKey(s.getItem()).toString().equals("tacz:ammo_box")) {
                    ammoBox = s;
                    break;
                }
            }
            if (ammoBox == null && ammoSlot < 9) {
                ammoBox = resolveDirect("tacz:ammo_box");
                if (!ammoBox.isEmpty()) {
                    player.getInventory().setItem(ammoSlot, ammoBox);
                }
            }
            if (ammoBox != null && !ammoBox.isEmpty()) {
                CompoundTag tag = ammoBox.getOrCreateTag();
                tag.putString("AmmoId", firstCaliber);
                tag.putInt("AmmoCount", 240);
                tag.putInt("Level", 1);
            }
        }

        player.inventoryMenu.broadcastChanges();
        return null;
    }

    private static final Map<String, String> TACZ_CALIBER_MAP = Map.ofEntries(
        // ══ TACZ Assault Rifles ══
        Map.entry("tacz:m4a1", "tacz:556x45"),
        Map.entry("tacz:hk416a5", "tacz:556x45"),
        Map.entry("tacz:hk416d", "tacz:556x45"),
        Map.entry("tacz:scar_l", "tacz:556x45"),
        Map.entry("tacz:m16a4", "tacz:556x45"),
        Map.entry("tacz:m16a1", "tacz:556x45"),
        Map.entry("tacz:aug", "tacz:556x45"),
        Map.entry("tacz:g36k", "tacz:556x45"),
        Map.entry("tacz:spr15hb", "tacz:556x45"),
        Map.entry("tacz:ak47", "tacz:762x39"),
        Map.entry("tacz:rpk", "tacz:762x39"),
        Map.entry("tacz:type_81", "tacz:762x39"),
        Map.entry("tacz:sks_tactical", "tacz:762x39"),
        Map.entry("tacz:qbz_95", "tacz:58x42"),
        Map.entry("tacz:qbz_191", "tacz:58x42"),
        // ══ TACZ DMRs / Battle Rifles ══
        Map.entry("tacz:mk14", "tacz:308"),
        Map.entry("tacz:scar_h", "tacz:308"),
        Map.entry("tacz:hk_g3", "tacz:308"),
        Map.entry("tacz:fn_fal", "tacz:308"),
        Map.entry("tacz:fn_evolys", "tacz:308"),
        // ══ TACZ Snipers ══
        Map.entry("tacz:ai_awp", "tacz:338"),
        Map.entry("tacz:m107", "tacz:50bmg"),
        Map.entry("tacz:m95", "tacz:50bmg"),
        Map.entry("tacz:m700", "tacz:30_06"),
        Map.entry("tacz:lonetrail", "tacz:30_06"),
        Map.entry("tacz:kar98", "tacz:792x57"),
        // ══ TACZ SMGs ══
        Map.entry("tacz:hk_mp5a5", "tacz:9mm"),
        Map.entry("tacz:uzi", "tacz:9mm"),
        Map.entry("tacz:b93r", "tacz:9mm"),
        Map.entry("tacz:vector45", "tacz:45acp"),
        Map.entry("tacz:ump45", "tacz:45acp"),
        Map.entry("tacz:p90", "tacz:57x28"),
        // ══ TACZ Pistols ══
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
        // ══ TACZ Shotguns ══
        Map.entry("tacz:aa12", "tacz:12g"),
        Map.entry("tacz:m1014", "tacz:12g"),
        Map.entry("tacz:spas_12", "tacz:12g"),
        Map.entry("tacz:m870", "tacz:12g"),
        Map.entry("tacz:db_long", "tacz:12g"),
        Map.entry("tacz:db_short", "tacz:12g"),
        // ══ TACZ LMGs ══
        Map.entry("tacz:m249", "tacz:556x45"),
        Map.entry("tacz:minigun", "tacz:308"),
        // ══ MaxStuff ══
        Map.entry("maxstuff:mk18", "tacz:556x45"),
        Map.entry("maxstuff:sr15", "tacz:556x45"),
        Map.entry("maxstuff:sr16", "tacz:556x45"),
        Map.entry("maxstuff:ar10", "tacz:308"),
        Map.entry("maxstuff:colt_933", "tacz:556x45"),
        Map.entry("maxstuff:ak12", "tacz:545x39"),
        Map.entry("maxstuff:ak15", "tacz:545x39"),
        Map.entry("maxstuff:ak19", "tacz:545x39"),
        Map.entry("maxstuff:ak74", "tacz:545x39"),
        Map.entry("maxstuff:ak74m", "tacz:545x39"),
        Map.entry("maxstuff:aks74u", "tacz:545x39"),
        Map.entry("maxstuff:ak_alpha", "tacz:545x39"),
        Map.entry("maxstuff:ak_delta", "tacz:545x39"),
        Map.entry("maxstuff:ak107", "tacz:545x39"),
        Map.entry("maxstuff:ak108", "tacz:545x39"),
        Map.entry("maxstuff:ak109", "tacz:545x39"),
        Map.entry("maxstuff:rpk16", "tacz:545x39"),
        Map.entry("maxstuff:rpk_74", "tacz:545x39"),
        Map.entry("maxstuff:mk11", "tacz:308"),
        Map.entry("maxstuff:mk47", "tacz:762x39"),
        Map.entry("maxstuff:scar_ssr", "tacz:308"),
        Map.entry("maxstuff:scar_hamr", "tacz:308"),
        Map.entry("maxstuff:beowulf_ecr", "tacz:556x45"),
        Map.entry("maxstuff:beowulf_tcr", "tacz:556x45"),
        Map.entry("maxstuff:mrad", "tacz:308"),
        Map.entry("maxstuff:msr", "tacz:308"),
        Map.entry("maxstuff:dragonuv_svd", "tacz:762x54"),
        Map.entry("maxstuff:dragonuv_svdm", "tacz:762x54"),
        Map.entry("maxstuff:dragonuv_svds", "tacz:762x54"),
        Map.entry("maxstuff:dp28", "tacz:762x54"),
        Map.entry("maxstuff:ai_awp", "tacz:338"),
        Map.entry("maxstuff:ai_aws", "tacz:338"),
        Map.entry("maxstuff:bfg50", "tacz:50bmg"),
        Map.entry("maxstuff:gm6_lynx", "tacz:50bmg"),
        Map.entry("maxstuff:m82a2", "tacz:50bmg"),
        Map.entry("maxstuff:thunderbird", "tacz:50bmg"),
        Map.entry("maxstuff:thunderbird_short", "tacz:50bmg"),
        Map.entry("maxstuff:excaliber", "tacz:50bmg"),
        Map.entry("maxstuff:hk_mp5k", "tacz:9mm"),
        Map.entry("maxstuff:hk_mp5sd", "tacz:9mm"),
        Map.entry("maxstuff:mp7", "tacz:46x30"),
        Map.entry("maxstuff:udp9", "tacz:9mm"),
        Map.entry("maxstuff:ump9", "tacz:9mm"),
        Map.entry("maxstuff:vector9", "tacz:9mm"),
        Map.entry("maxstuff:vityas", "tacz:9mm"),
        Map.entry("maxstuff:hk416c", "tacz:556x45"),
        Map.entry("maxstuff:hk417", "tacz:308"),
        Map.entry("maxstuff:hk_g33", "tacz:556x45"),
        Map.entry("maxstuff:honey_badger", "tacz:762x39"),
        Map.entry("maxstuff:saiga12", "tacz:12g"),
        Map.entry("maxstuff:saiga9", "tacz:9mm"),
        Map.entry("maxstuff:m870t", "tacz:12g"),
        Map.entry("maxstuff:genesis12", "tacz:12g"),
        Map.entry("maxstuff:genesis12_fl", "tacz:12g"),
        Map.entry("maxstuff:genesis12_dragons_breath", "tacz:12g"),
        Map.entry("maxstuff:sks_golden", "tacz:762x39"),
        Map.entry("maxstuff:sks_wooden", "tacz:762x39"),
        Map.entry("maxstuff:glock_18c", "tacz:9mm"),
        Map.entry("maxstuff:glock_54", "tacz:9mm"),
        Map.entry("maxstuff:m17", "tacz:9mm"),
        Map.entry("maxstuff:mk23", "tacz:45acp"),
        Map.entry("maxstuff:af2011", "tacz:9mm"),
        Map.entry("maxstuff:scar_15p", "tacz:556x45"),
        Map.entry("maxstuff:scar_pdw", "tacz:556x45"),
        Map.entry("maxstuff:scar_sbr", "tacz:556x45"),
        Map.entry("maxstuff:qbz_97", "tacz:58x42"),
        // ══ Daffas Arsenal ══
        Map.entry("daffas_arsenal:hk416", "tacz:556x45"),
        Map.entry("daffas_arsenal:hk416_sup", "tacz:556x45"),
        Map.entry("daffas_arsenal:hk417", "tacz:308"),
        Map.entry("daffas_arsenal:g36", "tacz:556x45"),
        Map.entry("daffas_arsenal:g36c", "tacz:556x45"),
        Map.entry("daffas_arsenal:g36c_commando", "tacz:556x45"),
        Map.entry("daffas_arsenal:sg550", "tacz:556x45"),
        Map.entry("daffas_arsenal:sg552", "tacz:556x45"),
        Map.entry("daffas_arsenal:sl8", "tacz:556x45"),
        Map.entry("daffas_arsenal:sgputer", "tacz:556x45"),
        Map.entry("daffas_arsenal:sgputer2", "tacz:556x45"),
        Map.entry("daffas_arsenal:ak47", "tacz:762x39"),
        Map.entry("daffas_arsenal:aksopmod", "tacz:762x39"),
        Map.entry("daffas_arsenal:ss1_v2", "tacz:762x39"),
        Map.entry("daffas_arsenal:ss1_v5", "tacz:762x39"),
        Map.entry("daffas_arsenal:ss1_v5g", "tacz:762x39"),
        Map.entry("daffas_arsenal:ssg69", "tacz:762x54"),
        Map.entry("daffas_arsenal:ssg69s", "tacz:762x54"),
        Map.entry("daffas_arsenal:spasi15", "tacz:50bmg"),
        Map.entry("daffas_arsenal:mp5k", "tacz:9mm"),
        Map.entry("daffas_arsenal:hk45", "tacz:45acp"),
        Map.entry("daffas_arsenal:hk45_sup", "tacz:45acp"),
        Map.entry("daffas_arsenal:p99_hak", "tacz:9mm"),
        Map.entry("daffas_arsenal:p99_nohak", "tacz:9mm"),
        Map.entry("daffas_arsenal:gerosa", "tacz:9mm"),
        Map.entry("daffas_arsenal:harpa9", "tacz:9mm"),
        Map.entry("daffas_arsenal:apacoba9", "tacz:9mm"),
        Map.entry("daffas_arsenal:apacoba9_sup", "tacz:9mm"),
        Map.entry("daffas_arsenal:makten_single", "tacz:12g"),
        Map.entry("daffas_arsenal:makten_single_taktis", "tacz:12g"),
        Map.entry("daffas_arsenal:migraine3", "tacz:9mm"),
        Map.entry("daffas_arsenal:samula3", "tacz:9mm"),
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

        // Throwables / tools / mines that don't need ammo
        String[] noAmmoPaths = {"hand_grenade", "rgo_grenade", "c4_bomb", "taser",
            "repair_tool", "m18_smoke_grenade", "claymore_mine", "blu_43_mine", "lunge_mine", "m_79"};
        for (String p : noAmmoPaths) {
            if (id.endsWith(p)) return ItemStack.EMPTY;
        }

        // RPG ammo
        if (id.equals("superbwarfare:rpg")) {
            return resolveDirect("superbwarfare:rpg_rocket_standard");
        }

        // Javelin ammo
        if (id.equals("superbwarfare:javelin")) {
            return resolveDirect("superbwarfare:javelin_missile");
        }

        // Igla MANPADS ammo (anti-air missile)
        if (id.equals("superbwarfare:igla_9k38")) {
            return resolveDirect("superbwarfare:medium_anti_air_missile");
        }

        // Superb Warfare conventional ammo
        if (!id.startsWith("superbwarfare:")) return ItemStack.EMPTY;

        String path = id.substring("superbwarfare:".length());

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
