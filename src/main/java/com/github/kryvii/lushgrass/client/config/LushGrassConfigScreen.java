package com.github.kryvii.lushgrass.client.config;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public final class LushGrassConfigScreen extends Screen {
    private static final Component MOD_NAME = Component.translatable("lush_grass.configuration.title");
    private static final Component TITLE = Component.translatable(
            "lush_grass.configuration.client.title",
            MOD_NAME
    );
    private static final Component VISUALS = Component.translatable("lush_grass.configuration.visuals");
    private static final Component EDIT = Component.translatable("lush_grass.configuration.visuals.button");
    private static final Component VISUALS_TOOLTIP =
            Component.translatable("lush_grass.configuration.visuals.tooltip");

    private static final int ROW_HEIGHT = 20;
    private static final int MAX_ROW_WIDTH = 310;
    private static final int EDIT_BUTTON_WIDTH = 80;

    private final Screen parent;
    private int rowLeft;
    private int rowTop;

    public LushGrassConfigScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        int rowWidth = Math.min(MAX_ROW_WIDTH, this.width - 40);
        this.rowLeft = (this.width - rowWidth) / 2;
        this.rowTop = this.height / 2 - ROW_HEIGHT / 2;

        Button editButton = Button.builder(EDIT, button -> {
                    if (this.minecraft != null) {
                        this.minecraft.gui.setScreen(new VisualsConfigScreen(this));
                    }
                })
                .bounds(this.rowLeft + rowWidth - EDIT_BUTTON_WIDTH, this.rowTop, EDIT_BUTTON_WIDTH, ROW_HEIGHT)
                .build();
        editButton.setTooltip(Tooltip.create(VISUALS_TOOLTIP));
        this.addRenderableWidget(editButton);
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .bounds(this.width / 2 - 100, this.height - 28, 200, ROW_HEIGHT)
                .build());
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(this.parent);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(this.font, this.title, this.width / 2, 20, 0xFFFFFFFF);
        graphics.text(
                this.font,
                VISUALS,
                this.rowLeft,
                this.rowTop + (ROW_HEIGHT - this.font.lineHeight) / 2,
                0xFFFFFFFF
        );
    }
}
