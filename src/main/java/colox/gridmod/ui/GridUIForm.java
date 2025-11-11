package colox.gridmod.ui;

import java.awt.Color;
import java.util.Arrays;
import java.util.Collections;

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
import necesse.gfx.forms.components.FormContentButton;
import necesse.gfx.forms.components.FormContentIconButton;
import necesse.gfx.forms.components.localComponents.FormLocalTextButton;
import necesse.gfx.forms.components.FormComponent;
import necesse.gfx.forms.events.FormEventListener;
import necesse.gfx.forms.events.FormInputEvent;
import necesse.gfx.gameFont.FontManager;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.ui.ButtonColor;

import colox.gridmod.config.GridConfig;
import colox.gridmod.paint.PaintControls;
import colox.gridmod.paint.PaintCategory;

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

    private enum Tab { GRID, PAINT, SETTLEMENT, GRID_COLORS, BP_COLORS, SETTLEMENT_COLORS, PAINT_CATEGORY_COLORS }
    private Tab activeTab = Tab.GRID;

    // Content boxes
    private FormContentBox gridBox;
    private FormContentBox paintBox;
    private FormContentBox settlementBox;
    private FormContentBox gridColorsBox;
    private FormContentBox bpColorsBox;
    private FormContentBox categoryColorsBox;
    private FormContentBox settlementColorsBox;

    private FormDropdownSelectionButton<String> categoryColorSelector;
    private FormSlider categoryColorRSlider, categoryColorGSlider, categoryColorBSlider, categoryColorASlider;
    private ColorPreview categoryColorPreview;
    private PaintCategory editingCategoryForSettings;
    private boolean categorySlidersUpdating = false;

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
    // Legacy color tab buttons removed; cogs open panels instead

    // -------- Card helper --------
    private static final int CARD_X = M - 4;
    private static final int CARD_PAD_X = 8;
    private static final int CARD_PAD_Y = 8;
    // 20-color quick palette (RGB 0..1)
    private static final float[][] PALETTE = new float[][]{
            {1f,1f,1f}, {0.9f,0.9f,0.9f}, {0.7f,0.7f,0.7f}, {0f,0f,0f},
            {1f,0f,0f}, {0f,1f,0f}, {0f,0f,1f}, {0f,1f,1f}, {1f,0f,1f}, {1f,1f,0f},
            {1f,0.5f,0f}, {0.6f,0f,0.8f}, {1f,0.4f,0.7f}, {0.59f,0.29f,0f}, {0.5f,1f,0f},
            {0f,0.5f,0.5f}, {0f,0f,0.5f}, {1f,0.84f,0f}, {0.75f,0.75f,0.75f}, {0.56f,0f,1f}
    };

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

    /** Non-interactive swatch used for previews. */
    private static final class ColorPreview extends FormCustomDraw {
        private java.awt.Color fill;
        public ColorPreview(int x, int y, int width, int height, java.awt.Color initial) {
            super(x, y, width, height);
            this.canBePutOnTopByClick = false;
            this.zIndex = Integer.MIN_VALUE / 4;
            this.fill = (initial == null) ? java.awt.Color.WHITE : initial;
        }
        public void setColor(java.awt.Color color) {
            if (color != null) this.fill = color;
        }
        @Override public boolean shouldUseMouseEvents() { return false; }
        @Override public java.util.List<java.awt.Rectangle> getHitboxes() { return java.util.Collections.emptyList(); }
        @Override
        public void draw(necesse.engine.gameLoop.tickManager.TickManager tm, necesse.entity.mobs.PlayerMob perspective, java.awt.Rectangle renderBox) {
            if (fill == null) return;
            java.awt.Color border = this.getInterfaceStyle().activeTextColor;
            Renderer.initQuadDraw(this.width, this.height).color(fill).draw(this.getX(), this.getY());
            Renderer.initQuadDraw(this.width, 1).color(border).draw(this.getX(), this.getY());
            Renderer.initQuadDraw(this.width, 1).color(border).draw(this.getX(), this.getY() + this.height - 1);
            Renderer.initQuadDraw(1, this.height).color(border).draw(this.getX(), this.getY());
            Renderer.initQuadDraw(1, this.height).color(border).draw(this.getX() + this.width - 1, this.getY());
        }
    }

    /** Small square button that renders a solid color inside its content area. */
    private static final class ColorButton extends FormContentButton {
        private float rf, gf, bf;

        public ColorButton(int x, int y, int size, float r, float g, float b) {
            super(x, y, size, FormInputSize.SIZE_24, ButtonColor.BASE);
            setColor(r, g, b);
        }

        public void setColor(float r, float g, float b) {
            this.rf = clamp01(r);
            this.gf = clamp01(g);
            this.bf = clamp01(b);
        }

        @Override
        protected void drawContent(int x, int y, int w, int h) {
            Renderer.initQuadDraw(Math.max(2, w - 6), Math.max(2, h - 6))
                    .color(rf, gf, bf, 1f)
                    .draw(x + 3, y + 3);
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

        categoryColorsBox = new FormContentBox(boxX, boxY, boxW, boxH);
        categoryColorsBox.alwaysShowVerticalScrollBar = true;
        this.addComponent(categoryColorsBox);

        settlementColorsBox = new FormContentBox(boxX, boxY, boxW, boxH);
        settlementColorsBox.alwaysShowVerticalScrollBar = true;
        this.addComponent(settlementColorsBox);

        // Start with only GRID visible
        paintBox.setHidden(true);
        settlementBox.setHidden(true);
        gridColorsBox.setHidden(true);
        bpColorsBox.setHidden(true);
        categoryColorsBox.setHidden(true);
        settlementColorsBox.setHidden(true);

        // ======== GRID tab (delegated) ========
        new GridTab(gridBox, v -> this.drawBaseAlpha = v, () -> switchTab(Tab.GRID_COLORS));

        // ======== PAINT tab ========
        new PaintTab(paintBox, () -> switchTab(Tab.BP_COLORS), () -> switchTab(Tab.PAINT_CATEGORY_COLORS));

        // ======== SETTLEMENT (non-color) ========
        buildSettlementContent();

        // ======== COLORS ========
        buildGridColorsContent();
        buildBlueprintColorsContent();
        buildPaintCategoryColorsContent();
        buildSettlementColorsContent();
    }

    // ---------- Tabs (single row) ----------
    private int buildTabsBelowTitle() {
        final int startX = M;
        final int gapX = 6;
        final FormInputSize tabSize = FormInputSize.SIZE_24;
        final int tabH = tabSize.height;

        final String gridLabel  = "Grid";
        final String paintLabel = "Paint & Blueprints";
        final String settlementLabel = "Settlement";

        int gridW   = computeTabWidth(gridLabel, tabSize);
        int paintW  = computeTabWidth(paintLabel, tabSize);
        int settleW = computeTabWidth(settlementLabel, tabSize);

        int yMain = M + LINE;
        int x = startX;

        gridTabBtn = new FormTextButton(gridLabel, x, yMain - 2, gridW, tabSize, ButtonColor.BASE);
        this.addComponent(gridTabBtn);
        x += gridW + gapX;

        paintTabBtn = new FormTextButton(paintLabel, x, yMain - 2, paintW, tabSize, ButtonColor.BASE);
        this.addComponent(paintTabBtn);
        x += paintW + gapX;

        settlementTabBtn = new FormTextButton(settlementLabel, x, yMain - 2, settleW, tabSize, ButtonColor.BASE);
        this.addComponent(settlementTabBtn);

        gridTabBtn.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> switchTab(Tab.GRID));
        paintTabBtn.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> switchTab(Tab.PAINT));
        settlementTabBtn.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> switchTab(Tab.SETTLEMENT));

        return (yMain + tabH) - (M + LINE);
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
        categoryColorsBox.setHidden(!(activeTab == Tab.PAINT_CATEGORY_COLORS));
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
        // Inline cog to open Settlement Colors
        int cogX = sx + 260;
        FormContentIconButton settleCog = addTo(settlementBox, new FormContentIconButton(cogX, sy - 4, FormInputSize.SIZE_24, ButtonColor.BASE, settlementBox.getInterfaceStyle().config_button_32));
        settleCog.onClicked(e -> switchTab(Tab.SETTLEMENT_COLORS));
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

        addTo(settlementBox, new FormLabel("Manual sizes (tiles) ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â applies only when Mode = manual", new FontOptions(14), FormLabel.ALIGN_LEFT, sx, sy));
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

        // Back to Grid tab
        FormLocalTextButton backBtnGrid = addTo(gridColorsBox, new FormLocalTextButton("ui", "backbutton", CARD_X + CARD_PAD_X, y, 120, FormInputSize.SIZE_24, ButtonColor.BASE));
        backBtnGrid.onClicked(e -> switchTab(Tab.GRID));
        y += FormInputSize.SIZE_24.height + 8;

        // Base grid color
        SectionCard cBase = addCard(gridColorsBox, y, inner);
        int gx = CARD_X + CARD_PAD_X;
        int gy = y + CARD_PAD_Y;

        addTo(gridColorsBox, new FormLabel("Base Grid Color", new FontOptions(16), FormLabel.ALIGN_LEFT, gx, gy));
        final int gridPrevW = 120, gridPrevH = 18;
        final int gridPrevX = gx + Math.max(220, usableRow) - gridPrevW;
        final int gridPrevY = gy;
        addTo(gridColorsBox, new FormCustomDraw(gridPrevX, gridPrevY, gridPrevW, gridPrevH) {
            { this.canBePutOnTopByClick = false; this.zIndex = Integer.MIN_VALUE / 4; }
            @Override public boolean shouldUseMouseEvents() { return false; }
            @Override public java.util.List<java.awt.Rectangle> getHitboxes() { return Collections.emptyList(); }
            @Override public void draw(necesse.engine.gameLoop.tickManager.TickManager tm, necesse.entity.mobs.PlayerMob p, java.awt.Rectangle rbox) {
                java.awt.Color border = getInterfaceStyle().activeTextColor;
                java.awt.Color fill = new java.awt.Color(Math.round(GridConfig.r*255), Math.round(GridConfig.g*255), Math.round(GridConfig.b*255));
                Renderer.initQuadDraw(this.width, this.height).color(fill).draw(this.getX(), this.getY());
                Renderer.initQuadDraw(this.width, 1).color(border).draw(this.getX(), this.getY());
                Renderer.initQuadDraw(this.width, 1).color(border).draw(this.getX(), this.getY()+this.height-1);
                Renderer.initQuadDraw(1, this.height).color(border).draw(this.getX(), this.getY());
                Renderer.initQuadDraw(1, this.height).color(border).draw(this.getX()+this.width-1, this.getY());
            }
        });
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

        // Palette for base grid
        int sw = 24, swGap = 4, swX = gx, swY = gy + 6;
        int perRow = Math.max(6, Math.min(10, (usableRow / (sw + swGap))));
        for (int i = 0; i < PALETTE.length; i++) {
            final float prR = PALETTE[i][0], prG = PALETTE[i][1], prB = PALETTE[i][2];
            ColorButton b = addTo(gridColorsBox, new ColorButton(swX, swY, sw, prR, prG, prB));
            b.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> { GridConfig.r = prR; GridConfig.g = prG; GridConfig.b = prB; GridConfig.markDirty(); GridConfig.saveIfDirty(); });
            if ((i+1) % perRow == 0) { swX = gx; swY += sw + swGap; } else { swX += sw + swGap; }
        }
        gy = swY + sw + 10;
        finishCard(cBase, gy);
        y = cBase.getY() + cBase.height + 10;

        // Chunk lines color
        SectionCard cChunk = addCard(gridColorsBox, y, inner);
        gx = CARD_X + CARD_PAD_X;
        gy = y + CARD_PAD_Y;

        addTo(gridColorsBox, new FormLabel("Chunk Lines Color", new FontOptions(14), FormLabel.ALIGN_LEFT, gx, gy));
        addTo(gridColorsBox, new FormCustomDraw(gridPrevX, gy, gridPrevW, gridPrevH) {
            { this.canBePutOnTopByClick = false; this.zIndex = Integer.MIN_VALUE / 4; }
            @Override public boolean shouldUseMouseEvents() { return false; }
            @Override public java.util.List<java.awt.Rectangle> getHitboxes() { return Collections.emptyList(); }
            @Override public void draw(necesse.engine.gameLoop.tickManager.TickManager tm, necesse.entity.mobs.PlayerMob p, java.awt.Rectangle rbox) {
                java.awt.Color border = getInterfaceStyle().activeTextColor;
                java.awt.Color fill = new java.awt.Color(Math.round(GridConfig.cr*255), Math.round(GridConfig.cg*255), Math.round(GridConfig.cb*255));
                Renderer.initQuadDraw(this.width, this.height).color(fill).draw(this.getX(), this.getY());
                Renderer.initQuadDraw(this.width, 1).color(border).draw(this.getX(), this.getY());
                Renderer.initQuadDraw(this.width, 1).color(border).draw(this.getX(), this.getY()+this.height-1);
                Renderer.initQuadDraw(1, this.height).color(border).draw(this.getX(), this.getY());
                Renderer.initQuadDraw(1, this.height).color(border).draw(this.getX()+this.width-1, this.getY());
            }
        });
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

        // Palette for chunk color
        sw = 24; swGap = 4; swX = gx; swY = gy + 6; perRow = Math.max(6, Math.min(10, (usableRow / (sw + swGap))));
        for (int i = 0; i < PALETTE.length; i++) {
            final float prR = PALETTE[i][0], prG = PALETTE[i][1], prB = PALETTE[i][2];
            ColorButton b = addTo(gridColorsBox, new ColorButton(swX, swY, sw, prR, prG, prB));
            b.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> { GridConfig.cr = prR; GridConfig.cg = prG; GridConfig.cb = prB; GridConfig.markDirty(); GridConfig.saveIfDirty(); });
            if ((i+1) % perRow == 0) { swX = gx; swY += sw + swGap; } else { swX += sw + swGap; }
        }
        gy = swY + sw + 10;
        finishCard(cChunk, gy);
        y = cChunk.getY() + cChunk.height + 10;

        // Sub-chunk lines color
        SectionCard cSub = addCard(gridColorsBox, y, inner);
        gx = CARD_X + CARD_PAD_X;
        gy = y + CARD_PAD_Y;

        addTo(gridColorsBox, new FormLabel("Sub-chunk Lines Color", new FontOptions(14), FormLabel.ALIGN_LEFT, gx, gy));
        addTo(gridColorsBox, new FormCustomDraw(gridPrevX, gy, gridPrevW, gridPrevH) {
            { this.canBePutOnTopByClick = false; this.zIndex = Integer.MIN_VALUE / 4; }
            @Override public boolean shouldUseMouseEvents() { return false; }
            @Override public java.util.List<java.awt.Rectangle> getHitboxes() { return Collections.emptyList(); }
            @Override public void draw(necesse.engine.gameLoop.tickManager.TickManager tm, necesse.entity.mobs.PlayerMob p, java.awt.Rectangle rbox) {
                java.awt.Color border = getInterfaceStyle().activeTextColor;
                java.awt.Color fill = new java.awt.Color(Math.round(GridConfig.scr*255), Math.round(GridConfig.scg*255), Math.round(GridConfig.scb*255));
                Renderer.initQuadDraw(this.width, this.height).color(fill).draw(this.getX(), this.getY());
                Renderer.initQuadDraw(this.width, 1).color(border).draw(this.getX(), this.getY());
                Renderer.initQuadDraw(this.width, 1).color(border).draw(this.getX(), this.getY()+this.height-1);
                Renderer.initQuadDraw(1, this.height).color(border).draw(this.getX(), this.getY());
                Renderer.initQuadDraw(1, this.height).color(border).draw(this.getX()+this.width-1, this.getY());
            }
        });
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

        // Palette for sub-chunk color
        sw = 24; swGap = 4; swX = gx; swY = gy + 6; perRow = Math.max(6, Math.min(10, (usableRow / (sw + swGap))));
        for (int i = 0; i < PALETTE.length; i++) {
            final float prR = PALETTE[i][0], prG = PALETTE[i][1], prB = PALETTE[i][2];
            ColorButton b = addTo(gridColorsBox, new ColorButton(swX, swY, sw, prR, prG, prB));
            b.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> { GridConfig.scr = prR; GridConfig.scg = prG; GridConfig.scb = prB; GridConfig.markDirty(); GridConfig.saveIfDirty(); });
            addTo(gridColorsBox, new FormCustomDraw(swX+3, swY+3, sw-6, sw-6) {
                { this.canBePutOnTopByClick = false; }
                @Override public boolean shouldUseMouseEvents() { return false; }
                @Override public java.util.List<java.awt.Rectangle> getHitboxes() { return Collections.emptyList(); }
                @Override public void draw(necesse.engine.gameLoop.tickManager.TickManager tm, necesse.entity.mobs.PlayerMob p, java.awt.Rectangle rbox) {
                    java.awt.Color fill = new java.awt.Color(Math.round(prR*255), Math.round(prG*255), Math.round(prB*255));
                    java.awt.Color border = getInterfaceStyle().activeTextColor;
                    Renderer.initQuadDraw(this.width, this.height).color(fill).draw(this.getX(), this.getY());
                    Renderer.initQuadDraw(this.width, 1).color(border).draw(this.getX(), this.getY());
                    Renderer.initQuadDraw(this.width, 1).color(border).draw(this.getX(), this.getY()+this.height-1);
                    Renderer.initQuadDraw(1, this.height).color(border).draw(this.getX(), this.getY());
                    Renderer.initQuadDraw(1, this.height).color(border).draw(this.getX()+this.width-1, this.getY());
                }
            });
            if ((i+1) % perRow == 0) { swX = gx; swY += sw + swGap; } else { swX += sw + swGap; }
        }
        gy = swY + sw + 10;
        finishCard(cSub, gy);
        gridColorsBox.fitContentBoxToComponents(6);
    }

    // ---------- BLUEPRINT / PAINT COLORS ----------
    private void buildBlueprintColorsContent() {
        final int inner = bpColorsBox.getMinContentWidth();
        final int usableRow = (inner - (CARD_X * 2)) - (CARD_PAD_X * 2);
        int y = 0;

        // Back to Paint tab
        FormLocalTextButton backBtnPaint = addTo(bpColorsBox, new FormLocalTextButton("ui", "backbutton", CARD_X + CARD_PAD_X, y, 120, FormInputSize.SIZE_24, ButtonColor.BASE));
        backBtnPaint.onClicked(e -> switchTab(Tab.PAINT));
        y += FormInputSize.SIZE_24.height + 8;

        // Brush paint
        SectionCard cardPaint = addCard(bpColorsBox, y, inner);
        int x = CARD_X + CARD_PAD_X;
        int cy = y + CARD_PAD_Y;

        addTo(bpColorsBox, new FormLabel("Brush Paint", new FontOptions(16), FormLabel.ALIGN_LEFT, x, cy));
        // Live preview (uses current GridConfig.paint* RGBA)
        final int previewW = 120;
        final int previewH = 18;
        final int previewX = x + Math.max(220, usableRow) - previewW;
        final int previewY = cy;
        addTo(bpColorsBox, new FormCustomDraw(previewX, previewY, previewW, previewH) {
            { this.canBePutOnTopByClick = false; this.zIndex = Integer.MIN_VALUE / 4; }
            @Override public boolean shouldUseMouseEvents() { return false; }
            @Override public java.util.List<java.awt.Rectangle> getHitboxes() { return Collections.emptyList(); }
            @Override public void draw(necesse.engine.gameLoop.tickManager.TickManager tm, necesse.entity.mobs.PlayerMob p, java.awt.Rectangle rbox) {
                float rr = GridConfig.paintR, gg = GridConfig.paintG, bb = GridConfig.paintB, aa = GridConfig.paintAlpha;
                java.awt.Color border = getInterfaceStyle().activeTextColor;
                java.awt.Color fill = new java.awt.Color(Math.round(rr*255), Math.round(gg*255), Math.round(bb*255), Math.round(aa*255));
                Renderer.initQuadDraw(this.width, this.height).color(fill).draw(this.getX(), this.getY());
                Renderer.initQuadDraw(this.width, 1).color(border).draw(this.getX(), this.getY());
                Renderer.initQuadDraw(this.width, 1).color(border).draw(this.getX(), this.getY() + this.height - 1);
                Renderer.initQuadDraw(1, this.height).color(border).draw(this.getX(), this.getY());
                Renderer.initQuadDraw(1, this.height).color(border).draw(this.getX() + this.width - 1, this.getY());
            }
        });
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
        cy += pa.getTotalHeight() + 6;

        // Quick palette (RGB only; preserves alpha)
        int swX = x;
        int swY = cy;
        int sw = 24; int swGap = 4; int perRow = Math.max(6, Math.min(10, (usableRow / (sw + swGap))));
        for (int i = 0; i < PALETTE.length; i++) {
            final float prR = PALETTE[i][0], prG = PALETTE[i][1], prB = PALETTE[i][2];
            ColorButton b = addTo(bpColorsBox, new ColorButton(swX, swY, sw, prR, prG, prB));
            b.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> { GridConfig.paintR = prR; GridConfig.paintG = prG; GridConfig.paintB = prB; GridConfig.markDirty(); GridConfig.saveIfDirty(); });

            if ((i+1) % perRow == 0) { swX = x; swY += sw + swGap; }
            else { swX += sw + swGap; }
        }
        cy = swY + sw + 10;

        finishCard(cardPaint, cy);
        y = cardPaint.getY() + cardPaint.height + 10;

        // Erase/clear preview
        SectionCard cardErase = addCard(bpColorsBox, y, inner);
        x = CARD_X + CARD_PAD_X;
        cy = y + CARD_PAD_Y;

        addTo(bpColorsBox, new FormLabel("Erase Preview", new FontOptions(14), FormLabel.ALIGN_LEFT, x, cy));
        // Preview for erase color
        addTo(bpColorsBox, new FormCustomDraw(previewX, cy, previewW, previewH) {
            { this.canBePutOnTopByClick = false; this.zIndex = Integer.MIN_VALUE / 4; }
            @Override public boolean shouldUseMouseEvents() { return false; }
            @Override public java.util.List<java.awt.Rectangle> getHitboxes() { return Collections.emptyList(); }
            @Override public void draw(necesse.engine.gameLoop.tickManager.TickManager tm, necesse.entity.mobs.PlayerMob p, java.awt.Rectangle rbox) {
                float rr = GridConfig.eraseR, gg = GridConfig.eraseG, bb = GridConfig.eraseB, aa = GridConfig.eraseAlpha;
                java.awt.Color border = getInterfaceStyle().activeTextColor;
                java.awt.Color fill = new java.awt.Color(Math.round(rr*255), Math.round(gg*255), Math.round(bb*255), Math.round(aa*255));
                Renderer.initQuadDraw(this.width, this.height).color(fill).draw(this.getX(), this.getY());
                Renderer.initQuadDraw(this.width, 1).color(border).draw(this.getX(), this.getY());
                Renderer.initQuadDraw(this.width, 1).color(border).draw(this.getX(), this.getY() + this.height - 1);
                Renderer.initQuadDraw(1, this.height).color(border).draw(this.getX(), this.getY());
                Renderer.initQuadDraw(1, this.height).color(border).draw(this.getX() + this.width - 1, this.getY());
            }
        });
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
        cy += ea.getTotalHeight() + 6;
        // Palette for erase
        swX = x; swY = cy;
        for (int i = 0; i < PALETTE.length; i++) {
            final float prR = PALETTE[i][0], prG = PALETTE[i][1], prB = PALETTE[i][2];
            ColorButton b = addTo(bpColorsBox, new ColorButton(swX, swY, sw, prR, prG, prB));
            b.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> { GridConfig.eraseR = prR; GridConfig.eraseG = prG; GridConfig.eraseB = prB; GridConfig.markDirty(); GridConfig.saveIfDirty(); });
            if ((i+1) % perRow == 0) { swX = x; swY += sw + swGap; } else { swX += sw + swGap; }
        }
        cy = swY + sw + 10;

        finishCard(cardErase, cy);
        y = cardErase.getY() + cardErase.height + 10;

        // Selection rectangle
        SectionCard cardSel = addCard(bpColorsBox, y, inner);
        x = CARD_X + CARD_PAD_X;
        cy = y + CARD_PAD_Y;

        addTo(bpColorsBox, new FormLabel("Selection Rectangle", new FontOptions(14), FormLabel.ALIGN_LEFT, x, cy));
        addTo(bpColorsBox, new FormCustomDraw(previewX, cy, previewW, previewH) {
            { this.canBePutOnTopByClick = false; this.zIndex = Integer.MIN_VALUE / 4; }
            @Override public boolean shouldUseMouseEvents() { return false; }
            @Override public java.util.List<java.awt.Rectangle> getHitboxes() { return Collections.emptyList(); }
            @Override public void draw(necesse.engine.gameLoop.tickManager.TickManager tm, necesse.entity.mobs.PlayerMob p, java.awt.Rectangle rbox) {
                float rr = GridConfig.selectionR, gg = GridConfig.selectionG, bb = GridConfig.selectionB, aa = GridConfig.selectionAlpha;
                java.awt.Color border = getInterfaceStyle().activeTextColor;
                java.awt.Color fill = new java.awt.Color(Math.round(rr*255), Math.round(gg*255), Math.round(bb*255), Math.round(aa*255));
                Renderer.initQuadDraw(this.width, this.height).color(fill).draw(this.getX(), this.getY());
                Renderer.initQuadDraw(this.width, 1).color(border).draw(this.getX(), this.getY());
                Renderer.initQuadDraw(this.width, 1).color(border).draw(this.getX(), this.getY() + this.height - 1);
                Renderer.initQuadDraw(1, this.height).color(border).draw(this.getX(), this.getY());
                Renderer.initQuadDraw(1, this.height).color(border).draw(this.getX() + this.width - 1, this.getY());
            }
        });
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
        cy += sa.getTotalHeight() + 6;
        // Palette for selection
        swX = x; swY = cy;
        for (int i = 0; i < PALETTE.length; i++) {
            final float prR = PALETTE[i][0], prG = PALETTE[i][1], prB = PALETTE[i][2];
            ColorButton b = addTo(bpColorsBox, new ColorButton(swX, swY, sw, prR, prG, prB));
            b.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> { GridConfig.selectionR = prR; GridConfig.selectionG = prG; GridConfig.selectionB = prB; GridConfig.markDirty(); GridConfig.saveIfDirty(); });
            if ((i+1) % perRow == 0) { swX = x; swY += sw + swGap; } else { swX += sw + swGap; }
        }
        cy = swY + sw + 10;

        finishCard(cardSel, cy);
        y = cardSel.getY() + cardSel.height + 10;

        // Blueprint ghost preview
        SectionCard cardGhost = addCard(bpColorsBox, y, inner);
        x = CARD_X + CARD_PAD_X;
        cy = y + CARD_PAD_Y;

        addTo(bpColorsBox, new FormLabel("Blueprint Ghost", new FontOptions(14), FormLabel.ALIGN_LEFT, x, cy));
        addTo(bpColorsBox, new FormCustomDraw(previewX, cy, previewW, previewH) {
            { this.canBePutOnTopByClick = false; this.zIndex = Integer.MIN_VALUE / 4; }
            @Override public boolean shouldUseMouseEvents() { return false; }
            @Override public java.util.List<java.awt.Rectangle> getHitboxes() { return Collections.emptyList(); }
            @Override public void draw(necesse.engine.gameLoop.tickManager.TickManager tm, necesse.entity.mobs.PlayerMob p, java.awt.Rectangle rbox) {
                float rr = GridConfig.bpGhostR, gg = GridConfig.bpGhostG, bb = GridConfig.bpGhostB, aa = GridConfig.bpGhostAlpha;
                java.awt.Color border = getInterfaceStyle().activeTextColor;
                java.awt.Color fill = new java.awt.Color(Math.round(rr*255), Math.round(gg*255), Math.round(bb*255), Math.round(aa*255));
                Renderer.initQuadDraw(this.width, this.height).color(fill).draw(this.getX(), this.getY());
                Renderer.initQuadDraw(this.width, 1).color(border).draw(this.getX(), this.getY());
                Renderer.initQuadDraw(this.width, 1).color(border).draw(this.getX(), this.getY() + this.height - 1);
                Renderer.initQuadDraw(1, this.height).color(border).draw(this.getX(), this.getY());
                Renderer.initQuadDraw(1, this.height).color(border).draw(this.getX() + this.width - 1, this.getY());
            }
        });
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
        cy += ga.getTotalHeight() + 6;
        // Palette for blueprint ghost
        swX = x; swY = cy;
        for (int i = 0; i < PALETTE.length; i++) {
            final float prR = PALETTE[i][0], prG = PALETTE[i][1], prB = PALETTE[i][2];
            ColorButton b = addTo(bpColorsBox, new ColorButton(swX, swY, sw, prR, prG, prB));
            b.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> { GridConfig.bpGhostR = prR; GridConfig.bpGhostG = prG; GridConfig.bpGhostB = prB; GridConfig.markDirty(); GridConfig.saveIfDirty(); });
            if ((i+1) % perRow == 0) { swX = x; swY += sw + swGap; } else { swX += sw + swGap; }
        }
        cy = swY + sw + 10;

        finishCard(cardGhost, cy);
        bpColorsBox.fitContentBoxToComponents(6);
    }

    private void buildPaintCategoryColorsContent() {
        final int inner = categoryColorsBox.getMinContentWidth();
        final int usableRow = (inner - (CARD_X * 2)) - (CARD_PAD_X * 2);
        int y = 0;

        FormLocalTextButton back = addTo(categoryColorsBox, new FormLocalTextButton("ui", "backbutton", CARD_X + CARD_PAD_X, y, 120, FormInputSize.SIZE_24, ButtonColor.BASE));
        back.onClicked(e -> switchTab(Tab.PAINT));
        y += FormInputSize.SIZE_24.height + 8;

        SectionCard card = addCard(categoryColorsBox, y, inner);
        int cx = CARD_X + CARD_PAD_X;
        int cy = y + CARD_PAD_Y;

        addTo(categoryColorsBox, new FormLabel("Paint categories", new FontOptions(16), FormLabel.ALIGN_LEFT, cx, cy));
        cy += LINE - 10;

        addTo(categoryColorsBox, new FormLabel("Category:", new FontOptions(12), FormLabel.ALIGN_LEFT, cx, cy));
        int ddW = Math.max(220, usableRow - 150);
        categoryColorSelector = addTo(categoryColorsBox, new FormDropdownSelectionButton<>(cx + 90, cy - 2, FormInputSize.SIZE_24, ButtonColor.BASE, ddW));
        for (PaintCategory cat : PaintCategory.values()) {
            categoryColorSelector.options.add(cat.id(), new StaticMessage(cat.label()));
        }
        PaintCategory startCat = GridConfig.getActivePaintCategory();
        categoryColorSelector.setSelected(startCat.id(), new StaticMessage(startCat.label()));
        categoryColorSelector.onSelected(e -> {
            PaintCategory selected = PaintCategory.byId(e.value);
            GridConfig.setActivePaintCategory(selected);
            selectCategory(selected);
        });
        cy += FormInputSize.SIZE_24.height + 10;

        final int previewW = 120, previewH = 18;
        int previewX = cx + Math.max(220, usableRow) - previewW;
        categoryColorPreview = addTo(categoryColorsBox,
                new ColorPreview(previewX, cy, previewW, previewH, paintColorToAwt(GridConfig.getPaintColor(startCat))));
        cy += previewH + 10;

        int sliderW = Math.max(220, usableRow);
        categoryColorRSlider = addTo(categoryColorsBox, new FormSlider("Red", cx, cy,
                Math.round(GridConfig.getPaintColor(startCat).r * 100f), 0, 100, sliderW));
        categoryColorRSlider.drawValue = true; categoryColorRSlider.drawValueInPercent = true;
        categoryColorRSlider.onChanged(e -> updatePaintCategoryFromSliders());
        cy += categoryColorRSlider.getTotalHeight() + 6;

        categoryColorGSlider = addTo(categoryColorsBox, new FormSlider("Green", cx, cy,
                Math.round(GridConfig.getPaintColor(startCat).g * 100f), 0, 100, sliderW));
        categoryColorGSlider.drawValue = true; categoryColorGSlider.drawValueInPercent = true;
        categoryColorGSlider.onChanged(e -> updatePaintCategoryFromSliders());
        cy += categoryColorGSlider.getTotalHeight() + 6;

        categoryColorBSlider = addTo(categoryColorsBox, new FormSlider("Blue", cx, cy,
                Math.round(GridConfig.getPaintColor(startCat).b * 100f), 0, 100, sliderW));
        categoryColorBSlider.drawValue = true; categoryColorBSlider.drawValueInPercent = true;
        categoryColorBSlider.onChanged(e -> updatePaintCategoryFromSliders());
        cy += categoryColorBSlider.getTotalHeight() + 6;

        categoryColorASlider = addTo(categoryColorsBox, new FormSlider("Alpha", cx, cy,
                Math.round(GridConfig.getPaintColor(startCat).a * 100f), 0, 100, sliderW));
        categoryColorASlider.drawValue = true; categoryColorASlider.drawValueInPercent = true;
        categoryColorASlider.onChanged(e -> updatePaintCategoryFromSliders());
        cy += categoryColorASlider.getTotalHeight() + 10;

        int sw = 24, swGap = 4;
        int swX = cx;
        int swY = cy;
        int perRow = Math.max(6, Math.min(10, (usableRow / (sw + swGap))));
        for (int i = 0; i < PALETTE.length; i++) {
            final float prR = PALETTE[i][0], prG = PALETTE[i][1], prB = PALETTE[i][2];
            ColorButton b = addTo(categoryColorsBox, new ColorButton(swX, swY, sw, prR, prG, prB));
            b.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> applyCategoryPaletteColor(prR, prG, prB));
            if ((i + 1) % perRow == 0) { swX = cx; swY += sw + swGap; }
            else { swX += sw + swGap; }
        }
        cy = swY + sw + 12;

        FormTextButton resetBtn = addTo(categoryColorsBox, new FormTextButton("Reset to default", cx, cy, 170,
                FormInputSize.SIZE_24, ButtonColor.BASE));
        resetBtn.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> resetCategoryToDefault());
        cy += FormInputSize.SIZE_24.height + 10;

        finishCard(card, cy);
        categoryColorsBox.fitContentBoxToComponents(6);

        selectCategory(startCat);
    }

    // ---------- SETTLEMENT COLORS ----------
    private void buildSettlementColorsContent() {
        final int inner = settlementColorsBox.getMinContentWidth();
        final int usableRow = (inner - (CARD_X * 2)) - (CARD_PAD_X * 2);
        int y = 0;

        // Back to Settlement tab
        FormLocalTextButton backBtnSettle = addTo(settlementColorsBox, new FormLocalTextButton("ui", "backbutton", CARD_X + CARD_PAD_X, y, 120, FormInputSize.SIZE_24, ButtonColor.BASE));
        backBtnSettle.onClicked(e -> switchTab(Tab.SETTLEMENT));
        y += FormInputSize.SIZE_24.height + 8;

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
        final int setPrevW = 120, setPrevH = 18;
        final int setPrevX = sx + Math.max(220, usableRow) - setPrevW;
        final int setPrevY = sy;
        addTo(settlementColorsBox, new FormCustomDraw(setPrevX, setPrevY, setPrevW, setPrevH) {
            { this.canBePutOnTopByClick = false; this.zIndex = Integer.MIN_VALUE / 4; }
            @Override public boolean shouldUseMouseEvents() { return false; }
            @Override public java.util.List<java.awt.Rectangle> getHitboxes() { return Collections.emptyList(); }
            @Override public void draw(necesse.engine.gameLoop.tickManager.TickManager tm, necesse.entity.mobs.PlayerMob p, java.awt.Rectangle rbox) {
                java.awt.Color border = getInterfaceStyle().activeTextColor;
                java.awt.Color fill = new java.awt.Color(Math.round(GridConfig.sbr*255), Math.round(GridConfig.sbg*255), Math.round(GridConfig.sbb*255));
                Renderer.initQuadDraw(this.width, this.height).color(fill).draw(this.getX(), this.getY());
                Renderer.initQuadDraw(this.width, 1).color(border).draw(this.getX(), this.getY());
                Renderer.initQuadDraw(this.width, 1).color(border).draw(this.getX(), this.getY()+this.height-1);
                Renderer.initQuadDraw(1, this.height).color(border).draw(this.getX(), this.getY());
                Renderer.initQuadDraw(1, this.height).color(border).draw(this.getX()+this.width-1, this.getY());
            }
        });
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

        // Palette for settlement color
        int sw = 24, swGap = 4, swX = sx, swY = sy + 6;
        int perRow = Math.max(6, Math.min(10, (usableRow / (sw + swGap))));
        for (int i = 0; i < PALETTE.length; i++) {
            final float prR = PALETTE[i][0], prG = PALETTE[i][1], prB = PALETTE[i][2];
            ColorButton b = addTo(settlementColorsBox, new ColorButton(swX, swY, sw, prR, prG, prB));
            b.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> { GridConfig.sbr = prR; GridConfig.sbg = prG; GridConfig.sbb = prB; GridConfig.markDirty(); GridConfig.saveIfDirty(); });
            addTo(settlementColorsBox, new FormCustomDraw(swX+3, swY+3, sw-6, sw-6) {
                { this.canBePutOnTopByClick = false; }
                @Override public boolean shouldUseMouseEvents() { return false; }
                @Override public java.util.List<java.awt.Rectangle> getHitboxes() { return Collections.emptyList(); }
                @Override public void draw(necesse.engine.gameLoop.tickManager.TickManager tm, necesse.entity.mobs.PlayerMob p, java.awt.Rectangle rbox) {
                    java.awt.Color fill = new java.awt.Color(Math.round(prR*255), Math.round(prG*255), Math.round(prB*255));
                    java.awt.Color border = getInterfaceStyle().activeTextColor;
                    Renderer.initQuadDraw(this.width, this.height).color(fill).draw(this.getX(), this.getY());
                    Renderer.initQuadDraw(this.width, 1).color(border).draw(this.getX(), this.getY());
                    Renderer.initQuadDraw(this.width, 1).color(border).draw(this.getX(), this.getY()+this.height-1);
                    Renderer.initQuadDraw(1, this.height).color(border).draw(this.getX(), this.getY());
                    Renderer.initQuadDraw(1, this.height).color(border).draw(this.getX()+this.width-1, this.getY());
                }
            });
            if ((i+1) % perRow == 0) { swX = sx; swY += sw + swGap; } else { swX += sw + swGap; }
        }
        sy = swY + sw + 10;

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
        settlementInfoLabel.setText("Side: " + tiles + " tiles (" + chunks + "ÃƒÆ’Ã¢â‚¬â€16-tile chunks per side)");
    }
    private void refreshSettlementInfoIfTier(int tier) {
        if (GridConfig.settlementTier == tier) refreshSettlementInfo();
    }

    private void selectCategory(PaintCategory category) {
        PaintCategory cat = (category == null) ? PaintCategory.TILES : category;
        editingCategoryForSettings = cat;
        if (categoryColorSelector != null) {
            categoryColorSelector.setSelected(cat.id(), new StaticMessage(cat.label()));
        }
        GridConfig.setActivePaintCategory(cat);
        updateSlidersFromCategory(cat);
    }

    private void updateSlidersFromCategory(PaintCategory category) {
        if (category == null) return;
        GridConfig.PaintColor color = GridConfig.getPaintColor(category);
        categorySlidersUpdating = true;
        if (categoryColorRSlider != null) categoryColorRSlider.setValue(Math.round(color.r * 100f));
        if (categoryColorGSlider != null) categoryColorGSlider.setValue(Math.round(color.g * 100f));
        if (categoryColorBSlider != null) categoryColorBSlider.setValue(Math.round(color.b * 100f));
        if (categoryColorASlider != null) categoryColorASlider.setValue(Math.round(color.a * 100f));
        categorySlidersUpdating = false;
        if (categoryColorPreview != null) categoryColorPreview.setColor(paintColorToAwt(color));
    }

    private void updatePaintCategoryFromSliders() {
        if (categorySlidersUpdating || editingCategoryForSettings == null) return;
        float r = categoryColorRSlider.getValue() / 100f;
        float g = categoryColorGSlider.getValue() / 100f;
        float b = categoryColorBSlider.getValue() / 100f;
        float a = categoryColorASlider.getValue() / 100f;
        GridConfig.setPaintColor(editingCategoryForSettings, r, g, b, a);
        if (categoryColorPreview != null) categoryColorPreview.setColor(new java.awt.Color(Math.max(0, Math.min(255, Math.round(r * 255f))),
                Math.max(0, Math.min(255, Math.round(g * 255f))),
                Math.max(0, Math.min(255, Math.round(b * 255f))),
                Math.max(0, Math.min(255, Math.round(a * 255f)))));
    }

    private void applyCategoryPaletteColor(float r, float g, float b) {
        if (editingCategoryForSettings == null) return;
        float a = categoryColorASlider.getValue() / 100f;
        GridConfig.setPaintColor(editingCategoryForSettings, r, g, b, a);
        updateSlidersFromCategory(editingCategoryForSettings);
    }

    private void resetCategoryToDefault() {
        if (editingCategoryForSettings == null) return;
        PaintCategory cat = editingCategoryForSettings;
        GridConfig.setPaintColor(cat, cat.defaultR(), cat.defaultG(), cat.defaultB(), cat.defaultA());
        updateSlidersFromCategory(cat);
    }

    private java.awt.Color paintColorToAwt(GridConfig.PaintColor color) {
        if (color == null) return new java.awt.Color(255, 255, 255, 255);
        return new java.awt.Color(Math.max(0, Math.min(255, Math.round(color.r * 255f))),
                Math.max(0, Math.min(255, Math.round(color.g * 255f))),
                Math.max(0, Math.min(255, Math.round(color.b * 255f))),
                Math.max(0, Math.min(255, Math.round(color.a * 255f))));
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



