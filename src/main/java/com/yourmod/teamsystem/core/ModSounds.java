package com.yourmod.teamsystem.core;

import com.yourmod.teamsystem.TeamSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
        DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, TeamSystem.MODID);

    public static final RegistryObject<SoundEvent> GUI_BUTTON_CLICK = register("gui.button_click");
    public static final RegistryObject<SoundEvent> GUI_BUTTON_HOVER = register("gui.button_hover");
    public static final RegistryObject<SoundEvent> GUI_ERROR = register("gui.error");
    public static final RegistryObject<SoundEvent> GUI_SUCCESS = register("gui.success");
    public static final RegistryObject<SoundEvent> GUI_TRANSITION = register("gui.transition");
    public static final RegistryObject<SoundEvent> GUI_FIRST_DEPLOY = register("gui.first_deploy");
    public static final RegistryObject<SoundEvent> GUI_RESPAWN = register("gui.respawn");
    public static final RegistryObject<SoundEvent> GAME_CAPTURE_POINT = register("game.capture_point");
    public static final RegistryObject<SoundEvent> GAME_VICTORY = register("game.victory");
    public static final RegistryObject<SoundEvent> GAME_DEFEAT = register("game.defeat");
    public static final RegistryObject<SoundEvent> GAME_MAP_CHANGE = register("game.map_change");
    public static final RegistryObject<SoundEvent> NOTIFICATION_INFO = register("notification.info");
    public static final RegistryObject<SoundEvent> NOTIFICATION_ALERT = register("notification.alert");

    private static RegistryObject<SoundEvent> register(String name) {
        ResourceLocation id = new ResourceLocation(TeamSystem.MODID, name);
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }

    public static void playGUISound(SoundEvent sound) {
        if (Minecraft.getInstance().level == null || Minecraft.getInstance().player == null) return;
        Minecraft.getInstance().getSoundManager().play(
            SimpleSoundInstance.forUI(sound, 1.0F, 1.0F));
    }

    public static void playGUISound(RegistryObject<SoundEvent> sound) {
        playGUISound(sound.get());
    }

    public static void playPositionedSound(Level level, SoundEvent sound, double x, double y, double z, float radius) {
        if (level.isClientSide) {
            level.playLocalSound(x, y, z, sound, SoundSource.PLAYERS, radius, 1.0F, false);
        } else if (level instanceof ServerLevel serverLevel) {
            BlockPos pos = BlockPos.containing(x, y, z);
            for (ServerPlayer player : serverLevel.players()) {
                if (player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) <= radius * radius) {
                    player.playNotifySound(sound, SoundSource.PLAYERS, radius, 1.0F);
                }
            }
        }
    }

    public static void playPositionedSound(Level level, RegistryObject<SoundEvent> sound, double x, double y, double z, float radius) {
        playPositionedSound(level, sound.get(), x, y, z, radius);
    }
}
