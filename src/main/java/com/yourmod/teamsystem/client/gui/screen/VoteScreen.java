package com.yourmod.teamsystem.client.gui.screen;

import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.client.gui.component.BButton;
import com.yourmod.teamsystem.client.gui.component.BProgressBar;
import com.yourmod.teamsystem.client.gui.component.BScrollPanel;
import com.yourmod.teamsystem.network.MapVotePacket;
import com.yourmod.teamsystem.network.PacketHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.ArrayList;

public class VoteScreen extends Screen {
    private final Screen parent;
    private BScrollPanel scroll;
    private List<MapOption> maps;

    private static class MapOption {
        String id;
        String name;
        int votes;
        int maxVotes;
    }

    public VoteScreen(Screen parent) {
        super(Component.literal("Map Vote"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        maps = new ArrayList<>();

        scroll = new BScrollPanel(10, 40, width - 20, height - 100);
        rebuildList();

        addRenderableWidget(new BButton(width / 2 - 50, height - 35, 100, 20, "Close", btn -> {
            minecraft.setScreen(parent);
        }));
    }

    public void setMaps(List<String> mapIds, List<String> mapNames, List<Integer> voteCounts) {
        maps.clear();
        for (int i = 0; i < mapIds.size() && i < mapNames.size(); i++) {
            MapOption mo = new MapOption();
            mo.id = mapIds.get(i);
            mo.name = mapNames.get(i);
            mo.votes = i < voteCounts.size() ? voteCounts.get(i) : 0;
            mo.maxVotes = 1;
            maps.add(mo);
        }
        if (!maps.isEmpty()) {
            int maxV = maps.stream().mapToInt(m -> m.votes).max().orElse(1);
            for (MapOption mo : maps) mo.maxVotes = Math.max(maxV, 1);
        }
        rebuildList();
    }

    private void rebuildList() {
        scroll.clearContent();
        int[] idx = {0};
        for (MapOption mo : maps) {
            int yOff = idx[0] * 40;
            scroll.addContent(g -> {
                int sx = scroll.getX() + 5;
                int sy = scroll.getY() + yOff - scroll.getScrollOffset();
                g.drawString(font, mo.name, sx, sy + 2, 0xFFFFFFFF);

                BProgressBar bar = new BProgressBar(sx, sy + 14, scroll.getWidth() - 20, 12, 0xFF00AAFF);
                bar.setFraction(mo.maxVotes > 0 ? (float) mo.votes / mo.maxVotes : 0);
                bar.render(g);

                String voteStr = mo.votes + " votes";
                g.drawString(font, voteStr, sx + scroll.getWidth() - 20 - font.width(voteStr), sy + 2, 0xFFAAAAAA);
            });
            idx[0]++;
        }
        scroll.setContentHeight(idx[0] * 40);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            int idx = (int) ((my - scroll.getY() + scroll.getScrollOffset()) / 40);
            if (idx >= 0 && idx < maps.size()) {
                PacketHandler.CHANNEL.sendToServer(new MapVotePacket(maps.get(idx).id));
            }
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        g.fill(0, 0, width, height, 0xCC111111);
        String title = "MAP VOTE";
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
