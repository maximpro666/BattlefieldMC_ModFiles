package com.pigeostudios.pwp.mixin;

import com.pigeostudios.pwp.client.ClientTeamData;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "bobView", at = @At("TAIL"))
    private void onBobView(PoseStack poseStack, float partialTick, CallbackInfo ci) {
        if (!ClientTeamData.isBleeding) return;

        float t = (System.currentTimeMillis() % 2000L) / 2000f;
        float bob = (float) Math.sin(t * Math.PI * 2) * 0.06f;
        poseStack.translate(0.0, bob, 0.0);
    }
}
