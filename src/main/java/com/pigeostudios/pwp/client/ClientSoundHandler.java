package com.pigeostudios.pwp.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.registries.ForgeRegistries;

import com.pigeostudios.pwp.core.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.RegistryObject;

public class ClientSoundHandler {
    public static void playGuiSound(RegistryObject<SoundEvent> sound) {
        float vol = ClientTeamData.guiVolume;
        if (vol <= 0) return;
        Minecraft.getInstance().getSoundManager().play(
            SimpleSoundInstance.forUI(sound.get(), vol, 1.0F));
    }

    public static void handleSoundPacket(String soundId, boolean positioned, double x, double y, double z, float volume) {
        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(soundId));
        if (sound == null) return;
        if (positioned) {
            var level = Minecraft.getInstance().level;
            if (level == null) return;
            level.playLocalSound(x, y, z, sound, SoundSource.PLAYERS, volume, 1.0F, false);
        } else {
            Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(sound, volume, 1.0F));
        }
    }
}
