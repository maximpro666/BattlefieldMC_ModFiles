package com.pigeostudios.pwp.service;

import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.core.*;
import com.pigeostudios.pwp.network.PacketHandler;
import com.pigeostudios.pwp.network.TeamTicketSyncPacket;
import net.minecraftforge.network.PacketDistributor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class TicketService {
    private static final int BLEED_INTERVAL = 20 * 5;
    private static final int PROGRESSION_INTERVAL = 20 * 60;
    private static final int OVERTIME_BLEED_INTERVAL = 20 * 60;

    private final Map<Team, AtomicInteger> tickets = new ConcurrentHashMap<>();
    private final Map<Team, AtomicInteger> bleedRates = new ConcurrentHashMap<>();
    private final AtomicInteger bleedCounter = new AtomicInteger(0);
    private final AtomicInteger progressionCounter = new AtomicInteger(0);
    private final AtomicInteger overtimeBleedCounter = new AtomicInteger(0);
    private boolean gameEnded;
    private final ConfigService config;

    public TicketService(ConfigService config) {
        this.config = config;
        this.gameEnded = false;
        for (Team t : Team.values()) {
            tickets.put(t, new AtomicInteger(100));
            bleedRates.put(t, new AtomicInteger(0));
        }
    }

    public void resetTickets(int amount) {
        amount = Math.max(0, amount);
        gameEnded = false;
        for (Map.Entry<Team, AtomicInteger> e : tickets.entrySet()) {
            e.getValue().set(amount);
        }
        for (AtomicInteger br : bleedRates.values()) br.set(0);
        bleedCounter.set(0);
        progressionCounter.set(0);
        overtimeBleedCounter.set(0);
        syncToAll();
    }

    public boolean deductTicket(Team team, int amount) {
        if (amount <= 0) return false;
        AtomicInteger t = tickets.get(team);
        if (t == null) return false;
        while (true) {
            int cur = t.get();
            if (cur <= 0) return false;
            if (t.compareAndSet(cur, Math.max(0, cur - amount))) {
                syncToAll();
                checkGameEnd(team);
                return true;
            }
        }
    }

    public boolean deductTicket(Team team) {
        return deductTicket(team, config.getTickets().infantryDeathCost);
    }

    public boolean deductVehicleSpawnCost(Team team, int ticketCost) {
        if (ticketCost <= 0) return true;
        return deductTicket(team, ticketCost);
    }

    public boolean deductVehicleLossCost(Team team) {
        return deductTicket(team, config.getTickets().vehicleLossCost);
    }

    public void addTickets(Team team, int amount) {
        if (amount <= 0 || team == null) return;
        AtomicInteger t = tickets.get(team);
        if (t == null) return;
        t.addAndGet(amount);
        syncToAll();
    }

    public void addObjectiveCaptureReward(Team team) {
        addTickets(team, config.getTickets().objectiveCaptureReward);
    }

    public void addDefenseReward(Team team) {
        addTickets(team, config.getTickets().objectiveDefenseReward);
    }

    public void addSquadPlayReward(Team team) {
        addTickets(team, config.getTickets().squadplayReward);
    }

    public void addCommanderReward(Team team) {
        addTickets(team, config.getTickets().commanderReward);
    }

    public void setTickets(Team team, int amount) {
        AtomicInteger t = tickets.get(team);
        if (t == null) return;
        t.set(Math.max(0, amount));
        syncToAll();
    }

    public int getTickets(Team team) {
        AtomicInteger t = tickets.get(team);
        return t != null ? t.get() : 0;
    }

    public void setBleedRate(Team team, int rate) {
        AtomicInteger br = bleedRates.get(team);
        if (br != null) br.set(Math.max(0, rate));
    }

    public int getBleedRate(Team team) {
        AtomicInteger br = bleedRates.get(team);
        return br != null ? br.get() : 0;
    }

    public void tick(boolean overtime) {
        int counter = bleedCounter.incrementAndGet();
        if (counter >= BLEED_INTERVAL) {
            bleedCounter.set(0);
            boolean changed = false;
            for (Team team : new Team[]{Team.NATO, Team.RUSSIA}) {
                int rate = getBleedRate(team);
                if (rate <= 0) continue;
                for (int i = 0; i < rate; i++) {
                    AtomicInteger t = tickets.get(team);
                    if (t == null || t.get() <= 0) break;
                    t.decrementAndGet();
                    changed = true;
                }
                checkGameEnd(team);
            }
            if (changed) syncToAll();
        }

        int progCounter = progressionCounter.incrementAndGet();
        if (progCounter >= PROGRESSION_INTERVAL) {
            progressionCounter.set(0);
            int reward = config.getTickets().matchProgressionReward;
            if (reward > 0) {
                for (Team team : new Team[]{Team.NATO, Team.RUSSIA}) {
                    addTickets(team, reward);
                }
            }
        }

        if (overtime) {
            int otCounter = overtimeBleedCounter.incrementAndGet();
            if (otCounter >= OVERTIME_BLEED_INTERVAL) {
                overtimeBleedCounter.set(0);
                int bleed = config.getTickets().overtimeBleedPerMinute;
                if (bleed > 0) {
                    int natoOwned = 0, russiaOwned = 0;
                    var cp = PWP.getCapturePointManager();
                    if (cp != null) {
                        for (var zone : cp.getActiveZones()) {
                            if (zone.getOwnerTeam() == Team.NATO) natoOwned++;
                            else if (zone.getOwnerTeam() == Team.RUSSIA) russiaOwned++;
                        }
                    }
                    if (natoOwned < russiaOwned) deductTicket(Team.NATO, bleed);
                    else if (russiaOwned < natoOwned) deductTicket(Team.RUSSIA, bleed);
                    else { deductTicket(Team.NATO, bleed / 2); deductTicket(Team.RUSSIA, bleed / 2); }
                }
            }
        }

        for (Team team : new Team[]{Team.NATO, Team.RUSSIA}) {
            if (getTickets(team) <= 15 && getTickets(team) > 0) {
                GameManager gm = PWP.getGameManager();
                if (gm != null) {
                    LifecycleNotifier.broadcastNotification(gm.getServer(), "match_tickets_low", 3000);
                }
                break;
            }
        }
    }

    public void syncToAll() {
        GameManager gm = PWP.getGameManager();
        if (gm == null || gm.getServer() == null) return;
        int time = gm.getMatchTimeRemaining();
        MapConfig map = gm.getCurrentMap();
        int maxTickets = map != null ? map.getTickets() : 100;
        TeamTicketSyncPacket packet = new TeamTicketSyncPacket(
            getTickets(Team.NATO), getTickets(Team.RUSSIA), time, maxTickets);
        PacketHandler.CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
    }

    private void checkGameEnd(Team team) {
        if (getTickets(team) > 0) return;
        if (gameEnded) return;
        GameManager game = PWP.getGameManager();
        if (game != null && game.isPlaying()) {
            gameEnded = true;
            Team winner = (team == Team.NATO) ? Team.RUSSIA : Team.NATO;
            game.endGame(winner);
        }
    }
}
