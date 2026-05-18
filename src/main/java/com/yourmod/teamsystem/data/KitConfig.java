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
        public Map<String, AttachmentLimit> attachment_limits = new HashMap<>();
        public KitRequirements requirements = new KitRequirements();
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

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
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

        ClassConfig assault = new ClassConfig();
        assault.display_name = "Assault";
        assault.icon = "tacz:assault_icon";

        KitDef rifleman = new KitDef();
        rifleman.display_name = "Rifleman";
        rifleman.description  = "Standard assault rifleman with AR and grenades.";
        rifleman.weapons.primary   = List.of("tacz:m4a1", "tacz:m16a4");
        rifleman.weapons.secondary = List.of("tacz:g17");
        rifleman.weapons.special   = List.of("tacz:m67");
        rifleman.weapons.grenade   = List.of("tacz:m67");
        rifleman.requirements.rank = 1;

        AttachmentLimit m4Limits = new AttachmentLimit();
        m4Limits.scope    = List.of("ironsights", "red_dot", "holographic");
        m4Limits.barrel   = List.of("standard", "compensator");
        m4Limits.grip     = List.of("none", "vertical", "angled");
        m4Limits.magazine = List.of("30rnd", "45rnd");
        rifleman.attachment_limits.put("tacz:m4a1", m4Limits);

        assault.kits.put("rifleman", rifleman);
        cfg.classes.put("assault", assault);

        return cfg;
    }
}
