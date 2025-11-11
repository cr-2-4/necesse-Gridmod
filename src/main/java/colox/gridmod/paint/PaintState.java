package colox.gridmod.paint;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;

import colox.gridmod.util.ConfigPaths;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;

public final class PaintState {
    public static boolean enabled = false;
    private static int brush = 1;
    public static float a = 0.25f, r = 0.2f, g = 0.8f, b = 1.0f;

    private static final Map<Long, PaintCell> painted = new HashMap<>();
    private static final PaintLayer[] LAYERS = PaintLayer.values();
    private static boolean dirty = false;
    private static File file = ConfigPaths.paintFile().toFile();

    private PaintState() {}

    // --- UI-friendly controls ---
    public static void setEnabled(boolean value) {
        enabled = value;
        markDirty(); saveIfDirty();
    }

    /** Brush side length clamped [1..32]. */
    public static void setBrush(int size) {
        if (size < 1) size = 1;
        if (size > 32) size = 32;
        brush = size;
        markDirty(); saveIfDirty();
    }

    public static void toggle() { enabled = !enabled; markDirty(); saveIfDirty(); }
    public static void clear()  { painted.clear(); markDirty(); saveIfDirty(); }

    public static int  getBrush()  { return brush; } // brush = side length (1..32)
    public static void incBrush()  { setBrush(brush + 1); }
    public static void decBrush()  { setBrush(brush - 1); }

    public static void add(int tx, int ty, String categoryId) {
        PaintCategory category = resolveCategory(categoryId);
        long k = key(tx, ty);
        PaintCell cell = painted.computeIfAbsent(k, kk -> new PaintCell());
        String normalized = category.id();
        String previous = cell.set(category.layer(), normalized);
        if (previous == null || !previous.equals(normalized)) dirty = true;
        if (cell.isEmpty()) painted.remove(k);
    }

    @Deprecated
    public static void add(int tx, int ty) {
        add(tx, ty, colox.gridmod.paint.PaintCategory.defaultCategory().id());
    }

    public static void remove(int tx, int ty, String categoryId) {
        PaintLayer layer = (categoryId == null) ? null : resolveCategory(categoryId).layer();
        long k = key(tx, ty);
        PaintCell cell = painted.get(k);
        if (cell == null) return;
        boolean changed;
        if (layer == null) {
            changed = painted.remove(k) != null;
        } else {
            changed = cell.clear(layer);
            if (cell.isEmpty()) painted.remove(k);
        }
        if (changed) dirty = true;
    }

    public static void removeAll(int tx, int ty) {
        if (painted.remove(key(tx, ty)) != null) dirty = true;
    }

    public static String getCategory(int tx, int ty) {
        PaintCell cell = painted.get(key(tx, ty));
        if (cell == null) return PaintCategory.defaultCategory().id();
        String cat = cell.topCategoryId();
        return (cat == null) ? PaintCategory.defaultCategory().id() : cat;
    }

    public static PaintEntry getPaintEntry(int tx, int ty) {
        PaintCell cell = painted.get(key(tx, ty));
        if (cell == null) return null;
        return cell.topEntry(tx, ty);
    }

    public static List<PaintEntry> iterateSnapshot() {
        List<PaintEntry> out = new ArrayList<>(painted.size());
        for (Map.Entry<Long, PaintCell> entry : painted.entrySet()) {
            long k = entry.getKey();
            int x = (int)(k >> 32);
            int y = (int) k;
            entry.getValue().collectEntries(x, y, out);
        }
        out.sort(Comparator.comparingInt(e -> e.layer.drawOrder()));
        return out;
    }

    public static void load() {
        try {
            Path p = ConfigPaths.paintFile();
            file = p.toFile();
            if (!file.exists()) { save(); return; }

            LoadData ld = new LoadData(file);
            enabled = ld.getBoolean("enabled", false);
            brush   = ld.getInt("brush", 1);
            if (brush < 1) brush = 1;
            if (brush > 32) brush = 32;

            a = ld.getFloat("a", a);
            r = ld.getFloat("r", r);
            g = ld.getFloat("g", g);
            b = ld.getFloat("b", b);

            painted.clear();
            String points = ld.getSafeString("points", "");
            int parsed = 0;
            if (!points.isEmpty()) {
                String[] pairs = points.split(";");
                for (String pair : pairs) {
                    if (pair.isEmpty()) continue;
                    String[] xy = pair.split(",");
                    if (xy.length < 2) continue;
                    int x = Integer.parseInt(xy[0].trim());
                    int y = Integer.parseInt(xy[1].trim());
                    String cat = (xy.length >= 3) ? xy[2].trim() : colox.gridmod.paint.PaintCategory.defaultCategory().id();
                    add(x, y, cat);
                    parsed++;
                }
            }
            dirty = false;
            System.out.println("[GridMod] PaintState.load path=" + file.getAbsolutePath() + " tiles=" + parsed);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static void save() {
        try {
            if (file == null) file = ConfigPaths.paintFile().toFile();
            SaveData sd = new SaveData("gridpaint");
            sd.addBoolean("enabled", enabled);
            sd.addInt("brush", brush);
            sd.addFloat("a", a);
            sd.addFloat("r", r); sd.addFloat("g", g); sd.addFloat("b", b);

            List<PaintEntry> snapshot = iterateSnapshot();
            StringBuilder sb = new StringBuilder(snapshot.size() * 16);
            for (PaintEntry entry : snapshot) {
                sb.append(entry.x).append(',').append(entry.y).append(',').append(entry.categoryId).append(';');
            }
            sd.addSafeString("points", sb.toString());

            ConfigPaths.modDataDir().toFile().mkdirs();
            sd.saveScript(file);
            dirty = false;
            System.out.println("[GridMod] PaintState.save path=" + file.getAbsolutePath() + " tiles=" + painted.size());
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static void saveIfDirty() { if (dirty) save(); }
    public static void markDirty()   { dirty = true; }

    private static long key(int x, int y) { return ((long)x << 32) | (y & 0xffffffffL); }

    private static PaintCategory resolveCategory(String categoryId) {
        return PaintCategory.byId(categoryId);
    }

    public static final class PaintEntry {
        public final int x;
        public final int y;
        public final String categoryId;
        public final PaintLayer layer;
        public PaintEntry(int x, int y, PaintLayer layer, String categoryId) {
            this.x = x;
            this.y = y;
            this.layer = layer;
            this.categoryId = (categoryId == null)
                    ? PaintCategory.defaultCategory().id()
                    : categoryId;
        }
    }

    private static final class PaintCell {
        private final String[] layers = new String[LAYERS.length];

        String set(PaintLayer layer, String categoryId) {
            if (layer == null) return null;
            int idx = layer.ordinal();
            String prev = layers[idx];
            layers[idx] = categoryId;
            return prev;
        }

        boolean clear(PaintLayer layer) {
            if (layer == null) return false;
            int idx = layer.ordinal();
            if (layers[idx] == null) return false;
            layers[idx] = null;
            return true;
        }

        boolean isEmpty() {
            for (String s : layers) {
                if (s != null) return false;
            }
            return true;
        }

        String topCategoryId() {
            for (int i = layers.length - 1; i >= 0; i--) {
                if (layers[i] != null) return layers[i];
            }
            return null;
        }

        PaintEntry topEntry(int x, int y) {
            for (int i = layers.length - 1; i >= 0; i--) {
                String cat = layers[i];
                if (cat != null) {
                    return new PaintEntry(x, y, LAYERS[i], cat);
                }
            }
            return null;
        }

        void collectEntries(int x, int y, List<PaintEntry> out) {
            for (int i = 0; i < layers.length; i++) {
                String cat = layers[i];
                if (cat == null) continue;
                out.add(new PaintEntry(x, y, LAYERS[i], cat));
            }
        }
    }
}
