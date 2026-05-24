package com.pigeostudios.pwp.service;

import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.core.LifecycleNotifier;
import com.pigeostudios.pwp.core.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

public class EventService {
    private static final int TICK_INTERVAL = 20; // check every second
    private int tickCounter;
    private int secondsUntilNextEvent;
    private final List<ActiveEvent> activeEvents = new ArrayList<>();
    private boolean enabled;
    private int minInterval;
    private int maxInterval;
    private final Map<MatchEventType, Integer> weightOverrides = new HashMap<>();
    private final Map<MatchEventType, Integer> durationOverrides = new HashMap<>();

    public EventService() {
        this.enabled = true;
        this.minInterval = 120;
        this.maxInterval = 300;
        this.secondsUntilNextEvent = 180 + new Random().nextInt(120);
    }

    public void applyConfig(int minInterval, int maxInterval, Map<MatchEventType, Integer> weights, Map<MatchEventType, Integer> durations) {
        this.minInterval = minInterval;
        this.maxInterval = maxInterval;
        this.weightOverrides.clear();
        if (weights != null) this.weightOverrides.putAll(weights);
        this.durationOverrides.clear();
        if (durations != null) this.durationOverrides.putAll(durations);
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean v) { this.enabled = v; }

    public void tick(MinecraftServer server, ServerLevel mapLevel, int matchTimeRemaining) {
        if (!enabled || server == null) return;
        tickCounter++;
        if (tickCounter < TICK_INTERVAL) return;
        tickCounter = 0;

        // tick active events
        tickActiveEvents(server, mapLevel);

        // countdown to next event
        if (secondsUntilNextEvent > 0) {
            secondsUntilNextEvent--;
            return;
        }

        // pick & fire
        MatchEventType type = pickEvent(matchTimeRemaining);
        if (type == null) return;

        int duration = getDuration(type);
        fireEvent(server, mapLevel, type, duration);
    }

    private void tickActiveEvents(MinecraftServer server, ServerLevel mapLevel) {
        Iterator<ActiveEvent> it = activeEvents.iterator();
        while (it.hasNext()) {
            ActiveEvent ae = it.next();
            ae.remainingTicks--;
            if (ae.remainingTicks <= 0) {
                endEvent(server, mapLevel, ae);
                it.remove();
            } else {
                tickEffect(server, mapLevel, ae);
            }
        }
    }

    private MatchEventType pickEvent(int matchTimeRemaining) {
        MatchEventType[] types = MatchEventType.values();
        int[] weights = new int[types.length];
        for (int i = 0; i < types.length; i++) {
            Integer override = weightOverrides.get(types[i]);
            if (override != null) {
                weights[i] = Math.max(0, override);
            } else {
                weights[i] = getBaseWeight(types[i], matchTimeRemaining);
            }
        }
        // if no active events, try to pick one that isn't already running
        Set<MatchEventType> activeTypes = new HashSet<>();
        for (ActiveEvent ae : activeEvents) activeTypes.add(ae.type);
        // zero-out weights for already-active types
        for (int i = 0; i < types.length; i++) {
            if (activeTypes.contains(types[i])) weights[i] = 0;
        }
        return MatchEventType.randomWeighted(weights);
    }

    private int getBaseWeight(MatchEventType type, int matchTimeRemaining) {
        // Some events are more likely later in the game
        if (matchTimeRemaining < 300) {
            // Late game: LAST_STAND, BOUNTY_HUNTER more likely
            if (type == MatchEventType.LAST_STAND) return type.getDefaultWeight() * 3;
            if (type == MatchEventType.BOUNTY_HUNTER) return type.getDefaultWeight() * 2;
            if (type == MatchEventType.VIP_PROTECT) return type.getDefaultWeight() * 2;
            if (type == MatchEventType.TRUCE) return type.getDefaultWeight() * 3;
        } else if (matchTimeRemaining < 900) {
            // Mid game: more combat events
            if (type.getCategory().equals("combat")) return (int)(type.getDefaultWeight() * 1.5);
            if (type.getCategory().equals("support")) return (int)(type.getDefaultWeight() * 1.3);
        }
        return type.getDefaultWeight();
    }

    private int getDuration(MatchEventType type) {
        Integer override = durationOverrides.get(type);
        return override != null ? override : type.getDefaultDuration();
    }

    private void fireEvent(MinecraftServer server, ServerLevel mapLevel, MatchEventType type, int duration) {
        activeEvents.add(new ActiveEvent(type, duration * 20));
        broadcastEventStart(server, type, duration);

        var w2 = com.pigeostudios.pwp.integration.Weather2Integration.getInstance();
        switch (type) {
            case REINFORCEMENTS -> {
                var ts = PWP.getServiceRegistry().getTickets();
                if (ts != null) { ts.addTickets(Team.NATO, 15); ts.addTickets(Team.RUSSIA, 15); }
            }
            case BOUNTY_HUNTER -> {
                ServerPlayer top = findTopPlayer(server);
                if (top != null) {
                    LifecycleNotifier.broadcastNotification(server, type.getStartKey() + "." + top.getScoreboardName(), 4000);
                }
            }
            case SANDSTORM -> {
                if (w2.isModPresent() && mapLevel != null) {
                    w2.startSandstorm(mapLevel, duration * 20);
                }
            }
            case RAINSTORM -> {
                if (w2.isModPresent() && mapLevel != null) {
                    w2.startStorm(mapLevel, duration * 20);
                }
            }
            default -> {}
        }

        scheduleNextEvent();
    }

    private void endEvent(MinecraftServer server, ServerLevel mapLevel, ActiveEvent ae) {
        var w2 = com.pigeostudios.pwp.integration.Weather2Integration.getInstance();
        switch (ae.type) {
            case SANDSTORM -> { if (w2.isModPresent() && mapLevel != null) w2.stopSandstorm(mapLevel); }
            case RAINSTORM  -> { if (w2.isModPresent() && mapLevel != null) w2.stopStorm(mapLevel); }
            default -> {}
        }
        broadcastEventEnd(server, ae.type);
    }

    private void tickEffect(MinecraftServer server, ServerLevel mapLevel, ActiveEvent ae) {
        // Weather effects maintained by Weather2Integration automatically
    }

    private void broadcastEventStart(MinecraftServer server, MatchEventType type, int duration) {
        LifecycleNotifier.broadcastNotification(server, type.getStartKey(), duration * 1000);
    }

    private void broadcastEventEnd(MinecraftServer server, MatchEventType type) {
        LifecycleNotifier.broadcastToAll(server, type.getEndKey());
    }

    private ServerPlayer findTopPlayer(MinecraftServer server) {
        ServerPlayer best = null;
        int bestScore = -1;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            int score = p.getScore();
            if (score > bestScore) {
                bestScore = score;
                best = p;
            }
        }
        return best;
    }

    private void scheduleNextEvent() {
        Random rng = new Random();
        secondsUntilNextEvent = minInterval + rng.nextInt(maxInterval - minInterval + 1);
    }

    public List<ActiveEvent> getActiveEvents() { return Collections.unmodifiableList(activeEvents); }
    public boolean isEventActive(MatchEventType type) {
        return activeEvents.stream().anyMatch(ae -> ae.type == type);
    }

    public void reset() {
        activeEvents.clear();
        tickCounter = 0;
        scheduleNextEvent();
    }

    public static class ActiveEvent {
        public final MatchEventType type;
        public int remainingTicks;

        ActiveEvent(MatchEventType type, int ticks) {
            this.type = type;
            this.remainingTicks = ticks;
        }
    }
}
