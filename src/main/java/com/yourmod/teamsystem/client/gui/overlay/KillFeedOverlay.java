package com.yourmod.teamsystem.client.gui.overlay;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Mod.EventBusSubscriber(modid = "teamsystem", value = Dist.CLIENT)
public class KillFeedOverlay {
    private static final Minecraft mc = Minecraft.getInstance();
    private static final List<KillFeedEntry> entries = new ArrayList<>();
    private static final long FADE_TIME = 5000;

    public static void addEntry(String message) {
        synchronized (entries) {
            entries.add(new KillFeedEntry(message, System.currentTimeMillis()));
            if (entries.size() > 20) {
                entries.remove(0);
            }
        }
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().getPath().equals("hotbar")) return;
        GuiGraphics gui = event.getGuiGraphics();
        int w = mc.getWindow().getGuiScaledWidth();
        long now = System.currentTimeMillis();

        synchronized (entries) {
            Iterator<KillFeedEntry> it = entries.iterator();
            int yOff = 10;
            while (it.hasNext()) {
                KillFeedEntry e = it.next();
                long age = now - e.timestamp;
                if (age > FADE_TIME) {
                    it.remove();
                    continue;
                }
                float alpha = 1.0f - (float) age / FADE_TIME;
                int a = Math.min(255, Math.max(0, (int) (alpha * 255)));
                int color = (a << 24) | 0xFFFFFF;
                int tw = mc.font.width(e.message);
                gui.fill(w - tw - 12, yOff - 1, w - 4, yOff + 9, (a / 2) << 24 | 0x000000);
                gui.drawString(mc.font, e.message, w - tw - 8, yOff, color);
                yOff += 12;
            }
        }
    }

    private record KillFeedEntry(String message, long timestamp) {}
}
