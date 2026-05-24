package com.pigeostudios.pwp.service;

import com.google.gson.*;
import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.vehicle.VehicleDefinition;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class ConfigService {
    private static final String CONFIG_DIR = "config/battlefield";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // ════════════════════════════════════════
    // Economy Config
    // ════════════════════════════════════════

    public static class BCConfig {
        public int killReward = 15;
        public int assistReward = 8;
        public int captureReward = 150;
        public int killStreak5 = 15;
        public int killStreak10 = 40;
        public int killStreak15 = 80;
        public int mvp1 = 75;
        public int mvp2 = 40;
        public int mvp3 = 20;
        public int heal4Hp = 1;
        public int revive = 10;
        public int medicStreak = 10;
        public int vehicleDestroy = 25;
        public int vehicleCrewPassive = 1;
    }

    public static class WCConfig {
        public int winEasy = 20;
        public int lossEasy = 12;
        public int winNormal = 60;
        public int lossNormal = 15;
        public int winHard = 200;
        public int lossHard = 60;
        public int dailyLogin = 10;
        public int firstWinDay = 25;
        public int casualStreak3 = 15;
        public int weekly5Wins = 100;
        public double squadMultiplier = 1.25;
    }

    public static class CapsConfig {
        public int bc = 9999;
        public int wcPerMatch = 300;
        public int vcPerMatch = 5000;
        public int vcPerMin = 400;
    }

    public static class FrontlineIncomeConfig {
        public int easyTicks = 45;
        public int normalTicks = 60;
        public int hardTicks = 150;
        public int ratePerInterval = 1;
    }

    public static class FOBConfig {
        public int costEasy = 80;
        public int costNormal = 300;
        public int costHard = 150;
        public int maxPerTeam = 2;
        public int cooldownEasy = 60;
        public int cooldownNormal = 90;
        public int cooldownHard = 120;
        public int upkeepPerMin = 5;
    }

    public static class VCConfig {
        public int smallCpPerMin = 50;
        public int mediumCpPerMin = 100;
        public int majorCpPerMin = 120;
        public int defenseBonusPerMin = 30;
        public int vehicleDestroyReward = 10;
    }

    public static class DynamicPricingConfig {
        public double phase2Discount = 0.1;
        public double phase3Discount = 0.2;
        public double phase4Discount = 0.3;
        public double overtimeDiscount = 0.3;
    }

    public static class DifficultyMultipliersConfig {
        public double easyVehiclePrice = 0.7;
        public double normalVehiclePrice = 1.0;
        public double hardVehiclePrice = 1.3;
        public double easyUpkeep = 0.3;
        public double normalUpkeep = 1.0;
        public double hardUpkeep = 1.2;
        public double easyAmmoPrice = 0.7;
        public double normalAmmoPrice = 1.0;
        public double hardAmmoPrice = 1.3;
        public double easyXpMultiplier = 0.7;
        public double normalXpMultiplier = 1.0;
        public double hardXpMultiplier = 1.5;
    }

    public static class EconomyConfig {
        public BCConfig bc = new BCConfig();
        public WCConfig wc = new WCConfig();
        public CapsConfig caps = new CapsConfig();
        public FrontlineIncomeConfig frontlineIncome = new FrontlineIncomeConfig();
        public FOBConfig fob = new FOBConfig();
        public VCConfig vc = new VCConfig();
        public DynamicPricingConfig dynamicPricing = new DynamicPricingConfig();
        public DifficultyMultipliersConfig difficultyMultipliers = new DifficultyMultipliersConfig();
    }

    // ════════════════════════════════════════
    // Tickets Config
    // ════════════════════════════════════════

    public static class TicketsConfig {
        public int infantryDeathCost = 1;
        public int vehicleLossCost = 15;
        public int objectiveCaptureReward = 10;
        public int objectiveDefenseReward = 5;
        public int squadplayReward = 2;
        public int commanderReward = 5;
        public int matchProgressionReward = 3;
        public int overtimeBleedPerMinute = 2;
        public int objectiveDrainPerMinute = 0;
    }

    // ════════════════════════════════════════
    // Events Config
    // ════════════════════════════════════════

    public static class EventTypeConfig {
        public int weight = -1;
        public int duration = -1;
    }

    public static class EventsConfig {
        public boolean enabled = true;
        public int minInterval = 120;
        public int maxInterval = 300;
        public Map<String, EventTypeConfig> types = new HashMap<>();
    }

    // ════════════════════════════════════════
    // Progression Config
    // ════════════════════════════════════════

    public static class RankConfig {
        public String id = "";
        public String displayName = "";
        public int minXp = 0;
        public int leadershipLevel = 0;
        public int vehicleAccessLevel = 0;
    }

    public static class XPBySourceConfig {
        public int VEHICLE_DESTROY = 100;
        public int INFANTRY_KILL = 25;
        public int HEAL = 8;
        public int RESUPPLY = 10;
        public int OBJECTIVE_DEFENSE = 40;
        public int MATCH_VICTORY = 250;
        public int REVIVE = 35;
        public int COMMANDER_ORDER = 20;
        public int VEHICLE_KILL = 75;
        public int ASSIST = 12;
        public int OBJECTIVE_CAPTURE = 80;
        public int SQUAD_PLAY = 15;
    }

    public static class ProgressionConfig {
        public List<RankConfig> ranks = new ArrayList<>();
        public XPBySourceConfig xpBySource = new XPBySourceConfig();
    }

    // ════════════════════════════════════════
    // Instance state
    // ════════════════════════════════════════

    private final EconomyConfig economy;
    private TicketsConfig tickets;
    private final ProgressionConfig progression;
    private EventsConfig events;
    private boolean initialized;

    public ConfigService() {
        this.economy = new EconomyConfig();
        this.tickets = new TicketsConfig();
        this.progression = new ProgressionConfig();
        this.events = new EventsConfig();
    }

    public void load() {
        loadEconomyConfig();
        loadTicketsConfig();
        loadProgressionConfig();
        loadEventsConfig();
        initialized = true;
        PWP.LOGGER.info("ConfigService loaded all configs");
    }

    private void loadEconomyConfig() {
        Path path = Paths.get(CONFIG_DIR, "economy.json");
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                EconomyConfig loaded = GSON.fromJson(reader, EconomyConfig.class);
                if (loaded != null) {
                    mergeEconomy(loaded);
                    PWP.LOGGER.info("ConfigService: loaded economy.json");
                    return;
                }
            } catch (IOException e) {
                PWP.LOGGER.warn("ConfigService: failed to read economy.json: {}", e.getMessage());
            }
        }
        PWP.LOGGER.warn("ConfigService: economy.json not found, using defaults");
        saveDefaults(Paths.get(CONFIG_DIR, "economy.json"), economy);
    }

    private void loadTicketsConfig() {
        Path path = Paths.get(CONFIG_DIR, "tickets.json");
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                TicketsConfig loaded = GSON.fromJson(reader, TicketsConfig.class);
                if (loaded != null) {
                    mergeTickets(loaded);
                    PWP.LOGGER.info("ConfigService: loaded tickets.json");
                    return;
                }
            } catch (IOException e) {
                PWP.LOGGER.warn("ConfigService: failed to read tickets.json: {}", e.getMessage());
            }
        }
        PWP.LOGGER.warn("ConfigService: tickets.json not found, using defaults");
        saveDefaults(Paths.get(CONFIG_DIR, "tickets.json"), tickets);
    }

    private void loadProgressionConfig() {
        Path path = Paths.get(CONFIG_DIR, "progression.json");
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                ProgressionConfig loaded = GSON.fromJson(reader, ProgressionConfig.class);
                if (loaded != null) {
                    mergeProgression(loaded);
                    PWP.LOGGER.info("ConfigService: loaded progression.json");
                    return;
                }
            } catch (IOException e) {
                PWP.LOGGER.warn("ConfigService: failed to read progression.json: {}", e.getMessage());
            }
        }
        PWP.LOGGER.warn("ConfigService: progression.json not found, using defaults");
        saveDefaults(Paths.get(CONFIG_DIR, "progression.json"), progression);
    }

    private void mergeEconomy(EconomyConfig loaded) {
        if (loaded.bc != null) economy.bc = loaded.bc;
        if (loaded.wc != null) economy.wc = loaded.wc;
        if (loaded.caps != null) economy.caps = loaded.caps;
        if (loaded.frontlineIncome != null) economy.frontlineIncome = loaded.frontlineIncome;
        if (loaded.fob != null) economy.fob = loaded.fob;
        if (loaded.vc != null) economy.vc = loaded.vc;
        if (loaded.dynamicPricing != null) economy.dynamicPricing = loaded.dynamicPricing;
        if (loaded.difficultyMultipliers != null) economy.difficultyMultipliers = loaded.difficultyMultipliers;
    }

    private void mergeTickets(TicketsConfig loaded) {
        tickets = loaded;
    }

    private void loadEventsConfig() {
        Path path = Paths.get(CONFIG_DIR, "events.json");
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                EventsConfig loaded = GSON.fromJson(reader, EventsConfig.class);
                if (loaded != null) {
                    events = loaded;
                    PWP.LOGGER.info("ConfigService: loaded events.json");
                    return;
                }
            } catch (IOException e) {
                PWP.LOGGER.warn("ConfigService: failed to read events.json: {}", e.getMessage());
            }
        }
        PWP.LOGGER.warn("ConfigService: events.json not found, using defaults");
        saveDefaults(Paths.get(CONFIG_DIR, "events.json"), events);
    }

    private void mergeProgression(ProgressionConfig loaded) {
        if (loaded.ranks != null && !loaded.ranks.isEmpty()) progression.ranks = loaded.ranks;
        if (loaded.xpBySource != null) progression.xpBySource = loaded.xpBySource;
    }

    private void saveDefaults(Path path, Object defaults) {
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(defaults, writer);
            }
        } catch (IOException e) {
            PWP.LOGGER.warn("ConfigService: failed to save defaults to {}: {}", path, e.getMessage());
        }
    }

    // ════════════════════════════════════════
    // Getters
    // ════════════════════════════════════════

    public EconomyConfig getEconomy() { return economy; }
    public TicketsConfig getTickets() { return tickets; }
    public ProgressionConfig getProgression() { return progression; }
    public EventsConfig getEvents() { return events; }
    public boolean isInitialized() { return initialized; }

    // Convenience — BC rewards
    public int getKillRewardBC() { return economy.bc.killReward; }
    public int getAssistRewardBC() { return economy.bc.assistReward; }
    public int getCaptureRewardBC() { return economy.bc.captureReward; }
    public int getVehicleDestroyBC() { return economy.bc.vehicleDestroy; }
    public int getHealRewardBC() { return economy.bc.heal4Hp; }
    public int getReviveRewardBC() { return economy.bc.revive; }

    // Convenience — caps
    public int getBCCap() { return economy.caps.bc; }
    public int getWCPerMatchCap() { return economy.caps.wcPerMatch; }
    public int getVCPerMatchCap() { return economy.caps.vcPerMatch; }

    // Convenience — tickets
    public int getInfantryDeathCost() { return tickets.infantryDeathCost; }
    public int getVehicleLossCost() { return tickets.vehicleLossCost; }
    public int getObjectiveCaptureReward() { return tickets.objectiveCaptureReward; }
    public int getObjectiveDefenseReward() { return tickets.objectiveDefenseReward; }

    // Convenience — progression
    public List<RankConfig> getRanks() { return progression.ranks; }
    public int getXpForSource(String source) {
        try {
            var field = XPBySourceConfig.class.getField(source);
            return field.getInt(progression.xpBySource);
        } catch (Exception e) {
            return 0;
        }
    }

    // Get rank by XP threshold
    public int getRankForXp(int totalXp) {
        int rank = 0;
        for (int i = 0; i < progression.ranks.size(); i++) {
            if (totalXp >= progression.ranks.get(i).minXp) {
                rank = i;
            } else {
                break;
            }
        }
        return rank;
    }

    // Get required vehicle tier for a rank
    public int getVehicleAccessLevel(int rankOrdinal) {
        if (rankOrdinal < 0 || rankOrdinal >= progression.ranks.size()) return 0;
        return progression.ranks.get(rankOrdinal).vehicleAccessLevel;
    }
}
