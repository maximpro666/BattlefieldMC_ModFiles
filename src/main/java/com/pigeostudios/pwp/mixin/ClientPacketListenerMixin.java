package com.pigeostudios.pwp.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    @Inject(method = "handleTeleportEntity", at = @At("HEAD"), cancellable = true)
    private void onHandleTeleportEntity(ClientboundTeleportEntityPacket packet, CallbackInfo ci) {
        if (Minecraft.getInstance().level == null) {
            ci.cancel();
        }
    }
}
