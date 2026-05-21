package com.yourmod.teamsystem.client.journeymap;

import journeymap.client.api.ClientPlugin;
import journeymap.client.api.IClientAPI;
import journeymap.client.api.IClientPlugin;
import journeymap.client.api.event.ClientEvent;

@ClientPlugin
public class TeamSystemJourneymapPlugin implements IClientPlugin {

    @Override
    public void initialize(IClientAPI jmClientApi) {
    }

    @Override
    public String getModId() {
        return "teamsystem";
    }

    @Override
    public void onEvent(ClientEvent event) {
    }
}
