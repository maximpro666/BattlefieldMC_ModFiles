package com.yourmod.teamsystem.client.gui.screen;

import com.yourmod.teamsystem.client.KitData;
import com.yourmod.teamsystem.client.gui.component.BButton;
import com.yourmod.teamsystem.client.gui.component.BScrollPanel;
import com.yourmod.teamsystem.network.KitSavePacket;
import com.yourmod.teamsystem.network.PacketHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class KitLoadoutScreen extends Screen {
    private final Screen parent;
    private KitData selectedKit;
    private BScrollPanel scroll;

    public KitLoadoutScreen(Screen parent, KitData kit) {
        super(Component.literal("Kit Loadout"));
        this.parent = parent;
        this.selectedKit = kit;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        scroll = new BScrollPanel(10, 40, width - 20, height - 100);
        rebuildContent();
        addRenderableWidget(new BButton(cx - 50, height - 35, 100, 20, "Back", btn -> minecraft.setScreen(parent)));
    }

    private void rebuildContent() {
        scroll.clearContent();
        scroll.addContent(g -> {
            int sx = scroll.getX() + 5;
            int sy = scroll.getY() - scroll.getScrollOffset();
            g.drawString(font, "Kit: " + selectedKit.name(), sx, sy + 4, 0xFF00AAFF);
            g.drawString(font, selectedKit.description(), sx, sy + 18, 0xFFFFFFFF);
            g.drawString(font, "Min Rank: " + selectedKit.minRank(), sx, sy + 32, 0xFFAAAAAA);
            g.drawString(font, "Cooldown: " + selectedKit.cooldown(), sx, sy + 46, 0xFFAAAAAA);
        });
        scroll.setContentHeight(60);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        g.fill(0, 0, width, height, 0xCC111111);
        String title = "KIT LOADOUT - " + selectedKit.name();
        int titleW = font.width(title);
        g.drawString(font, title, (width - titleW) / 2, 16, 0xFF00AAFF);
        scroll.render(g);
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scroll.onScroll(mouseX, mouseY, delta);
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
