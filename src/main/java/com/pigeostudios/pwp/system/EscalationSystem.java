package com.pigeostudios.pwp.system;

import com.pigeostudios.pwp.core.GameManager;
import com.pigeostudios.pwp.core.Team;
import com.pigeostudios.pwp.service.TicketService;
import com.pigeostudios.pwp.state.EscalationPhase;
import com.pigeostudios.pwp.state.MatchState;
import com.pigeostudios.pwp.state.PressureState;

public class EscalationSystem {
    public void tick(MatchState matchState, PressureState pressureState,
                     TicketService ticketSvc, GameManager gm) {
        if (gm == null || ticketSvc == null) return;
        int elapsed = gm.getMatchTimeRemaining() > 0 ? 1800 - gm.getMatchTimeRemaining() : 0;

        int natoTickets = ticketSvc.getTickets(Team.NATO);
        int russiaTickets = ticketSvc.getTickets(Team.RUSSIA);
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
