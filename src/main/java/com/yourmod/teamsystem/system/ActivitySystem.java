package com.yourmod.teamsystem.system;

import com.yourmod.teamsystem.state.FrontlineState;

public class ActivitySystem {
    private static final int DECAY_INTERVAL = 100;
    private static final int DECAY_AMOUNT = 5;

    private int tickCounter = 0;

    public void tick(FrontlineState state) {
        tickCounter++;
        if (tickCounter < DECAY_INTERVAL) return;
        tickCounter = 0;
        state.decay(DECAY_AMOUNT);
    }

    public void reset() {
        tickCounter = 0;
    }
}
