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
import colox.gridmod.paint.PaintBlueprints;
import colox.gridmod.paint.PaintCategory;
import colox.gridmod.paint.PaintControls;
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
            PanelType.GRID,
            PanelType.SETTLEMENT
    };

    private static final IdentityHashMap<Object, PanelsHost> HOSTS = new IdentityHashMap<>();
    private static boolean buttonClickedThisTick;

    private enum PanelType { PAINT, BLUEPRINTS, GRID, SETTLEMENT }

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
            boolean showSettlement = paintEnabled;
            boolean showGrid = GridConfig.gridEnabled;

            updatePanel(PanelType.PAINT, showPaint);
            updatePanel(PanelType.BLUEPRINTS, showBlueprints);
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
            y += 30;
            for (PaintCategory cat : PaintCategory.values()) {
                rows.add(new CategoryRow(cat, y));
                y += 26;
            }
            listBox.setContentBox(new Rectangle(0, 0, PANEL_WIDTH, Math.max(y + 12, listBox.getHeight())));
        }

        @Override
        protected void refreshContent() {
            if (paintToggle != null) paintToggle.checked = PaintState.enabled;
            hoverMaster.checked = GridConfig.isHoverLabelsEnabled();
            PaintCategory active = GridConfig.getActivePaintCategory();
            for (CategoryRow row : rows) row.sync(active, hoverMaster.checked);
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
        private FormSlider chunkSpan;
        private FormSlider chunkAlpha;
        private FormSlider subChunkSpan;
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
            chunkSpan = content.addComponent(new FormSlider("Chunk span", 12, y, GridConfig.chunkSpanTiles, 8, 64, sliderW));
            chunkSpan.drawValue = true;
            chunkSpan.drawValueInPercent = false;
            chunkSpan.onChanged(e -> updateChunkSpan());
            y += FormInputSize.SIZE_24.height + 12;
            chunkAlpha = content.addComponent(new FormSlider("Chunk alpha", 12, y, Math.round(GridConfig.chunkAlpha * 100), 0, 100, sliderW));
            chunkAlpha.onChanged(e -> updateAlpha("chunk"));
            y += FormInputSize.SIZE_24.height + 12;
            subChunkToggle = content.addComponent(new FormCheckBox("Sub-chunk lines", 12, y, GridConfig.showSubChunkLines));
            subChunkToggle.onClicked(e -> toggleSubChunks(subChunkToggle.checked));
            y += 28;
            subChunkSpan = content.addComponent(new FormSlider("Sub-chunk span", 12, y, GridConfig.subChunkSpanTiles, 4, 32, sliderW));
            subChunkSpan.drawValue = true;
            subChunkSpan.drawValueInPercent = false;
            subChunkSpan.onChanged(e -> updateSubChunkSpan());
            y += FormInputSize.SIZE_24.height + 12;
            subAlpha = content.addComponent(new FormSlider("Sub-chunk alpha", 12, y, Math.round(GridConfig.subChunkAlpha * 100), 0, 100, sliderW));
            subAlpha.onChanged(e -> updateAlpha("sub"));
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

        private void updateChunkSpan() {
            int value = chunkSpan.getValue();
            if (GridConfig.chunkSpanTiles == value) return;
            GridConfig.chunkSpanTiles = value;
            GridConfig.markDirty();
            GridConfig.saveIfDirty();
        }

        private void updateSubChunkSpan() {
            int value = subChunkSpan.getValue();
            if (GridConfig.subChunkSpanTiles == value) return;
            GridConfig.subChunkSpanTiles = value;
            GridConfig.markDirty();
            GridConfig.saveIfDirty();
        }

        @Override
        protected void refreshContent() {
            internal = true;
            if (gridToggle != null) gridToggle.checked = GridConfig.gridEnabled;
            gridAlpha.setValue(Math.round(GridConfig.lineAlpha * 100));
            if (chunkToggle != null) chunkToggle.checked = GridConfig.showChunkLines;
            chunkSpan.setValue(GridConfig.chunkSpanTiles);
            chunkAlpha.setValue(Math.round(GridConfig.chunkAlpha * 100));
            if (subChunkToggle != null) subChunkToggle.checked = GridConfig.showSubChunkLines;
            subChunkSpan.setValue(GridConfig.subChunkSpanTiles);
            subAlpha.setValue(Math.round(GridConfig.subChunkAlpha * 100));
            internal = false;
        }
    }

    private static final class SettlementPanel extends SidePanelForm {
        private FormLabel info;
        private FormCheckBox boundsToggle;

        SettlementPanel() {
            super("Settlement", "Settlement bounds", 180);
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
            FormTextButton place = content.addComponent(new FormTextButton("Place here", 12, y, PANEL_WIDTH - 24, FormInputSize.SIZE_24, ButtonColor.BASE));
            place.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> {
                buttonClickedThisTick = true;
                PaintControls.placeHereAndEnable();
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
        }
    }

    private static final class BlueprintPanel extends SidePanelForm {
        private FormDropdownSelectionButton<String> dropdown;
        private FormLabel statusLabel;
        private FormLabel selectionLabel;
        private String currentBlueprint;
        private long lastSaveClick;
        private List<ModeButton> modeButtons;
        private FormTextButton flipBtn;

        BlueprintPanel() {
            super("Blueprints", "Blueprint tools", 360);
        }

        @Override
        protected void buildContent(FormContentBox content) {
            int dropdownWidth = PANEL_WIDTH - 150;
            dropdown = content.addComponent(new FormDropdownSelectionButton<>(12, 6, FormInputSize.SIZE_24, ButtonColor.BASE, dropdownWidth));
            dropdown.onSelected(e -> {
                currentBlueprint = e.value;
                GridConfig.selectedBlueprint = currentBlueprint;
                GridConfig.markDirty();
            });
            FormTextButton refresh = content.addComponent(new FormTextButton("Refresh", 24 + dropdownWidth, 6, 100, FormInputSize.SIZE_24, ButtonColor.BASE));
            refresh.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> refreshBlueprintOptions());

            int y = 6 + FormInputSize.SIZE_24.height + 8;
            FormTextButton save = content.addComponent(new FormTextButton("Save (dbl-click)", 12, y, PANEL_WIDTH - 24, FormInputSize.SIZE_24, ButtonColor.BASE));
            save.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> handleSave());
            y += FormInputSize.SIZE_24.height + 6;
            FormTextButton load = content.addComponent(new FormTextButton("Load", 12, y, PANEL_WIDTH - 24, FormInputSize.SIZE_24, ButtonColor.BASE));
            load.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> handleLoad());
            y += FormInputSize.SIZE_24.height + 10;

            content.addComponent(new FormLabel("Selection mode", new FontOptions(14), FormLabel.ALIGN_LEFT, 12, y));
            y += 22;
            y = buildModeButtons(content, y);

            flipBtn = content.addComponent(new FormTextButton("Flip", 12, y, 100, FormInputSize.SIZE_24, ButtonColor.BASE));
            flipBtn.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> {
                if (BlueprintPlacement.active) {
                    BlueprintPlacement.toggleFlip();
                    updateModeButtons();
                }
            });
            y += FormInputSize.SIZE_24.height + 8;

            statusLabel = content.addComponent(new FormLabel("", new FontOptions(12), FormLabel.ALIGN_LEFT, 12, y));
            y += 18;
            selectionLabel = content.addComponent(new FormLabel("", new FontOptions(12), FormLabel.ALIGN_LEFT, 12, y));
            refreshBlueprintOptions();
            updateModeButtons();
        }

        private int buildModeButtons(FormContentBox content, int y) {
            SelectionState.Mode[] modes = SelectionState.Mode.values();
            int btnW = (PANEL_WIDTH - 30) / Math.min(4, modes.length);
            SelectionState.Mode[] order = new SelectionState.Mode[]{SelectionState.Mode.RECT, SelectionState.Mode.EDGE, SelectionState.Mode.EDGE_FILL, SelectionState.Mode.LASSO_FILL};
            int x = 12;
            modeButtons = new ArrayList<>();
            for (SelectionState.Mode mode : order) {
                FormTextButton btn = content.addComponent(new FormTextButton(modeLabel(mode), x, y, btnW - 4, FormInputSize.SIZE_24, ButtonColor.BASE));
                btn.onClicked((FormEventListener<FormInputEvent<FormButton>>) e -> {
                    SelectionState.setMode(mode);
                    updateModeButtons();
                });
                modeButtons.add(new ModeButton(mode, btn));
                x += btnW;
            }
            return y + FormInputSize.SIZE_24.height + 8;
        }

        private String modeLabel(SelectionState.Mode mode) {
            switch (mode) {
                case EDGE: return "Edge";
                case EDGE_FILL: return "Edge+Fill";
                case LASSO_FILL: return "Lasso";
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
            long now = System.currentTimeMillis();
            if (now - lastSaveClick < 600) {
                PaintBlueprints.saveBlueprint(name);
                statusLabel.setText("Saved '" + name + "'");
                lastSaveClick = 0;
            } else {
                lastSaveClick = now;
                statusLabel.setText("Click again to save '" + name + "'");
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
        }

        private String getCurrentBlueprintName() {
            return currentBlueprint;
        }

        private void refreshBlueprintOptions() {
            dropdown.options.clear();
            String[] names = PaintBlueprints.listBlueprints();
            if (names.length == 0) {
                names = new String[]{"quick"};
            }
            currentBlueprint = GridConfig.selectedBlueprint;
            if (currentBlueprint == null || currentBlueprint.isBlank()) currentBlueprint = names[0];
            boolean found = false;
            for (String n : names) {
                dropdown.options.add(n, new StaticMessage(n));
                if (n.equals(currentBlueprint)) found = true;
            }
            if (!found) currentBlueprint = names[0];
            dropdown.setSelected(currentBlueprint, new StaticMessage(currentBlueprint));
            statusLabel.setText("");
        }

        private void updateModeButtons() {
            SelectionState.Mode mode = SelectionState.getMode();
            for (ModeButton entry : modeButtons) {
                String text = entry.mode == mode ? ("> " + modeLabel(entry.mode)) : modeLabel(entry.mode);
                entry.button.setText(text);
            }
            if (flipBtn != null) {
                flipBtn.setText(BlueprintPlacement.isFlipped() ? "Flip (X)" : "Flip");
                flipBtn.setActive(BlueprintPlacement.active);
            }
            selectionLabel.setText("Mode: " + modeLabel(mode));
        }

        @Override
        protected void refreshContent() {
            updateModeButtons();
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
        System.out.println("[GridMod] PaintQuickPaletteOverlay: failed to attach form to manager "
                + mgr.getClass().getName());
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
                    System.out.println("[GridMod] PaintQuickPaletteOverlay: scheduled add failed.");
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
