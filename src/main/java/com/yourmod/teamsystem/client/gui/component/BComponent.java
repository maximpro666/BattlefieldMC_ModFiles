package com.yourmod.teamsystem.client.gui.component;

import net.minecraft.client.gui.GuiGraphics;

@FunctionalInterface
public interface BComponent {
    void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick);
}
