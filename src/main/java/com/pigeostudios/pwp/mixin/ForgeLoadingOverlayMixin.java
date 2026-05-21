package com.pigeostudios.pwp.mixin;

import com.pigeostudios.pwp.client.gui.UITheme;
import com.pigeostudios.pwp.client.gui.component.AnimationHelper;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraftforge.client.loading.ForgeLoadingOverlay;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ForgeLoadingOverlay.class)
public class ForgeLoadingOverlayMixin {

    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private Object displayWindow;
    @Shadow private long fadeOutStart;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        long millis = Util.getMillis();
        float fadeOutTimer = fadeOutStart > -1L ? (float)(millis - fadeOutStart) / 1000.0F : -1.0F;
        float fade = 1.0F - Mth.clamp(fadeOutTimer - 1.0F, 0.0F, 1.0F);

        int w = g.guiWidth();
        int h = g.guiHeight();

        g.fill(0, 0, w, h, UITheme.BG_BLACK);

        var font = minecraft.font;
        if (font != null) {
            String title = "BATTLEFIELD";
            String sub = "TACTICAL COMBAT";
            int cx = w / 2;
            int cy = h / 2;
            int alpha = (int)(fade * 0xFF);

            g.drawString(font, title, cx - font.width(title) / 2, cy - 50,
                AnimationHelper.withAlpha(UITheme.ACCENT, alpha));
            g.drawString(font, sub, cx - font.width(sub) / 2, cy - 30,
                AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int)(fade * 180)));

            g.fill(cx - 60, cy - 18, cx + 60, cy - 16,
                AnimationHelper.withAlpha(UITheme.ACCENT, alpha));
        }

        int barW = 260;
        int barH = 4;
        int barX = (w - barW) / 2;
        int barY = h / 2 + 20;

        g.fill(barX, barY, barX + barW, barY + barH,
            AnimationHelper.withAlpha(0xFF1A1A1A, (int)(fade * 0xFF)));

        float progress = fadeOutStart == -1L ? 0.5f : Math.min(1f, (millis - fadeOutStart) / 2000f);
        int fillW = (int)(barW * progress);
        if (fillW > 0) {
            g.fill(barX, barY, barX + fillW, barY + barH,
                AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fade * 0xFF)));
        }

        g.fill(barX - 1, barY - 1, barX + barW + 1, barY,
            AnimationHelper.withAlpha(UITheme.BORDER, (int)(fade * 0xFF)));
        g.fill(barX - 1, barY + barH, barX + barW + 1, barY + barH + 1,
            AnimationHelper.withAlpha(UITheme.BORDER, (int)(fade * 0xFF)));
        g.fill(barX - 1, barY, barX, barY + barH,
            AnimationHelper.withAlpha(UITheme.BORDER, (int)(fade * 0xFF)));
        g.fill(barX + barW, barY, barX + barW + 1, barY + barH,
            AnimationHelper.withAlpha(UITheme.BORDER, (int)(fade * 0xFF)));

        int corner = 24;
        int cd = 12;
        int fa = (int)(fade * 0x99);
        g.fill(corner, corner, corner + cd, corner + 1, AnimationHelper.withAlpha(UITheme.ACCENT, fa));
        g.fill(corner, corner, corner + 1, corner + cd, AnimationHelper.withAlpha(UITheme.ACCENT, fa));
        g.fill(w - corner - cd, corner, w - corner, corner + 1, AnimationHelper.withAlpha(UITheme.ACCENT, fa));
        g.fill(w - corner - 1, corner, w - corner, corner + cd, AnimationHelper.withAlpha(UITheme.ACCENT, fa));
        g.fill(corner, h - corner - 1, corner + cd, h - corner, AnimationHelper.withAlpha(UITheme.ACCENT, fa));
        g.fill(corner, h - corner - cd, corner + 1, h - corner, AnimationHelper.withAlpha(UITheme.ACCENT, fa));
        g.fill(w - corner - cd, h - corner - 1, w - corner, h - corner, AnimationHelper.withAlpha(UITheme.ACCENT, fa));
        g.fill(w - corner - 1, h - corner - cd, w - corner, h - corner, AnimationHelper.withAlpha(UITheme.ACCENT, fa));

        if (font != null) {
            String ver = "PWP v2.0 \u00B7 MC 1.20.1 Forge";
            int vw = font.width(ver);
            g.drawString(font, ver, (w - vw) / 2, h - 16,
                AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int)(fade * 0x55)));
        }

        if (fadeOutTimer >= 2.0F) {
            minecraft.setOverlay(null);
            try {
                displayWindow.getClass().getMethod("close").invoke(displayWindow);
            } catch (Exception ignored) {}
        }

        ci.cancel();
    }
}
