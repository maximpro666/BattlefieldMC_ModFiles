package com.yourmod.teamsystem.client.journeymap;

import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.client.PlayerListEntry;
import com.yourmod.teamsystem.core.Team;
import journeymap.client.api.ClientPlugin;
import journeymap.client.api.IClientAPI;
import journeymap.client.api.IClientPlugin;
import journeymap.client.api.event.ClientEvent;
import journeymap.client.api.event.forge.EntityRadarUpdateEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@ClientPlugin
public class TeamSystemJourneymapPlugin implements IClientPlugin {

    private static IClientAPI api;

    @Override
    public void initialize(IClientAPI jmClientApi) {
        api = jmClientApi;
        JourneyMapIntegration.onPluginInit(api);
    }

    @Override
    public String getModId() {
        return "teamsystem";
    }

    @Override
    public void onEvent(ClientEvent event) {
        if (event.type == ClientEvent.Type.DISPLAY_UPDATE && api != null) {
            JourneyMapIntegration.resendAll();
        }
    }

    @SubscribeEvent
    public void onEntityRadarUpdate(EntityRadarUpdateEvent event) {
        if (Minecraft.getInstance().level == null) return;
        var ref = event.getWrappedEntity().getEntityLivingRef();
        if (ref == null) return;
        var entity = ref.get();
        if (!(entity instanceof Player player)) return;
        if (player == Minecraft.getInstance().player) return;
        Team playerTeam = ClientTeamData.getLocalPlayerTeam();
        if (playerTeam == Team.SPECTATOR) return;
        if (ClientTeamData.playerDataMap == null) return;
        PlayerListEntry ple = ClientTeamData.playerDataMap.get(player.getUUID());
        if (ple == null) return;
        if (ple.teamOrdinal() != playerTeam.ordinal()) {
            event.setCanceled(true);
        }
    }

    public static IClientAPI getAPI() {
        return api;
    }
}
