package colox.gridmod.ui;

import necesse.engine.localization.message.StaticMessage;
import necesse.gfx.fairType.FairType;
import necesse.gfx.forms.components.FormButton;
import necesse.gfx.forms.components.FormCheckBox;
import necesse.gfx.forms.components.FormContentBox;
import necesse.gfx.forms.components.FormContentIconButton;
import necesse.gfx.forms.components.FormCustomDraw;
import necesse.gfx.forms.components.FormDropdownSelectionButton;
import necesse.gfx.forms.components.FormFairTypeLabel;
import necesse.gfx.forms.components.FormInputSize;
import necesse.gfx.forms.components.FormLabel;
import necesse.gfx.forms.components.FormSlider;
import necesse.gfx.forms.components.FormTextButton;
import necesse.gfx.forms.events.FormEventListener;
import necesse.gfx.forms.events.FormInputEvent;
import necesse.gfx.forms.position.FormFixedPosition;
import necesse.gfx.forms.position.FormPositionContainer;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.ui.ButtonColor;

import colox.gridmod.config.GridConfig;
import colox.gridmod.paint.DefaultBlueprintRegistry;
import colox.gridmod.paint.PaintCategory;
import colox.gridmod.paint.PaintLayerFilter;
import colox.gridmod.paint.PaintState;
import colox.gridmod.paint.SelectionState;

/*
// ===========================================================================
// PaintTab
// - Status text sits on its own line directly under the section titles
//   ("Blueprints" and "Global Blueprints"). No overlap, no clipping.
// - Adds "Show paint overlay" checkbox to toggle visibility independently.
// ===========================================================================
*/
public class PaintTab {

    private static final int M = 12;
    private static final int LINE = 30;

    private static final int CARD_X = M - 4;
    private static final int CARD_PAD_X = 8;
    private static final int CARD_PAD_Y = 8;

    private static final class SectionCard extends FormCustomDraw {
        public SectionCard(int x, int y, int width, int height) {
            super(x, y, width, height);
            this.canBePutOnTopByClick = false;
            this.zIndex = Integer.MIN_VALUE / 2;
        }
        @Override public boolean shouldUseMouseEvents() { return false; }
        @Override public java.util.List<java.awt.Rectangle> getHitboxes() { return java.util.Collections.emptyList(); }
        @Override
        public void draw(necesse.engine.gameLoop.tickManager.TickManager tm,
                         necesse.entity.mobs.PlayerMob perspective,
                         java.awt.Rectangle renderBox) {
            java.awt.Color base = this.getInterfaceStyle().activeElementColor;
            java.awt.Color bg     = new java.awt.Color(base.getRed(), base.getGreen(), base.getBlue(), 28);
            java.awt.Color border = this.getInterfaceStyle().activeTextColor;
            java.awt.Color borderA= new java.awt.Color(border.getRed(), border.getGreen(), border.getBlue(), 100);

            necesse.gfx.Renderer.initQuadDraw(this.width, this.height).color(bg).draw(this.getX(), this.getY());
            necesse.gfx.Renderer.initQuadDraw(this.width, 1).color(borderA).draw(this.getX(), this.getY());
            necesse.gfx.Renderer.initQuadDraw(this.width, 1).color(borderA).draw(this.getX(), this.getY() + this.height - 1);
            necesse.gfx.Renderer.initQuadDraw(1, this.height).color(borderA).draw(this.getX(), this.getY());
            necesse.gfx.Renderer.initQuadDraw(1, this.height).color(borderA).draw(this.getX() + this.width - 1, this.getY());
        }
    }

    private final FormContentBox box;
    private final Runnable openPaintColors;
    private FormDropdownSelectionButton<String> paintCategorySelector;
    private FormDropdownSelectionButton<String> layerFilterDropdown;
    private FormCheckBox eraseManualCheck;

    // Paint basics
    private FormCheckBox  paintEnableCheck;
    private FormCheckBox  paintVisibleCheck; // NEW: show/hide paint overlay
    private FormSlider    brushSlider;
    private FormTextButton paintClearBtn;
    private FormLabel     brushValueLabel;

    private FormLabel blueprintStatusLabel;

    // Selection
    private FormDropdownSelectionButton<String> selModeDropdown;
    private FormLabel selCountLabel;
    private FormTextButton selClearBtn;

    public PaintTab(FormContentBox box, Runnable openPaintColors) {
        this.box = box;
        this.openPaintColors = (openPaintColors == null) ? () -> {} : openPaintColors;
        build();
    }

    private <T extends necesse.gfx.forms.components.FormComponent & FormPositionContainer> T add(T c, int x, int y) {
        c.setPosition(new FormFixedPosition(x, y));
        box.getComponentList().addComponent(c);
        return c;
    }
    private SectionCard addCard(int y, int innerWidth) {
        int w = innerWidth - (CARD_X * 2);
        return add(new SectionCard(CARD_X, y, w, 10), CARD_X, y);
    }
    private void finishCard(SectionCard card, int bottomContentY) {
        int desired = (bottomContentY - card.getY()) + CARD_PAD_Y;
        if (desired < 10) desired = 10;
        card.height = desired;
    }

    private void build() {
        final int innerWidth = box.getMinContentWidth();
        final int cardWidth  = innerWidth - (CARD_X * 2);
        final int usableRow  = cardWidth - (CARD_PAD_X * 2);

        int py = 0;

        // Paint basics
        SectionCard cardPaint = addCard(py, innerWidth);
        int px = CARD_X + CARD_PAD_X;
        int pY = py + CARD_PAD_Y;

        add(new FormLabel("Erase / Blueprint / Selection view options", new FontOptions(16), FormLabel.ALIGN_LEFT, px, pY), px, pY);
        pY += LINE - 6;

        // NEW: visual-only toggle for paint layer visibility
        paintVisibleCheck = add(new FormCheckBox("Show paint overlay", px, pY, GridConfig.paintVisible), px, pY);
        paintVisibleCheck.onClicked(e -> {
            GridConfig.paintVisible = ((FormCheckBox)e.from).checked;
            GridConfig.markDirty(); GridConfig.saveIfDirty();
        });
        pY += LINE;

        // Existing toggle that controls input/painting ability
        paintEnableCheck = add(new FormCheckBox("Paint enabled", px, pY, PaintState.enabled), px, pY);
        paintEnableCheck.onClicked((FormEventListener<FormInputEvent<FormCheckBox>>) e ->
            PaintState.setEnabled(((FormCheckBox)e.from).checked)
        );
        pY += LINE;

        add(new FormLabel("Choose paint", new FontOptions(12), FormLabel.ALIGN_LEFT, px, pY), px, pY);
        pY += LINE - 10;
        int ddW = Math.max(180, Math.min(240, usableRow - 60));
        paintCategorySelector = add(new FormDropdownSelectionButton<>(px, pY - 2, FormInputSize.SIZE_24, ButtonColor.BASE, ddW), px, pY - 2);
        for (PaintCategory cat : PaintCategory.values()) {
            paintCategorySelector.options.add(cat.id(), new StaticMessage(cat.label()));
        }
        PaintCategory initialCategory = GridConfig.getActivePaintCategory();
        paintCategorySelector.setSelected(initialCategory.id(), new StaticMessage(initialCategory.label()));
        paintCategorySelector.onSelected(e -> {
            PaintCategory selected = PaintCategory.byId(e.value);
            GridConfig.setActivePaintCategory(selected);
            refreshEraseFilterDropdown();
        });
        pY += FormInputSize.SIZE_24.height + 10;

        int cogX = px + ddW + 8;
        FormContentIconButton paintCog = add(new FormContentIconButton(cogX, pY - FormInputSize.SIZE_24.height - 6, FormInputSize.SIZE_24, ButtonColor.BASE, box.getInterfaceStyle().config_button_32), cogX, pY - FormInputSize.SIZE_24.height - 6);
        paintCog.onClicked(e -> this.openPaintColors.run());

        int brushTrackWidth = Math.max(160, usableRow - 120);
        brushSlider = add(new FormSlider("Brush size", px, pY, PaintState.getBrush(), 1, 32, brushTrackWidth), px, pY);
        brushSlider.drawValue = false;
        brushSlider.drawValueInPercent = false;

        brushValueLabel = add(new FormLabel("", new FontOptions(14), FormLabel.ALIGN_LEFT, px + brushTrackWidth + 18, pY), px + brushTrackWidth + 18, pY);
        updateBrushValueLabel(brushSlider.getValue());

        brushSlider.onChanged(e -> {
            int size = ((FormSlider)e.from).getValue();
            PaintState.setBrush(size);
            updateBrushValueLabel(size);
        });
        pY += brushSlider.getTotalHeight() + 10;

        paintClearBtn = add(new FormTextButton("Clear paint", px, pY, 160, FormInputSize.SIZE_24, ButtonColor.BASE), px, pY);
        paintClearBtn.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> PaintState.clear());
        pY += FormInputSize.SIZE_24.height + 8;

        add(new FormLabel("Erase by layer", new FontOptions(12), FormLabel.ALIGN_LEFT, px, pY), px, pY);
        pY += LINE - 10;
        int filterDdWidth = Math.max(180, Math.min(240, usableRow - 60));
        layerFilterDropdown = add(new FormDropdownSelectionButton<>(px, pY - 2, FormInputSize.SIZE_24, ButtonColor.BASE, filterDdWidth), px, pY - 2);
        for (PaintLayerFilter filter : PaintLayerFilter.values()) {
            layerFilterDropdown.options.add(filter.id(), new StaticMessage(filter.label()));
        }
        layerFilterDropdown.onSelected(e -> {
            if (!GridConfig.isPaintEraseOverride()) {
                refreshEraseFilterDropdown();
                return;
            }
            PaintLayerFilter selectedFilter = PaintLayerFilter.byId(e.value);
            GridConfig.setPaintEraseFilter(selectedFilter);
            GridConfig.saveIfDirty();
        });
        pY += FormInputSize.SIZE_24.height + 10;

        eraseManualCheck = add(new FormCheckBox("Select erase layer manually", px, pY, GridConfig.isPaintEraseOverride()), px, pY);
        eraseManualCheck.onClicked(e -> {
            GridConfig.setPaintEraseOverride(eraseManualCheck.checked);
            GridConfig.saveIfDirty();
            refreshEraseFilterDropdown();
        });
        pY += LINE;

        refreshEraseFilterDropdown();

        finishCard(cardPaint, pY);
        py = cardPaint.getY() + cardPaint.height + 10;

        // Blueprints (relative)
        SectionCard cardBP = addCard(py, innerWidth);
        px = CARD_X + CARD_PAD_X;
        pY = py + CARD_PAD_Y;

        add(new FormLabel("Blueprints", new FontOptions(16), FormLabel.ALIGN_LEFT, px, pY), px, pY);
        pY += LINE;

        blueprintStatusLabel = add(new FormLabel("", new FontOptions(12), FormLabel.ALIGN_LEFT, px, pY), px, pY);
        updateBlueprintStatusLabel();
        pY += LINE;

        FormFairTypeLabel blueprintInstructions = add(new FormFairTypeLabel("", px, pY), px, pY);
        blueprintInstructions.setFontOptions(new FontOptions(12));
        blueprintInstructions.setTextAlign(FairType.TextAlign.LEFT);
        blueprintInstructions.setText("Manage blueprints through the Quick Palette sidebar.\nUse that panel for save/load, defaults, and import/export.");
        pY += 46;

        finishCard(cardBP, pY + 4);
        py = cardBP.getY() + cardBP.height + 10;

        // Selection
        SectionCard cardSEL = addCard(py, innerWidth);
        px = CARD_X + CARD_PAD_X;
        pY = py + CARD_PAD_Y;

        add(new FormLabel("Selection", new FontOptions(16), FormLabel.ALIGN_LEFT, px, pY), px, pY);
        pY += LINE;

        selModeDropdown = add(new FormDropdownSelectionButton<>(px, pY - 2, FormInputSize.SIZE_24, ButtonColor.BASE, 280), px, pY - 2);
        selModeDropdown.options.add("None",             new StaticMessage("Mode: None"));
        selModeDropdown.options.add("Rect",             new StaticMessage("Mode: Rectangle"));
        selModeDropdown.options.add("Lasso (edge)",     new StaticMessage("Mode: Lasso (stroke edges)"));
        selModeDropdown.options.add("Lasso (edge+fill)",new StaticMessage("Mode: Lasso (stroke + fill)"));
        selModeDropdown.options.add("All",              new StaticMessage("Mode: Select all"));
        selModeDropdown.setSelected("None", new StaticMessage("Mode: None"));
        selModeDropdown.onSelected(e -> {
            String v = e.value == null ? "None" : e.value;
            switch (v) {
                default:
                case "None":               SelectionState.setMode(SelectionState.Mode.NONE);      break;
                case "Rect":               SelectionState.setMode(SelectionState.Mode.RECT);      break;
                case "Lasso (edge)":       SelectionState.setMode(SelectionState.Mode.EDGE);      break;
                case "Lasso (edge+fill)":  SelectionState.setMode(SelectionState.Mode.EDGE_FILL); break;
                case "All":                SelectionState.setMode(SelectionState.Mode.ALL);      break;
            }
            updateSelCountLabel();
        });

        selClearBtn = add(new FormTextButton("Clear", px + 290, pY - 2, 90, FormInputSize.SIZE_24, ButtonColor.BASE), px + 290, pY - 2);
        selClearBtn.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> {
            SelectionState.clear();
            updateSelCountLabel();
        });

        pY += LINE;

        selCountLabel = add(new FormLabel("Selected: 0 cells", new FontOptions(14), FormLabel.ALIGN_LEFT, px, pY), px, pY);
        pY += LINE + 6;

        SelectionState.setChangeListener(this::updateSelCountLabel);
        updateSelCountLabel();

        // Passive updater
        add(new FormCustomDraw(0, 0, 1, 1) {
            @Override public boolean shouldUseMouseEvents() { return false; }
            @Override public void draw(necesse.engine.gameLoop.tickManager.TickManager tm,
                                       necesse.entity.mobs.PlayerMob perspective,
                                       java.awt.Rectangle renderBox) {
                updateSelCountLabel();
                updateBlueprintStatusLabel();
            }
        }, 0, 0);

        finishCard(cardSEL, pY + LINE + 6);
        py = cardSEL.getY() + cardSEL.height + 10;

        box.fitContentBoxToComponents(6);
        updateSelCountLabel();
    }


    private void updateSelCountLabel() {
        if (selCountLabel == null) return;
        int n = SelectionState.getSelectedCount();
        String mode;
        switch (SelectionState.getMode()) {
            case RECT:       mode = "Rect"; break;
            case EDGE:       mode = "Lasso (edge)"; break;
            case EDGE_FILL:  mode = "Lasso (edge+fill)"; break;
            default:         mode = "None";
        }
        selCountLabel.setText("Mode: " + mode + " - Selected: " + n + " cells");
    }

    private void updateBlueprintStatusLabel() {
        if (blueprintStatusLabel == null) return;
        String selection = GridConfig.selectedBlueprint;
        blueprintStatusLabel.setText("Current blueprint: " + describeBlueprint(selection));
    }

    private String describeBlueprint(String key) {
        if (key == null || key.isBlank()) return "(none)";
        if (DefaultBlueprintRegistry.isDefaultKey(key)) {
            DefaultBlueprintRegistry.DefaultBlueprint info = DefaultBlueprintRegistry.find(DefaultBlueprintRegistry.keyToId(key));
            if (info != null) return info.name + " (default)";
            return "(default)";
        }
        return key;
    }

    private void updateBrushValueLabel(int size) {
        if (brushValueLabel != null) brushValueLabel.setText(size + "x" + size);
    }

    private void refreshEraseFilterDropdown() {
        if (layerFilterDropdown == null) return;
        boolean manual = GridConfig.isPaintEraseOverride();
        if (eraseManualCheck != null) eraseManualCheck.checked = manual;
        layerFilterDropdown.setActive(manual);
        PaintLayerFilter displayFilter = manual
                ? GridConfig.getPaintEraseFilter()
                : GridConfig.getEffectivePaintEraseFilter();
        layerFilterDropdown.setSelected(displayFilter.id(), new StaticMessage(displayFilter.label()));
    }

}
