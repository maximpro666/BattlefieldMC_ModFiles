package com.yourmod.teamsystem.system;

import com.yourmod.teamsystem.core.GameManager;
import com.yourmod.teamsystem.core.Team;
import com.yourmod.teamsystem.core.TicketManager;
import com.yourmod.teamsystem.state.EscalationPhase;
import com.yourmod.teamsystem.state.MatchState;
import com.yourmod.teamsystem.state.PressureState;

public class EscalationSystem {
    public void tick(MatchState matchState, PressureState pressureState,
                     TicketManager ticketMgr, GameManager gm) {
        if (gm == null || ticketMgr == null) return;
        int elapsed = gm.getMatchTimeRemaining() > 0 ? 1800 - gm.getMatchTimeRemaining() : 0;

        int natoTickets = ticketMgr.getTickets(Team.NATO);
        int russiaTickets = ticketMgr.getTickets(Team.RUSSIA);
        int maxTickets = Math.max(natoTickets + russiaTickets, 1);

        double ticketLossFactor = 1.0 - (double)(natoTickets + russiaTickets) / (maxTickets * 2);
        double pressureFactor = Math.min(1.0,
            (pressureState.getGround(Team.NATO) + pressureState.getGround(Team.RUSSIA)) / 50.0);
        double timeFactor = Math.min(1.0, elapsed / 1800.0);

        double progress = ticketLossFactor * 0.4 + pressureFactor * 0.35 + timeFactor * 0.25;
        matchState.setEscalationProgress(progress);
        matchState.setCurrentEscalation(resolvePhase(progress));
    }

    private EscalationPhase resolvePhase(double progress) {
        if (progress > 0.7) return EscalationPhase.TOTAL_WAR;
        if (progress > 0.4) return EscalationPhase.BATTLE;
        if (progress > 0.15) return EscalationPhase.CONFLICT;
        return EscalationPhase.SKIRMISH;
    }
}
