package com.pigeostudios.pwp.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class KitConfig {

    public Map<String, ClassConfig> classes = new LinkedHashMap<>();

    public static class ClassConfig {
        public String display_name;
        public String icon;
        public Map<String, KitDef> kits = new LinkedHashMap<>();
    }

    public static class KitDef {
        public String display_name;
        public String description;
        public KitWeapons weapons = new KitWeapons();
        public KitArmor armor = new KitArmor();
        public Map<String, AttachmentLimit> attachment_limits = new HashMap<>();
        public KitRequirements requirements = new KitRequirements();
    }

    public static class KitArmor {
        public List<String> helmet = new ArrayList<>();
        public List<String> chestplate = new ArrayList<>();
        public List<String> backpack = new ArrayList<>();
        public List<String> shoulderpads = new ArrayList<>();
    }

    public static class KitWeapons {
        public List<String> primary   = new ArrayList<>();
        public List<String> secondary = new ArrayList<>();
        public List<String> special   = new ArrayList<>();
        public List<String> grenade   = new ArrayList<>();
    }

    public static class AttachmentLimit {
        public List<String> scope       = new ArrayList<>();
        public List<String> barrel      = new ArrayList<>();
        public List<String> grip        = new ArrayList<>();
        public List<String> magazine    = new ArrayList<>();
        public List<String> ammo        = new ArrayList<>();
        public List<String> muzzle      = new ArrayList<>();
        public List<String> underbarrel = new ArrayList<>();

        public List<String> forCategory(String cat) {
            return switch (cat) {
                case "scope"       -> scope;
                case "barrel"      -> barrel;
                case "grip"        -> grip;
                case "magazine"    -> magazine;
                case "ammo"        -> ammo;
                case "muzzle"      -> muzzle;
                case "underbarrel" -> underbarrel;
                default            -> Collections.emptyList();
            };
        }
    }

    public static class KitRequirements {
        public int rank = 1;
        public int sp_cost = 0;
        public int bc_cost = 0;
        public String team = null;
    }

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static KitConfig INSTANCE;

    public static KitConfig get() { return INSTANCE; }

    /** Used by KitAdminSavePacket to replace the in-memory config after admin edit */
    public static void set(KitConfig cfg) { INSTANCE = cfg; }

    public static KitConfig loadOrCreate(Path worldDir) {
        Path file = worldDir.resolve("pwp/kits.json");
        if (Files.exists(file)) {
            try {
                KitConfig cfg = GSON.fromJson(Files.readString(file), KitConfig.class);
                if (cfg != null) { INSTANCE = cfg; return INSTANCE; }
            } catch (IOException | com.google.gson.JsonSyntaxException e) {
                e.printStackTrace();
            }
        }
        INSTANCE = buildDefault();
        INSTANCE.save(worldDir);
        return INSTANCE;
    }

    public void save(Path worldDir) {
        try {
            Path dir = worldDir.resolve("pwp");
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("kits.json"), GSON.toJson(this));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ═══════════════════════════════════════════════
    // WEAPON POOLS
    // ═══════════════════════════════════════════════

    private static final List<String> NATO_AR = List.of(
        "tacz:m4a1", "tacz:scar_l", "tacz:g36k", "tacz:hk416a5", "tacz:aug",
        "tacz:m16a4", "tacz:spr15hb",
        "maxstuff:mk18", "maxstuff:sr15", "maxstuff:sr16",
        "maxstuff:ar10", "maxstuff:colt_933",
        "daffas_arsenal:hk416", "daffas_arsenal:g36", "daffas_arsenal:sg550", "daffas_arsenal:sg552");

    private static final List<String> RU_AR = List.of(
        "tacz:ak47", "tacz:rpk", "tacz:type_81", "tacz:qbz_191",
        "maxstuff:ak12", "maxstuff:ak15", "maxstuff:ak19",
        "maxstuff:ak74", "maxstuff:ak74m", "maxstuff:aks74u",
        "maxstuff:ak_alpha", "maxstuff:ak_delta",
        "maxstuff:rpk16", "maxstuff:rpk_74",
        "daffas_arsenal:ak47", "daffas_arsenal:aksopmod");

    private static final List<String> SMG = List.of(
        "tacz:hk_mp5a5", "tacz:uzi", "tacz:p90", "tacz:vector45", "tacz:ump45", "tacz:b93r",
        "maxstuff:hk_mp5k", "maxstuff:hk_mp5sd", "maxstuff:mp7", "maxstuff:udp9",
        "maxstuff:ump9", "maxstuff:vector9", "maxstuff:vityas",
        "daffas_arsenal:mp5k");

    private static final List<String> NATO_DMR = List.of(
        "tacz:mk14", "tacz:scar_h", "tacz:hk_g3", "tacz:fn_fal", "tacz:fn_evolys",
        "tacz:sks_tactical",
        "maxstuff:mk11", "maxstuff:mk47", "maxstuff:scar_ssr", "maxstuff:scar_hamr",
        "maxstuff:beowulf_ecr", "maxstuff:beowulf_tcr",
        "maxstuff:mrad", "maxstuff:msr",
        "daffas_arsenal:hk417", "daffas_arsenal:sl8", "daffas_arsenal:sgputer", "daffas_arsenal:sgputer2");

    private static final List<String> RU_DMR = List.of(
        "tacz:sks_tactical", "tacz:scar_h", "tacz:fn_fal",
        "maxstuff:dragonuv_svd", "maxstuff:dragonuv_svdm", "maxstuff:dragonuv_svds",
        "maxstuff:mk47", "maxstuff:scar_ssr",
        "maxstuff:sks_golden", "maxstuff:sks_wooden",
        "daffas_arsenal:ss1_v2", "daffas_arsenal:ss1_v5", "daffas_arsenal:ss1_v5g",
        "daffas_arsenal:ssg69", "daffas_arsenal:ssg69s");

    private static final List<String> SNIPER = List.of(
        "tacz:ai_awp", "tacz:m107", "tacz:m95", "tacz:m700", "tacz:lonetrail", "tacz:kar98",
        "maxstuff:ai_awp", "maxstuff:ai_aws", "maxstuff:bfg50",
        "maxstuff:gm6_lynx", "maxstuff:m82a2", "maxstuff:thunderbird", "maxstuff:thunderbird_short",
        "maxstuff:excaliber",
        "daffas_arsenal:spasi15");

    private static final List<String> LMG = List.of(
        "tacz:m249", "tacz:minigun", "tacz:rpk", "tacz:fn_evolys",
        "maxstuff:dp28", "maxstuff:rpk16", "maxstuff:rpk_74",
        "maxstuff:genesis12", "maxstuff:genesis12_fl");

    private static final List<String> SHOTGUN = List.of(
        "tacz:aa12", "tacz:m1014", "tacz:spas_12", "tacz:m870", "tacz:db_long", "tacz:db_short",
        "maxstuff:m870t", "maxstuff:saiga12", "maxstuff:saiga9",
        "maxstuff:genesis12", "maxstuff:genesis12_dragons_breath",
        "daffas_arsenal:spasi15");

    private static final List<String> SECONDARY = List.of(
        "tacz:glock_17", "tacz:m1911", "tacz:cz75", "tacz:deagle",
        "tacz:p320", "tacz:hk_mk23", "tacz:rhino357",
        "tacz:m9a4", "tacz:taurus500", "tacz:timeless50",
        "maxstuff:glock_18c", "maxstuff:glock_54", "maxstuff:m17",
        "maxstuff:mk23", "maxstuff:af2011",
        "daffas_arsenal:hk45", "daffas_arsenal:p99_hak", "daffas_arsenal:p99_nohak");

    private static final List<String> GRENADES = List.of(
        "warbornexplosives:m67", "warbornexplosives:l109a2", "warbornexplosives:dm51a3",
        "warbornexplosives:f_1", "warbornexplosives:rgd_5", "warbornexplosives:rgn", "warbornexplosives:rgo",
        "warbornexplosives:rdg_2", "warbornexplosives:m18_smoke",
        "warbornexplosives:pmn_1", "warbornexplosives:lepestok");

    // ROLE-SPECIFIC SPECIAL SLOTS
    private static final List<String> SPECIAL_ANTI_VEHICLE = List.of(
        "superbwarfare:rpg", "superbwarfare:javelin", "superbwarfare:igla_9k38",
        "superbwarfare:repair_tool");
    private static final List<String> SPECIAL_ENGINEER = List.of(
        "superbwarfare:rpg", "superbwarfare:repair_tool", "superbwarfare:c4_bomb",
        "superbwarfare:claymore_mine", "superbwarfare:blu_43_mine");
    private static final List<String> SPECIAL_DEMO = List.of(
        "superbwarfare:c4_bomb", "superbwarfare:claymore_mine",
        "superbwarfare:blu_43_mine", "superbwarfare:m18_smoke_grenade");
    private static final List<String> SPECIAL_LIGHT = List.of(
        "superbwarfare:m18_smoke_grenade");

    // ═══════════════════════════════════════════════
    // ARMOR SETS
    // ═══════════════════════════════════════════════

    private static KitArmor natoArmor() {
        KitArmor a = new KitArmor();
        a.helmet = List.of("warborn:nato_helmet", "warborn:nato_sqad_leader_helmet",
            "warborn:nato_mg_helmet", "warborn:beta7_helmet", "warborn:beta7_nvg_helmet");
        a.chestplate = List.of("warborn:nato_sqad_leader_chestplate", "warborn:nato_mg_chestplate",
            "warborn:nato_ukr_chestplate", "warborn:beta7_chestplate");
        a.backpack = List.of("warborn:nato_sqad_leader_backpack", "warborn:nato_mg_backpack",
            "warborn:nato_ukr_backpack");
        a.shoulderpads = List.of("warborn:nato_shoulderpads", "warborn:beta7_shoulderpads");
        return a;
    }

    private static KitArmor ruArmor() {
        KitArmor a = new KitArmor();
        a.helmet = List.of("warborn:ru_helmet", "warborn:shturmovikv2_helmet",
            "warborn:squad_lider_ru_helmet", "warborn:mashinegunner_ru_helmet", "warborn:razvetchik_helmet");
        a.chestplate = List.of("warborn:shturmovik_ru_chestplate", "warborn:shturmovikv2_chestplate",
            "warborn:mashinegunner_ru_chestplate", "warborn:squad_lider_ru_chestplate", "warborn:razvetchik_chestplate");
        a.backpack = List.of("warborn:shturmovik_ru_backpack", "warborn:squad_lider_ru_backpack");
        a.shoulderpads = List.of("warborn:ru_shoulderpads");
        return a;
    }

    // ═══════════════════════════════════════════════
    // KIT BUILDER HELPER
    // ═══════════════════════════════════════════════

    private static void addKit(KitConfig cfg, String classId, String classDisplay, String classIcon,
                                String kitId, String kitDisplay, String description,
                                List<String> primary, List<String> secondary,
                                List<String> special, List<String> grenade,
                                KitArmor armor, int rank, int spCost, String team) {
        ClassConfig cl = cfg.classes.get(classId);
        if (cl == null) {
            cl = new ClassConfig();
            cl.display_name = classDisplay;
            cl.icon = classIcon;
            cfg.classes.put(classId, cl);
        }
        KitDef kit = new KitDef();
        kit.display_name = kitDisplay;
        kit.description = description;
        kit.weapons.primary = primary;
        kit.weapons.secondary = secondary;
        kit.weapons.special = special;
        kit.weapons.grenade = grenade;
        kit.armor = armor;
        kit.requirements.rank = rank;
        kit.requirements.sp_cost = spCost;
        kit.requirements.team = team;
        cl.kits.put(kitId, kit);
    }

    // ═══════════════════════════════════════════════
    // BUILD DEFAULT
    // ═══════════════════════════════════════════════

    private static KitConfig buildDefault() {
        KitConfig cfg = new KitConfig();
        KitArmor natoA = natoArmor();
        KitArmor ruA = ruArmor();

        // =========================================================
        // NATO
        // =========================================================

        // ── ASSAULT ──────────────────────────────────

        addKit(cfg, "nato_assault", "Assault", "warborn:nato_helmet",
            "rifleman", "Rifleman", "Standard NATO rifleman equipped for frontline combat.",
            NATO_AR, SECONDARY, SPECIAL_LIGHT, GRENADES, natoA, 1, 1, "NATO");

        addKit(cfg, "nato_assault", "Assault", "warborn:nato_sqad_leader_helmet",
            "breacher", "Breacher", "CQB assault operator with explosives and SMGs.",
            SMG, SECONDARY, SPECIAL_DEMO, GRENADES, natoA, 2, 2, "NATO");

        addKit(cfg, "nato_assault", "Assault", "warborn:beta7_helmet",
            "shocktrooper", "Shock Trooper", "Elite NATO assault unit with advanced armor.",
            NATO_AR, SECONDARY, SPECIAL_ENGINEER, GRENADES, natoA, 4, 4, "NATO");

        // ── MARKSMAN ────────────────────────────────

        addKit(cfg, "nato_marksman", "Marksman", "warborn:nato_sqad_leader_helmet",
            "designated_marksman", "Designated Marksman", "Mid-range precision fire support.",
            NATO_DMR, SECONDARY, SPECIAL_LIGHT, GRENADES, natoA, 2, 2, "NATO");

        addKit(cfg, "nato_marksman", "Marksman", "warborn:beta7_nvg_helmet",
            "recon_marksman", "Recon Marksman", "Advanced recon unit with high-grade optics and armor.",
            NATO_DMR, SECONDARY, SPECIAL_ENGINEER, GRENADES, natoA, 3, 3, "NATO");

        // ── SNIPER ─────────────────────────────────

        addKit(cfg, "nato_sniper", "Sniper", "warborn:beta7_nvg_helmet",
            "scout_sniper", "Scout Sniper", "Long-range reconnaissance and elimination.",
            SNIPER, SECONDARY, SPECIAL_LIGHT, GRENADES, natoA, 3, 4, "NATO");

        addKit(cfg, "nato_sniper", "Sniper", "warborn:beta7_helmet",
            "heavy_sniper", "Heavy Sniper", "Elite anti-material sniper specialist.",
            SNIPER, SECONDARY, SPECIAL_ANTI_VEHICLE, GRENADES, natoA, 5, 5, "NATO");

        // ── SUPPORT ────────────────────────────────

        addKit(cfg, "nato_support", "Support", "warborn:nato_mg_helmet",
            "machinegunner", "Machinegunner", "Provides suppressive fire with heavy machine guns.",
            LMG, SECONDARY, List.of(), GRENADES, natoA, 3, 3, "NATO");

        addKit(cfg, "nato_support", "Support", "warborn:nato_mg_helmet",
            "heavy_support", "Heavy Support", "Elite suppression specialist with fortified armor.",
            LMG, SECONDARY, SPECIAL_LIGHT, GRENADES, natoA, 4, 4, "NATO");

        // ── ENGINEER ───────────────────────────────

        addKit(cfg, "nato_engineer", "Engineer", "warborn:nato_ukr_helmet",
            "combat_engineer", "Combat Engineer", "Repair specialist equipped with demolition charges.",
            NATO_AR, SECONDARY, SPECIAL_ENGINEER, GRENADES, natoA, 2, 3, "NATO");

        addKit(cfg, "nato_engineer", "Engineer", "warborn:nato_ukr_helmet",
            "anti_vehicle", "Anti-Vehicle", "Heavy anti-armor NATO specialist.",
            NATO_AR, SECONDARY, SPECIAL_ANTI_VEHICLE, GRENADES, natoA, 3, 3, "NATO");

        // ── MEDIC ──────────────────────────────────

        addKit(cfg, "nato_medic", "Medic", "warborn:beta7_helmet",
            "combat_medic", "Combat Medic", "Medical support with smoke screening capability.",
            NATO_AR, SECONDARY, SPECIAL_LIGHT, GRENADES, natoA, 1, 0, "NATO");

        addKit(cfg, "nato_medic", "Medic", "warborn:beta7_nvg_helmet",
            "field_surgeon", "Field Surgeon", "Veteran NATO medic with reinforced protection.",
            SMG, SECONDARY, SPECIAL_LIGHT, GRENADES, natoA, 2, 0, "NATO");

        // =========================================================
        // RUSSIA
        // =========================================================

        // ── SHTURMOVIK ─────────────────────────────

        addKit(cfg, "ru_assault", "Shturmovik", "warborn:ru_helmet",
            "shturmovik", "Shturmovik", "Standard Russian frontline assault infantry.",
            RU_AR, SECONDARY, SPECIAL_LIGHT, GRENADES, ruA, 1, 1, "RUSSIA");

        addKit(cfg, "ru_assault", "Shturmovik", "warborn:shturmovikv2_helmet",
            "storm_group", "Storm Group", "Close-quarters Russian assault detachment.",
            SMG, SECONDARY, SPECIAL_DEMO, GRENADES, ruA, 2, 2, "RUSSIA");

        addKit(cfg, "ru_assault", "Shturmovik", "warborn:squad_lider_ru_helmet",
            "guardsman", "Guardsman", "Elite assault operator equipped for breakthrough attacks.",
            RU_AR, SECONDARY, SPECIAL_ENGINEER, GRENADES, ruA, 4, 4, "RUSSIA");

        // ── PODDERZHKA ─────────────────────────────

        addKit(cfg, "ru_support", "Podderzhka", "warborn:razvetchik_helmet",
            "support_marksman", "Support Marksman", "Russian precision infantry support unit.",
            RU_DMR, SECONDARY, SPECIAL_LIGHT, GRENADES, ruA, 2, 2, "RUSSIA");

        addKit(cfg, "ru_support", "Podderzhka", "warborn:mashinegunner_ru_helmet",
            "pulemetchik", "Pulemetchik", "Machine gunner specialized in area denial.",
            LMG, SECONDARY, List.of(), GRENADES, ruA, 3, 3, "RUSSIA");

        addKit(cfg, "ru_support", "Podderzhka", "warborn:mashinegunner_ru_helmet",
            "heavy_gunner", "Heavy Gunner", "Elite Russian heavy suppression specialist.",
            LMG, SECONDARY, SPECIAL_LIGHT, GRENADES, ruA, 4, 4, "RUSSIA");

        // ── SNAYPER ────────────────────────────────

        addKit(cfg, "ru_sniper", "Snayper", "warborn:razvetchik_helmet",
            "razvedchik_sniper", "Razvedchik Sniper", "Recon sniper trained for stealth operations.",
            SNIPER, SECONDARY, SPECIAL_LIGHT, GRENADES, ruA, 3, 4, "RUSSIA");

        addKit(cfg, "ru_sniper", "Snayper", "warborn:shturmovikv2_helmet",
            "anti_material_sniper", "Anti-Material Sniper", "Heavy-caliber sniper with anti-vehicle capability.",
            SNIPER, SECONDARY, SPECIAL_ANTI_VEHICLE, GRENADES, ruA, 5, 5, "RUSSIA");

        // ── TYAZHOLY ───────────────────────────────

        addKit(cfg, "ru_heavy", "Tyazholy", "warborn:mashinegunner_ru_helmet",
            "heavy_assault", "Heavy Assault", "Armored breakthrough infantry equipped for sustained combat.",
            SHOTGUN, SECONDARY, SPECIAL_ENGINEER, GRENADES, ruA, 3, 3, "RUSSIA");

        addKit(cfg, "ru_heavy", "Tyazholy", "warborn:squad_lider_ru_helmet",
            "juggernaut", "Juggernaut", "Elite heavy Russian assault operator.",
            LMG, SECONDARY, SPECIAL_ENGINEER, GRENADES, ruA, 5, 5, "RUSSIA");

        // ── SAPER ──────────────────────────────────

        addKit(cfg, "ru_engineer", "Saper", "warborn:razvetchik_helmet",
            "combat_saper", "Combat Saper", "Explosives and repair specialist.",
            RU_AR, SECONDARY, SPECIAL_ENGINEER, GRENADES, ruA, 2, 3, "RUSSIA");

        addKit(cfg, "ru_engineer", "Saper", "warborn:razvetchik_helmet",
            "anti_tank_saper", "Anti-Tank Saper", "Dedicated anti-vehicle specialist.",
            RU_AR, SECONDARY, SPECIAL_ANTI_VEHICLE, GRENADES, ruA, 3, 3, "RUSSIA");

        // ── MEDIK ──────────────────────────────────

        addKit(cfg, "ru_medic", "Medik", "warborn:ru_helmet",
            "polevoy_medik", "Polevoy Medik", "Russian combat medic with smoke support.",
            RU_AR, SECONDARY, SPECIAL_LIGHT, GRENADES, ruA, 1, 0, "RUSSIA");

        addKit(cfg, "ru_medic", "Medik", "warborn:shturmovikv2_helmet",
            "sanitar", "Sanitar", "Veteran field medic equipped for frontline rescue.",
            SMG, SECONDARY, SPECIAL_LIGHT, GRENADES, ruA, 2, 0, "RUSSIA");

        return cfg;
    }
}
