package com.yourmod.teamsystem.network;

import com.yourmod.teamsystem.client.gui.screen.AdminPanel;
import com.yourmod.teamsystem.client.gui.screen.ClassSelectionScreen;
import com.yourmod.teamsystem.client.gui.screen.TeamSelectionScreen;
import net.minecraft.client.Minecraft;

public class ClientScreenAccessor {

    public static void openTeamSelection() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.level != null) {
            mc.setScreen(new TeamSelectionScreen());
        }
    }

    public static void openLoadout() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.level != null) {
            mc.setScreen(new ClassSelectionScreen());
        }
    }

    public static void openAdminPanel() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.level != null) {
            mc.setScreen(new AdminPanel());
        }
    }
}
