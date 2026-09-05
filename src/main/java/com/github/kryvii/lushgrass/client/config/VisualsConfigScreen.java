package com.github.kryvii.lushgrass.client.config;

import com.github.kryvii.lushgrass.client.event.ClientConfigEvents;
import com.github.kryvii.lushgrass.config.ClientConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

final class VisualsConfigScreen extends Screen {
    private static final Component TITLE = Component.translatable("lush_grass.configuration.visuals");

    private static final Component SECTION_GRASS_APPEARANCE =
            Component.translatable("lush_grass.configuration.visuals.section.grass_appearance");
    private static final Component SECTION_DEVELOPED_AREAS =
            Component.translatable("lush_grass.configuration.visuals.section.developed_areas");

    private static final Component FULL_COVERAGE =
            Component.translatable("lush_grass.configuration.visuals.full_grass_block_coverage");
    private static final Component FULL_COVERAGE_TOOLTIP =
            Component.translatable("lush_grass.configuration.visuals.full_grass_block_coverage.tooltip");

    private static final Component GRASS_DENSITY =
            Component.translatable("lush_grass.configuration.visuals.grass_density");
    private static final Component GRASS_DENSITY_TOOLTIP =
            Component.translatable("lush_grass.configuration.visuals.grass_density.tooltip");

    private static final Component TUFT_VARIATION =
            Component.translatable("lush_grass.configuration.visuals.tuft_variation");
    private static final Component TUFT_VARIATION_TOOLTIP =
            Component.translatable("lush_grass.configuration.visuals.tuft_variation.tooltip");

    private static final Component SUPPRESS_DEVELOPED =
            Component.translatable("lush_grass.configuration.developed.suppress_tufts");
    private static final Component SUPPRESS_DEVELOPED_TOOLTIP =
            Component.translatable("lush_grass.configuration.developed.suppress_tufts.tooltip");

    private static final Component RADIUS =
            Component.translatable("lush_grass.configuration.developed.radius");
    private static final Component RADIUS_TOOLTIP =
            Component.translatable("lush_grass.configuration.developed.radius.tooltip");

    private final Screen parent;
    private boolean fullCoverage;
    private int grassDensity;
    private int tuftVariation;
    private boolean suppressDeveloped;
    private int radius;

    VisualsConfigScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
        this.fullCoverage = ClientConfig.fullGrassBlockCoverage();
        this.grassDensity = ClientConfig.grassDensity();
        this.tuftVariation = ClientConfig.tuftVariation();
        this.suppressDeveloped = ClientConfig.suppressTuftsInDevelopedAreas();
        this.radius = ClientConfig.developedAreaRadius();
    }

    @Override
    protected void init() {
        int buttonWidth = Math.min(310, this.width - 40);
        int left = (this.width - buttonWidth) / 2;
        int firstRow = this.height / 2 - 94;

        this.addRenderableWidget(CycleButton.onOffBuilder(this.fullCoverage)
                .withTooltip(value -> Tooltip.create(FULL_COVERAGE_TOOLTIP))
                .create(left, firstRow, buttonWidth, 20, FULL_COVERAGE,
                        (button, value) -> this.fullCoverage = value));

        AbstractSliderButton densitySlider = new AbstractSliderButton(
                left,
                firstRow + 24,
                buttonWidth,
                20,
                grassDensityLabel(),
                this.grassDensity / 4.0D
        ) {
            @Override
            protected void updateMessage() {
                setMessage(grassDensityLabel());
            }

            @Override
            protected void applyValue() {
                grassDensity = Math.max(0, Math.min(4, (int) Math.round(this.value * 4.0D)));
                this.value = grassDensity / 4.0D;
            }
        };
        densitySlider.setTooltip(Tooltip.create(GRASS_DENSITY_TOOLTIP));
        this.addRenderableWidget(densitySlider);

        AbstractSliderButton variationSlider = new AbstractSliderButton(
                left,
                firstRow + 48,
                buttonWidth,
                20,
                tuftVariationLabel(),
                this.tuftVariation / 2.0D
        ) {
            @Override
            protected void updateMessage() {
                setMessage(tuftVariationLabel());
            }

            @Override
            protected void applyValue() {
                tuftVariation = Math.max(0, Math.min(2, (int) Math.round(this.value * 2.0D)));
                this.value = tuftVariation / 2.0D;
            }
        };
        variationSlider.setTooltip(Tooltip.create(TUFT_VARIATION_TOOLTIP));
        this.addRenderableWidget(variationSlider);

        this.addRenderableWidget(CycleButton.onOffBuilder(this.suppressDeveloped)
                .withTooltip(value -> Tooltip.create(SUPPRESS_DEVELOPED_TOOLTIP))
                .create(left, firstRow + 120, buttonWidth, 20, SUPPRESS_DEVELOPED,
                        (button, value) -> this.suppressDeveloped = value));

        AbstractSliderButton radiusSlider = new AbstractSliderButton(
                left,
                firstRow + 144,
                buttonWidth,
                20,
                radiusLabel(),
                this.radius / 16.0D
        ) {
            @Override
            protected void updateMessage() {
                setMessage(radiusLabel());
            }

            @Override
            protected void applyValue() {
                radius = (int) Math.round(this.value * 16.0D);
            }
        };
        radiusSlider.setTooltip(Tooltip.create(RADIUS_TOOLTIP));
        this.addRenderableWidget(radiusSlider);

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .bounds(this.width / 2 - 100, this.height - 28, 200, 20)
                .build());
    }

    private Component grassDensityLabel() {
        return Component.translatable(
                "lush_grass.configuration.visuals.grass_density",
                Component.translatable("lush_grass.configuration.visuals.grass_density.value." + this.grassDensity)
        );
    }

    private Component tuftVariationLabel() {
        return Component.translatable(
                "lush_grass.configuration.visuals.tuft_variation",
                Component.translatable("lush_grass.configuration.visuals.tuft_variation.value." + this.tuftVariation)
        );
    }

    private Component radiusLabel() {
        return Component.translatable("lush_grass.configuration.developed.radius", this.radius);
    }

    @Override
    public void onClose() {
        if (ClientConfig.update(
                this.fullCoverage,
                this.grassDensity,
                this.tuftVariation,
                this.suppressDeveloped,
                this.radius
        )) {
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

        int firstRow = this.height / 2 - 94;

        graphics.centeredText(this.font, this.title, this.width / 2, 20, 0xFFFFFFFF);
        graphics.centeredText(this.font, SECTION_GRASS_APPEARANCE, this.width / 2, firstRow - 24, 0xFFFFFFFF);
        graphics.centeredText(this.font, SECTION_DEVELOPED_AREAS, this.width / 2, firstRow + 96, 0xFFFFFFFF);
    }
}
