package colox.gridmod.ui;

import java.awt.Color;
import java.util.Arrays;

import necesse.engine.input.Control;
import necesse.engine.localization.message.StaticMessage;
import necesse.gfx.Renderer;
import necesse.gfx.forms.Form;
import necesse.gfx.forms.components.FormButton;
import necesse.gfx.forms.components.FormCheckBox;
import necesse.gfx.forms.components.FormContentBox;
import necesse.gfx.forms.components.FormCustomDraw;
import necesse.gfx.forms.components.FormDropdownSelectionButton;
import necesse.gfx.forms.components.FormInputSize;
import necesse.gfx.forms.components.FormLabel;
import necesse.gfx.forms.components.FormSlider;
import necesse.gfx.forms.components.FormTextButton;
import necesse.gfx.forms.components.FormComponent;
import necesse.gfx.forms.events.FormEventListener;
import necesse.gfx.forms.events.FormInputEvent;
import necesse.gfx.gameFont.FontManager;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.ui.ButtonColor;

import colox.gridmod.config.GridConfig;
import colox.gridmod.paint.PaintControls;

/**
 * GridUIForm
 * Window frame + tabs + content.
 * - Grid tab is owned by GridTab (Option A). It also handles UI opacity.
 * - Grid Colors has RGB only (no alpha here).
 * - Blueprint Colors + Settlement Colors live in this file.
 * - Settlement tab (non-color) lives here.
 */
public class GridUIForm extends Form {

    private static final int WIDTH  = 800;
    private static final int HEIGHT = 720;
    private static final int M = 12;
    private static final int LINE = 30;

    private enum Tab { GRID, PAINT, SETTLEMENT, GRID_COLORS, BP_COLORS, SETTLEMENT_COLORS }
    private Tab activeTab = Tab.GRID;

    // Content boxes
    private FormContentBox gridBox;
    private FormContentBox paintBox;
    private FormContentBox settlementBox;
    private FormContentBox gridColorsBox;
    private FormContentBox bpColorsBox;
    private FormContentBox settlementColorsBox;

    // --- Settlement controls (non-color) ---
    private FormDropdownSelectionButton<String> settlementModeDD;
    private FormDropdownSelectionButton<String> settlementTierDD;
    private FormLabel settlementInfoLabel;

    private FormSlider t1Slider, t2Slider, t3Slider, t4Slider, t5Slider;

    // --- Settlement style sliders (color tab)
    private FormSlider outlineThickSlider, outlineAlphaSlider, fillAlphaSlider;
    private FormSlider colorRSlider, colorGSlider, colorBSlider;

    // Tabs
    private FormTextButton gridTabBtn;
    private FormTextButton paintTabBtn;
    private FormTextButton settlementTabBtn;
    private FormTextButton gridColorsTabBtn;
    private FormTextButton bpColorsTabBtn;
    private FormTextButton settlementColorsTabBtn;

    // -------- Card helper --------
    private static final int CARD_X = M - 4;
    private static final int CARD_PAD_X = 8;
    private static final int CARD_PAD_Y = 8;

    /** Section background + border; decorative (no mouse handling). Honors GridConfig.uiOpacity. */
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
            Color base = this.getInterfaceStyle().activeElementColor;
            int aBG = Math.round(28 * GridConfig.uiOpacity);
            int aBR = Math.round(100 * GridConfig.uiOpacity);
            Color bg   = new Color(base.getRed(), base.getGreen(), base.getBlue(), aBG);
            Color border = this.getInterfaceStyle().activeTextColor;
            Color borderA = new Color(border.getRed(), border.getGreen(), border.getBlue(), aBR);

            Renderer.initQuadDraw(this.width, this.height).color(bg).draw(this.getX(), this.getY());
            Renderer.initQuadDraw(this.width, 1).color(borderA).draw(this.getX(), this.getY());
            Renderer.initQuadDraw(this.width, 1).color(borderA).draw(this.getX(), this.getY() + this.height - 1);
            Renderer.initQuadDraw(1, this.height).color(borderA).draw(this.getX(), this.getY());
            Renderer.initQuadDraw(1, this.height).color(borderA).draw(this.getX() + this.width - 1, this.getY());
        }
    }

    private SectionCard addCard(FormContentBox box, int y, int innerWidth) {
        int w = innerWidth - (CARD_X * 2);
        SectionCard c = new SectionCard(CARD_X, y, w, 10);
        box.getComponentList().addComponent(c);
        return c;
    }
    private <T extends FormComponent> T addTo(FormContentBox box, T comp) {
        box.getComponentList().addComponent(comp);
        return comp;
    }
    private void finishCard(SectionCard card, int bottomContentY) {
        int desired = (bottomContentY - card.getY()) + CARD_PAD_Y;
        if (desired < 10) desired = 10;
        card.height = desired;
    }

    public GridUIForm() {
        super(WIDTH, HEIGHT);

        // Form base opacity starts from config (GridTab updates this live)
        this.drawBaseAlpha = GridConfig.uiOpacity;

        // Title + top-right "X" close
        FormLabel title = new FormLabel("Grid Mod Settings", new FontOptions(18), FormLabel.ALIGN_LEFT, M, M);
        this.addComponent(title);

        FormTextButton closeX = new FormTextButton("X", WIDTH - M - 28, M - 4, 28, FormInputSize.SIZE_24, ButtonColor.BASE);
        closeX.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> {
            GridConfig.saveIfDirty();
            GridUI.tryClose(this);
        });
        this.addComponent(closeX);

        // Tabs under the title
        int tabsBlockHeight = buildTabsBelowTitle();

        // Content area below tabs
        int contentTopY = M + LINE + tabsBlockHeight + 6;
        int boxX = M;
        int boxY = contentTopY;
        int boxW = WIDTH - 2 * M;
        int boxH = HEIGHT - contentTopY - M;

        // Content boxes
        gridBox = new FormContentBox(boxX, boxY, boxW, boxH);
        gridBox.alwaysShowVerticalScrollBar = true;
        this.addComponent(gridBox);

        paintBox = new FormContentBox(boxX, boxY, boxW, boxH);
        paintBox.alwaysShowVerticalScrollBar = true;
        this.addComponent(paintBox);

        settlementBox = new FormContentBox(boxX, boxY, boxW, boxH);
        settlementBox.alwaysShowVerticalScrollBar = true;
        this.addComponent(settlementBox);

        gridColorsBox = new FormContentBox(boxX, boxY, boxW, boxH);
        gridColorsBox.alwaysShowVerticalScrollBar = true;
        this.addComponent(gridColorsBox);

        bpColorsBox = new FormContentBox(boxX, boxY, boxW, boxH);
        bpColorsBox.alwaysShowVerticalScrollBar = true;
        this.addComponent(bpColorsBox);

        settlementColorsBox = new FormContentBox(boxX, boxY, boxW, boxH);
        settlementColorsBox.alwaysShowVerticalScrollBar = true;
        this.addComponent(settlementColorsBox);

        // Start with only GRID visible
        paintBox.setHidden(true);
        settlementBox.setHidden(true);
        gridColorsBox.setHidden(true);
        bpColorsBox.setHidden(true);
        settlementColorsBox.setHidden(true);

        // ======== GRID tab (delegated) ========
        new GridTab(gridBox, v -> this.drawBaseAlpha = v);

        // ======== PAINT tab ========
        new PaintTab(paintBox);

        // ======== SETTLEMENT (non-color) ========
        buildSettlementContent();

        // ======== COLORS ========
        buildGridColorsContent();
        buildBlueprintColorsContent();
        buildSettlementColorsContent();
    }

    // ---------- Tabs (two rows: main, then colors) ----------
    private int buildTabsBelowTitle() {
        final int startX = M;
        final int gapX = 6;
        final int gapY = 6;
        final FormInputSize tabSize = FormInputSize.SIZE_24;
        final int tabH = tabSize.height;

        // Labels
        final String gridLabel  = "Grid";
        final String paintLabel = "Paint & Blueprints";
        final String settlementLabel = "Settlement";

        final String gridColorsLabel  = "Grid Colors";
        final String bpColorsLabel    = "Blueprint Colors";
        final String settleColorsLabel = "Settlement Colors";

        // Widths
        int gridW       = computeTabWidth(gridLabel, tabSize);
        int paintW      = computeTabWidth(paintLabel, tabSize);
        int settleW     = computeTabWidth(settlementLabel, tabSize);

        int gridColorsW = computeTabWidth(gridColorsLabel, tabSize);
        int bpColorsW   = computeTabWidth(bpColorsLabel, tabSize);
        int settleColorsW = computeTabWidth(settleColorsLabel, tabSize);

        // First row (MAIN TABS)
        int yMain = M + LINE;     // directly below the title
        int x = startX;

        gridTabBtn = new FormTextButton(gridLabel, x, yMain - 2, gridW, tabSize, ButtonColor.BASE);
        this.addComponent(gridTabBtn);
        x += gridW + gapX;

        paintTabBtn = new FormTextButton(paintLabel, x, yMain - 2, paintW, tabSize, ButtonColor.BASE);
        this.addComponent(paintTabBtn);
        x += paintW + gapX;

        settlementTabBtn = new FormTextButton(settlementLabel, x, yMain - 2, settleW, tabSize, ButtonColor.BASE);
        this.addComponent(settlementTabBtn);

        // Second row (COLOR TABS) – always under the first row
        int yColors = yMain + tabH + gapY;
        x = startX;

        gridColorsTabBtn = new FormTextButton(gridColorsLabel, x, yColors - 2, gridColorsW, tabSize, ButtonColor.BASE);
        this.addComponent(gridColorsTabBtn);
        x += gridColorsW + gapX;

        bpColorsTabBtn = new FormTextButton(bpColorsLabel, x, yColors - 2, bpColorsW, tabSize, ButtonColor.BASE);
        this.addComponent(bpColorsTabBtn);
        x += bpColorsW + gapX;

        settlementColorsTabBtn = new FormTextButton(settleColorsLabel, x, yColors - 2, settleColorsW, tabSize, ButtonColor.BASE);
        this.addComponent(settlementColorsTabBtn);

        // Wiring
        gridTabBtn.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> switchTab(Tab.GRID));
        paintTabBtn.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> switchTab(Tab.PAINT));
        settlementTabBtn.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> switchTab(Tab.SETTLEMENT));

        gridColorsTabBtn.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> switchTab(Tab.GRID_COLORS));
        bpColorsTabBtn.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> switchTab(Tab.BP_COLORS));
        settlementColorsTabBtn.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> switchTab(Tab.SETTLEMENT_COLORS));

        // Tell caller how much vertical space the two rows occupy beneath the title.
        return (yColors + tabH) - (M + LINE);
    }

    private int computeTabWidth(String label, FormInputSize size) {
        FontOptions fo = size.getFontOptions();
        int textW = FontManager.bit.getWidthCeil(label, fo);
        int padding = 24;
        int minW = 84;
        int maxW = 220;
        int w = textW + padding;
        if (w < minW) w = minW;
        if (w > maxW) w = maxW;
        return w;
    }

    private void switchTab(Tab tab) {
        if (activeTab == tab) return;
        activeTab = tab;

        gridBox.setHidden(!(activeTab == Tab.GRID));
        paintBox.setHidden(!(activeTab == Tab.PAINT));
        settlementBox.setHidden(!(activeTab == Tab.SETTLEMENT));
        gridColorsBox.setHidden(!(activeTab == Tab.GRID_COLORS));
        bpColorsBox.setHidden(!(activeTab == Tab.BP_COLORS));
        settlementColorsBox.setHidden(!(activeTab == Tab.SETTLEMENT_COLORS));
    }

    // ---------- SETTLEMENT (non-color) ----------
    private void buildSettlementContent() {
        final int innerWidth = settlementBox.getMinContentWidth();
        final int cardWidth  = innerWidth - (CARD_X * 2);
        final int usableRow  = cardWidth - (CARD_PAD_X * 2);
        int y = 0;
        int ddW = 180;

        // Card 1
        SectionCard cardMain = addCard(settlementBox, y, innerWidth);
        int sx = CARD_X + CARD_PAD_X;
        int sy = y + CARD_PAD_Y;

        addTo(settlementBox, new FormLabel("Settlement Overlay", new FontOptions(16), FormLabel.ALIGN_LEFT, sx, sy));
        sy += LINE - 6;

        FormCheckBox enable = addTo(settlementBox, new FormCheckBox("Show settlement bounds", sx, sy, GridConfig.settlementEnabled));
        enable.onClicked((FormEventListener<FormInputEvent<FormCheckBox>>) e -> {
            boolean next = ((FormCheckBox)e.from).checked;
            boolean prev = GridConfig.settlementEnabled;
            if (next && !prev && !GridConfig.hasSettlementAnchor()) {
                PaintControls.placeHere();
            }
            GridConfig.settlementEnabled = next;
            GridConfig.markDirty(); GridConfig.saveIfDirty();
        });
        sy += LINE;

        // Place here
        int btnW = 150;
        FormTextButton placeBtn = addTo(settlementBox, new FormTextButton("Place here", sx, sy - 2, btnW, FormInputSize.SIZE_24, ButtonColor.BASE));
        placeBtn.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> {
            PaintControls.placeHereAndEnable();
            GridConfig.markDirty(); GridConfig.saveIfDirty();
            enable.checked = true;
            refreshSettlementInfo();
        });
        sy += LINE;

        // Mode
        addTo(settlementBox, new FormLabel("Mode:", new FontOptions(14), FormLabel.ALIGN_LEFT, sx, sy));
        settlementModeDD = addTo(settlementBox, new FormDropdownSelectionButton<>(sx + 80, sy - 2, FormInputSize.SIZE_24, ButtonColor.BASE, ddW));
        settlementModeDD.options.add("builtin", new StaticMessage("builtin"));
        settlementModeDD.options.add("manual",  new StaticMessage("manual (experimental)"));
        String curMode = (GridConfig.settlementMode == null || GridConfig.settlementMode.isBlank())
                ? "builtin" : GridConfig.settlementMode.toLowerCase();
        if (!Arrays.asList("builtin","manual").contains(curMode)) curMode = "builtin";
        settlementModeDD.setSelected(curMode, modeLabel(curMode));
        settlementModeDD.onSelected(e -> {
            String nextMode = e.value;
            if (!Arrays.asList("builtin", "manual").contains(nextMode)) return;

            int oldSide = GridConfig.currentTierSideTiles();

            GridConfig.settlementMode = nextMode;
            GridConfig.settlementTier = Math.max(1, Math.min(GridConfig.maxTier(), GridConfig.settlementTier));

            int newSide = GridConfig.currentTierSideTiles();

            if (GridConfig.hasSettlementAnchor()) {
                int halfDelta = (newSide - oldSide) / 2;
                GridConfig.settlementAnchorTx -= halfDelta;
                GridConfig.settlementAnchorTy -= halfDelta;
            }

            GridConfig.markDirty(); GridConfig.saveIfDirty();
            settlementModeDD.setSelected(nextMode, modeLabel(nextMode));
            refreshTierDropdown();
            refreshSettlementInfo();
        });
        sy += LINE;

        // Tier
        addTo(settlementBox, new FormLabel("Tier:", new FontOptions(14), FormLabel.ALIGN_LEFT, sx, sy));
        settlementTierDD = addTo(settlementBox, new FormDropdownSelectionButton<>(sx + 80, sy - 2, FormInputSize.SIZE_24, ButtonColor.BASE, ddW));
        populateTierDropdown();
        settlementTierDD.onSelected(e -> {
            try {
                int wanted = Integer.parseInt(e.value);
                int oldSide = GridConfig.currentTierSideTiles();

                GridConfig.settlementTier = Math.max(1, Math.min(GridConfig.maxTier(), wanted));

                int newSide = GridConfig.currentTierSideTiles();

                if (GridConfig.hasSettlementAnchor()) {
                    int halfDelta = (newSide - oldSide) / 2;
                    GridConfig.settlementAnchorTx -= halfDelta;
                    GridConfig.settlementAnchorTy -= halfDelta;
                }

                GridConfig.markDirty(); GridConfig.saveIfDirty();
                refreshSettlementInfo();
            } catch (NumberFormatException ignored) {}
        });
        sy += LINE;

        // Info
        settlementInfoLabel = addTo(settlementBox, new FormLabel("", new FontOptions(12), FormLabel.ALIGN_LEFT, sx, sy));
        refreshSettlementInfo();
        sy += LINE;

        finishCard(cardMain, sy);
        y = cardMain.getY() + cardMain.height + 10;

        // Card 2: Manual sizes
        SectionCard cardManual = addCard(settlementBox, y, innerWidth);
        sx = CARD_X + CARD_PAD_X;
        sy = y + CARD_PAD_Y;

        addTo(settlementBox, new FormLabel("Manual sizes (tiles) — applies only when Mode = manual", new FontOptions(14), FormLabel.ALIGN_LEFT, sx, sy));
        sy += LINE - 8;

        int sliderW = Math.max(220, usableRow);

        t1Slider = addTo(settlementBox, new FormSlider("Tier 1", sx, sy, GridConfig.settlementSizeT1, 16, 512, sliderW));
        t1Slider.drawValue = true;
        t1Slider.onChanged(e -> { GridConfig.settlementSizeT1 = ((FormSlider)e.from).getValue(); GridConfig.markDirty(); GridConfig.saveIfDirty(); refreshSettlementInfoIfTier(1); });
        sy += t1Slider.getTotalHeight() + 6;

        t2Slider = addTo(settlementBox, new FormSlider("Tier 2", sx, sy, GridConfig.settlementSizeT2, 16, 512, sliderW));
        t2Slider.drawValue = true;
        t2Slider.onChanged(e -> { GridConfig.settlementSizeT2 = ((FormSlider)e.from).getValue(); GridConfig.markDirty(); GridConfig.saveIfDirty(); refreshSettlementInfoIfTier(2); });
        sy += t2Slider.getTotalHeight() + 6;

        t3Slider = addTo(settlementBox, new FormSlider("Tier 3", sx, sy, GridConfig.settlementSizeT3, 16, 512, sliderW));
        t3Slider.drawValue = true;
        t3Slider.onChanged(e -> { GridConfig.settlementSizeT3 = ((FormSlider)e.from).getValue(); GridConfig.markDirty(); GridConfig.saveIfDirty(); refreshSettlementInfoIfTier(3); });
        sy += t3Slider.getTotalHeight() + 6;

        t4Slider = addTo(settlementBox, new FormSlider("Tier 4", sx, sy, GridConfig.settlementSizeT4, 16, 512, sliderW));
        t4Slider.drawValue = true;
        t4Slider.onChanged(e -> { GridConfig.settlementSizeT4 = ((FormSlider)e.from).getValue(); GridConfig.markDirty(); GridConfig.saveIfDirty(); refreshSettlementInfoIfTier(4); });
        sy += t4Slider.getTotalHeight() + 6;

        t5Slider = addTo(settlementBox, new FormSlider("Tier 5", sx, sy, GridConfig.settlementSizeT5, 16, 512, sliderW));
        t5Slider.drawValue = true;
        t5Slider.onChanged(e -> { GridConfig.settlementSizeT5 = ((FormSlider)e.from).getValue(); GridConfig.markDirty(); GridConfig.saveIfDirty(); refreshSettlementInfoIfTier(5); });
        sy += t5Slider.getTotalHeight() + 6;

        finishCard(cardManual, sy);
        settlementBox.fitContentBoxToComponents(6);
    }

    // ---------- GRID COLORS (RGB only) ----------
    private void buildGridColorsContent() {
        final int inner = gridColorsBox.getMinContentWidth();
        final int usableRow  = (inner - (CARD_X * 2)) - (CARD_PAD_X * 2);
        int y = 0;

        // Base grid color
        SectionCard cBase = addCard(gridColorsBox, y, inner);
        int gx = CARD_X + CARD_PAD_X;
        int gy = y + CARD_PAD_Y;

        addTo(gridColorsBox, new FormLabel("Base Grid Color", new FontOptions(16), FormLabel.ALIGN_LEFT, gx, gy));
        gy += LINE - 10;

        FormSlider baseR = addTo(gridColorsBox, new FormSlider("Red", gx, gy, Math.round(GridConfig.r * 100f), 0, 100, Math.max(220, usableRow)));
        baseR.drawValue = true; baseR.drawValueInPercent = true;
        baseR.onChanged(e -> { GridConfig.r = clamp01(((FormSlider)e.from).getValue()/100f); GridConfig.markDirty(); GridConfig.saveIfDirty(); });
        gy += baseR.getTotalHeight() + 6;

        FormSlider baseG = addTo(gridColorsBox, new FormSlider("Green", gx, gy, Math.round(GridConfig.g * 100f), 0, 100, Math.max(220, usableRow)));
        baseG.drawValue = true; baseG.drawValueInPercent = true;
        baseG.onChanged(e -> { GridConfig.g = clamp01(((FormSlider)e.from).getValue()/100f); GridConfig.markDirty(); GridConfig.saveIfDirty(); });
        gy += baseG.getTotalHeight() + 6;

        FormSlider baseB = addTo(gridColorsBox, new FormSlider("Blue", gx, gy, Math.round(GridConfig.b * 100f), 0, 100, Math.max(220, usableRow)));
        baseB.drawValue = true; baseB.drawValueInPercent = true;
        baseB.onChanged(e -> { GridConfig.b = clamp01(((FormSlider)e.from).getValue()/100f); GridConfig.markDirty(); GridConfig.saveIfDirty(); });
        gy += baseB.getTotalHeight() + 6;

        finishCard(cBase, gy);
        y = cBase.getY() + cBase.height + 10;

        // Chunk lines color
        SectionCard cChunk = addCard(gridColorsBox, y, inner);
        gx = CARD_X + CARD_PAD_X;
        gy = y + CARD_PAD_Y;

        addTo(gridColorsBox, new FormLabel("Chunk Lines Color", new FontOptions(14), FormLabel.ALIGN_LEFT, gx, gy));
        gy += LINE - 12;

        FormSlider ccr = addTo(gridColorsBox, new FormSlider("Red", gx, gy, Math.round(GridConfig.cr * 100f), 0, 100, Math.max(220, usableRow)));
        ccr.drawValue = true; ccr.drawValueInPercent = true;
        ccr.onChanged(e -> { GridConfig.cr = clamp01(((FormSlider)e.from).getValue()/100f); GridConfig.markDirty(); GridConfig.saveIfDirty(); });
        gy += ccr.getTotalHeight() + 6;

        FormSlider ccg = addTo(gridColorsBox, new FormSlider("Green", gx, gy, Math.round(GridConfig.cg * 100f), 0, 100, Math.max(220, usableRow)));
        ccg.drawValue = true; ccg.drawValueInPercent = true;
        ccg.onChanged(e -> { GridConfig.cg = clamp01(((FormSlider)e.from).getValue()/100f); GridConfig.markDirty(); GridConfig.saveIfDirty(); });
        gy += ccg.getTotalHeight() + 6;

        FormSlider ccb = addTo(gridColorsBox, new FormSlider("Blue", gx, gy, Math.round(GridConfig.cb * 100f), 0, 100, Math.max(220, usableRow)));
        ccb.drawValue = true; ccb.drawValueInPercent = true;
        ccb.onChanged(e -> { GridConfig.cb = clamp01(((FormSlider)e.from).getValue()/100f); GridConfig.markDirty(); GridConfig.saveIfDirty(); });
        gy += ccb.getTotalHeight() + 6;

        finishCard(cChunk, gy);
        y = cChunk.getY() + cChunk.height + 10;

        // Sub-chunk lines color
        SectionCard cSub = addCard(gridColorsBox, y, inner);
        gx = CARD_X + CARD_PAD_X;
        gy = y + CARD_PAD_Y;

        addTo(gridColorsBox, new FormLabel("Sub-chunk Lines Color", new FontOptions(14), FormLabel.ALIGN_LEFT, gx, gy));
        gy += LINE - 12;

        FormSlider scr = addTo(gridColorsBox, new FormSlider("Red", gx, gy, Math.round(GridConfig.scr * 100f), 0, 100, Math.max(220, usableRow)));
        scr.drawValue = true; scr.drawValueInPercent = true;
        scr.onChanged(e -> { GridConfig.scr = clamp01(((FormSlider)e.from).getValue()/100f); GridConfig.markDirty(); GridConfig.saveIfDirty(); });
        gy += scr.getTotalHeight() + 6;

        FormSlider scg = addTo(gridColorsBox, new FormSlider("Green", gx, gy, Math.round(GridConfig.scg * 100f), 0, 100, Math.max(220, usableRow)));
        scg.drawValue = true; scg.drawValueInPercent = true;
        scg.onChanged(e -> { GridConfig.scg = clamp01(((FormSlider)e.from).getValue()/100f); GridConfig.markDirty(); GridConfig.saveIfDirty(); });
        gy += scg.getTotalHeight() + 6;

        FormSlider scb = addTo(gridColorsBox, new FormSlider("Blue", gx, gy, Math.round(GridConfig.scb * 100f), 0, 100, Math.max(220, usableRow)));
        scb.drawValue = true; scb.drawValueInPercent = true;
        scb.onChanged(e -> { GridConfig.scb = clamp01(((FormSlider)e.from).getValue()/100f); GridConfig.markDirty(); GridConfig.saveIfDirty(); });
        gy += scb.getTotalHeight() + 6;

        finishCard(cSub, gy);
        gridColorsBox.fitContentBoxToComponents(6);
    }

    // ---------- BLUEPRINT / PAINT COLORS ----------
    private void buildBlueprintColorsContent() {
        final int inner = bpColorsBox.getMinContentWidth();
        final int usableRow = (inner - (CARD_X * 2)) - (CARD_PAD_X * 2);
        int y = 0;

        // Brush paint
        SectionCard cardPaint = addCard(bpColorsBox, y, inner);
        int x = CARD_X + CARD_PAD_X;
        int cy = y + CARD_PAD_Y;

        addTo(bpColorsBox, new FormLabel("Brush Paint", new FontOptions(16), FormLabel.ALIGN_LEFT, x, cy));
        cy += LINE - 10;

        FormSlider pr = addTo(bpColorsBox, new FormSlider("Red", x, cy, Math.round(GridConfig.paintR * 100f), 0, 100, Math.max(220, usableRow)));
        pr.drawValue = true; pr.drawValueInPercent = true;
        pr.onChanged(e -> { GridConfig.paintR = clamp01(((FormSlider)e.from).getValue()/100f); GridConfig.markDirty(); GridConfig.saveIfDirty(); });
        cy += pr.getTotalHeight() + 6;

        FormSlider pg = addTo(bpColorsBox, new FormSlider("Green", x, cy, Math.round(GridConfig.paintG * 100f), 0, 100, Math.max(220, usableRow)));
        pg.drawValue = true; pg.drawValueInPercent = true;
        pg.onChanged(e -> { GridConfig.paintG = clamp01(((FormSlider)e.from).getValue()/100f); GridConfig.markDirty(); GridConfig.saveIfDirty(); });
        cy += pg.getTotalHeight() + 6;

        FormSlider pb = addTo(bpColorsBox, new FormSlider("Blue", x, cy, Math.round(GridConfig.paintB * 100f), 0, 100, Math.max(220, usableRow)));
        pb.drawValue = true; pb.drawValueInPercent = true;
        pb.onChanged(e -> { GridConfig.paintB = clamp01(((FormSlider)e.from).getValue()/100f); GridConfig.markDirty(); GridConfig.saveIfDirty(); });
        cy += pb.getTotalHeight() + 6;

        FormSlider pa = addTo(bpColorsBox, new FormSlider("Alpha", x, cy, Math.round(GridConfig.paintAlpha * 100f), 0, 100, Math.max(220, usableRow)));
        pa.drawValue = true; pa.drawValueInPercent = true;
        pa.onChanged(e -> { GridConfig.paintAlpha = clamp01(((FormSlider)e.from).getValue()/100f); GridConfig.markDirty(); GridConfig.saveIfDirty(); });
        cy += pa.getTotalHeight() + 10;

        finishCard(cardPaint, cy);
        y = cardPaint.getY() + cardPaint.height + 10;

        // Erase/clear preview
        SectionCard cardErase = addCard(bpColorsBox, y, inner);
        x = CARD_X + CARD_PAD_X;
        cy = y + CARD_PAD_Y;

        addTo(bpColorsBox, new FormLabel("Erase Preview", new FontOptions(14), FormLabel.ALIGN_LEFT, x, cy));
        cy += LINE - 12;

        FormSlider er = addTo(bpColorsBox, new FormSlider("Red", x, cy, Math.round(GridConfig.eraseR * 100f), 0, 100, Math.max(220, usableRow)));
        er.drawValue = true; er.drawValueInPercent = true;
        er.onChanged(e -> { GridConfig.eraseR = clamp01(((FormSlider)e.from).getValue()/100f); GridConfig.markDirty(); GridConfig.saveIfDirty(); });
        cy += er.getTotalHeight() + 6;

        FormSlider eg = addTo(bpColorsBox, new FormSlider("Green", x, cy, Math.round(GridConfig.eraseG * 100f), 0, 100, Math.max(220, usableRow)));
        eg.drawValue = true; eg.drawValueInPercent = true;
        eg.onChanged(e -> { GridConfig.eraseG = clamp01(((FormSlider)e.from).getValue()/100f); GridConfig.markDirty(); GridConfig.saveIfDirty(); });
        cy += eg.getTotalHeight() + 6;

        FormSlider eb = addTo(bpColorsBox, new FormSlider("Blue", x, cy, Math.round(GridConfig.eraseB * 100f), 0, 100, Math.max(220, usableRow)));
        eb.drawValue = true; eb.drawValueInPercent = true;
        eb.onChanged(e -> { GridConfig.eraseB = clamp01(((FormSlider)e.from).getValue()/100f); GridConfig.markDirty(); GridConfig.saveIfDirty(); });
        cy += eb.getTotalHeight() + 6;

        FormSlider ea = addTo(bpColorsBox, new FormSlider("Alpha", x, cy, Math.round(GridConfig.eraseAlpha * 100f), 0, 100, Math.max(220, usableRow)));
        ea.drawValue = true; ea.drawValueInPercent = true;
        ea.onChanged(e -> { GridConfig.eraseAlpha = clamp01(((FormSlider)e.from).getValue()/100f); GridConfig.markDirty(); GridConfig.saveIfDirty(); });
        cy += ea.getTotalHeight() + 10;

        finishCard(cardErase, cy);
        y = cardErase.getY() + cardErase.height + 10;

        // Selection rectangle
        SectionCard cardSel = addCard(bpColorsBox, y, inner);
        x = CARD_X + CARD_PAD_X;
        cy = y + CARD_PAD_Y;

        addTo(bpColorsBox, new FormLabel("Selection Rectangle", new FontOptions(14), FormLabel.ALIGN_LEFT, x, cy));
        cy += LINE - 12;

        FormSlider sr = addTo(bpColorsBox, new FormSlider("Red", x, cy, Math.round(GridConfig.selectionR * 100f), 0, 100, Math.max(220, usableRow)));
        sr.drawValue = true; sr.drawValueInPercent = true;
        sr.onChanged(e -> { GridConfig.selectionR = clamp01(((FormSlider)e.from).getValue()/100f); GridConfig.markDirty(); GridConfig.saveIfDirty(); });
        cy += sr.getTotalHeight() + 6;

        FormSlider sg = addTo(bpColorsBox, new FormSlider("Green", x, cy, Math.round(GridConfig.selectionG * 100f), 0, 100, Math.max(220, usableRow)));
        sg.drawValue = true; sg.drawValueInPercent = true;
        sg.onChanged(e -> { GridConfig.selectionG = clamp01(((FormSlider)e.from).getValue()/100f); GridConfig.markDirty(); GridConfig.saveIfDirty(); });
        cy += sg.getTotalHeight() + 6;

        FormSlider sb = addTo(bpColorsBox, new FormSlider("Blue", x, cy, Math.round(GridConfig.selectionB * 100f), 0, 100, Math.max(220, usableRow)));
        sb.drawValue = true; sb.drawValueInPercent = true;
        sb.onChanged(e -> { GridConfig.selectionB = clamp01(((FormSlider)e.from).getValue()/100f); GridConfig.markDirty(); GridConfig.saveIfDirty(); });
        cy += sb.getTotalHeight() + 6;

        FormSlider sa = addTo(bpColorsBox, new FormSlider("Alpha", x, cy, Math.round(GridConfig.selectionAlpha * 100f), 0, 100, Math.max(220, usableRow)));
        sa.drawValue = true; sa.drawValueInPercent = true;
        sa.onChanged(e -> { GridConfig.selectionAlpha = clamp01(((FormSlider)e.from).getValue()/100f); GridConfig.markDirty(); GridConfig.saveIfDirty(); });
        cy += sa.getTotalHeight() + 10;

        finishCard(cardSel, cy);
        y = cardSel.getY() + cardSel.height + 10;

        // Blueprint ghost preview
        SectionCard cardGhost = addCard(bpColorsBox, y, inner);
        x = CARD_X + CARD_PAD_X;
        cy = y + CARD_PAD_Y;

        addTo(bpColorsBox, new FormLabel("Blueprint Ghost", new FontOptions(14), FormLabel.ALIGN_LEFT, x, cy));
        cy += LINE - 12;

        FormSlider gr = addTo(bpColorsBox, new FormSlider("Red", x, cy, Math.round(GridConfig.bpGhostR * 100f), 0, 100, Math.max(220, usableRow)));
        gr.drawValue = true; gr.drawValueInPercent = true;
        gr.onChanged(e -> { GridConfig.bpGhostR = clamp01(((FormSlider)e.from).getValue()/100f); GridConfig.markDirty(); GridConfig.saveIfDirty(); });
        cy += gr.getTotalHeight() + 6;

        FormSlider gg = addTo(bpColorsBox, new FormSlider("Green", x, cy, Math.round(GridConfig.bpGhostG * 100f), 0, 100, Math.max(220, usableRow)));
        gg.drawValue = true; gg.drawValueInPercent = true;
        gg.onChanged(e -> { GridConfig.bpGhostG = clamp01(((FormSlider)e.from).getValue()/100f); GridConfig.markDirty(); GridConfig.saveIfDirty(); });
        cy += gg.getTotalHeight() + 6;

        FormSlider gb = addTo(bpColorsBox, new FormSlider("Blue", x, cy, Math.round(GridConfig.bpGhostB * 100f), 0, 100, Math.max(220, usableRow)));
        gb.drawValue = true; gb.drawValueInPercent = true;
        gb.onChanged(e -> { GridConfig.bpGhostB = clamp01(((FormSlider)e.from).getValue()/100f); GridConfig.markDirty(); GridConfig.saveIfDirty(); });
        cy += gb.getTotalHeight() + 6;

        FormSlider ga = addTo(bpColorsBox, new FormSlider("Alpha", x, cy, Math.round(GridConfig.bpGhostAlpha * 100f), 0, 100, Math.max(220, usableRow)));
        ga.drawValue = true; ga.drawValueInPercent = true;
        ga.onChanged(e -> { GridConfig.bpGhostAlpha = clamp01(((FormSlider)e.from).getValue()/100f); GridConfig.markDirty(); GridConfig.saveIfDirty(); });
        cy += ga.getTotalHeight() + 10;

        finishCard(cardGhost, cy);
        bpColorsBox.fitContentBoxToComponents(6);
    }

    // ---------- SETTLEMENT COLORS ----------
    private void buildSettlementColorsContent() {
        final int inner = settlementColorsBox.getMinContentWidth();
        final int usableRow = (inner - (CARD_X * 2)) - (CARD_PAD_X * 2);
        int y = 0;

        SectionCard style = addCard(settlementColorsBox, y, inner);
        int sx = CARD_X + CARD_PAD_X;
        int sy = y + CARD_PAD_Y;

        addTo(settlementColorsBox, new FormLabel("Settlement Style", new FontOptions(16), FormLabel.ALIGN_LEFT, sx, sy));
        sy += LINE - 10;

        outlineThickSlider = addTo(settlementColorsBox, new FormSlider("Outline thickness (px)", sx, sy, GridConfig.settlementOutlineThickness, 1, 6, Math.max(220, usableRow)));
        outlineThickSlider.drawValue = true;
        outlineThickSlider.onChanged(e -> { GridConfig.settlementOutlineThickness = ((FormSlider)e.from).getValue(); GridConfig.markDirty(); GridConfig.saveIfDirty(); });
        sy += outlineThickSlider.getTotalHeight() + 6;

        outlineAlphaSlider = addTo(settlementColorsBox, new FormSlider("Outline alpha", sx, sy, Math.round(GridConfig.settlementOutlineAlpha * 100f), 0, 100, Math.max(220, usableRow)));
        outlineAlphaSlider.drawValue = true;
        outlineAlphaSlider.drawValueInPercent = true;
        outlineAlphaSlider.onChanged(e -> { GridConfig.settlementOutlineAlpha = clamp01(((FormSlider)e.from).getValue()/100f); GridConfig.markDirty(); GridConfig.saveIfDirty(); });
        sy += outlineAlphaSlider.getTotalHeight() + 6;

        fillAlphaSlider = addTo(settlementColorsBox, new FormSlider("Fill alpha", sx, sy, Math.round(GridConfig.settlementFillAlpha * 100f), 0, 100, Math.max(220, usableRow)));
        fillAlphaSlider.drawValue = true;
        fillAlphaSlider.drawValueInPercent = true;
        fillAlphaSlider.onChanged(e -> { GridConfig.settlementFillAlpha = clamp01(((FormSlider)e.from).getValue()/100f); GridConfig.markDirty(); GridConfig.saveIfDirty(); });
        sy += fillAlphaSlider.getTotalHeight() + 12;

        addTo(settlementColorsBox, new FormLabel("Color (RGB)", new FontOptions(12), FormLabel.ALIGN_LEFT, sx, sy));
        sy += LINE - 12;

        colorRSlider = addTo(settlementColorsBox, new FormSlider("Red", sx, sy, Math.round(GridConfig.sbr * 100f), 0, 100, Math.max(220, usableRow)));
        colorRSlider.drawValue = true;
        colorRSlider.drawValueInPercent = true;
        colorRSlider.onChanged(e -> { GridConfig.sbr = clamp01(((FormSlider)e.from).getValue()/100f); GridConfig.markDirty(); GridConfig.saveIfDirty(); });
        sy += colorRSlider.getTotalHeight() + 6;

        colorGSlider = addTo(settlementColorsBox, new FormSlider("Green", sx, sy, Math.round(GridConfig.sbg * 100f), 0, 100, Math.max(220, usableRow)));
        colorGSlider.drawValue = true;
        colorGSlider.drawValueInPercent = true;
        colorGSlider.onChanged(e -> { GridConfig.sbg = clamp01(((FormSlider)e.from).getValue()/100f); GridConfig.markDirty(); GridConfig.saveIfDirty(); });
        sy += colorGSlider.getTotalHeight() + 6;

        colorBSlider = addTo(settlementColorsBox, new FormSlider("Blue", sx, sy, Math.round(GridConfig.sbb * 100f), 0, 100, Math.max(220, usableRow)));
        colorBSlider.drawValue = true;
        colorBSlider.drawValueInPercent = true;
        colorBSlider.onChanged(e -> { GridConfig.sbb = clamp01(((FormSlider)e.from).getValue()/100f); GridConfig.markDirty(); GridConfig.saveIfDirty(); });
        sy += colorBSlider.getTotalHeight() + 6;

        finishCard(style, sy);
        settlementColorsBox.fitContentBoxToComponents(6);
    }

    // ---------- helpers ----------
    private void populateTierDropdown() {
        settlementTierDD.options.clear();
        int max = GridConfig.maxTier();
        for (int i = 1; i <= max; i++) {
            settlementTierDD.options.add(Integer.toString(i), new StaticMessage("Tier " + i));
        }
        String cur = Integer.toString(Math.max(1, Math.min(max, GridConfig.settlementTier)));
        settlementTierDD.setSelected(cur, new StaticMessage("Tier " + cur));
    }

    private void refreshTierDropdown() {
        int wanted = GridConfig.settlementTier;
        populateTierDropdown();
        GridConfig.settlementTier = Math.max(1, Math.min(GridConfig.maxTier(), wanted));
        GridConfig.markDirty(); GridConfig.saveIfDirty();
        String cur = Integer.toString(GridConfig.settlementTier);
        settlementTierDD.setSelected(cur, new StaticMessage("Tier " + cur));
    }

    private void refreshSettlementInfo() {
        if (settlementInfoLabel == null) return;
        int tiles = GridConfig.currentTierSideTiles();
        int chunks = Math.max(1, tiles / 16);
        settlementInfoLabel.setText("Side: " + tiles + " tiles (" + chunks + "×16-tile chunks per side)");
    }
    private void refreshSettlementInfoIfTier(int tier) {
        if (GridConfig.settlementTier == tier) refreshSettlementInfo();
    }

    private static float clamp01(float v) { return v < 0f ? 0f : (v > 1f ? 1f : v); }

    private static StaticMessage modeLabel(String v) {
        if ("manual".equalsIgnoreCase(v)) return new StaticMessage("manual (experimental)");
        return new StaticMessage("builtin");
    }

    @SuppressWarnings("unused")
    private static String getInputMarkup(Control c) {
        if (c == null) return "(Unbound)";
        int key = c.getKey();
        if (key == -1) return "(Unbound)";
        return "[input=" + key + "]";
    }
}
