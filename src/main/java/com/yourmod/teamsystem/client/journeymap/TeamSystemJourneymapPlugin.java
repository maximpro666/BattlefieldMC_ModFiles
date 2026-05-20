package com.yourmod.teamsystem.client.journeymap;

import journeymap.client.api.ClientPlugin;
import journeymap.client.api.IClientAPI;
import journeymap.client.api.IClientPlugin;
import journeymap.client.api.event.ClientEvent;

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

    public static IClientAPI getAPI() {
        return api;
    }
}