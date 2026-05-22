package com.pigeostudios.pwp.core;

import com.pigeostudios.pwp.PWP;
import de.maxhenkel.voicechat.api.Group;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VoiceGroupManager {

    public static final int CHANNEL_LOCAL = 0;
    public static final int CHANNEL_SQUAD = 1;
    public static final int CHANNEL_TEAM  = 2;

    private static final String TEAM_NATO_GROUP  = "ts_nato";
    private static final String TEAM_RUSSIA_GROUP = "ts_russia";

    private VoicechatServerApi api;
    private Group natoGroup;
    private Group russiaGroup;
    private final Map<Integer, Group> squadGroups = new HashMap<>();
    private boolean available = false;

    public VoiceGroupManager() {
    }

    public void init(VoicechatServerApi voicechatApi) {
        this.api = voicechatApi;
        natoGroup = api.groupBuilder()
            .setName(TEAM_NATO_GROUP)
            .setType(Group.Type.ISOLATED)
            .setPersistent(false)
            .build();

        russiaGroup = api.groupBuilder()
            .setName(TEAM_RUSSIA_GROUP)
            .setType(Group.Type.ISOLATED)
            .setPersistent(false)
            .build();

        available = true;
        PWP.LOGGER.info("Voice Group Manager initialized");
    }

    @Nullable
    private VoicechatConnection getConnection(UUID playerUUID) {
        if (api == null) return null;
        return api.getConnectionOf(playerUUID);
    }

    public void joinChannel(ServerPlayer player, int channel) {
        if (!available) return;
        UUID uuid = player.getUUID();

        if (channel == CHANNEL_LOCAL) {
            leaveChannel(uuid);
            return;
        }

        VoicechatConnection conn = getConnection(uuid);
        if (conn == null) return;

        if (channel == CHANNEL_SQUAD) {
            SquadManager sm = PWP.getSquadManager();
            if (sm == null) { leaveChannel(uuid); return; }
            Squad squad = sm.getPlayerSquad(uuid);
            if (squad == null) { leaveChannel(uuid); return; }
            Group group = getOrCreateSquadGroup(squad.getSquadId());
            conn.setGroup(group);
        } else if (channel == CHANNEL_TEAM) {
            Team team = PWP.getTeamManager().getOrCreatePlayerData(uuid).getTeam();
            if (team == Team.NATO) {
                conn.setGroup(natoGroup);
            } else if (team == Team.RUSSIA) {
                conn.setGroup(russiaGroup);
            } else {
                leaveChannel(uuid);
            }
        }
    }

    public void leaveChannel(UUID playerUUID) {
        if (!available) return;
        VoicechatConnection conn = getConnection(playerUUID);
        if (conn == null) return;
        conn.setGroup(null);
    }

    private Group getOrCreateSquadGroup(int squadId) {
        Group group = squadGroups.get(squadId);
        if (group == null) {
            group = api.groupBuilder()
                .setName("ts_squad_" + squadId)
                .setType(Group.Type.ISOLATED)
                .setPersistent(false)
                .build();
            squadGroups.put(squadId, group);
        }
        return group;
    }

    public void removeSquadGroup(int squadId) {
        Group group = squadGroups.remove(squadId);
        if (group != null && api != null) {
            api.removeGroup(group.getId());
        }
    }

    public void cleanup() {
        if (api == null) return;
        for (Map.Entry<Integer, Group> entry : squadGroups.entrySet()) {
            api.removeGroup(entry.getValue().getId());
        }
        squadGroups.clear();
        if (natoGroup != null) api.removeGroup(natoGroup.getId());
        if (russiaGroup != null) api.removeGroup(russiaGroup.getId());
        natoGroup = null;
        russiaGroup = null;
        available = false;
    }

    public boolean isAvailable() {
        return available;
    }

    public VoicechatServerApi getApi() {
        return api;
    }
}
