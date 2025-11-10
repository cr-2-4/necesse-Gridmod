package colox.gridmod.ui;

import java.util.Arrays;
import java.util.function.Consumer;

import necesse.engine.localization.message.StaticMessage;
import necesse.gfx.forms.components.*;
import necesse.gfx.forms.position.FormFixedPosition;
import necesse.gfx.forms.components.FormContentIconButton;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.ui.ButtonColor;

import colox.gridmod.config.GridConfig;
import colox.gridmod.overlay.GridToggle;

final class GridTab {

    private static final int M = 12;
    private static final int LINE = 30;
    private static final int CARD_X = M - 4;
    private static final int CARD_PAD_X = 8;
    private static final int CARD_PAD_Y = 8;

    // Controls
    private FormCheckBox gridEnableCheck;
    private FormSlider   uiOpacitySlider;
    private FormSlider   alphaSlider;
    private FormCheckBox chunkCheck;
    private FormDropdownSelectionButton<String> chunkSpan;
    private FormSlider   chunkThickSlider;
    private FormSlider   chunkAlphaSlider;

    private FormCheckBox subChunkCheck;
    private FormSlider   subThickSlider;
    private FormSlider   subAlphaSlider;

    private final Consumer<Float> onUiOpacityChanged;
    private final Runnable openGridColors;

    GridTab(FormContentBox box, Consumer<Float> onUiOpacityChanged, Runnable openGridColors) {
        this.onUiOpacityChanged = (onUiOpacityChanged == null) ? v -> {} : onUiOpacityChanged;
        this.openGridColors = (openGridColors == null) ? () -> {} : openGridColors;
        build(box);
    }

    private <T extends FormComponent & necesse.gfx.forms.position.FormPositionContainer>
    T add(FormContentBox box, T comp, int x, int y) {
        comp.setPosition(new FormFixedPosition(x, y));
        box.getComponentList().addComponent(comp);
        return comp;
    }

    private void build(FormContentBox box) {
        final int innerWidth = box.getMinContentWidth();
        int y = 0;

        // Card 1: Base grid
        UiParts.SectionCard cardBase = addCard(box, y, innerWidth);
        int gx = CARD_X + CARD_PAD_X;
        int gy = y + CARD_PAD_Y;

        add(box, new FormLabel("Grid", new FontOptions(16), FormLabel.ALIGN_LEFT, gx, gy), gx, gy);
        gy += LINE - 6;

        gridEnableCheck = add(box, new FormCheckBox("Enable grid", gx, gy, GridToggle.isEnabled()), gx, gy);
        gridEnableCheck.onClicked(e -> GridToggle.setEnabled(((FormCheckBox)e.from).checked));
        // Inline cog to open Grid Colors
        int cogX = gx + 220;
        FormContentIconButton gridCog = add(box, new FormContentIconButton(cogX, gy - 4, FormInputSize.SIZE_24, ButtonColor.BASE, box.getInterfaceStyle().config_button_32), cogX, gy - 4);
        gridCog.onClicked(e -> this.openGridColors.run());
        gy += LINE;

        int sliderWidth = Math.max(180, innerWidth - (CARD_X * 2) - (gx - CARD_X));

        // UI opacity (moved here from GridUIForm)
        uiOpacitySlider = add(box, new FormSlider("UI opacity", gx, gy,
                Math.round(GridConfig.uiOpacity * 100f), 20, 100, sliderWidth), gx, gy);
        uiOpacitySlider.drawValue = true;
        uiOpacitySlider.drawValueInPercent = true;
        uiOpacitySlider.onChanged(e -> {
            int v = ((FormSlider)e.from).getValue();
            GridConfig.uiOpacity = Math.max(0.2f, Math.min(1f, v / 100f));
            GridConfig.markDirty(); GridConfig.saveIfDirty();
            onUiOpacityChanged.accept(GridConfig.uiOpacity); // update form background immediately
        });
        gy += uiOpacitySlider.getTotalHeight() + 8;

        alphaSlider = add(box, new FormSlider("Grid alpha", gx, gy,
                Math.round(GridConfig.lineAlpha * 100f), 0, 100, sliderWidth), gx, gy);
        alphaSlider.onChanged(e -> {
            int v = ((FormSlider)e.from).getValue();
            GridConfig.lineAlpha = clamp01(v / 100f);
            GridConfig.markDirty(); GridConfig.saveIfDirty();
        });
        gy += alphaSlider.getTotalHeight() + 10;

        UiParts.finishCard(cardBase, gy);
        y = cardBase.getY() + cardBase.height + 10;

        // Card 2: Chunk lines
        UiParts.SectionCard cardChunk = addCard(box, y, innerWidth);
        gx = CARD_X + CARD_PAD_X;
        gy = y + CARD_PAD_Y;

        add(box, new FormLabel("Chunk lines", new FontOptions(14), FormLabel.ALIGN_LEFT, gx, gy), gx, gy);
        gy += LINE - 12;

        chunkCheck = add(box, new FormCheckBox("Show chunk lines", gx, gy, GridConfig.showChunkLines), gx, gy);
        chunkCheck.onClicked(e -> {
            GridConfig.showChunkLines = ((FormCheckBox)e.from).checked;
            GridConfig.markDirty(); GridConfig.saveIfDirty();
        });

        int ddW = 160;
        chunkSpan = add(box, new FormDropdownSelectionButton<>(gx + 220, gy - 2, FormInputSize.SIZE_24, ButtonColor.BASE, ddW), gx + 220, gy - 2);
        chunkSpan.options.add("8",  new StaticMessage("Span: 8"));
        chunkSpan.options.add("16", new StaticMessage("Span: 16"));
        chunkSpan.options.add("32", new StaticMessage("Span: 32"));
        chunkSpan.options.add("64", new StaticMessage("Span: 64"));
        String curSpan = Integer.toString(GridConfig.chunkSpanTiles);
        if (!Arrays.asList("8","16","32","64").contains(curSpan)) curSpan = "16";
        chunkSpan.setSelected(curSpan, new StaticMessage("Span: " + curSpan));
        chunkSpan.onSelected(e -> {
            try {
                int v = Integer.parseInt(e.value);
                GridConfig.chunkSpanTiles = v;
                GridConfig.subChunkSpanTiles = Math.max(2, v / 4);
                GridConfig.markDirty(); GridConfig.saveIfDirty();
            } catch (NumberFormatException ignored) {}
        });
        gy += LINE;

        chunkThickSlider = add(box, new FormSlider("Chunk thickness", gx, gy, GridConfig.chunkThickness, 1, 4, sliderWidth), gx, gy);
        chunkThickSlider.drawValue = true;
        chunkThickSlider.drawValueInPercent = false;
        chunkThickSlider.onChanged(e -> {
            GridConfig.chunkThickness = ((FormSlider)e.from).getValue();
            GridConfig.markDirty(); GridConfig.saveIfDirty();
        });
        gy += chunkThickSlider.getTotalHeight() + 8;

        chunkAlphaSlider = add(box, new FormSlider("Chunk alpha", gx, gy, Math.round(GridConfig.chunkAlpha * 100f), 0, 100, sliderWidth), gx, gy);
        chunkAlphaSlider.onChanged(e -> {
            GridConfig.chunkAlpha = clamp01(((FormSlider)e.from).getValue() / 100f);
            GridConfig.markDirty(); GridConfig.saveIfDirty();
        });
        gy += chunkAlphaSlider.getTotalHeight() + 10;

        UiParts.finishCard(cardChunk, gy);
        y = cardChunk.getY() + cardChunk.height + 10;

        // Card 3: Sub-chunk lines
        UiParts.SectionCard cardSub = addCard(box, y, innerWidth);
        gx = CARD_X + CARD_PAD_X;
        gy = y + CARD_PAD_Y;

        add(box, new FormLabel("Sub-chunk lines", new FontOptions(14), FormLabel.ALIGN_LEFT, gx, gy), gx, gy);
        gy += LINE - 12;

        subChunkCheck = add(box, new FormCheckBox("Show sub-chunk lines", gx, gy, GridConfig.showSubChunkLines), gx, gy);
        subChunkCheck.onClicked(e -> {
            GridConfig.showSubChunkLines = ((FormCheckBox)e.from).checked;
            GridConfig.markDirty(); GridConfig.saveIfDirty();
        });
        gy += LINE;

        subThickSlider = add(box, new FormSlider("Sub-chunk thickness", gx, gy, GridConfig.subChunkThickness, 1, 3, sliderWidth), gx, gy);
        subThickSlider.drawValue = true;
        subThickSlider.drawValueInPercent = false;
        subThickSlider.onChanged(e -> {
            GridConfig.subChunkThickness = ((FormSlider)e.from).getValue();
            GridConfig.markDirty(); GridConfig.saveIfDirty();
        });
        gy += subThickSlider.getTotalHeight() + 8;

        subAlphaSlider = add(box, new FormSlider("Sub-chunk alpha", gx, gy, Math.round(GridConfig.subChunkAlpha * 100f), 0, 100, sliderWidth), gx, gy);
        subAlphaSlider.onChanged(e -> {
            GridConfig.subChunkAlpha = clamp01(((FormSlider)e.from).getValue() / 100f);
            GridConfig.markDirty(); GridConfig.saveIfDirty();
        });
        gy += subAlphaSlider.getTotalHeight() + 6;

        UiParts.finishCard(cardSub, gy);
    }

    private UiParts.SectionCard addCard(FormContentBox box, int y, int innerWidth) {
        int w = innerWidth - (CARD_X * 2);
        return add(box, new UiParts.SectionCard(CARD_X, y, w, 10), CARD_X, y);
    }

    private static float clamp01(float v) { return v < 0f ? 0f : (v > 1f ? 1f : v); }
}
