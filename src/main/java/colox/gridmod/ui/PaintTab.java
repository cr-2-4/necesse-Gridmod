package colox.gridmod.ui;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import necesse.engine.input.Control;
import necesse.engine.localization.message.StaticMessage;
import necesse.gfx.fairType.FairType;
import necesse.gfx.fairType.TypeParsers;
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
import necesse.gfx.forms.components.FormTextInput;
import necesse.gfx.forms.events.FormEventListener;
import necesse.gfx.forms.events.FormInputEvent;
import necesse.gfx.forms.position.FormFixedPosition;
import necesse.gfx.forms.position.FormPositionContainer;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.ui.ButtonColor;

import colox.gridmod.config.GridConfig;
import colox.gridmod.input.GridKeybinds;
import colox.gridmod.paint.BlueprintPlacement;
import colox.gridmod.paint.PaintBlueprints;
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

    // width reserved for inline status labels (right side)
    private static final int STATUS_W = 300;

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

    // Relative Blueprints
    private FormDropdownSelectionButton<String> bpDropdown;
    private FormTextButton bpSaveBtn, bpLoadBtn, bpRefreshBtn;
    private FormLabel bpInlineStatus;

    // Transform + state
    private FormTextButton rotateCwBtn, rotateCcwBtn, flipBtn;
    private FormLabel placementStateLabel;

    // Selection
    private FormDropdownSelectionButton<String> selModeDropdown;
    private FormLabel selCountLabel;
    private FormTextButton selClearBtn;

    // Global Blueprints
    private FormDropdownSelectionButton<String> gbpDropdown;
    private FormTextButton gbpNewBtn, gbpSaveBtn, gbpLoadBtn, gbpRefreshBtn, gbpDeleteBtn;
    private FormTextInput gbpRenameInput;
    private FormTextButton gbpRenameApplyBtn;
    private FormLabel gbpInlineStatus;

    // Misc status (bottom of Global card)
    private FormLabel statusLine;
    private long statusClearAtMs = 0L;

    // overwrite arming
    private String overwriteArmedName = null;
    private long overwriteArmUntilMs = 0L;
    private String overwriteArmedGlobalName = null;
    private long overwriteGlobalArmUntilMs = 0L;

    // auto-clear timers for inline statuses
    private long bpStatusClearAtMs = 0L;
    private long gbpStatusClearAtMs = 0L;

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

        // status line directly under "Blueprints", aligned right
        int bpStatusY = pY + 2;
        int bpStatusX = px + usableRow - STATUS_W;
        bpInlineStatus = add(new FormLabel("", new FontOptions(12), FormLabel.ALIGN_LEFT, bpStatusX, bpStatusY, STATUS_W), bpStatusX, bpStatusY);

        pY += 18; // small gap below the status line

        int rowY = pY;
        int bw   = 90;
        int gap  = 6;

        int dropdownWidth = Math.max(200, usableRow - (gap + 3 * bw));

        bpDropdown = add(new FormDropdownSelectionButton<>(px, rowY, FormInputSize.SIZE_24, ButtonColor.BASE, dropdownWidth), px, rowY);

        int bx = px + dropdownWidth + gap;
        bpSaveBtn    = add(new FormTextButton("Save",    bx,                 rowY, bw, FormInputSize.SIZE_24, ButtonColor.BASE), bx, rowY);
        bpLoadBtn    = add(new FormTextButton("Load",    bx + bw + gap,      rowY, bw, FormInputSize.SIZE_24, ButtonColor.BASE), bx + bw + gap, rowY);
        bpRefreshBtn = add(new FormTextButton("Refresh", bx + 2*(bw + gap),  rowY, bw, FormInputSize.SIZE_24, ButtonColor.BASE), bx + 2*(bw + gap), rowY);

        bpDropdown.onSelected(e -> {
            GridConfig.selectedBlueprint = e.value;
            GridConfig.markDirty();
            GridConfig.saveIfDirty();
        });

        bpSaveBtn.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> {
            String name = currentBP();
            if (name.isBlank()) return;

            long now = nowMs();
            boolean exists = PaintBlueprints.exists(name);
            if (exists) {
                if (!name.equals(overwriteArmedName) || now > overwriteArmUntilMs) {
                    overwriteArmedName = name;
                    overwriteArmUntilMs = now + 5000L;
                    setBpStatus("Overwrite \"" + name + "\"? Click Save again.");
                    return;
                }
            }
            PaintBlueprints.saveBlueprint(name);
            refreshBlueprints();
            setBpStatus("Saved \"" + name + "\"");
            overwriteArmedName = null;
            overwriteArmUntilMs = 0L;
        });

        bpLoadBtn.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> {
            String name = currentBP();
            if (name.isBlank()) return;
            List<BlueprintPlacement.BlueprintTile> rel = PaintBlueprints.loadRelative(name);
            if (!rel.isEmpty()) {
                BlueprintPlacement.begin(rel);
                setBpStatus("Loaded \"" + name + "\" (" + rel.size() + " cells)");
                updateTransformButtonsActive();
            } else {
                setBpStatus("Loaded \"" + name + "\" (0 cells)");
            }
        });

        bpRefreshBtn.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> {
            refreshBlueprints();
            setBpStatus("Refreshed list");
        });

        // Transform buttons row
        int tRowY = rowY + LINE + 8;
        rotateCwBtn  = add(new FormTextButton("Rotate CW",  px,                tRowY, 110, FormInputSize.SIZE_24, ButtonColor.BASE), px,                tRowY);
        rotateCcwBtn = add(new FormTextButton("Rotate CCW", px + 116,          tRowY, 110, FormInputSize.SIZE_24, ButtonColor.BASE), px + 116,          tRowY);
        flipBtn      = add(new FormTextButton("Flip",       px + 116 + 116,    tRowY, 80,  FormInputSize.SIZE_24, ButtonColor.BASE), px + 232,          tRowY);

        rotateCwBtn.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> {
            if (BlueprintPlacement.active) BlueprintPlacement.rotateCW();
        });
        rotateCcwBtn.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> {
            if (BlueprintPlacement.active) BlueprintPlacement.rotateCCW();
        });
        flipBtn.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> {
            if (BlueprintPlacement.active) BlueprintPlacement.toggleFlip();
        });

        placementStateLabel = add(new FormLabel("", new FontOptions(12), FormLabel.ALIGN_LEFT, px, tRowY + LINE), px, tRowY + LINE);

        int keyHelpY = tRowY + LINE + 18;
        String rotateCwMarkup  = getInputMarkup(GridKeybinds.BP_ROTATE_CW);
        String rotateCcwMarkup = getInputMarkup(GridKeybinds.BP_ROTATE_CCW);
        String flipMarkup      = getInputMarkup(GridKeybinds.BP_FLIP);

        String keyLines =
              "Rotate CW:  " + rotateCwMarkup  + "\n"
            + "Rotate CCW: " + rotateCcwMarkup + "\n"
            + "Flip:       " + flipMarkup;

        FormFairTypeLabel keyHelp = add(new FormFairTypeLabel("", px, keyHelpY), px, keyHelpY);
        keyHelp.setFontOptions(new FontOptions(13));
        keyHelp.setParsers(TypeParsers.InputIcon(new FontOptions(13)), TypeParsers.GAME_COLOR);
        keyHelp.setTextAlign(FairType.TextAlign.LEFT);
        keyHelp.setText(keyLines);

        finishCard(cardBP, keyHelpY + (int)(LINE * 2.0f));
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
                updateTransformButtonsActive();
                long now = nowMs();
                if (statusClearAtMs > 0 && now > statusClearAtMs) {
                    if (statusLine != null) statusLine.setText("");
                    statusClearAtMs = 0L;
                }
                if (bpStatusClearAtMs > 0 && now > bpStatusClearAtMs) {
                    if (bpInlineStatus != null) bpInlineStatus.setText("");
                    bpStatusClearAtMs = 0L;
                }
                if (gbpStatusClearAtMs > 0 && now > gbpStatusClearAtMs) {
                    if (gbpInlineStatus != null) gbpInlineStatus.setText("");
                    gbpStatusClearAtMs = 0L;
                }
                if (overwriteArmUntilMs > 0 && now > overwriteArmUntilMs) {
                    overwriteArmedName = null;
                    overwriteArmUntilMs = 0L;
                }
                if (overwriteGlobalArmUntilMs > 0 && now > overwriteGlobalArmUntilMs) {
                    overwriteArmedGlobalName = null;
                    overwriteGlobalArmUntilMs = 0L;
                }
            }
        }, 0, 0);

        finishCard(cardSEL, pY + LINE + 6);
        py = cardSEL.getY() + cardSEL.height + 10;

        // Global Blueprints
        SectionCard cardGBP = addCard(py, innerWidth);
        px = CARD_X + CARD_PAD_X;
        pY = py + CARD_PAD_Y;

        add(new FormLabel("Global Blueprints", new FontOptions(16), FormLabel.ALIGN_LEFT, px, pY), px, pY);

        // status line under the title, aligned right
        int gbpStatusY = pY + 2;
        int gbpStatusX = px + usableRow - STATUS_W;
        gbpInlineStatus = add(new FormLabel("", new FontOptions(12), FormLabel.ALIGN_LEFT, gbpStatusX, gbpStatusY, STATUS_W), gbpStatusX, gbpStatusY);

        pY += 18;

        int growY = pY;
        int gbw  = 78;
        int ggap = 6;

        int gDropdownWidth = Math.max(160, usableRow - (ggap + 5 * gbw));

        gbpDropdown = add(new FormDropdownSelectionButton<>(px, growY, FormInputSize.SIZE_24, ButtonColor.BASE, gDropdownWidth), px, growY);

        int gbx = px + gDropdownWidth + ggap;
        gbpNewBtn     = add(new FormTextButton("New",     gbx,                 growY, gbw, FormInputSize.SIZE_24, ButtonColor.BASE), gbx, growY);
        gbpSaveBtn    = add(new FormTextButton("Save",    gbx + gbw + 3,       growY, gbw, FormInputSize.SIZE_24, ButtonColor.BASE), gbx + gbw + 3, growY);
        gbpLoadBtn    = add(new FormTextButton("Load",    gbx + 2*gbw + 6,     growY, gbw, FormInputSize.SIZE_24, ButtonColor.BASE), gbx + 2*gbw + 6, growY);
        gbpRefreshBtn = add(new FormTextButton("Refresh", gbx + 3*gbw + 9,     growY, gbw, FormInputSize.SIZE_24, ButtonColor.BASE), gbx + 3*gbw + 9, growY);
        gbpDeleteBtn  = add(new FormTextButton("Delete",  gbx + 4*gbw + 12,    growY, gbw, FormInputSize.SIZE_24, ButtonColor.BASE), gbx + 4*gbw + 12, growY);

        gbpDropdown.onSelected(e -> {
            GridConfig.selectedGlobalBlueprint = e.value;
            GridConfig.markDirty();
            GridConfig.saveIfDirty();
            gbpRenameInput.setText(currentGlobalBP());
        });

        gbpNewBtn.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> {
            String newName = proposeNewGlobalName();
            boolean created = PaintBlueprints.createGlobalEmpty(newName);
            GridConfig.selectedGlobalBlueprint = newName;
            GridConfig.markDirty(); GridConfig.saveIfDirty();
            refreshGlobalBlueprints();
            gbpRenameInput.setText(newName);
            setGbpStatus(created ? ("Created global \"" + newName + "\"") : ("Selected global \"" + newName + "\""));
        });

        gbpSaveBtn.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> {
            String name = currentGlobalBP();
            if (name.isBlank()) return;

            long now = nowMs();
            boolean exists = PaintBlueprints.globalExists(name);
            if (exists) {
                if (!name.equals(overwriteArmedGlobalName) || now > overwriteGlobalArmUntilMs) {
                    overwriteArmedGlobalName = name;
                    overwriteGlobalArmUntilMs = now + 5000L;
                    setGbpStatus("Overwrite global \"" + name + "\"? Click Save again.");
                    return;
                }
            }
            int n = PaintBlueprints.saveGlobal(name);
            refreshGlobalBlueprints();
            setGbpStatus("Saved global \"" + name + "\" (" + n + " cells)");
            overwriteArmedGlobalName = null;
            overwriteGlobalArmUntilMs = 0L;
        });

        gbpLoadBtn.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> {
            String name = currentGlobalBP();
            if (name.isBlank()) return;
            int n = PaintBlueprints.loadGlobal(name);
            setGbpStatus("Loaded global \"" + name + "\" (" + n + " cells)");
        });

        gbpRefreshBtn.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> {
            refreshGlobalBlueprints();
            setGbpStatus("Refreshed global list");
        });

        gbpDeleteBtn.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> {
            String name = currentGlobalBP();
            if (name.isBlank()) return;
            boolean ok = PaintBlueprints.deleteGlobal(name);
            if (ok) {
                GridConfig.selectedGlobalBlueprint = "global_quick";
                GridConfig.markDirty(); GridConfig.saveIfDirty();
                refreshGlobalBlueprints();
                gbpRenameInput.setText(currentGlobalBP());
                setGbpStatus("Deleted global \"" + name + "\"");
            } else {
                setGbpStatus("Delete global failed");
            }
        });

        // Rename row (global)
        int gRenameY = growY + LINE + 8;
        add(new FormLabel("Rename to:", new FontOptions(14), FormLabel.ALIGN_LEFT, M, gRenameY + 6), M, gRenameY + 6);

        int griX = px + 110;
        int griW = 280;
        gbpRenameInput = add(new FormTextInput(griX, gRenameY, FormInputSize.SIZE_24, griW, 40), griX, gRenameY);
        gbpRenameInput.placeHolder = new StaticMessage("new global name");
        gbpRenameInput.rightClickToClear = true;
        gbpRenameInput.rightClickToClearTooltip = new StaticMessage("Clear");
        gbpRenameInput.onSubmit((FormEventListener<FormInputEvent<FormTextInput>>) e -> applyGlobalRename());

        gbpRenameApplyBtn = add(new FormTextButton("Apply", griX + griW + 8, gRenameY, 90, FormInputSize.SIZE_24, ButtonColor.BASE),
                                griX + griW + 8, gRenameY);
        gbpRenameApplyBtn.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> applyGlobalRename());

        // Misc status line
        int statusY = gRenameY + LINE + 16;
        statusLine = add(new FormLabel("", new FontOptions(12), FormLabel.ALIGN_LEFT, px, statusY), px, statusY);

        finishCard(cardGBP, statusY + (int)(LINE * 1.2f));
        py = cardGBP.getY() + cardGBP.height + 10;

        box.fitContentBoxToComponents(6);

        // init data
        refreshBlueprints();
        refreshGlobalBlueprints();
        gbpRenameInput.setText(currentGlobalBP());
        updateTransformButtonsActive();
        updateSelCountLabel();
    }

    // helpers …

    private void applyGlobalRename() {
        String oldName = currentGlobalBP();
        if (oldName.isBlank()) return;

        String raw = gbpRenameInput.getText();
        if (raw == null) raw = "";
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return;
        if (trimmed.equals(oldName)) return;

        boolean ok = PaintBlueprints.renameGlobal(oldName, trimmed);
        if (ok) {
            GridConfig.selectedGlobalBlueprint = trimmed;
            GridConfig.markDirty(); GridConfig.saveIfDirty();
            refreshGlobalBlueprints();
            gbpRenameInput.setText(trimmed);
            setGbpStatus("Renamed global to \"" + trimmed + "\"");
        } else {
            gbpRenameInput.setText(oldName);
            setGbpStatus("Rename global failed");
        }
    }

    private void refreshBlueprints() {
        bpDropdown.options.clear();
        String[] names = PaintBlueprints.listBlueprints();
        Arrays.sort(names, String::compareToIgnoreCase);
        Set<String> seen = new HashSet<>(names.length + 1);
        for (String n : names) { bpDropdown.options.add(n, new StaticMessage(n)); seen.add(n); }
        if (GridConfig.selectedBlueprint != null && !GridConfig.selectedBlueprint.isBlank() && !seen.contains(GridConfig.selectedBlueprint)) {
            String sel = GridConfig.selectedBlueprint;
            bpDropdown.options.add(sel, new StaticMessage(sel));
        }
        String toSelect = currentBP();
        bpDropdown.setSelected(toSelect, new StaticMessage(toSelect));
    }

    private void refreshGlobalBlueprints() {
        gbpDropdown.options.clear();
        String[] names = PaintBlueprints.listGlobalBlueprints();
        Arrays.sort(names, String::compareToIgnoreCase);
        Set<String> seen = new HashSet<>(names.length + 1);
        for (String n : names) { gbpDropdown.options.add(n, new StaticMessage(n)); seen.add(n); }
        if (GridConfig.selectedGlobalBlueprint != null && !GridConfig.selectedGlobalBlueprint.isBlank() && !seen.contains(GridConfig.selectedGlobalBlueprint)) {
            String sel = GridConfig.selectedGlobalBlueprint;
            gbpDropdown.options.add(sel, new StaticMessage(sel));
        }
        String toSelect = currentGlobalBP();
        gbpDropdown.setSelected(toSelect, new StaticMessage(toSelect));
    }

    private String currentBP() {
        String s = GridConfig.selectedBlueprint;
        return (s == null || s.isBlank()) ? "quick" : s;
    }
    private String currentGlobalBP() {
        String s = GridConfig.selectedGlobalBlueprint;
        return (s == null || s.isBlank()) ? "global_quick" : s;
        }

    private String proposeNewGlobalName() {
        Set<String> existing = new HashSet<>(Arrays.asList(PaintBlueprints.listGlobalBlueprints()));
        for (int i = 1; i <= 9999; i++) {
            String cand = "gbp_" + i;
            if (!existing.contains(cand)) return cand;
        }
        return "gbp_new";
    }

    private void setBpStatus(String msg) {
        if (bpInlineStatus != null) bpInlineStatus.setText(msg == null ? "" : msg);
        bpStatusClearAtMs = nowMs() + 4000L;
    }
    private void setGbpStatus(String msg) {
        if (gbpInlineStatus != null) gbpInlineStatus.setText(msg == null ? "" : msg);
        gbpStatusClearAtMs = nowMs() + 4000L;
    }
    @SuppressWarnings("unused")
    private void setStatus(String msg) {
        if (statusLine != null) statusLine.setText(msg == null ? "" : msg);
        statusClearAtMs = nowMs() + 4000L;
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

    private void updateTransformButtonsActive() {
        boolean active = BlueprintPlacement.active;
        if (rotateCwBtn != null)  rotateCwBtn.setActive(active);
        if (rotateCcwBtn != null) rotateCcwBtn.setActive(active);
        if (flipBtn != null)      flipBtn.setActive(active);
        if (placementStateLabel != null) {
            placementStateLabel.setText(active ? "" : "Placement inactive (Load a blueprint to enable)");
        }
    }

    private static long nowMs() { return System.currentTimeMillis(); }

    @SuppressWarnings("unused")
    private static String getInputMarkup(Control c) {
        if (c == null) return "(Unbound)";
        int key = c.getKey();
        if (key == -1) return "(Unbound)";
        return "[input=" + key + "]";
    }
}
