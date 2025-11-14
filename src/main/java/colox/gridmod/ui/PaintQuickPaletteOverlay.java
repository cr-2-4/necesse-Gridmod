package colox.gridmod.ui;

import java.awt.Rectangle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;

import colox.gridmod.config.GridConfig;
import colox.gridmod.paint.BlueprintPlacement;
import colox.gridmod.paint.DefaultBlueprintRegistry;
import colox.gridmod.paint.PaintBlueprints;
import colox.gridmod.paint.PaintCategory;
import colox.gridmod.paint.PaintControls;
import colox.gridmod.paint.PaintLayer;
import colox.gridmod.paint.PaintLayerFilter;
import colox.gridmod.paint.PaintState;
import colox.gridmod.paint.SelectionState;
import necesse.engine.GlobalData;
import necesse.engine.input.InputEvent;
import necesse.engine.input.InputPosition;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.gfx.Renderer;
import necesse.gfx.forms.Form;
import necesse.gfx.forms.components.FormButton;
import necesse.gfx.forms.components.FormCheckBox;
import necesse.gfx.forms.components.FormComponent;
import necesse.gfx.forms.components.FormContentBox;
import necesse.gfx.forms.components.FormCustomDraw;
import necesse.gfx.forms.components.FormDropdownSelectionButton;
import necesse.gfx.forms.components.FormInputSize;
import necesse.gfx.forms.components.FormLabel;
import necesse.gfx.forms.components.FormSlider;
import necesse.gfx.forms.components.FormTextInput;
import necesse.gfx.forms.components.FormTextButton;
import necesse.gfx.forms.events.FormEventListener;
import necesse.gfx.forms.events.FormInputEvent;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.ui.ButtonColor;
import necesse.engine.localization.message.StaticMessage;

/**
 * Side-panel overlay buttons for quick paint/grid/settlement/blueprint tweaks.
 */
public final class PaintQuickPaletteOverlay {

    private PaintQuickPaletteOverlay() {}

    private static final int BASE_X = 20;
    private static final int BASE_Y = 20;
    private static final int PANEL_GAP = 8;
    private static final int PANEL_WIDTH = 320;
    private static final int TOGGLE_WIDTH = 110;
    private static final int COLLAPSED_HEIGHT = FormInputSize.SIZE_24.height + 16;
    private static final int COLLAPSED_WIDTH = TOGGLE_WIDTH + 20;

    private static final PanelType[] PANEL_ORDER = new PanelType[]{
            PanelType.PAINT,
            PanelType.BLUEPRINTS,
            PanelType.GLOBAL_BLUEPRINTS,
            PanelType.GRID,
            PanelType.SETTLEMENT
    };

    private static final class LayerGroup {
        final String title;
        final PaintLayer[] layers;

        LayerGroup(String title, PaintLayer... layers) {
            this.title = title;
            this.layers = layers;
        }

        boolean contains(PaintLayer layer) {
            for (PaintLayer l : layers) if (l == layer) return true;
            return false;
        }
    }

    private static final LayerGroup[] PAINT_LAYER_GROUPS = new LayerGroup[]{
            new LayerGroup("Bottom layer paints", PaintLayer.TERRAIN),
            new LayerGroup("Middle layer paints", PaintLayer.OBJECT),
            new LayerGroup("Wall background paints", PaintLayer.WALL),
            new LayerGroup("Lighting (wall-mounted)", PaintLayer.WALL_LIGHTING),
            new LayerGroup("Wall attachments", PaintLayer.WALL_ATTACHMENT),
            new LayerGroup("Top layer paints (table decor)", PaintLayer.TABLETOP)
    };

    private static final IdentityHashMap<Object, PanelsHost> HOSTS = new IdentityHashMap<>();
    private static boolean buttonClickedThisTick;

    private enum PanelType { PAINT, BLUEPRINTS, GLOBAL_BLUEPRINTS, GRID, SETTLEMENT }

    public static void tick(boolean paintEnabled) {
        buttonClickedThisTick = false;
        Object mgr = getActiveFormManager();
        if (mgr == null) {
            disposeAll();
            return;
        }
        PanelsHost host = HOSTS.get(mgr);
        if (host == null) {
            host = new PanelsHost(mgr);
            HOSTS.put(mgr, host);
        }
        host.tick(paintEnabled);
    }

    public static void disposeAll() {
        for (PanelsHost host : HOSTS.values()) {
            host.dispose();
        }
        HOSTS.clear();
    }

    public static boolean consumeToggleClick() {
        boolean value = buttonClickedThisTick;
        buttonClickedThisTick = false;
        return value;
    }

    public static boolean isPaletteExpanded() {
        for (PanelsHost host : HOSTS.values()) {
            if (host.hasExpandedPanel()) return true;
        }
        return false;
    }

    public static boolean isMouseOverUi() {
        for (PanelsHost host : HOSTS.values()) {
            if (host.isMouseOver()) return true;
        }
        return false;
    }

    private static Object getActiveFormManager() {
        try {
            return GlobalData.getCurrentState() != null
                    ? GlobalData.getCurrentState().getFormManager()
                    : null;
        } catch (Throwable ignored) {}
        return null;
    }

    private static final class PanelsHost {
        final Object manager;
        final EnumMap<PanelType, SidePanelForm> panels = new EnumMap<>(PanelType.class);

        PanelsHost(Object manager) {
            this.manager = manager;
        }

        void tick(boolean paintEnabled) {
            boolean showPaint = paintEnabled;
            boolean showBlueprints = paintEnabled;
            boolean showGlobalBlueprints = GridConfig.gridEnabled;
            boolean showSettlement = paintEnabled;
            boolean showGrid = GridConfig.gridEnabled;

            updatePanel(PanelType.PAINT, showPaint);
            updatePanel(PanelType.BLUEPRINTS, showBlueprints);
            updatePanel(PanelType.GLOBAL_BLUEPRINTS, showGlobalBlueprints);
            updatePanel(PanelType.SETTLEMENT, showSettlement);
            updatePanel(PanelType.GRID, showGrid);

            int y = BASE_Y;
            for (PanelType type : PANEL_ORDER) {
                SidePanelForm form = panels.get(type);
                if (form == null) continue;
                form.setPosition(BASE_X, y);
                form.refresh();
                y += form.getCurrentHeight() + PANEL_GAP;
            }

            if (panels.isEmpty()) {
                HOSTS.remove(manager);
            }
        }

        private void updatePanel(PanelType type, boolean needed) {
            SidePanelForm form = panels.get(type);
            if (needed) {
                if (form == null) {
                    form = createForm(type);
                    if (attachForm(manager, form)) {
                        panels.put(type, form);
                    }
                }
            } else if (form != null) {
                form.collapse();
                detachForm(manager, form);
                panels.remove(type);
            }
        }

        private SidePanelForm createForm(PanelType type) {
            switch (type) {
                case GRID: return new GridPanel();
                case BLUEPRINTS: return new BlueprintPanel();
                case GLOBAL_BLUEPRINTS: return new GlobalBlueprintPanel();
                case SETTLEMENT: return new SettlementPanel();
                case PAINT:
                default: return new PaintPanel();
            }
        }

        boolean hasExpandedPanel() {
            for (SidePanelForm form : panels.values()) if (form.isExpanded()) return true;
            return false;
        }

        boolean isMouseOver() {
            for (SidePanelForm form : panels.values()) if (form.isMouseOverPanel()) return true;
            return false;
        }

        void dispose() {
            for (SidePanelForm form : panels.values()) {
                detachForm(manager, form);
            }
            panels.clear();
        }
    }

    private abstract static class SidePanelForm extends Form {
        private final String buttonLabel;
        private final String title;
        protected final FormTextButton toggleBtn;
        protected final FormLabel titleLabel;
        protected final FormContentBox content;
        private final int expandedHeight;
        private boolean expanded;

        SidePanelForm(String buttonLabel, String title, int expandedHeight) {
            super(PANEL_WIDTH, COLLAPSED_HEIGHT);
            this.buttonLabel = buttonLabel;
            this.title = title;
            this.expandedHeight = Math.max(expandedHeight, COLLAPSED_HEIGHT + 40);
            this.toggleBtn = addComponent(new FormTextButton(buttonLabel, 0, 0, TOGGLE_WIDTH, FormInputSize.SIZE_24, ButtonColor.BASE));
            this.titleLabel = addComponent(new FormLabel(title, new FontOptions(16), FormLabel.ALIGN_LEFT, 12, 54));
            this.content = addComponent(new FormContentBox(0, 80, PANEL_WIDTH, this.expandedHeight - 92));
            this.content.setHidden(true);
            this.titleLabel.setText("");
            this.toggleBtn.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> {
                buttonClickedThisTick = true;
                setExpanded(!expanded);
            });
            buildContent(content);
            setExpanded(false);
        }

        protected abstract void buildContent(FormContentBox content);

        protected abstract void refreshContent();

        void refresh() {
            refreshContent();
        }

        int getCurrentHeight() {
            return expanded ? expandedHeight : COLLAPSED_HEIGHT;
        }

        boolean isExpanded() {
            return expanded;
        }

        private void setExpanded(boolean expanded) {
            this.expanded = expanded;
            this.setHeight(expanded ? expandedHeight : COLLAPSED_HEIGHT);
            this.setWidth(expanded ? PANEL_WIDTH : COLLAPSED_WIDTH);
            this.drawBase = expanded;
            this.drawEdge = expanded;
            this.content.setHidden(!expanded);
            this.titleLabel.setText(expanded ? title : "");
            this.toggleBtn.setText(expanded ? "Close" : buttonLabel);
            applyFormDecorations(this, expanded);
        }

        void collapse() {
            if (expanded) {
                setExpanded(false);
            }
        }

        boolean isMouseOverPanel() {
            GameWindow window = WindowManager.getWindow();
            if (window == null) return false;
            InputPosition pos = window.mousePos();
            if (pos == null) return false;
            InputEvent event = InputEvent.MouseMoveEvent(pos, null);
            if (expanded && this.isMouseOver(event)) return true;
            return toggleBtn.isMouseOver(event);
        }
    }

    private static final class PaintPanel extends SidePanelForm {
        private FormContentBox listBox;
        private FormCheckBox hoverMaster;
        private FormCheckBox paintToggle;
        private FormCheckBox eraseManualCheck;
        private FormDropdownSelectionButton<String> layerFilterDropdown;
        private List<CategoryRow> rows;

        PaintPanel() {
            super("Paints", "Paint types", 420);
        }

        @Override
        protected void buildContent(FormContentBox content) {
            this.listBox = content;
            int y = 6;
            paintToggle = addTo(listBox, new FormCheckBox("Paint enabled", 12, y, PaintState.enabled));
            paintToggle.onClicked(e -> PaintState.setEnabled(paintToggle.checked));
            y += 26;
            hoverMaster = addTo(listBox, new FormCheckBox("Show tile type on hover", 12, y, GridConfig.isHoverLabelsEnabled()));
            hoverMaster.onClicked(e -> GridConfig.setHoverLabelsEnabled(hoverMaster.checked));
            rows = new ArrayList<>();
            y += 18;
            listBox.addComponent(new FormLabel("Erase by layer", new FontOptions(12), FormLabel.ALIGN_LEFT, 12, y));
            y += 18;
            layerFilterDropdown = listBox.addComponent(new FormDropdownSelectionButton<>(12, y, FormInputSize.SIZE_24, ButtonColor.BASE, PANEL_WIDTH - 40));
            for (PaintLayerFilter filter : PaintLayerFilter.values()) {
                layerFilterDropdown.options.add(filter.id(), new StaticMessage(filter.label()));
            }
            layerFilterDropdown.onSelected(e -> {
                if (!GridConfig.isPaintEraseOverride()) {
                    refreshEraseControls();
                    return;
                }
                GridConfig.setPaintEraseFilter(PaintLayerFilter.byId(e.value));
                GridConfig.saveIfDirty();
            });
            y += FormInputSize.SIZE_24.height + 12;

            eraseManualCheck = listBox.addComponent(new FormCheckBox("Select erase layer manually", 12, y, GridConfig.isPaintEraseOverride()));
            eraseManualCheck.onClicked(e -> {
                GridConfig.setPaintEraseOverride(eraseManualCheck.checked);
                GridConfig.saveIfDirty();
                refreshEraseControls();
            });
            y += 28;
            for (LayerGroup group : PAINT_LAYER_GROUPS) {
                addTo(listBox, new FormLabel(group.title, new FontOptions(13), FormLabel.ALIGN_LEFT, 12, y));
                y += 20;
                for (PaintCategory cat : PaintCategory.values()) {
                    if (!group.contains(cat.layer())) continue;
                    rows.add(new CategoryRow(cat, y));
                    y += 26;
                }
                y += 10;
            }
            refreshEraseControls();
            listBox.setContentBox(new Rectangle(0, 0, PANEL_WIDTH, Math.max(y + 12, listBox.getHeight())));
        }

        @Override
        protected void refreshContent() {
            if (paintToggle != null) paintToggle.checked = PaintState.enabled;
            hoverMaster.checked = GridConfig.isHoverLabelsEnabled();
            refreshEraseControls();
            PaintCategory active = GridConfig.getActivePaintCategory();
            for (CategoryRow row : rows) row.sync(active, hoverMaster.checked);
        }

        private void refreshEraseControls() {
            if (layerFilterDropdown == null) return;
            boolean manual = GridConfig.isPaintEraseOverride();
            if (eraseManualCheck != null) eraseManualCheck.checked = manual;
            layerFilterDropdown.setActive(manual);
            PaintLayerFilter display = manual
                    ? GridConfig.getPaintEraseFilter()
                    : GridConfig.getEffectivePaintEraseFilter();
            layerFilterDropdown.setSelected(display.id(), new StaticMessage(display.label()));
        }

        private class CategoryRow {
            final PaintCategory category;
            final FormCheckBox select;
            final FormCheckBox hoverToggle;

            CategoryRow(PaintCategory category, int y) {
                this.category = category;
                addTo(listBox, new ColorSwatch(category, 8, y + 4, 18, 18));
                this.select = addTo(listBox, new FormCheckBox(category.label(), 36, y, PANEL_WIDTH - 140, false));
                this.select.onClicked((FormEventListener<FormInputEvent<FormCheckBox>>) e -> GridConfig.setActivePaintCategory(category));
                this.hoverToggle = addTo(listBox, new FormCheckBox("hover", PANEL_WIDTH - 94, y, 70, GridConfig.isHoverCategoryAllowed(category)));
                this.hoverToggle.onClicked((FormEventListener<FormInputEvent<FormCheckBox>>) e -> GridConfig.setHoverCategoryAllowed(category, hoverToggle.checked));
            }

            void sync(PaintCategory active, boolean hoverMasterEnabled) {
                this.select.checked = category == active;
                this.hoverToggle.checked = GridConfig.isHoverCategoryAllowed(category);
                this.hoverToggle.setActive(hoverMasterEnabled);
            }
        }

        private class ColorSwatch extends FormCustomDraw {
            private final PaintCategory category;
            ColorSwatch(PaintCategory category, int x, int y, int w, int h) {
                super(x, y, w, h);
                this.category = category;
                this.canBePutOnTopByClick = false;
            }
            @Override
            public boolean shouldUseMouseEvents() { return false; }
            @Override
            public void draw(necesse.engine.gameLoop.tickManager.TickManager tm, necesse.entity.mobs.PlayerMob mob, Rectangle box) {
                GridConfig.PaintColor color = GridConfig.getPaintColor(category);
                Renderer.initQuadDraw(this.width, this.height).color(color.r, color.g, color.b, 1f).draw(getX(), getY());
                Renderer.initQuadDraw(this.width, 1).color(0f, 0f, 0f, 0.6f).draw(getX(), getY());
                Renderer.initQuadDraw(this.width, 1).color(0f, 0f, 0f, 0.6f).draw(getX(), getY() + this.height - 1);
                Renderer.initQuadDraw(1, this.height).color(0f, 0f, 0f, 0.6f).draw(getX(), getY());
                Renderer.initQuadDraw(1, this.height).color(0f, 0f, 0f, 0.6f).draw(getX() + this.width - 1, getY());
            }
        }
    }

    private static final class GridPanel extends SidePanelForm {
        private FormCheckBox gridToggle;
        private FormCheckBox chunkToggle;
        private FormCheckBox subChunkToggle;
        private FormSlider gridAlpha;
        private FormDropdownSelectionButton<String> chunkSpanDropdown;
        private FormSlider chunkThickness;
        private FormSlider chunkAlpha;
        private FormSlider subThickness;
        private FormSlider subAlpha;
        private boolean internal;

        GridPanel() {
            super("Grid", "Grid visibility", 420);
        }

        @Override
        protected void buildContent(FormContentBox content) {
            int sliderW = PANEL_WIDTH - 40;
            int y = 6;
            gridToggle = content.addComponent(new FormCheckBox("Grid lines", 12, y, GridConfig.gridEnabled));
            gridToggle.onClicked(e -> toggleGrid(gridToggle.checked));
            y += 28;
            gridAlpha = content.addComponent(new FormSlider("Grid alpha", 12, y, Math.round(GridConfig.lineAlpha * 100), 0, 100, sliderW));
            gridAlpha.onChanged(e -> updateAlpha("grid"));
            y += FormInputSize.SIZE_24.height + 12;
            chunkToggle = content.addComponent(new FormCheckBox("Chunk lines", 12, y, GridConfig.showChunkLines));
            chunkToggle.onClicked(e -> toggleChunks(chunkToggle.checked));
            y += 28;
            content.addComponent(new FormLabel("Chunk span", new FontOptions(12), FormLabel.ALIGN_LEFT, 12, y));
            chunkSpanDropdown = content.addComponent(new FormDropdownSelectionButton<>(12, y + 18, FormInputSize.SIZE_24, ButtonColor.BASE, sliderW));
            int[] spans = new int[]{8, 16, 32, 64};
            for (int span : spans) {
                String key = Integer.toString(span);
                chunkSpanDropdown.options.add(key, new StaticMessage(span + " tiles"));
            }
            setChunkSpanSelection();
            chunkSpanDropdown.onSelected(e -> {
                int span = Integer.parseInt(e.value);
                GridConfig.chunkSpanTiles = span;
                GridConfig.subChunkSpanTiles = Math.max(2, span / 4);
                GridConfig.markDirty(); GridConfig.saveIfDirty();
            });
            y += FormInputSize.SIZE_24.height + 28;
            chunkThickness = content.addComponent(new FormSlider("Chunk thickness", 12, y, GridConfig.chunkThickness, 1, 4, sliderW));
            chunkThickness.drawValue = true;
            chunkThickness.drawValueInPercent = false;
            chunkThickness.onChanged(e -> {
                GridConfig.chunkThickness = ((FormSlider)e.from).getValue();
                GridConfig.markDirty(); GridConfig.saveIfDirty();
            });
            y += chunkThickness.getTotalHeight() + 12;
            chunkAlpha = content.addComponent(new FormSlider("Chunk alpha", 12, y, Math.round(GridConfig.chunkAlpha * 100), 0, 100, sliderW));
            chunkAlpha.onChanged(e -> updateAlpha("chunk"));
            y += FormInputSize.SIZE_24.height + 12;
            subChunkToggle = content.addComponent(new FormCheckBox("Sub-chunk lines", 12, y, GridConfig.showSubChunkLines));
            subChunkToggle.onClicked(e -> toggleSubChunks(subChunkToggle.checked));
            y += 28;
            subThickness = content.addComponent(new FormSlider("Sub-chunk thickness", 12, y, GridConfig.subChunkThickness, 1, 3, sliderW));
            subThickness.drawValue = true;
            subThickness.drawValueInPercent = false;
            subThickness.onChanged(e -> {
                GridConfig.subChunkThickness = ((FormSlider)e.from).getValue();
                GridConfig.markDirty(); GridConfig.saveIfDirty();
            });
            y += subThickness.getTotalHeight() + 12;
            subAlpha = content.addComponent(new FormSlider("Sub-chunk alpha", 12, y, Math.round(GridConfig.subChunkAlpha * 100), 0, 100, sliderW));
            subAlpha.onChanged(e -> updateAlpha("sub"));
            int totalHeight = y + subAlpha.getTotalHeight() + 12;
            content.setContentBox(new Rectangle(0, 0, PANEL_WIDTH, Math.max(totalHeight, content.getHeight())));
        }

        private void updateAlpha(String type) {
            if (internal) return;
            switch (type) {
                case "grid": GridConfig.lineAlpha = gridAlpha.getValue() / 100f; break;
                case "chunk": GridConfig.chunkAlpha = chunkAlpha.getValue() / 100f; break;
                case "sub": GridConfig.subChunkAlpha = subAlpha.getValue() / 100f; break;
            }
            GridConfig.markDirty();
            GridConfig.saveIfDirty();
        }

        private void toggleGrid(boolean enabled) {
            GridConfig.gridEnabled = enabled;
            GridConfig.markDirty();
            GridConfig.saveIfDirty();
        }

        private void toggleChunks(boolean enabled) {
            GridConfig.showChunkLines = enabled;
            GridConfig.markDirty();
            GridConfig.saveIfDirty();
        }

        private void toggleSubChunks(boolean enabled) {
            GridConfig.showSubChunkLines = enabled;
            GridConfig.markDirty();
            GridConfig.saveIfDirty();
        }

        @Override
        protected void refreshContent() {
            internal = true;
            if (gridToggle != null) gridToggle.checked = GridConfig.gridEnabled;
            gridAlpha.setValue(Math.round(GridConfig.lineAlpha * 100));
            if (chunkToggle != null) chunkToggle.checked = GridConfig.showChunkLines;
            setChunkSpanSelection();
            chunkThickness.setValue(GridConfig.chunkThickness);
            chunkAlpha.setValue(Math.round(GridConfig.chunkAlpha * 100));
            if (subChunkToggle != null) subChunkToggle.checked = GridConfig.showSubChunkLines;
            subThickness.setValue(GridConfig.subChunkThickness);
            subAlpha.setValue(Math.round(GridConfig.subChunkAlpha * 100));
            internal = false;
        }

        private void setChunkSpanSelection() {
            if (chunkSpanDropdown == null) return;
            String key = Integer.toString(GridConfig.chunkSpanTiles);
            chunkSpanDropdown.setSelected(key, new StaticMessage(key + " tiles"));
        }
    }

    private static final class SettlementPanel extends SidePanelForm {
        private FormLabel info;
        private FormCheckBox boundsToggle;
        private FormDropdownSelectionButton<String> tierDropdown;

        SettlementPanel() {
            super("Settlement", "Settlement bounds", 240);
        }

        @Override
        protected void buildContent(FormContentBox content) {
            int y = 6;
            boundsToggle = content.addComponent(new FormCheckBox("Show settlement bounds", 12, y, GridConfig.settlementEnabled));
            boundsToggle.onClicked(e -> {
                GridConfig.settlementEnabled = boundsToggle.checked;
                GridConfig.markDirty(); GridConfig.saveIfDirty();
            });
            y += 26;
            info = content.addComponent(new FormLabel("", new FontOptions(14), FormLabel.ALIGN_LEFT, 12, y));
            y += 30;

            content.addComponent(new FormLabel("Tier", new FontOptions(14), FormLabel.ALIGN_LEFT, 12, y));
            y += 20;
            tierDropdown = content.addComponent(new FormDropdownSelectionButton<>(12, y, FormInputSize.SIZE_24, ButtonColor.BASE, PANEL_WIDTH - 24));
            populateTierDropdown();
            tierDropdown.onSelected(e -> {
                try {
                    int wanted = Integer.parseInt(e.value);
                    GridConfig.settlementTier = Math.max(1, Math.min(GridConfig.maxTier(), wanted));
                    GridConfig.markDirty(); GridConfig.saveIfDirty();
                    refreshContent();
                } catch (NumberFormatException ignored) {}
            });
            y += FormInputSize.SIZE_24.height + 8;

            FormTextButton place = content.addComponent(new FormTextButton("Place at flag", 12, y, PANEL_WIDTH - 24, FormInputSize.SIZE_24, ButtonColor.BASE));
            place.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> {
                buttonClickedThisTick = true;
                PaintControls.placeAtCurrentSettlementFlag();
            });
            y += FormInputSize.SIZE_24.height + 8;
            FormTextButton tierBtn = content.addComponent(new FormTextButton("Cycle tier", 12, y, PANEL_WIDTH - 24, FormInputSize.SIZE_24, ButtonColor.BASE));
            tierBtn.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> {
                buttonClickedThisTick = true;
                GridConfig.cycleSettlementTier();
            });
        }

        @Override
        protected void refreshContent() {
            if (boundsToggle != null) boundsToggle.checked = GridConfig.settlementEnabled;
            String status = GridConfig.settlementEnabled ? "ON" : "OFF";
            info.setText("Status " + status + " | Tier " + GridConfig.settlementTier
                    + " | " + GridConfig.currentTierSideTiles() + " tiles");
            if (tierDropdown != null) {
                populateTierDropdown();
                tierDropdown.setSelected(Integer.toString(GridConfig.settlementTier),
                        new StaticMessage("Tier " + GridConfig.settlementTier));
            }
        }

        private void populateTierDropdown() {
            if (tierDropdown == null) return;
            tierDropdown.options.clear();
            int maxTier = GridConfig.maxTier();
            for (int i = 1; i <= maxTier; i++) {
                String key = Integer.toString(i);
                tierDropdown.options.add(key, new StaticMessage("Tier " + i));
            }
        }
    }

    private static final class BlueprintPanel extends SidePanelForm {
        private FormDropdownSelectionButton<String> dropdown;
        private FormDropdownSelectionButton<String> defaultDropdown;
        private FormLabel statusLabel;
        private FormLabel selectionLabel;
        private String currentBlueprint;
        private long lastSaveClick;
        private String lastSaveName;
        private long lastSaveAsClick;
        private String lastSaveAsName;
        private List<ModeButton> modeButtons;
        private FormTextButton flipBtn;
        private FormTextButton rotateCwBtn;
        private FormTextButton rotateCcwBtn;
        private FormDropdownSelectionButton<String> selectionLayerDropdown;
        private FormTextInput saveAsInput;
        private FormTextInput renameInput;

        BlueprintPanel() {
            super("Blueprints", "Blueprint tools", 460);
        }

        @Override
        protected void buildContent(FormContentBox content) {
            int dropdownWidth = PANEL_WIDTH - 130;

            int y = 6;
            content.addComponent(new FormLabel("Default blueprints", new FontOptions(12), FormLabel.ALIGN_LEFT, 12, y));
            y += 18;
            defaultDropdown = content.addComponent(new FormDropdownSelectionButton<>(12, y, FormInputSize.SIZE_24, ButtonColor.BASE, dropdownWidth));
            populateDefaultDropdown();
            FormTextButton loadDefault = content.addComponent(new FormTextButton("Load default", 24 + dropdownWidth, y, PANEL_WIDTH - (36 + dropdownWidth), FormInputSize.SIZE_24, ButtonColor.BASE));
            loadDefault.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> handleLoadDefault());
            y += FormInputSize.SIZE_24.height + 12;

            content.addComponent(new FormLabel("Your blueprints", new FontOptions(12), FormLabel.ALIGN_LEFT, 12, y));
            y += 18;
            dropdown = content.addComponent(new FormDropdownSelectionButton<>(12, y, FormInputSize.SIZE_24, ButtonColor.BASE, dropdownWidth));
            dropdown.onSelected(e -> {
                currentBlueprint = e.value;
                GridConfig.selectedBlueprint = currentBlueprint;
                GridConfig.markDirty();
            });
            FormTextButton refresh = content.addComponent(new FormTextButton("Refresh", 24 + dropdownWidth, y, PANEL_WIDTH - (36 + dropdownWidth), FormInputSize.SIZE_24, ButtonColor.BASE));
            refresh.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> refreshBlueprintOptions());
            y += FormInputSize.SIZE_24.height + 10;

            int actionWidth = (PANEL_WIDTH - 36) / 2;
            FormTextButton loadBtn = content.addComponent(new FormTextButton("Load", 12, y, actionWidth, FormInputSize.SIZE_24, ButtonColor.BASE));
            loadBtn.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> handleLoad());
            FormTextButton deleteBtn = content.addComponent(new FormTextButton("Delete", 24 + actionWidth, y, actionWidth, FormInputSize.SIZE_24, ButtonColor.BASE));
            deleteBtn.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> handleDelete());
            y += FormInputSize.SIZE_24.height + 6;

            // Leave space before rename / selection controls
            y += 10;

            content.addComponent(new FormLabel("Rename blueprint", new FontOptions(12), FormLabel.ALIGN_LEFT, 12, y));
            y += 18;
            renameInput = content.addComponent(new FormTextInput(12, y, FormInputSize.SIZE_24, PANEL_WIDTH - 140, 32));
            renameInput.placeHolder = new StaticMessage("new name");
            FormTextButton renameBtn = content.addComponent(new FormTextButton("Rename", PANEL_WIDTH - 120, y, 108, FormInputSize.SIZE_24, ButtonColor.BASE));
            renameBtn.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> handleRename());
            y += FormInputSize.SIZE_24.height + 16;

            content.addComponent(new FormLabel("Selection layer", new FontOptions(12), FormLabel.ALIGN_LEFT, 12, y));
            y += 18;
            selectionLayerDropdown = content.addComponent(new FormDropdownSelectionButton<>(12, y, FormInputSize.SIZE_24, ButtonColor.BASE, PANEL_WIDTH - 24));
            for (PaintLayerFilter filter : PaintLayerFilter.values()) {
                selectionLayerDropdown.options.add(filter.id(), new StaticMessage(filter.label()));
            }
            PaintLayerFilter currentSelectionFilter = GridConfig.getPaintSelectionFilter();
            selectionLayerDropdown.setSelected(currentSelectionFilter.id(), new StaticMessage(currentSelectionFilter.label()));
            selectionLayerDropdown.onSelected(e -> {
                GridConfig.setPaintSelectionFilter(PaintLayerFilter.byId(e.value));
                updateModeButtons();
                GridConfig.saveIfDirty();
            });
            y += FormInputSize.SIZE_24.height + 16;

            content.addComponent(new FormLabel("Selection mode", new FontOptions(14), FormLabel.ALIGN_LEFT, 12, y));
            y += 22;
            y = buildModeButtons(content, y);

            // Selection → blueprint: Save / Save As (selection-based)
            content.addComponent(new FormLabel("Selection \u2192 blueprint", new FontOptions(12), FormLabel.ALIGN_LEFT, 12, y));
            y += 18;
            FormTextButton saveBtn = content.addComponent(new FormTextButton("Save (dbl-click)", 12, y, PANEL_WIDTH - 24, FormInputSize.SIZE_24, ButtonColor.BASE));
            saveBtn.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> handleSave());
            y += FormInputSize.SIZE_24.height + 6;

            saveAsInput = content.addComponent(new FormTextInput(12, y, FormInputSize.SIZE_24, PANEL_WIDTH - 140, 32));
            saveAsInput.placeHolder = new StaticMessage("bp_selection");
            FormTextButton saveAsBtn = content.addComponent(new FormTextButton("Save As", PANEL_WIDTH - 120, y, 108, FormInputSize.SIZE_24, ButtonColor.BASE));
            saveAsBtn.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> handleSaveSelectionAs());
            y += FormInputSize.SIZE_24.height + 16;

            int controlY = y;
            int halfWidth = (PANEL_WIDTH - 36) / 2;
            flipBtn = content.addComponent(new FormTextButton("Flip", 12, controlY, halfWidth, FormInputSize.SIZE_24, ButtonColor.BASE));
            flipBtn.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> {
                if (BlueprintPlacement.active) {
                    BlueprintPlacement.toggleFlip();
                    updateModeButtons();
                }
            });
            rotateCwBtn = content.addComponent(new FormTextButton("Rotate CW", 24 + halfWidth, controlY, halfWidth, FormInputSize.SIZE_24, ButtonColor.BASE));
            rotateCwBtn.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> {
                if (BlueprintPlacement.active) {
                    BlueprintPlacement.rotateCW();
                    updateModeButtons();
                }
            });
            controlY += FormInputSize.SIZE_24.height + 6;
            rotateCcwBtn = content.addComponent(new FormTextButton("Rotate CCW", 12, controlY, PANEL_WIDTH - 24, FormInputSize.SIZE_24, ButtonColor.BASE));
            rotateCcwBtn.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> {
                if (BlueprintPlacement.active) {
                    BlueprintPlacement.rotateCCW();
                    updateModeButtons();
                }
            });
            y = controlY + FormInputSize.SIZE_24.height + 8;

            statusLabel = content.addComponent(new FormLabel("", new FontOptions(12), FormLabel.ALIGN_LEFT, 12, y));
            y += 24;
            selectionLabel = content.addComponent(new FormLabel("", new FontOptions(12), FormLabel.ALIGN_LEFT, 12, y));
            y += 20;
            content.setContentBox(new Rectangle(0, 0, PANEL_WIDTH, Math.max(y, content.getHeight())));
            refreshBlueprintOptions();
            updateModeButtons();
        }

        private int buildModeButtons(FormContentBox content, int y) {
            modeButtons = new ArrayList<>();

            int halfWidth = (PANEL_WIDTH - 36) / 2;

            // First row: Rect / Edge
            int x = 12;
            SelectionState.Mode[] firstRow = new SelectionState.Mode[]{
                    SelectionState.Mode.RECT,
                    SelectionState.Mode.EDGE
            };
            for (SelectionState.Mode mode : firstRow) {
                FormTextButton btn = content.addComponent(
                        new FormTextButton(modeLabel(mode), x, y, halfWidth, FormInputSize.SIZE_24, ButtonColor.BASE));
                btn.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> {
                    SelectionState.setMode(mode);
                    updateModeButtons();
                });
                modeButtons.add(new ModeButton(mode, btn));
                x += halfWidth + 12;
            }
            y += FormInputSize.SIZE_24.height + 6;

            // Second row: Edge+Fill (full width)
            SelectionState.Mode edgeFillMode = SelectionState.Mode.EDGE_FILL;
            FormTextButton edgeFillBtn = content.addComponent(
                    new FormTextButton(modeLabel(edgeFillMode), 12, y, PANEL_WIDTH - 24, FormInputSize.SIZE_24, ButtonColor.BASE));
            edgeFillBtn.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> {
                SelectionState.setMode(edgeFillMode);
                updateModeButtons();
            });
            modeButtons.add(new ModeButton(edgeFillMode, edgeFillBtn));
            y += FormInputSize.SIZE_24.height + 6;

            // Third row: All (full width)
            FormTextButton allBtn = content.addComponent(
                    new FormTextButton("All", 12, y, PANEL_WIDTH - 24, FormInputSize.SIZE_24, ButtonColor.BASE));
            allBtn.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> {
                SelectionState.setMode(SelectionState.Mode.ALL);
                updateModeButtons();
            });
            modeButtons.add(new ModeButton(SelectionState.Mode.ALL, allBtn));
            return y + FormInputSize.SIZE_24.height + 8;
        }

        private String modeLabel(SelectionState.Mode mode) {
            switch (mode) {
                case EDGE: return "Edge";
                case EDGE_FILL: return "Edge+Fill";
                case ALL: return "All";
                case RECT:
                default: return "Rect";
            }
        }

        private void handleSave() {
            String name = getCurrentBlueprintName();
            if (name == null) {
                statusLabel.setText("No blueprint selected");
                return;
            }
            if (DefaultBlueprintRegistry.isDefaultKey(name)) {
                statusLabel.setText("Cannot overwrite default; use Save As with a new name.");
                return;
            }
            List<long[]> points = SelectionState.getSelectedPoints();
            if (points.isEmpty()) {
                statusLabel.setText("No tiles selected");
                return;
            }
            long now = System.currentTimeMillis();
            boolean exists = PaintBlueprints.exists(name);
            if (exists && (!name.equals(lastSaveName) || now - lastSaveClick > 5000)) {
                lastSaveName = name;
                lastSaveClick = now;
                statusLabel.setText("Click Save again within 5s to overwrite '" + name + "' with current selection.");
                return;
            }
            int written = PaintBlueprints.saveSelectionAs(name, points);
            lastSaveClick = 0;
            lastSaveName = null;
            if (written > 0) {
                statusLabel.setText("Saved '" + name + "' (" + written + " tiles)");
                refreshBlueprintOptions();
            } else {
                statusLabel.setText("Save failed");
            }
        }

        private void handleLoad() {
            String name = getCurrentBlueprintName();
            if (name == null) {
                statusLabel.setText("No blueprint selected");
                return;
            }
            List<BlueprintPlacement.BlueprintTile> rel = PaintBlueprints.loadRelative(name);
            if (rel.isEmpty()) {
                statusLabel.setText("Blueprint '" + name + "' empty");
                return;
            }
            BlueprintPlacement.begin(rel);
            statusLabel.setText("Loaded '" + name + "'");
            updateModeButtons();
        }

        private void handleSaveSelectionAs() {
            String raw = saveAsInput == null ? null : saveAsInput.getText();
            String name = raw == null ? "" : raw.trim();
            if (name.isEmpty()) {
                statusLabel.setText("Enter a name to save");
                return;
            }
            List<long[]> points = SelectionState.getSelectedPoints();
            if (points.isEmpty()) {
                statusLabel.setText("No tiles selected");
                return;
            }
            long now = System.currentTimeMillis();
            boolean exists = PaintBlueprints.exists(name);
            if (exists && (!name.equals(lastSaveAsName) || now - lastSaveAsClick > 5000)) {
                lastSaveAsName = name;
                lastSaveAsClick = now;
                statusLabel.setText("Click Save As again within 5s to overwrite '" + name + "' with current selection.");
                return;
            }
            int written = PaintBlueprints.saveSelectionAs(name, points);
            lastSaveAsClick = 0;
            lastSaveAsName = null;
            if (written > 0) {
                statusLabel.setText("Saved '" + name + "' (" + written + " tiles)");
                saveAsInput.setText(name);
                refreshBlueprintOptions();
            } else {
                statusLabel.setText("Save failed");
            }
        }

        private void handleRename() {
            if (renameInput == null) return;
            String current = getCurrentBlueprintName();
            if (current == null || current.isBlank()) {
                statusLabel.setText("No blueprint selected");
                return;
            }
            if (DefaultBlueprintRegistry.isDefaultKey(current)) {
                statusLabel.setText("Cannot rename default; import it to edit.");
                return;
            }
            String raw = renameInput.getText();
            String next = raw == null ? "" : raw.trim();
            if (next.isEmpty()) {
                statusLabel.setText("Enter a new name");
                return;
            }
            if (next.equals(current)) {
                statusLabel.setText("Name unchanged");
                return;
            }
            boolean ok = PaintBlueprints.renameBlueprint(current, next);
            if (ok) {
                currentBlueprint = next;
                GridConfig.selectedBlueprint = next;
                GridConfig.markDirty(); GridConfig.saveIfDirty();
                renameInput.setText(next);
                refreshBlueprintOptions();
                statusLabel.setText("Renamed to '" + next + "'");
            } else {
                statusLabel.setText("Rename failed");
            }
        }

        private void handleDelete() {
            String name = getCurrentBlueprintName();
            if (name == null || name.isBlank()) {
                statusLabel.setText("No blueprint selected");
                return;
            }
            if (DefaultBlueprintRegistry.isDefaultKey(name)) {
                statusLabel.setText("Cannot delete default; import it to edit.");
                return;
            }
            boolean ok = PaintBlueprints.deleteBlueprint(name);
            if (ok) {
                currentBlueprint = "";
                GridConfig.selectedBlueprint = "";
                GridConfig.markDirty(); GridConfig.saveIfDirty();
                refreshBlueprintOptions();
                statusLabel.setText("Deleted '" + name + "'");
            } else {
                statusLabel.setText("Delete failed");
            }
        }

        private String getCurrentBlueprintName() {
            return currentBlueprint;
        }

        private void refreshBlueprintOptions() {
            dropdown.options.clear();
            dropdown.options.add("", new StaticMessage("(none)"));

            List<DefaultBlueprintRegistry.DefaultBlueprint> defaults = DefaultBlueprintRegistry.values();

            String[] names = PaintBlueprints.listBlueprints();
            for (String n : names) {
                dropdown.options.add(n, new StaticMessage(n));
            }

            currentBlueprint = GridConfig.selectedBlueprint;
            if (currentBlueprint == null) currentBlueprint = "";
            currentBlueprint = currentBlueprint.trim();

            boolean found = blueprintAvailable(currentBlueprint, names, defaults);
            if (!found) {
                currentBlueprint = "";
            }

            GridConfig.selectedBlueprint = currentBlueprint;
            dropdown.setSelected(currentBlueprint, new StaticMessage(blueprintLabel(currentBlueprint)));
            statusLabel.setText(blueprintDescription(currentBlueprint));
            if (renameInput != null) renameInput.setText(DefaultBlueprintRegistry.isDefaultKey(currentBlueprint) ? "" : currentBlueprint);
        }

        private void populateDefaultDropdown() {
            if (defaultDropdown == null) return;
            defaultDropdown.options.clear();
            defaultDropdown.options.add("", new StaticMessage("(choose)"));
            for (DefaultBlueprintRegistry.DefaultBlueprint info : DefaultBlueprintRegistry.values()) {
                defaultDropdown.options.add(info.id, new StaticMessage(info.name));
            }
            defaultDropdown.setSelected("", new StaticMessage("(choose)"));
        }

        private boolean blueprintAvailable(String key, String[] names, List<DefaultBlueprintRegistry.DefaultBlueprint> defaults) {
            if (key == null || key.isEmpty()) return true;
            if (DefaultBlueprintRegistry.isDefaultKey(key)) {
                String id = DefaultBlueprintRegistry.keyToId(key);
                return DefaultBlueprintRegistry.find(id) != null;
            }
            for (String name : names) {
                if (name.equals(key)) return true;
            }
            return false;
        }

        private String blueprintLabel(String key) {
            if (key == null || key.isEmpty()) return "(none)";
            if (DefaultBlueprintRegistry.isDefaultKey(key)) {
                String id = DefaultBlueprintRegistry.keyToId(key);
                DefaultBlueprintRegistry.DefaultBlueprint info = DefaultBlueprintRegistry.find(id);
                return info == null ? "(default)" : info.name + " (default)";
            }
            return key;
        }

        private String blueprintDescription(String key) {
            if (!DefaultBlueprintRegistry.isDefaultKey(key)) return "";
            String id = DefaultBlueprintRegistry.keyToId(key);
            DefaultBlueprintRegistry.DefaultBlueprint info = DefaultBlueprintRegistry.find(id);
            if (info == null) return "";
            return info.description;
        }

        private void handleLoadDefault() {
            if (defaultDropdown == null) return;
            String id = defaultDropdown.getSelected();
            if (id == null || id.isBlank()) {
                statusLabel.setText("Choose a default first");
                return;
            }
            DefaultBlueprintRegistry.DefaultBlueprint info = DefaultBlueprintRegistry.find(id);
            if (info == null) {
                statusLabel.setText("Default missing");
                return;
            }
            List<BlueprintPlacement.BlueprintTile> tiles = DefaultBlueprintRegistry.load(id);
            if (tiles.isEmpty()) {
                statusLabel.setText("Default blueprint is empty");
                return;
            }
            currentBlueprint = DefaultBlueprintRegistry.selectionKey(info);
            GridConfig.selectedBlueprint = currentBlueprint;
            GridConfig.markDirty(); GridConfig.saveIfDirty();
            refreshBlueprintOptions();
            BlueprintPlacement.begin(tiles);
            if (selectionLabel != null) {
                selectionLabel.setText("Placement active (default)");
            }
            statusLabel.setText("Loaded default '" + info.name + "'");
        }

        private void updateModeButtons() {
            SelectionState.Mode mode = SelectionState.getMode();
            for (ModeButton entry : modeButtons) {
                String text = entry.mode == mode ? ("> " + modeLabel(entry.mode)) : modeLabel(entry.mode);
                entry.button.setText(text);
            }
            boolean placementActive = BlueprintPlacement.active;
            if (flipBtn != null) {
                flipBtn.setText(BlueprintPlacement.isFlipped() ? "Flip (X)" : "Flip");
                flipBtn.setActive(placementActive);
            }
            if (rotateCwBtn != null) rotateCwBtn.setActive(placementActive);
            if (rotateCcwBtn != null) rotateCcwBtn.setActive(placementActive);
            if (selectionLabel != null) {
                PaintLayerFilter filter = GridConfig.getPaintSelectionFilter();
                selectionLabel.setText("Mode: " + modeLabel(mode) + " | Layer: " + filter.label());
            }
        }

        @Override
        protected void refreshContent() {
            updateModeButtons();
            if (selectionLayerDropdown != null) {
                PaintLayerFilter current = GridConfig.getPaintSelectionFilter();
                selectionLayerDropdown.setSelected(current.id(), new StaticMessage(current.label()));
            }
        }

        private static final class ModeButton {
            final SelectionState.Mode mode;
            final FormTextButton button;
            ModeButton(SelectionState.Mode mode, FormTextButton button) {
                this.mode = mode;
                this.button = button;
            }
        }
    }

    private static final class GlobalBlueprintPanel extends SidePanelForm {
        private FormDropdownSelectionButton<String> dropdown;
        private FormTextInput nameInput;
        private FormLabel status;
        private FormTextButton newBtn;

        GlobalBlueprintPanel() {
            super("Global BPs", "Global blueprints", 360);
        }

        @Override
        protected void buildContent(FormContentBox content) {
            int dropdownWidth = PANEL_WIDTH - 150;
            dropdown = content.addComponent(new FormDropdownSelectionButton<>(12, 6, FormInputSize.SIZE_24, ButtonColor.BASE, dropdownWidth));
            dropdown.onSelected(e -> {
                nameInput.setText(e.value);
                GridConfig.selectedGlobalBlueprint = e.value;
                GridConfig.markDirty();
            });
            FormTextButton refresh = content.addComponent(new FormTextButton("Refresh", 24 + dropdownWidth, 6, 100, FormInputSize.SIZE_24, ButtonColor.BASE));
            refresh.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> refreshOptions());

            int y = 6 + FormInputSize.SIZE_24.height + 10;
            nameInput = new FormTextInput(12, y, FormInputSize.SIZE_24, PANEL_WIDTH - 24, 40);
            content.addComponent(nameInput);
            nameInput.setText("");
            y += FormInputSize.SIZE_24.height + 10;

            newBtn = content.addComponent(new FormTextButton("New", 12, y, PANEL_WIDTH - 24, FormInputSize.SIZE_24, ButtonColor.BASE));
            newBtn.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> handleNew());
            y += FormInputSize.SIZE_24.height + 6;

            FormTextButton save = content.addComponent(new FormTextButton("Save", 12, y, PANEL_WIDTH - 24, FormInputSize.SIZE_24, ButtonColor.BASE));
            save.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> handleSave());
            y += FormInputSize.SIZE_24.height + 6;

            FormTextButton load = content.addComponent(new FormTextButton("Load", 12, y, PANEL_WIDTH - 24, FormInputSize.SIZE_24, ButtonColor.BASE));
            load.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> handleLoad());
            y += FormInputSize.SIZE_24.height + 6;

            FormTextButton delete = content.addComponent(new FormTextButton("Delete", 12, y, PANEL_WIDTH - 24, FormInputSize.SIZE_24, ButtonColor.BASE));
            delete.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> handleDelete());
            y += FormInputSize.SIZE_24.height + 10;

            status = content.addComponent(new FormLabel("", new FontOptions(12), FormLabel.ALIGN_LEFT, 12, y));
            y += 18;

            refreshOptions();
            content.setContentBox(new Rectangle(0, 0, PANEL_WIDTH, Math.max(y + 10, content.getHeight())));
        }

        private String currentName() {
            String text = nameInput.getText().trim();
            if (text.isEmpty()) return null;
            return text;
        }

        private void handleNew() {
            String name = currentName();
            if (name == null) {
                status.setText("Enter a name first");
                return;
            }
            boolean created = PaintBlueprints.createGlobalEmpty(name);
            GridConfig.selectedGlobalBlueprint = name;
            GridConfig.markDirty();
            refreshOptions(false);
            status.setText(created ? "Created '" + name + "'" : "Selected existing '" + name + "'");
        }

        private void handleSave() {
            String name = currentName();
            if (name == null) {
                status.setText("Enter a name first");
                return;
            }
            PaintBlueprints.saveGlobal(name);
            GridConfig.selectedGlobalBlueprint = name;
            GridConfig.markDirty();
            refreshOptions(false);
            status.setText("Saved '" + name + "'");
        }

        private void handleLoad() {
            String name = currentName();
            if (name == null) {
                status.setText("Enter a name first");
                return;
            }
            int restored = PaintBlueprints.loadGlobal(name);
            status.setText(restored > 0 ? "Loaded '" + name + "'" : "Nothing loaded");
        }

        private void handleDelete() {
            String name = currentName();
            if (name == null) {
                status.setText("Enter a name first");
                return;
            }
            if (PaintBlueprints.deleteGlobal(name)) {
                status.setText("Deleted '" + name + "'");
                refreshOptions();
            } else {
                status.setText("Delete failed");
            }
        }

        private void refreshOptions() {
            refreshOptions(false);
        }

        private void refreshOptions(boolean preserveText) {
            dropdown.options.clear();
            String[] names = PaintBlueprints.listGlobalBlueprints();
            String selected = GridConfig.selectedGlobalBlueprint;
            boolean found = false;
            for (String n : names) {
                dropdown.options.add(n, new StaticMessage(n));
                if (n.equals(selected)) found = true;
            }
            if (!found && names.length > 0) selected = names[0];
            if (names.length == 0) {
                dropdown.options.add("_none", new StaticMessage("(none)"));
                dropdown.setSelected("_none", new StaticMessage("(none)"));
                nameInput.setText("");
                return;
            }
            dropdown.setSelected(selected, new StaticMessage(selected));
            if (!preserveText) {
                nameInput.setText(selected);
            }
        }

        @Override
        protected void refreshContent() {
            // Handled manually via save/load/delete; avoid overwriting the text field every tick.
        }
    }

    private static <T extends FormComponent> T addTo(FormContentBox box, T component) {
        box.addComponent(component);
        return component;
    }

    private static boolean attachForm(Object mgr, Form form) {
        if (mgr == null || form == null) return false;
        if (invoke(mgr, "addComponent", form)) {
            form.setHidden(false);
            return true;
        }
        if (scheduleAdd(mgr, form)) return true;
        Object list = getComponentList(mgr);
        if (list != null && tryAddToList(list, form)) {
            form.setHidden(false);
            return true;
        }
        return false;
    }

    private static void detachForm(Object mgr, Form form) {
        if (mgr == null || form == null) return;
        if (!invoke(mgr, "removeComponent", form)) {
            invoke(mgr, "remove", form);
        }
        form.setHidden(true);
        invoke(form, "dispose");
    }

    private static boolean scheduleAdd(Object mgr, Form form) {
        Method timeout = findMethodExact(mgr.getClass(), "setTimeout", Runnable.class, long.class);
        if (timeout == null) return false;
        final Object managerRef = mgr;
        final Form formRef = form;
        Runnable task = () -> {
            if (!invoke(managerRef, "addComponent", formRef)) {
                Object list = getComponentList(managerRef);
                if (list == null || !tryAddToList(list, formRef)) {
                    return;
                }
            }
            formRef.setHidden(false);
        };
        try {
            timeout.setAccessible(true);
            timeout.invoke(mgr, task, 0L);
            return true;
        } catch (Throwable ignored) {}
        return false;
    }

    private static Object getComponentList(Object mgr) {
        Class<?> c = mgr.getClass();
        while (c != null) {
            try {
                Field f = c.getDeclaredField("components");
                f.setAccessible(true);
                Object value = f.get(mgr);
                if (value != null) return value;
            } catch (NoSuchFieldException ignored) {
            } catch (IllegalAccessException e) {
                break;
            }
            c = c.getSuperclass();
        }
        return invokeAndReturn(mgr, "getComponentList");
    }

    private static boolean tryAddToList(Object list, Form form) {
        if (list == null || form == null) return false;
        for (String name : new String[]{"addComponent", "add", "addLast", "push", "offer"}) {
            if (invoke(list, name, form)) return true;
        }
        return false;
    }

    private static void applyFormDecorations(Form form, boolean draw) {
        if (form == null) return;
        Method setter = findMethodExact(form.getClass(), "setDrawDecorations", boolean.class);
        if (setter != null) {
            try {
                setter.setAccessible(true);
                setter.invoke(form, draw);
                return;
            } catch (Throwable ignored) {}
        }
        Field field = findField(Form.class, "drawDecorations");
        if (field != null) {
            try {
                field.setAccessible(true);
                field.setBoolean(form, draw);
            } catch (Throwable ignored) {}
        }
    }

    private static Object invokeAndReturn(Object target, String method, Object... args) {
        Method m = findMethodByArgs(target.getClass(), method, args);
        if (m == null) return null;
        try {
            m.setAccessible(true);
            return m.invoke(target, args);
        } catch (Throwable ignored) {}
        return null;
    }

    private static boolean invoke(Object target, String method, Object... args) {
        if (target == null) return false;
        Method m = findMethodByArgs(target.getClass(), method, args);
        if (m == null) return false;
        try {
            m.setAccessible(true);
            m.invoke(target, args);
            return true;
        } catch (Throwable ignored) {}
        return false;
    }

    private static Method findMethodByArgs(Class<?> type, String name, Object... args) {
        Class<?> cls = type;
        while (cls != null) {
            for (Method method : cls.getDeclaredMethods()) {
                if (!method.getName().equals(name)) continue;
                if (!parametersMatch(method.getParameterTypes(), args)) continue;
                return method;
            }
            cls = cls.getSuperclass();
        }
        return null;
    }

    private static Method findMethodExact(Class<?> type, String name, Class<?>... params) {
        Class<?> cls = type;
        while (cls != null) {
            try {
                return cls.getDeclaredMethod(name, params);
            } catch (NoSuchMethodException ignored) {}
            cls = cls.getSuperclass();
        }
        return null;
    }

    private static Field findField(Class<?> type, String name) {
        Class<?> cls = type;
        while (cls != null) {
            try {
                return cls.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {}
            cls = cls.getSuperclass();
        }
        return null;
    }

    private static boolean parametersMatch(Class<?>[] params, Object[] args) {
        if (params.length != args.length) return false;
        for (int i = 0; i < params.length; i++) {
            if (!paramAccepts(params[i], args[i])) return false;
        }
        return true;
    }

    private static boolean paramAccepts(Class<?> param, Object arg) {
        if (arg == null) return !param.isPrimitive();
        Class<?> argClass = arg.getClass();
        if (param.isPrimitive()) {
            Class<?> boxed = boxed(param);
            return boxed != null && boxed.isAssignableFrom(argClass);
        }
        return param.isAssignableFrom(argClass);
    }

    private static Class<?> boxed(Class<?> primitive) {
        if (primitive == boolean.class) return Boolean.class;
        if (primitive == byte.class) return Byte.class;
        if (primitive == short.class) return Short.class;
        if (primitive == int.class) return Integer.class;
        if (primitive == long.class) return Long.class;
        if (primitive == float.class) return Float.class;
        if (primitive == double.class) return Double.class;
        if (primitive == char.class) return Character.class;
        return null;
    }
}
