package colox.gridmod.ui;

import java.util.function.Consumer;

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
    }

    private UiParts.SectionCard addCard(FormContentBox box, int y, int innerWidth) {
        int w = innerWidth - (CARD_X * 2);
        return add(box, new UiParts.SectionCard(CARD_X, y, w, 10), CARD_X, y);
    }

    private static float clamp01(float v) { return v < 0f ? 0f : (v > 1f ? 1f : v); }
}
