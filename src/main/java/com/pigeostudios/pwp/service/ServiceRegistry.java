package com.pigeostudios.pwp.service;

import com.pigeostudios.pwp.PWP;
import net.minecraft.server.MinecraftServer;
import java.util.HashMap;
import java.util.Map;

public class ServiceRegistry {
    private static ServiceRegistry INSTANCE;

    private ConfigService configService;
    private PersistenceService persistenceService;
    private EconomyService economyService;
    private VehicleService vehicleService;
    private FOBService fobService;
    private KitService kitService;
    private TicketService ticketService;
    private AntiAbuseService antiAbuseService;
    private EventService eventService;

    private boolean initialized;

    public ServiceRegistry() {
        INSTANCE = this;
    }

    public static ServiceRegistry getInstance() {
        return INSTANCE;
    }

    public void init(MinecraftServer server) {
        if (initialized) return;

        configService = new ConfigService();
        configService.load();

        persistenceService = new PersistenceService();
        persistenceService.init();

        economyService = new EconomyService(configService, persistenceService);

        vehicleService = new VehicleService(economyService);
        fobService = new FOBService(economyService, configService);
        kitService = new KitService(economyService);
        ticketService = new TicketService(configService);
        antiAbuseService = new AntiAbuseService();
        eventService = new EventService();
        applyEventConfig();

        initialized = true;
        PWP.LOGGER.info("ServiceRegistry initialized");
    }

    private void applyEventConfig() {
        var cfg = configService.getEvents();
        if (cfg == null) return;
        eventService.setEnabled(cfg.enabled);
        Map<MatchEventType, Integer> weights = new HashMap<>();
        Map<MatchEventType, Integer> durations = new HashMap<>();
        if (cfg.types != null) {
            for (var entry : cfg.types.entrySet()) {
                try {
                    MatchEventType type = MatchEventType.valueOf(entry.getKey().toUpperCase());
                    if (entry.getValue().weight >= 0) weights.put(type, entry.getValue().weight);
                    if (entry.getValue().duration >= 0) durations.put(type, entry.getValue().duration);
                } catch (IllegalArgumentException e) {
                    PWP.LOGGER.warn("ServiceRegistry: unknown event type in config: {}", entry.getKey());
                }
            }
        }
        eventService.applyConfig(cfg.minInterval, cfg.maxInterval, weights, durations);
    }

    public void shutdown() {
        if (!initialized) return;

        if (economyService != null) economyService.flushToTeamManager(PWP.getTeamManager());
        if (persistenceService != null) persistenceService.shutdown();

        initialized = false;
        INSTANCE = null;
        PWP.LOGGER.info("ServiceRegistry shut down");
    }

    public ConfigService getConfig() { return configService; }
    public PersistenceService getPersistence() { return persistenceService; }
    public EconomyService getEconomy() { return economyService; }
    public VehicleService getVehicle() { return vehicleService; }
    public FOBService getFOB() { return fobService; }
    public KitService getKit() { return kitService; }
    public TicketService getTickets() { return ticketService; }
    public AntiAbuseService getAntiAbuse() { return antiAbuseService; }
    public EventService getEvents() { return eventService; }

    public boolean isInitialized() { return initialized; }
}
