package com.pigeostudios.pwp.client.gui.widget;

import com.pigeostudios.pwp.client.gui.UITheme;
import com.pigeostudios.pwp.client.gui.component.AnimationHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class BWidgets {

    public static class BToggle extends AbstractWidget {
        private boolean value;
        private float slideProgress = 0f;
        private Consumer<Boolean> onChange;

        public BToggle(int x, int y, boolean initialValue) {
            super(x, y, 40, 20, Component.empty());
            this.value = initialValue;
            this.slideProgress = initialValue ? 1.0f : 0f;
        }

        public BToggle setOnChange(Consumer<Boolean> onChange) {
            this.onChange = onChange;
            return this;
        }

        public boolean getValue() { return value; }
        public void setValue(boolean v) { this.value = v; }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            float target = value ? 1.0f : 0f;
            slideProgress = AnimationHelper.lerp(slideProgress, target, 0.2f);

            int trackColor = value ? UITheme.ACCENT_DIM : UITheme.BG_SLOT;
            graphics.fill(getX(), getY(), getX() + width, getY() + height, trackColor);

            int borderColor = isHovered() ? UITheme.ACCENT : UITheme.BORDER;
            graphics.fill(getX(), getY(), getX() + width, getY() + 1, borderColor);
            graphics.fill(getX(), getY() + height - 1, getX() + width, getY() + height, borderColor);
            graphics.fill(getX(), getY(), getX() + 1, getY() + height, borderColor);
            graphics.fill(getX() + width - 1, getY(), getX() + width, getY() + height, borderColor);

            int knobSize = height - 4;
            int knobX = getX() + 2 + (int)((width - knobSize - 4) * slideProgress);
            int knobY = getY() + 2;
            int knobColor = value ? UITheme.ACCENT : UITheme.TEXT_SECONDARY;
            graphics.fill(knobX, knobY, knobX + knobSize, knobY + knobSize, knobColor);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            value = !value;
            if (onChange != null) onChange.accept(value);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            output.add(NarratedElementType.TITLE, Component.literal(value ? "ON" : "OFF"));
        }
    }

    public static class BDropdown extends AbstractWidget {
        private final List<String> options;
        private int selectedIndex;
        private boolean expanded = false;
        private Consumer<Integer> onChange;

        public BDropdown(int x, int y, int width, List<String> options, int selectedIndex) {
            super(x, y, width, 22, Component.empty());
            this.options = new ArrayList<>(options);
            this.selectedIndex = Math.max(0, Math.min(selectedIndex, options.size() - 1));
        }

        public BDropdown setOnChange(Consumer<Integer> onChange) {
            this.onChange = onChange;
            return this;
        }

        public int getSelectedIndex() { return selectedIndex; }
        public String getSelectedValue() { return options.get(selectedIndex); }
        public void setSelectedIndex(int index) {
            this.selectedIndex = Math.max(0, Math.min(index, options.size() - 1));
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int bgColor = isHovered() ? UITheme.BG_SURFACE : UITheme.BG_PANEL;
            graphics.fill(getX(), getY(), getX() + width, getY() + height, bgColor);

            int borderColor = isHovered() ? UITheme.ACCENT : UITheme.BORDER;
            graphics.fill(getX(), getY(), getX() + width, getY() + 1, borderColor);
            graphics.fill(getX(), getY() + height - 1, getX() + width, getY() + height, borderColor);
            graphics.fill(getX(), getY(), getX() + 1, getY() + height, borderColor);
            graphics.fill(getX() + width - 1, getY(), getX() + width, getY() + height, borderColor);

            graphics.drawString(Minecraft.getInstance().font, options.get(selectedIndex),
                getX() + 6, getY() + (height - 8) / 2, UITheme.TEXT_PRIMARY);

            String arrow = expanded ? "\u25B2" : "\u25BC";
            graphics.drawString(Minecraft.getInstance().font, arrow,
                getX() + width - 16, getY() + (height - 8) / 2, UITheme.TEXT_SECONDARY);

            if (expanded) {
                int listHeight = Math.min(options.size() * 20, 200);
                int listY = getY() + height + 2;

                graphics.fill(getX(), listY, getX() + width, listY + listHeight, UITheme.BG_PANEL);
                graphics.fill(getX(), listY, getX() + width, listY + 1, UITheme.BORDER);
                graphics.fill(getX(), listY + listHeight - 1, getX() + width, listY + listHeight, UITheme.BORDER);
                graphics.fill(getX(), listY, getX() + 1, listY + listHeight, UITheme.BORDER);
                graphics.fill(getX() + width - 1, listY, getX() + width, listY + listHeight, UITheme.BORDER);

                for (int i = 0; i < options.size(); i++) {
                    int optY = listY + i * 20;
                    if (mouseX >= getX() && mouseX < getX() + width &&
                        mouseY >= optY && mouseY < optY + 20) {
                        graphics.fill(getX(), optY, getX() + width, optY + 20, UITheme.ACCENT_GHOST);
                    }
                    if (i == selectedIndex) {
                        graphics.fill(getX(), optY, getX() + 2, optY + 20, UITheme.ACCENT);
                    }
                    graphics.drawString(Minecraft.getInstance().font, options.get(i),
                        getX() + 6, optY + 6,
                        i == selectedIndex ? UITheme.ACCENT : UITheme.TEXT_PRIMARY);
                }
            }
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            if (expanded) {
                int listY = getY() + height + 2;
                int relY = (int)(mouseY - listY);
                if (relY >= 0 && relY < options.size() * 20) {
                    int clickedIndex = relY / 20;
                    if (clickedIndex != selectedIndex) {
                        selectedIndex = clickedIndex;
                        if (onChange != null) onChange.accept(selectedIndex);
                    }
                }
                expanded = false;
            } else {
                expanded = true;
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            output.add(NarratedElementType.TITLE,
                Component.literal("Selected: " + options.get(selectedIndex)));
        }
    }

    public static class BNumberInput extends AbstractWidget {
        private int value;
        private final int min;
        private final int max;
        private Consumer<Integer> onChange;

        public BNumberInput(int x, int y, int width, int value, int min, int max) {
            super(x, y, width, 22, Component.empty());
            this.value = Math.max(min, Math.min(value, max));
            this.min = min;
            this.max = max;
        }

        public BNumberInput setOnChange(Consumer<Integer> onChange) {
            this.onChange = onChange;
            return this;
        }

        public int getValue() { return value; }
        public void setValue(int v) { this.value = Math.max(min, Math.min(v, max)); }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int buttonWidth = 22;

            graphics.fill(getX(), getY(), getX() + width, getY() + height, UITheme.BG_PANEL);

            int borderColor = UITheme.BORDER;
            graphics.fill(getX(), getY(), getX() + width, getY() + 1, borderColor);
            graphics.fill(getX(), getY() + height - 1, getX() + width, getY() + height, borderColor);
            graphics.fill(getX(), getY(), getX() + 1, getY() + height, borderColor);
            graphics.fill(getX() + width - 1, getY(), getX() + width, getY() + height, borderColor);

            boolean minusHover = mouseX >= getX() && mouseX < getX() + buttonWidth
                && mouseY >= getY() && mouseY < getY() + height;
            if (minusHover) {
                graphics.fill(getX(), getY(), getX() + buttonWidth, getY() + height, UITheme.ACCENT_GHOST);
            }
            graphics.drawCenteredString(Minecraft.getInstance().font, "-",
                getX() + buttonWidth / 2, getY() + (height - 8) / 2, UITheme.TEXT_PRIMARY);

            boolean plusHover = mouseX >= getX() + width - buttonWidth && mouseX < getX() + width
                && mouseY >= getY() && mouseY < getY() + height;
            if (plusHover) {
                graphics.fill(getX() + width - buttonWidth, getY(), getX() + width, getY() + height, UITheme.ACCENT_GHOST);
            }
            graphics.drawCenteredString(Minecraft.getInstance().font, "+",
                getX() + width - buttonWidth / 2, getY() + (height - 8) / 2, UITheme.TEXT_PRIMARY);

            graphics.drawCenteredString(Minecraft.getInstance().font, String.valueOf(value),
                getX() + width / 2, getY() + (height - 8) / 2, UITheme.TEXT_PRIMARY);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            int buttonWidth = 22;
            if (mouseX < getX() + buttonWidth) {
                if (value > min) { value--; if (onChange != null) onChange.accept(value); }
            } else if (mouseX >= getX() + width - buttonWidth) {
                if (value < max) { value++; if (onChange != null) onChange.accept(value); }
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            output.add(NarratedElementType.TITLE, Component.literal("Value: " + value));
        }
    }

    public static class BTag extends AbstractWidget {
        private final String text;
        private final int bgColor;

        public BTag(int x, int y, String text, int bgColor) {
            super(x, y, Minecraft.getInstance().font.width(text) + 12, 18, Component.literal(text));
            this.text = text;
            this.bgColor = bgColor;
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(getX(), getY(), getX() + width, getY() + height,
                AnimationHelper.withAlpha(bgColor, 60));
            graphics.fill(getX(), getY(), getX() + width, getY() + 1,
                AnimationHelper.withAlpha(bgColor, 180));
            graphics.fill(getX(), getY() + height - 1, getX() + width, getY() + height,
                AnimationHelper.withAlpha(bgColor, 180));
            graphics.drawString(Minecraft.getInstance().font, text, getX() + 6, getY() + 5, bgColor);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            output.add(NarratedElementType.TITLE, Component.literal(text));
        }
    }

    public static class BLockOverlay extends AbstractWidget {
        private final String lockText;

        public BLockOverlay(int x, int y, int width, int height, String lockText) {
            super(x, y, width, height, Component.literal(lockText));
            this.lockText = lockText;
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(getX(), getY(), getX() + width, getY() + height,
                AnimationHelper.withAlpha(0xFF000000, 153));

            graphics.drawCenteredString(Minecraft.getInstance().font, "\uD83D\uDD12",
                getX() + width / 2, getY() + height / 2 - 12, UITheme.TEXT_MUTED);

            if (lockText != null && !lockText.isEmpty()) {
                graphics.pose().pushPose();
                graphics.pose().translate(getX() + width / 2f, getY() + height / 2f + 4, 0);
                graphics.pose().scale(0.7f, 0.7f, 1.0f);
                graphics.drawCenteredString(Minecraft.getInstance().font, lockText, 0, 0, UITheme.TEXT_SECONDARY);
                graphics.pose().popPose();
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            output.add(NarratedElementType.TITLE, Component.literal("Locked: " + lockText));
        }
    }
}
