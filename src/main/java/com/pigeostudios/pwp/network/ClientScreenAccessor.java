package com.pigeostudios.pwp.network;

import com.pigeostudios.pwp.client.gui.screen.AdminPanel;
import com.pigeostudios.pwp.client.gui.screen.ClassSelectionScreen;
import com.pigeostudios.pwp.client.gui.screen.MatchResultsScreen;
import com.pigeostudios.pwp.client.gui.screen.ReportScreen;
import com.pigeostudios.pwp.client.gui.screen.TOSAgreementScreen;
import com.pigeostudios.pwp.client.gui.screen.TeamSelectionScreen;
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

    public static void openMatchResults(OpenMatchResultsPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new MatchResultsScreen(packet));
    }

    public static void openReportScreen() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.level != null) {
            mc.setScreen(new ReportScreen());
        }
    }

    public static void openTOS(String tosUrl, String privacyUrl) {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new TOSAgreementScreen(tosUrl, privacyUrl));
    }
}
