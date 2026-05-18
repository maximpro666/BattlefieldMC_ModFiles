package com.yourmod.teamsystem.client.gui.screen;

import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.client.KitEntry;
import com.yourmod.teamsystem.client.gui.component.BButton;
import com.yourmod.teamsystem.client.gui.component.BScrollPanel;
import com.yourmod.teamsystem.network.KitSelectPacket;
import com.yourmod.teamsystem.network.PacketHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class KitSelectionScreen extends Screen {
    private final Screen parent;
    private BScrollPanel scroll;

    private static final int LEFT_W = 200;

    public KitSelectionScreen(Screen parent) {
        super(Component.literal("Kit Selection"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        scroll = new BScrollPanel(4, 40, LEFT_W - 8, height - 80);
        scroll.setContentHeight(Math.max(height - 80, ClientTeamData.availableKits.size() * 60));
        rebuildKitList();

        int rx = LEFT_W + 10;
        int by = height - 30;
        addRenderableWidget(new BButton(rx, by, 100, 20, "Back", btn -> minecraft.setScreen(parent)));
    }

    private void rebuildKitList() {
        scroll.clearContent();
        int[] idx = {0};
        for (KitEntry kit : ClientTeamData.availableKits) {
            int yOff = idx[0] * 60;
            scroll.addContent(g -> {
                int sx = scroll.getX() + 5;
                int sy = scroll.getY() + yOff - scroll.getScrollOffset();
                boolean locked = ClientTeamData.localPlayerRank < kit.minRankOrdinal();
                String name = kit.name();
                String desc = kit.description();
                int col = locked ? 0xFF888888 : 0xFFFFFFFF;
                g.drawString(font, name, sx, sy + 4, col);
                g.drawString(font, desc, sx, sy + 16, 0xFFAAAAAA);
                if (locked) {
                    g.drawString(font, "LOCKED (Rank " + kit.minRankOrdinal() + ")", sx, sy + 28, 0xFFFF4444);
                }
            });
            idx[0]++;
        }
        scroll.setContentHeight(idx[0] * 60);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            int sy = scroll.getY();
            int idx = (int) ((my - sy + scroll.getScrollOffset()) / 60);
            if (idx >= 0 && idx < ClientTeamData.availableKits.size()) {
                KitEntry kit = ClientTeamData.availableKits.get(idx);
                if (ClientTeamData.localPlayerRank >= kit.minRankOrdinal()) {
                    PacketHandler.CHANNEL.sendToServer(new KitSelectPacket(kit.id()));
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        g.fill(0, 0, width, height, 0xCC111111);
        String title = "KIT SELECTION";
        int titleW = font.width(title);
        g.drawString(font, title, (width - titleW) / 2, 16, 0xFF00AAFF);
        scroll.render(g);
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX < LEFT_W) {
            scroll.onScroll(mouseX, mouseY, delta);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
