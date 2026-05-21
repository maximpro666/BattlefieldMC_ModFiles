package com.yourmod.teamsystem.mixin;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.simple.IndexedMessageCodec;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.function.Supplier;

@Mixin(value = IndexedMessageCodec.class, remap = false)
public class IndexedMessageCodecMixin {

    @Inject(
        method = "consume",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private void onConsume(FriendlyByteBuf payload, int loginIndex, Supplier<NetworkEvent.Context> contextSupplier, CallbackInfo ci) {
        if (payload == null || !payload.isReadable()) {
            ci.cancel();
        }
    }
}
