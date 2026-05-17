package com.yourmod.teamsystem.core;

import net.minecraft.nbt.CompoundTag;

public class MapConfig {
    private String name;
    private String worldFolder;
    private boolean enabled;
    private boolean hasRespawn;
    private boolean hasCapturePoints;
    private boolean hasRegen;
    private boolean hasWorldBorder;
    private int worldBorderCenterX;
    private int worldBorderCenterZ;
    private int worldBorderSize;
    private int tickets;
    private int lobbyWaitTime;
    private int[] natoSpawn;
    private int[] russiaSpawn;
    private int baseRadius;

    public MapConfig() {
        this.name = "";
        this.worldFolder = "";
        this.enabled = true;
        this.hasRespawn = true;
        this.hasCapturePoints = true;
        this.hasRegen = true;
        this.hasWorldBorder = true;
        this.worldBorderCenterX = 0;
        this.worldBorderCenterZ = 0;
        this.worldBorderSize = 1000;
        this.tickets = 100;
        this.lobbyWaitTime = 30;
        this.natoSpawn = new int[]{0, 64, 0};
        this.russiaSpawn = new int[]{0, 64, 0};
        this.baseRadius = 30;
    }

    public MapConfig(String name, String worldFolder, boolean enabled, boolean hasRespawn,
                     boolean hasCapturePoints, boolean hasRegen, boolean hasWorldBorder,
                     int worldBorderCenterX, int worldBorderCenterZ, int worldBorderSize,
                     int tickets, int lobbyWaitTime,
                     int[] natoSpawn, int[] russiaSpawn, int baseRadius) {
        this.name = name;
        this.worldFolder = worldFolder;
        this.enabled = enabled;
        this.hasRespawn = hasRespawn;
        this.hasCapturePoints = hasCapturePoints;
        this.hasRegen = hasRegen;
        this.hasWorldBorder = hasWorldBorder;
        this.worldBorderCenterX = worldBorderCenterX;
        this.worldBorderCenterZ = worldBorderCenterZ;
        this.worldBorderSize = worldBorderSize;
        this.tickets = tickets;
        this.lobbyWaitTime = lobbyWaitTime;
        this.natoSpawn = natoSpawn != null ? natoSpawn : new int[]{0, 64, 0};
        this.russiaSpawn = russiaSpawn != null ? russiaSpawn : new int[]{0, 64, 0};
        this.baseRadius = baseRadius > 0 ? baseRadius : 30;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getWorldFolder() { return worldFolder; }
    public void setWorldFolder(String worldFolder) { this.worldFolder = worldFolder; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean hasRespawn() { return hasRespawn; }
    public void setHasRespawn(boolean hasRespawn) { this.hasRespawn = hasRespawn; }

    public boolean hasCapturePoints() { return hasCapturePoints; }
    public void setHasCapturePoints(boolean hasCapturePoints) { this.hasCapturePoints = hasCapturePoints; }

    public boolean hasRegen() { return hasRegen; }
    public void setHasRegen(boolean hasRegen) { this.hasRegen = hasRegen; }

    public boolean hasWorldBorder() { return hasWorldBorder; }
    public void setHasWorldBorder(boolean hasWorldBorder) { this.hasWorldBorder = hasWorldBorder; }

    public int getWorldBorderCenterX() { return worldBorderCenterX; }
    public void setWorldBorderCenterX(int x) { this.worldBorderCenterX = x; }

    public int getWorldBorderCenterZ() { return worldBorderCenterZ; }
    public void setWorldBorderCenterZ(int z) { this.worldBorderCenterZ = z; }

    public int getWorldBorderSize() { return worldBorderSize; }
    public void setWorldBorderSize(int size) { this.worldBorderSize = size; }

    public int getTickets() { return tickets; }
    public void setTickets(int tickets) { this.tickets = Math.max(1, tickets); }

    public int getLobbyWaitTime() { return lobbyWaitTime; }
    public void setLobbyWaitTime(int seconds) { this.lobbyWaitTime = Math.max(5, seconds); }

    public boolean hasTeamSpawns() { return natoSpawn != null && russiaSpawn != null; }
    public int[] getNatoSpawn() { return natoSpawn; }
    public void setNatoSpawn(int[] pos) { this.natoSpawn = pos; }
    public int[] getRussiaSpawn() { return russiaSpawn; }
    public void setRussiaSpawn(int[] pos) { this.russiaSpawn = pos; }
    public int getBaseRadius() { return baseRadius; }
    public void setBaseRadius(int radius) { this.baseRadius = Math.max(1, radius); }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Name", name);
        tag.putString("WorldFolder", worldFolder);
        tag.putBoolean("Enabled", enabled);
        tag.putBoolean("HasRespawn", hasRespawn);
        tag.putBoolean("HasCapturePoints", hasCapturePoints);
        tag.putBoolean("HasRegen", hasRegen);
        tag.putBoolean("HasWorldBorder", hasWorldBorder);
        tag.putInt("WorldBorderCenterX", worldBorderCenterX);
        tag.putInt("WorldBorderCenterZ", worldBorderCenterZ);
        tag.putInt("WorldBorderSize", worldBorderSize);
        tag.putInt("Tickets", tickets);
        tag.putInt("LobbyWaitTime", lobbyWaitTime);
        tag.putInt("NatoSpawnX", natoSpawn[0]);
        tag.putInt("NatoSpawnY", natoSpawn[1]);
        tag.putInt("NatoSpawnZ", natoSpawn[2]);
        tag.putInt("RussiaSpawnX", russiaSpawn[0]);
        tag.putInt("RussiaSpawnY", russiaSpawn[1]);
        tag.putInt("RussiaSpawnZ", russiaSpawn[2]);
        tag.putInt("BaseRadius", baseRadius);
        return tag;
    }

    public static MapConfig fromNBT(CompoundTag tag) {
        MapConfig config = new MapConfig();
        config.name = tag.getString("Name");
        config.worldFolder = tag.getString("WorldFolder");
        config.enabled = tag.getBoolean("Enabled");
        config.hasRespawn = tag.getBoolean("HasRespawn");
        config.hasCapturePoints = tag.getBoolean("HasCapturePoints");
        config.hasRegen = tag.getBoolean("HasRegen");
        config.hasWorldBorder = tag.getBoolean("HasWorldBorder");
        config.worldBorderCenterX = tag.getInt("WorldBorderCenterX");
        config.worldBorderCenterZ = tag.getInt("WorldBorderCenterZ");
        config.worldBorderSize = tag.getInt("WorldBorderSize");
        config.tickets = tag.getInt("Tickets");
        config.lobbyWaitTime = tag.getInt("LobbyWaitTime");
        config.natoSpawn = new int[]{tag.getInt("NatoSpawnX"), tag.getInt("NatoSpawnY"), tag.getInt("NatoSpawnZ")};
        config.russiaSpawn = new int[]{tag.getInt("RussiaSpawnX"), tag.getInt("RussiaSpawnY"), tag.getInt("RussiaSpawnZ")};
        config.baseRadius = tag.getInt("BaseRadius");
        return config;
    }

    @Override
    public String toString() {
        return "MapConfig{name='" + name + "', worldFolder='" + worldFolder + "', enabled=" + enabled + "}";
    }
}
