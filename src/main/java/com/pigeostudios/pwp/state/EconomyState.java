package com.pigeostudios.pwp.state;

import com.pigeostudios.pwp.core.Team;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EconomyState {
    private final Map<UUID, Integer> matchCredits = new HashMap<>();
    private final Map<UUID, Integer> warCredits = new HashMap<>();
    private final int[] vehicleCredits = new int[2];

    public int getBC(UUID uuid) {
        return matchCredits.getOrDefault(uuid, 0);
    }

    public void setBC(UUID uuid, int amount) {
        matchCredits.put(uuid, Math.max(0, amount));
    }

    public void addBC(UUID uuid, int amount) {
        if (amount <= 0) return;
        matchCredits.merge(uuid, amount, Integer::sum);
    }

    public boolean deductBC(UUID uuid, int amount) {
        if (amount <= 0) return true;
        int current = getBC(uuid);
        if (current < amount) return false;
        matchCredits.put(uuid, current - amount);
        return true;
    }

    public int getWC(UUID uuid) {
        return warCredits.getOrDefault(uuid, 0);
    }

    public void setWC(UUID uuid, int amount) {
        warCredits.put(uuid, Math.max(0, amount));
    }

    public void addWC(UUID uuid, int amount) {
        if (amount <= 0) return;
        warCredits.merge(uuid, amount, Integer::sum);
    }

    public boolean deductWC(UUID uuid, int amount) {
        if (amount <= 0) return true;
        int current = getWC(uuid);
        if (current < amount) return false;
        warCredits.put(uuid, current - amount);
        return true;
    }

    public int getVC(Team team) {
        return vehicleCredits[team.ordinal()];
    }

    public void addVC(Team team, int amount) {
        vehicleCredits[team.ordinal()] = Math.max(0, vehicleCredits[team.ordinal()] + amount);
    }

    public boolean deductVC(Team team, int amount) {
        int idx = team.ordinal();
        if (vehicleCredits[idx] < amount) return false;
        vehicleCredits[idx] -= amount;
        return true;
    }

    public void setVC(Team team, int amount) {
        vehicleCredits[team.ordinal()] = Math.max(0, amount);
    }

    public void loadFrom(Map<UUID, Integer> bcMap, Map<UUID, Integer> wcMap) {
        matchCredits.clear();
        warCredits.clear();
        for (Map.Entry<UUID, Integer> e : bcMap.entrySet()) {
            if (e.getValue() > 0) matchCredits.put(e.getKey(), e.getValue());
        }
        for (Map.Entry<UUID, Integer> e : wcMap.entrySet()) {
            if (e.getValue() > 0) warCredits.put(e.getKey(), e.getValue());
        }
    }

    public void resetMatch() {
        matchCredits.clear();
        vehicleCredits[0] = 0;
        vehicleCredits[1] = 0;
    }

    public void resetForPlayer(UUID uuid) {
        matchCredits.remove(uuid);
    }

    public Map<UUID, Integer> getMatchCreditsView() {
        return matchCredits;
    }

    public Map<UUID, Integer> getWarCreditsView() {
        return warCredits;
    }

    public void flushTo(Map<UUID, Integer> bcOut, Map<UUID, Integer> wcOut) {
        bcOut.putAll(matchCredits);
        wcOut.putAll(warCredits);
    }
}
