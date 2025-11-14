package colox.gridmod.config;

import java.io.File;
import java.util.HashMap;

import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import colox.gridmod.paint.DefaultBlueprintRegistry;
import colox.gridmod.paint.PaintCategory;
import colox.gridmod.paint.PaintLayerFilter;
import colox.gridmod.paint.SelectionState;
import colox.gridmod.util.ConfigPaths;

public final class GridConfig {

    // ===== Visible settings (defaults) =====
    public static int   tileSize = 32;

    public static boolean gridEnabled = true;

    public static float lineAlpha = 0.25f;
    public static float r = 1f, g = 1f, b = 1f;

    // Show/hide paint overlay without disabling painting logic itself
    public static boolean paintVisible = true;

    // ----- Chunk lines -----
    public static boolean showChunkLines = true;
    public static int     chunkSpanTiles = 16;
    public static int     chunkThickness = 2;
    public static float   chunkAlpha = 0.45f;
    public static float   cr = 1f, cg = 0.55f, cb = 0f;

    // ----- Sub-chunk lines -----
    public static boolean showSubChunkLines = false;
    public static int     subChunkSpanTiles = 8;
    public static int     subChunkThickness = 1;
    public static float   subChunkAlpha = 0.25f;
    public static float   scr = 0.3f, scg = 0.9f, scb = 1f;

    // ----- Blueprint/paint colors -----
    public static float paintR = 0.95f, paintG = 0.95f, paintB = 0.95f, paintAlpha = 0.50f;
    public static float eraseR = 1.00f, eraseG = 0.35f, eraseB = 0.35f, eraseAlpha = 0.50f;
    public static float selectionR = 0.20f, selectionG = 0.80f, selectionB = 1.00f, selectionAlpha = 0.50f;
    public static float bpGhostR = 1.00f, bpGhostG = 1.00f, bpGhostB = 1.00f, bpGhostAlpha = 0.50f;
    public static String activePaintCategory = PaintCategory.defaultCategory().id();
    public static boolean hoverLabelsEnabled = true;
    public static PaintLayerFilter paintEraseFilter = PaintLayerFilter.ALL;
    public static boolean paintEraseOverride = false;
    public static PaintLayerFilter paintSelectionFilter = PaintLayerFilter.ALL;
    private static final HashMap<String, PaintColor> paintCategoryColors = new HashMap<>();
    private static final HashMap<String, Boolean> hoverCategoryVisibility = new HashMap<>();

    static {
        resetPaintCategoryColors();
        resetHoverCategoryVisibility();
    }

    public static String selectedBlueprint = "quick";
    public static String selectedGlobalBlueprint = "global_quick";

    public static float uiOpacity = 1.0f;

    // ===== Settlement =====
    public static boolean settlementEnabled = false;
    public static String settlementMode = "builtin";
    public static int settlementTier = 1;

    public static int settlementSizeT1 = 64;
    public static int settlementSizeT2 = 96;
    public static int settlementSizeT3 = 128;
    public static int settlementSizeT4 = 160;
    public static int settlementSizeT5 = 192;

    public static float sbr = 1.0f, sbg = 0.4f, sbb = 0.0f;
    public static float settlementOutlineAlpha = 0.85f;
    public static int   settlementOutlineThickness = 2;
    public static float settlementFillAlpha = 0.12f;

    public static int settlementFlagTx = 0;
    public static int settlementFlagTy = 0;

    private static final int[] BUILTIN_SIDE_TILES = new int[] {
        80, 112, 144, 176, 208, 240, 272
    };

    private static final File CONFIG_FILE = ConfigPaths.settingsFile().toFile();
    private static boolean dirty = false;

    private GridConfig() {}

    public static void markDirty() { dirty = true; }
    public static void saveIfDirty() { if (dirty) save(); }

    public static void load() {
        try {
            resetPaintCategoryColors();
            resetHoverCategoryVisibility();
            if (!CONFIG_FILE.exists()) { save(); return; }
            LoadData ld = new LoadData(CONFIG_FILE);

            tileSize = ld.getInt("tileSize", tileSize);

            gridEnabled = ld.getBoolean("gridEnabled", gridEnabled);

            lineAlpha = ld.getFloat("lineAlpha", lineAlpha);
            r = ld.getFloat("r", r);
            g = ld.getFloat("g", g);
            b = ld.getFloat("b", b);

            paintVisible = ld.getBoolean("paintVisible", paintVisible);
            hoverLabelsEnabled = ld.getBoolean("hoverLabelsEnabled", hoverLabelsEnabled);

            String hoverStates = ld.getSafeString("hoverCategoryVisibility", "");
            if (!hoverStates.isEmpty()) {
                for (String part : hoverStates.split(";")) {
                    if (part.isEmpty() || !part.contains("=")) continue;
                    String[] kv = part.split("=", 2);
                    PaintCategory cat = PaintCategory.byId(kv[0]);
                    if (cat == null) continue;
                    boolean enabled = !"0".equals(kv[1]);
                    hoverCategoryVisibility.put(cat.id(), enabled);
                }
            }

            showChunkLines = ld.getBoolean("showChunkLines", showChunkLines);
            chunkSpanTiles = ld.getInt("chunkSpanTiles", chunkSpanTiles);
            chunkThickness = ld.getInt("chunkThickness", chunkThickness);
            chunkAlpha = ld.getFloat("chunkAlpha", chunkAlpha);
            cr = ld.getFloat("cr", cr);
            cg = ld.getFloat("cg", cg);
            cb = ld.getFloat("cb", cb);

            showSubChunkLines = ld.getBoolean("showSubChunkLines", showSubChunkLines);
            subChunkSpanTiles = ld.getInt("subChunkSpanTiles", subChunkSpanTiles);
            subChunkThickness = ld.getInt("subChunkThickness", subChunkThickness);
            subChunkAlpha = ld.getFloat("subChunkAlpha", subChunkAlpha);
            scr = ld.getFloat("scr", scr);
            scg = ld.getFloat("scg", scg);
            scb = ld.getFloat("scb", scb);

            paintR = ld.getFloat("paintR", paintR);
            paintG = ld.getFloat("paintG", paintG);
            paintB = ld.getFloat("paintB", paintB);
            paintAlpha = ld.getFloat("paintAlpha", paintAlpha);

            eraseR = ld.getFloat("eraseR", eraseR);
            eraseG = ld.getFloat("eraseG", eraseG);
            eraseB = ld.getFloat("eraseB", eraseB);
            eraseAlpha = ld.getFloat("eraseAlpha", eraseAlpha);

            selectionR = ld.getFloat("selectionR", selectionR);
            selectionG = ld.getFloat("selectionG", selectionG);
            selectionB = ld.getFloat("selectionB", selectionB);
            selectionAlpha = ld.getFloat("selectionAlpha", selectionAlpha);

            bpGhostR = ld.getFloat("bpGhostR", bpGhostR);
            bpGhostG = ld.getFloat("bpGhostG", bpGhostG);
            bpGhostB = ld.getFloat("bpGhostB", bpGhostB);
            bpGhostAlpha = ld.getFloat("bpGhostAlpha", bpGhostAlpha);

            activePaintCategory = ld.getSafeString("activePaintCategory", activePaintCategory);
            paintEraseFilter = PaintLayerFilter.byId(ld.getSafeString("paintEraseFilter", paintEraseFilter.id()));
            paintEraseOverride = ld.getBoolean("paintEraseOverride", paintEraseOverride);
            paintSelectionFilter = PaintLayerFilter.byId(ld.getSafeString("paintSelectionFilter", paintSelectionFilter.id()));
            for (PaintCategory cat : PaintCategory.values()) {
                float r = ld.getFloat("paintCategory." + cat.id() + ".r", cat.defaultR());
                float g = ld.getFloat("paintCategory." + cat.id() + ".g", cat.defaultG());
                float b = ld.getFloat("paintCategory." + cat.id() + ".b", cat.defaultB());
                float a = ld.getFloat("paintCategory." + cat.id() + ".a", cat.defaultA());
                paintCategoryColors.put(cat.id(), new PaintColor(r, g, b, a));
            }

            selectedBlueprint = DefaultBlueprintRegistry.canonicalKey(ld.getSafeString("selectedBlueprint", selectedBlueprint));
            selectedGlobalBlueprint = DefaultBlueprintRegistry.canonicalKey(ld.getSafeString("selectedGlobalBlueprint", selectedGlobalBlueprint));

            uiOpacity = ld.getFloat("uiOpacity", uiOpacity);

            settlementEnabled = ld.getBoolean("settlementEnabled", settlementEnabled);
            settlementMode = ld.getSafeString("settlementMode", settlementMode);
            settlementTier = ld.getInt("settlementTier", settlementTier);

            settlementSizeT1 = ld.getInt("settlementSizeT1", settlementSizeT1);
            settlementSizeT2 = ld.getInt("settlementSizeT2", settlementSizeT2);
            settlementSizeT3 = ld.getInt("settlementSizeT3", settlementSizeT3);
            settlementSizeT4 = ld.getInt("settlementSizeT4", settlementSizeT4);
            settlementSizeT5 = ld.getInt("settlementSizeT5", settlementSizeT5);

            sbr = ld.getFloat("sbr", sbr);
            sbg = ld.getFloat("sbg", sbg);
            sbb = ld.getFloat("sbb", sbb);

            settlementOutlineAlpha = ld.getFloat("settlementOutlineAlpha", settlementOutlineAlpha);
            settlementOutlineThickness = ld.getInt("settlementOutlineThickness", settlementOutlineThickness);
            settlementFillAlpha = ld.getFloat("settlementFillAlpha", settlementFillAlpha);

            settlementFlagTx = ld.getInt("settlementFlagTx", settlementFlagTx);
            settlementFlagTy = ld.getInt("settlementFlagTy", settlementFlagTy);

            clamp();
            dirty = false;
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static void save() {
        try {
            clamp();
            SaveData sd = new SaveData("gridmod");

            sd.addInt("tileSize", tileSize);

            sd.addBoolean("gridEnabled", gridEnabled);

            sd.addFloat("lineAlpha", lineAlpha);
            sd.addFloat("r", r);  sd.addFloat("g", g);  sd.addFloat("b", b);

            sd.addBoolean("paintVisible", paintVisible);
            sd.addBoolean("hoverLabelsEnabled", hoverLabelsEnabled);
            StringBuilder hoverBuilder = new StringBuilder();
            for (PaintCategory cat : PaintCategory.values()) {
                boolean enabled = hoverCategoryVisibility.getOrDefault(cat.id(), Boolean.TRUE);
                hoverBuilder.append(cat.id()).append('=').append(enabled ? '1' : '0').append(';');
            }
            sd.addSafeString("hoverCategoryVisibility", hoverBuilder.toString());

            sd.addBoolean("showChunkLines", showChunkLines);
            sd.addInt("chunkSpanTiles", chunkSpanTiles);
            sd.addInt("chunkThickness", chunkThickness);
            sd.addFloat("chunkAlpha", chunkAlpha);
            sd.addFloat("cr", cr); sd.addFloat("cg", cg); sd.addFloat("cb", cb);

            sd.addBoolean("showSubChunkLines", showSubChunkLines);
            sd.addInt("subChunkSpanTiles", subChunkSpanTiles);
            sd.addInt("subChunkThickness", subChunkThickness);
            sd.addFloat("subChunkAlpha", subChunkAlpha);
            sd.addFloat("scr", scr); sd.addFloat("scg", scg); sd.addFloat("scb", scb);

            sd.addFloat("paintR", paintR); sd.addFloat("paintG", paintG); sd.addFloat("paintB", paintB); sd.addFloat("paintAlpha", paintAlpha);
            sd.addFloat("eraseR", eraseR); sd.addFloat("eraseG", eraseG); sd.addFloat("eraseB", eraseB); sd.addFloat("eraseAlpha", eraseAlpha);
            sd.addFloat("selectionR", selectionR); sd.addFloat("selectionG", selectionG); sd.addFloat("selectionB", selectionB); sd.addFloat("selectionAlpha", selectionAlpha);
            sd.addFloat("bpGhostR", bpGhostR); sd.addFloat("bpGhostG", bpGhostG); sd.addFloat("bpGhostB", bpGhostB); sd.addFloat("bpGhostAlpha", bpGhostAlpha);
            for (PaintCategory cat : PaintCategory.values()) {
                PaintColor color = getPaintColor(cat);
                sd.addFloat("paintCategory." + cat.id() + ".r", color.r);
                sd.addFloat("paintCategory." + cat.id() + ".g", color.g);
                sd.addFloat("paintCategory." + cat.id() + ".b", color.b);
                sd.addFloat("paintCategory." + cat.id() + ".a", color.a);
            }
            sd.addSafeString("activePaintCategory", activePaintCategory);
            sd.addSafeString("paintEraseFilter", paintEraseFilter.id());
            sd.addBoolean("paintEraseOverride", paintEraseOverride);
            sd.addSafeString("paintSelectionFilter", paintSelectionFilter.id());

            sd.addSafeString("selectedBlueprint", selectedBlueprint);
            sd.addSafeString("selectedGlobalBlueprint", selectedGlobalBlueprint);

            sd.addFloat("uiOpacity", uiOpacity);

            sd.addBoolean("settlementEnabled", settlementEnabled);
            sd.addSafeString("settlementMode", settlementMode);
            sd.addInt("settlementTier", settlementTier);

            sd.addInt("settlementSizeT1", settlementSizeT1);
            sd.addInt("settlementSizeT2", settlementSizeT2);
            sd.addInt("settlementSizeT3", settlementSizeT3);
            sd.addInt("settlementSizeT4", settlementSizeT4);
            sd.addInt("settlementSizeT5", settlementSizeT5);

            sd.addFloat("sbr", sbr); sd.addFloat("sbg", sbg); sd.addFloat("sbb", sbb);
            sd.addFloat("settlementOutlineAlpha", settlementOutlineAlpha);
            sd.addInt("settlementOutlineThickness", settlementOutlineThickness);
            sd.addFloat("settlementFillAlpha", settlementFillAlpha);

            sd.addInt("settlementFlagTx", settlementFlagTx);
            sd.addInt("settlementFlagTy", settlementFlagTy);

            sd.saveScript(CONFIG_FILE);
            dirty = false;
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static void clamp() {
        if (tileSize < 8) tileSize = 8;

        if (chunkSpanTiles < 2) chunkSpanTiles = 2;
        if (chunkThickness < 1) chunkThickness = 1;
        if (subChunkThickness < 1) subChunkThickness = 1;

        if (subChunkSpanTiles < 2 || (chunkSpanTiles % subChunkSpanTiles) != 0) {
            subChunkSpanTiles = Math.max(2, chunkSpanTiles / 4);
        }

        lineAlpha = clamp01(lineAlpha);
        chunkAlpha = clamp01(chunkAlpha);
        subChunkAlpha = clamp01(subChunkAlpha);

        r = clamp01(r); g = clamp01(g); b = clamp01(b);
        cr = clamp01(cr); cg = clamp01(cg); cb = clamp01(cb);
        scr = clamp01(scr); scg = clamp01(scg); scb = clamp01(scb);

        paintR = clamp01(paintR); paintG = clamp01(paintG); paintB = clamp01(paintB); paintAlpha = clamp01(paintAlpha);
        eraseR = clamp01(eraseR); eraseG = clamp01(eraseG); eraseB = clamp01(eraseB); eraseAlpha = clamp01(eraseAlpha);
        selectionR = clamp01(selectionR); selectionG = clamp01(selectionG); selectionB = clamp01(selectionB); selectionAlpha = clamp01(selectionAlpha);
        bpGhostR = clamp01(bpGhostR); bpGhostG = clamp01(bpGhostG); bpGhostB = clamp01(bpGhostB); bpGhostAlpha = clamp01(bpGhostAlpha);

        if (selectedBlueprint == null) selectedBlueprint = "quick";
        if (selectedGlobalBlueprint == null || selectedGlobalBlueprint.isBlank()) selectedGlobalBlueprint = "global_quick";

        uiOpacity = clamp01(uiOpacity);

        if (settlementMode == null || settlementMode.isBlank()) settlementMode = "builtin";
        settlementTier = Math.max(1, Math.min(maxTier(), settlementTier));

        if (settlementSizeT1 < 2) settlementSizeT1 = 2;
        if (settlementSizeT2 < 2) settlementSizeT2 = 2;
        if (settlementSizeT3 < 2) settlementSizeT3 = 2;
        if (settlementSizeT4 < 2) settlementSizeT4 = 2;
        if (settlementSizeT5 < 2) settlementSizeT5 = 2;

        sbr = clamp01(sbr); sbg = clamp01(sbg); sbb = clamp01(sbb);
        settlementOutlineAlpha = clamp01(settlementOutlineAlpha);
        settlementFillAlpha = clamp01(settlementFillAlpha);
        if (settlementOutlineThickness < 1) settlementOutlineThickness = 1;
    }

    private static float clamp01(float v) { return v < 0f ? 0f : (v > 1f ? 1f : v); }

    public static boolean hasSettlementFlag() {
        return (settlementFlagTx != 0 || settlementFlagTy != 0);
    }

    public static int maxTier() {
        return "builtin".equalsIgnoreCase(settlementMode) ? BUILTIN_SIDE_TILES.length : 5;
    }

    public static int currentTierSideTiles() {
        int t = Math.max(1, Math.min(maxTier(), settlementTier));
        if ("builtin".equalsIgnoreCase(settlementMode)) return BUILTIN_SIDE_TILES[t - 1];
        switch (t) {
            case 1: default: return settlementSizeT1;
            case 2: return settlementSizeT2;
            case 3: return settlementSizeT3;
            case 4: return settlementSizeT4;
            case 5: return settlementSizeT5;
        }
    }

    public static int currentTierChunks() {
        int tiles = currentTierSideTiles();
        return Math.max(1, tiles / 16);
    }

    public static void cycleSettlementTier() {
        settlementTier++;
        if (settlementTier > maxTier()) settlementTier = 1;
        markDirty(); saveIfDirty();
    }

    public static void addAlpha(float delta) { lineAlpha = clamp01(lineAlpha + delta); markDirty(); saveIfDirty(); }
    public static void addChunkAlpha(float delta) { chunkAlpha = clamp01(chunkAlpha + delta); markDirty(); saveIfDirty(); }
    public static void addSubAlpha(float delta) { subChunkAlpha = clamp01(subChunkAlpha + delta); markDirty(); saveIfDirty(); }

    public static void cycleChunkSpan() {
        int next;
        switch (chunkSpanTiles) {
            case 8: next = 16; break;
            case 16: next = 32; break;
            case 32: next = 64; break;
            default: next = 16; break;
        }
        chunkSpanTiles = next;
        subChunkSpanTiles = Math.max(2, chunkSpanTiles / 4);
        markDirty(); saveIfDirty();
    }

    // Convenience for keybind
    public static void togglePaintVisible() {
        paintVisible = !paintVisible;
        markDirty();
        saveIfDirty();
    }

    public static PaintCategory getActivePaintCategory() {
        return PaintCategory.byId(activePaintCategory);
    }

    public static void setActivePaintCategory(PaintCategory category) {
        if (category == null) category = PaintCategory.defaultCategory();
        if (!category.id().equals(activePaintCategory)) {
            activePaintCategory = category.id();
            markDirty();
        }
    }

    public static boolean isHoverLabelsEnabled() {
        return hoverLabelsEnabled;
    }

    public static void setHoverLabelsEnabled(boolean enabled) {
        if (hoverLabelsEnabled != enabled) {
            hoverLabelsEnabled = enabled;
            markDirty();
        }
    }

    public static boolean isHoverCategoryAllowed(PaintCategory category) {
        if (category == null) category = PaintCategory.defaultCategory();
        Boolean enabled = hoverCategoryVisibility.get(category.id());
        return enabled == null ? true : enabled.booleanValue();
    }

    public static void setHoverCategoryAllowed(PaintCategory category, boolean allowed) {
        if (category == null) category = PaintCategory.defaultCategory();
        hoverCategoryVisibility.put(category.id(), allowed);
        markDirty();
    }

    public static PaintLayerFilter getPaintEraseFilter() {
        return paintEraseFilter;
    }

    public static void setPaintEraseFilter(PaintLayerFilter filter) {
        if (filter == null) filter = PaintLayerFilter.ALL;
        if (paintEraseFilter != filter) {
            paintEraseFilter = filter;
            markDirty();
        }
    }

    public static boolean isPaintEraseOverride() {
        return paintEraseOverride;
    }

    public static void setPaintEraseOverride(boolean override) {
        if (paintEraseOverride != override) {
            paintEraseOverride = override;
            markDirty();
        }
    }

    public static PaintLayerFilter getEffectivePaintEraseFilter() {
        if (paintEraseOverride) {
            return paintEraseFilter;
        }
        PaintCategory active = getActivePaintCategory();
        return PaintLayerFilter.forLayer(active.layer());
    }

    public static PaintLayerFilter getPaintSelectionFilter() {
        return paintSelectionFilter;
    }

    public static void setPaintSelectionFilter(PaintLayerFilter filter) {
        if (filter == null) filter = PaintLayerFilter.ALL;
        if (paintSelectionFilter != filter) {
            paintSelectionFilter = filter;
            markDirty();
            SelectionState.refreshSelection();
        }
    }

    public static void resetHoverCategoryVisibility() {
        hoverCategoryVisibility.clear();
        for (PaintCategory cat : PaintCategory.values()) {
            hoverCategoryVisibility.put(cat.id(), Boolean.TRUE);
        }
    }

    // ---------- Paint category helpers ----------
    public static PaintColor getPaintColor(PaintCategory category) {
        if (category == null) category = PaintCategory.defaultCategory();
        PaintColor color = paintCategoryColors.get(category.id());
        if (color == null) {
            color = new PaintColor(category.defaultR(), category.defaultG(), category.defaultB(), category.defaultA());
            paintCategoryColors.put(category.id(), color);
        }
        return color;
    }

    public static void setPaintColor(PaintCategory category, float r, float g, float b, float a) {
        if (category == null) category = PaintCategory.defaultCategory();
        paintCategoryColors.put(category.id(), new PaintColor(r, g, b, a));
        markDirty();
    }

    public static void resetPaintCategoryColors() {
        paintCategoryColors.clear();
        for (PaintCategory cat : PaintCategory.values()) {
            paintCategoryColors.put(cat.id(), new PaintColor(cat.defaultR(), cat.defaultG(), cat.defaultB(), cat.defaultA()));
        }
    }

    public static final class PaintColor {
        public final float r, g, b, a;
        public PaintColor(float r, float g, float b, float a) {
            this.r = clamp01(r);
            this.g = clamp01(g);
            this.b = clamp01(b);
            this.a = clamp01(a);
        }
    }
}
