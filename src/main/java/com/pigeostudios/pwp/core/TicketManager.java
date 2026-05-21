package com.pigeostudios.pwp.core;

import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.network.PacketHandler;
import com.pigeostudios.pwp.network.TeamTicketSyncPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;


public class TicketManager {
    private static final int DEFAULT_TICKETS = 100;
    private static final int BLEED_INTERVAL = 20 * 5;

    private int natoTickets;
    private int russiaTickets;
    private int natoBleedRate;
    private int russiaBleedRate;
    private int bleedCounter;

    public TicketManager() {
        this.natoTickets = DEFAULT_TICKETS;
        this.russiaTickets = DEFAULT_TICKETS;
        this.natoBleedRate = 0;
        this.russiaBleedRate = 0;
        this.bleedCounter = 0;
    }

    public int getTickets(Team team) {
        if (team == Team.NATO) return natoTickets;
        if (team == Team.RUSSIA) return russiaTickets;
        return 0;
    }

    public void setTickets(Team team, int amount) {
        amount = Math.max(0, amount);
        if (team == Team.NATO) natoTickets = amount;
        else if (team == Team.RUSSIA) russiaTickets = amount;
        syncToAll();
    }

    public void resetTickets() {
        natoTickets = DEFAULT_TICKETS;
        russiaTickets = DEFAULT_TICKETS;
        natoBleedRate = 0;
        russiaBleedRate = 0;
        bleedCounter = 0;
        syncToAll();
    }

    public void resetTickets(int tickets) {
        natoTickets = Math.max(0, tickets);
        russiaTickets = Math.max(0, tickets);
        natoBleedRate = 0;
        russiaBleedRate = 0;
        bleedCounter = 0;
        syncToAll();
    }

    public boolean deductTicket(Team team) {
        if (team == Team.NATO && natoTickets > 0) {
            natoTickets--;
            syncToAll();
            checkGameEnd(team);
            return true;
        } else if (team == Team.RUSSIA && russiaTickets > 0) {
            russiaTickets--;
            syncToAll();
            checkGameEnd(team);
            return true;
        }
        return false;
    }

    public void setBleedRate(Team team, int rate) {
        if (team == Team.NATO) natoBleedRate = Math.max(0, rate);
        else if (team == Team.RUSSIA) russiaBleedRate = Math.max(0, rate);
    }

    public int getBleedRate(Team team) {
        if (team == Team.NATO) return natoBleedRate;
        if (team == Team.RUSSIA) return russiaBleedRate;
        return 0;
    }

    public void tick() {
        bleedCounter++;
        if (bleedCounter < BLEED_INTERVAL) return;
        bleedCounter = 0;

        if (natoBleedRate > 0) {
            for (int i = 0; i < natoBleedRate; i++) {
                if (natoTickets <= 0) break;
                natoTickets--;
            }
            syncToAll();
            checkGameEnd(Team.NATO);
        }

        if (russiaBleedRate > 0) {
            for (int i = 0; i < russiaBleedRate; i++) {
                if (russiaTickets <= 0) break;
                russiaTickets--;
            }
            syncToAll();
            checkGameEnd(Team.RUSSIA);
        }
    }

    private void checkGameEnd(Team team) {
        if ((team == Team.NATO && natoTickets <= 0) || (team == Team.RUSSIA && russiaTickets <= 0)) {
            GameManager game = PWP.getGameManager();
            if (game != null && game.isPlaying()) {
                Team winner = (team == Team.NATO) ? Team.RUSSIA : Team.NATO;
                game.endGame(winner);
            }
        }
    }

    public void syncToAll() {
        GameManager gm = PWP.getGameManager();
        if (gm == null || gm.getServer() == null) return;
        int time = gm.getMatchTimeRemaining();
        MapConfig map = gm.getCurrentMap();
        int maxTickets = map != null ? map.getTickets() : 100;
        TeamTicketSyncPacket packet = new TeamTicketSyncPacket(natoTickets, russiaTickets, time, maxTickets);
        for (ServerPlayer player : gm.getServer().getPlayerList().getPlayers()) {
            PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
        }
    }
}
