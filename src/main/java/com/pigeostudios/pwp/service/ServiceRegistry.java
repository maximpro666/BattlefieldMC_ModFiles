package com.pigeostudios.pwp.service;

import com.pigeostudios.pwp.PWP;
import net.minecraft.server.MinecraftServer;

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

        initialized = true;
        PWP.LOGGER.info("ServiceRegistry initialized");
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

    public boolean isInitialized() { return initialized; }
}
