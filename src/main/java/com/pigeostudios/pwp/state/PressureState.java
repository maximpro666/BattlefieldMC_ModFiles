package com.pigeostudios.pwp.state;

import com.pigeostudios.pwp.core.Team;

public class PressureState {
    private final double[][] values = new double[2][3];

    public double get(Team team, PressureType type) {
        return values[team.ordinal()][type.ordinal()];
    }

    public void add(Team team, PressureType type, double amount) {
        values[team.ordinal()][type.ordinal()] += amount;
    }

    public void add(Team team, double ground, double air, double siege) {
        int idx = team.ordinal();
        values[idx][0] += ground;
        values[idx][1] += air;
        values[idx][2] += siege;
    }

    public void remove(Team team, PressureType type, double amount) {
        double[] teamArr = values[team.ordinal()];
        teamArr[type.ordinal()] = Math.max(0, teamArr[type.ordinal()] - amount);
    }

    public void remove(Team team, double ground, double air, double siege) {
        int idx = team.ordinal();
        values[idx][0] = Math.max(0, values[idx][0] - ground);
        values[idx][1] = Math.max(0, values[idx][1] - air);
        values[idx][2] = Math.max(0, values[idx][2] - siege);
    }

    public boolean isHigh(Team team, PressureType type) {
        return get(team, type) >= (type == PressureType.AIR ? 10 : 20);
    }

    public void decay(double factor) {
        for (int t = 0; t < 2; t++) {
            for (int p = 0; p < 3; p++) {
                double v = values[t][p];
                if (v < 0.1) {
                    values[t][p] = 0.0;
                } else {
                    values[t][p] = v * factor;
                }
            }
        }
    }

    public double getTotal(Team team) {
        double[] arr = values[team.ordinal()];
        return arr[0] + arr[1] + arr[2];
    }

    public boolean isEmpty(Team team) {
        double[] arr = values[team.ordinal()];
        return arr[0] <= 0 && arr[1] <= 0 && arr[2] <= 0;
    }

    public void reset() {
        for (int t = 0; t < 2; t++) {
            for (int p = 0; p < 3; p++) {
                values[t][p] = 0.0;
            }
        }
    }

    public double getGround(Team team) { return values[team.ordinal()][0]; }
    public double getAir(Team team) { return values[team.ordinal()][1]; }
    public double getSiege(Team team) { return values[team.ordinal()][2]; }
}
