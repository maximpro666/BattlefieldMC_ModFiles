package com.yourmod.teamsystem.state;

import com.yourmod.teamsystem.vehicle.VehicleDefinition;

public class MatchState {
    private EscalationPhase currentEscalation = EscalationPhase.SKIRMISH;
    private double escalationProgress = 0;

    public EscalationPhase getCurrentEscalation() {
        return currentEscalation;
    }

    public void setCurrentEscalation(EscalationPhase phase) {
        this.currentEscalation = phase;
    }

    public double getEscalationProgress() {
        return escalationProgress;
    }

    public void setEscalationProgress(double progress) {
        this.escalationProgress = Math.max(0, Math.min(1, progress));
    }

    public boolean isVehicleAllowed(VehicleDefinition def) {
        return def.getPhases().contains(currentEscalation.name());
    }

    public boolean isPhaseAtLeast(EscalationPhase phase) {
        return currentEscalation.ordinal() >= phase.ordinal();
    }

    public void reset() {
        currentEscalation = EscalationPhase.SKIRMISH;
        escalationProgress = 0;
    }
}
