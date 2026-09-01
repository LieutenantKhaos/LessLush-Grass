package com.github.kryvii.lushgrass.client.config;

import com.github.kryvii.lushgrass.client.event.ClientConfigEvents;
import com.github.kryvii.lushgrass.config.ClientConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

final class VisualsConfigScreen extends Screen {
    private static final Component TITLE = Component.translatable("lush_grass.configuration.visuals");
    private static final Component FULL_COVERAGE =
            Component.translatable("lush_grass.configuration.visuals.full_grass_block_coverage");
    private static final Component FULL_COVERAGE_TOOLTIP =
            Component.translatable("lush_grass.configuration.visuals.full_grass_block_coverage.tooltip");
    private static final Component GRASS_TUFTS =
            Component.translatable("lush_grass.configuration.visuals.render_grass_tufts");
    private static final Component GRASS_TUFTS_TOOLTIP =
            Component.translatable("lush_grass.configuration.visuals.render_grass_tufts.tooltip");
    private static final Component SUPPRESS_DEVELOPED =
            Component.translatable("lush_grass.configuration.developed.suppress_tufts");
    private static final Component SUPPRESS_DEVELOPED_TOOLTIP =
            Component.translatable("lush_grass.configuration.developed.suppress_tufts.tooltip");
    private static final Component RADIUS =
            Component.translatable("lush_grass.configuration.developed.radius");
    private static final Component RADIUS_TOOLTIP =
            Component.translatable("lush_grass.configuration.developed.radius.tooltip");
    private static final Component RANDOM_ORIENTATION =
            Component.translatable("lush_grass.configuration.visuals.randomize_tuft_orientation");
    private static final Component RANDOM_ORIENTATION_TOOLTIP =
            Component.translatable("lush_grass.configuration.visuals.randomize_tuft_orientation.tooltip");
    private static final Component PIXEL_PERFECT =
            Component.translatable("lush_grass.configuration.visuals.pixel_perfect_tufts");
    private static final Component PIXEL_PERFECT_TOOLTIP =
            Component.translatable("lush_grass.configuration.visuals.pixel_perfect_tufts.tooltip");

    private final Screen parent;
    private boolean fullCoverage;
    private boolean grassTufts;
    private boolean suppressDeveloped;
    private int radius;
    private boolean randomOrientation;
    private boolean pixelPerfectTufts;

    VisualsConfigScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
        this.fullCoverage = ClientConfig.fullGrassBlockCoverage();
        this.grassTufts = ClientConfig.renderGrassTufts();
        this.suppressDeveloped = ClientConfig.suppressTuftsInDevelopedAreas();
        this.radius = ClientConfig.developedAreaRadius();
        this.randomOrientation = ClientConfig.randomizeTuftOrientation();
        this.pixelPerfectTufts = ClientConfig.pixelPerfectTufts();
    }

    @Override
    protected void init() {
        int buttonWidth = Math.min(310, this.width - 40);
        int left = (this.width - buttonWidth) / 2;
        int firstRow = this.height / 2 - 82;

        this.addRenderableWidget(CycleButton.onOffBuilder(this.fullCoverage)
                .withTooltip(value -> Tooltip.create(FULL_COVERAGE_TOOLTIP))
                .create(left, firstRow, buttonWidth, 20, FULL_COVERAGE,
                        (button, value) -> this.fullCoverage = value));
        this.addRenderableWidget(CycleButton.onOffBuilder(this.grassTufts)
                .withTooltip(value -> Tooltip.create(GRASS_TUFTS_TOOLTIP))
                .create(left, firstRow + 24, buttonWidth, 20, GRASS_TUFTS,
                        (button, value) -> this.grassTufts = value));
        this.addRenderableWidget(CycleButton.onOffBuilder(this.suppressDeveloped)
                .withTooltip(value -> Tooltip.create(SUPPRESS_DEVELOPED_TOOLTIP))
                .create(left, firstRow + 48, buttonWidth, 20, SUPPRESS_DEVELOPED,
                        (button, value) -> this.suppressDeveloped = value));

        Button radiusButton = Button.builder(radiusLabel(), button -> {
                    this.radius = (this.radius + 1) % 17;
                    button.setMessage(radiusLabel());
                })
                .bounds(left, firstRow + 72, buttonWidth, 20)
                .build();
        this.addRenderableWidget(CycleButton.onOffBuilder(this.randomOrientation)
                .withTooltip(value -> Tooltip.create(RANDOM_ORIENTATION_TOOLTIP))
                .create(left, firstRow + 96, buttonWidth, 20, RANDOM_ORIENTATION,
                        (button, value) -> this.randomOrientation = value));
        radiusButton.setTooltip(Tooltip.create(RADIUS_TOOLTIP));
        this.addRenderableWidget(radiusButton);

        this.addRenderableWidget(CycleButton.onOffBuilder(this.pixelPerfectTufts)
                .withTooltip(value -> Tooltip.create(PIXEL_PERFECT_TOOLTIP))
                .create(left, firstRow + 120, buttonWidth, 20, PIXEL_PERFECT,
                        (button, value) -> this.pixelPerfectTufts = value));

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .bounds(this.width / 2 - 100, this.height - 28, 200, 20)
                .build());
    }

    private Component radiusLabel() {
        return Component.translatable("lush_grass.configuration.developed.radius", this.radius);
    }

    @Override
    public void onClose() {
        if (ClientConfig.update(this.fullCoverage, this.grassTufts, this.suppressDeveloped, this.radius, this.randomOrientation, this.pixelPerfectTufts)) {
            ClientConfig.save();
            ClientConfigEvents.refreshRenderer();
        }
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(this.parent);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(this.font, this.title, this.width / 2, 20, 0xFFFFFFFF);
    }
}
