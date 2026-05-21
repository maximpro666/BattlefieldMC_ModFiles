package com.yourmod.teamsystem.core;

import com.yourmod.teamsystem.TeamSystem;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;

public class TeamVoicePlugin implements VoicechatPlugin {

    private static VoiceGroupManager groupManager;

    @Override
    public String getPluginId() {
        return "teamsystem";
    }

    @Override
    public void initialize(VoicechatApi api) {
        if (api instanceof VoicechatServerApi serverApi) {
            groupManager = new VoiceGroupManager();
            groupManager.init(serverApi);
            TeamSystem.LOGGER.info("TeamVoicePlugin initialized with SV API");
        }
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophone);
    }

    private void onMicrophone(MicrophonePacketEvent event) {
    }

    public static VoiceGroupManager getGroupManager() {
        return groupManager;
    }
}
