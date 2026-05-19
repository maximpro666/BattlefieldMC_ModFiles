package com.yourmod.teamsystem.data;

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
        public String team = null;
    }

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static KitConfig INSTANCE;

    public static KitConfig get() { return INSTANCE; }

    /** Used by KitAdminSavePacket to replace the in-memory config after admin edit */
    public static void set(KitConfig cfg) { INSTANCE = cfg; }

    public static KitConfig loadOrCreate(Path worldDir) {
        Path file = worldDir.resolve("teamsystem/kits.json");
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
            Path dir = worldDir.resolve("teamsystem");
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("kits.json"), GSON.toJson(this));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static KitConfig buildDefault() {
        KitConfig cfg = new KitConfig();

        // ===== Assault (NATO) =====
        ClassConfig nato_assault = new ClassConfig();
        nato_assault.display_name = "Assault";
        nato_assault.icon = "warborn:nato_helmet";

        KitDef rifleman = new KitDef();
        rifleman.display_name = "Rifleman";
        rifleman.description  = "RPG + frags + smoke.";
        rifleman.weapons.special   = List.of("superbwarfare:rpg", "superbwarfare:javelin", "superbwarfare:igla_9k38",
            "superbwarfare:repair_tool", "superbwarfare:c4_bomb", "superbwarfare:claymore_mine", "superbwarfare:explosive_mine",
            "superbwarfare:m18_smoke_grenade");
        rifleman.weapons.grenade   = List.of("warbornexplosives:m67", "warbornexplosives:l109a2", "warbornexplosives:dm51a3",
            "warbornexplosives:f_1", "warbornexplosives:rgd_5", "warbornexplosives:rgn", "warbornexplosives:rgo",
            "warbornexplosives:rdg_2", "warbornexplosives:m18_smoke");
        rifleman.armor.helmet      = List.of("warborn:nato_helmet", "warborn:nato_sqad_leader_helmet",
            "warborn:nato_mg_helmet", "warborn:beta7_helmet", "warborn:beta7_nvg_helmet");
        rifleman.armor.chestplate  = List.of("warborn:nato_sqad_leader_chestplate", "warborn:nato_mg_chestplate",
            "warborn:nato_ukr_chestplate", "warborn:beta7_chestplate");
        rifleman.armor.backpack    = List.of("warborn:nato_sqad_leader_backpack", "warborn:nato_mg_backpack",
            "warborn:nato_ukr_backpack");
        rifleman.armor.shoulderpads = List.of("warborn:nato_shoulderpads", "warborn:beta7_shoulderpads");
        rifleman.requirements.rank = 1;
        rifleman.requirements.team = "NATO";
        nato_assault.kits.put("rifleman", rifleman);

        KitDef grenadier = new KitDef();
        grenadier.display_name = "Grenadier";
        grenadier.description  = "Explosives & anti-personnel.";
        grenadier.weapons.special   = List.of("superbwarfare:c4_bomb", "superbwarfare:claymore_mine", "superbwarfare:explosive_mine",
            "superbwarfare:rpg", "superbwarfare:repair_tool", "superbwarfare:m18_smoke_grenade");
        grenadier.weapons.grenade   = List.of("warbornexplosives:m67", "warbornexplosives:l109a2", "warbornexplosives:dm51a3",
            "warbornexplosives:f_1", "warbornexplosives:rgd_5", "warbornexplosives:rgn", "warbornexplosives:rgo",
            "warbornexplosives:rdg_2", "warbornexplosives:m18_smoke",
            "warbornexplosives:pmn_1", "warbornexplosives:lepestok");
        grenadier.armor.helmet      = List.of("warborn:nato_helmet", "warborn:nato_helmet_woodland");
        grenadier.armor.chestplate  = List.of("warborn:nato_sqad_leader_chestplate", "warborn:nato_mg_chestplate");
        grenadier.armor.backpack    = List.of("warborn:nato_sqad_leader_backpack", "warborn:nato_mg_backpack");
        grenadier.armor.shoulderpads = List.of("warborn:nato_shoulderpads", "warborn:nato_shoulderpads_woodland");
        grenadier.requirements.rank = 1;
        grenadier.requirements.team = "NATO";
        nato_assault.kits.put("grenadier", grenadier);

        cfg.classes.put("nato_assault", nato_assault);

        // ===== Shturmovik (RU) =====
        ClassConfig ru_assault = new ClassConfig();
        ru_assault.display_name = "Shturmovik";
        ru_assault.icon = "warborn:ru_helmet";

        KitDef ru_rifleman = new KitDef();
        ru_rifleman.display_name = "Shturmovik";
        ru_rifleman.description  = "RPG + frags + smoke.";
        ru_rifleman.weapons.special   = List.of("superbwarfare:rpg", "superbwarfare:javelin", "superbwarfare:igla_9k38",
            "superbwarfare:repair_tool", "superbwarfare:c4_bomb", "superbwarfare:claymore_mine", "superbwarfare:explosive_mine",
            "superbwarfare:m18_smoke_grenade");
        ru_rifleman.weapons.grenade   = List.of("warbornexplosives:m67", "warbornexplosives:l109a2", "warbornexplosives:dm51a3",
            "warbornexplosives:f_1", "warbornexplosives:rgd_5", "warbornexplosives:rgn", "warbornexplosives:rgo",
            "warbornexplosives:rdg_2", "warbornexplosives:m18_smoke");
        ru_rifleman.armor.helmet      = List.of("warborn:ru_helmet", "warborn:shturmovikv2_helmet",
            "warborn:squad_lider_ru_helmet", "warborn:mashinegunner_ru_helmet", "warborn:razvetchik_helmet");
        ru_rifleman.armor.chestplate  = List.of("warborn:shturmovik_ru_chestplate", "warborn:shturmovikv2_chestplate",
            "warborn:mashinegunner_ru_chestplate", "warborn:squad_lider_ru_chestplate", "warborn:razvetchik_chestplate");
        ru_rifleman.armor.backpack    = List.of("warborn:shturmovik_ru_backpack", "warborn:squad_lider_ru_backpack");
        ru_rifleman.armor.shoulderpads = List.of("warborn:ru_shoulderpads");
        ru_rifleman.requirements.rank = 1;
        ru_rifleman.requirements.team = "RUSSIA";
        ru_assault.kits.put("shturmovik", ru_rifleman);

        cfg.classes.put("ru_assault", ru_assault);

        return cfg;
    }
}
