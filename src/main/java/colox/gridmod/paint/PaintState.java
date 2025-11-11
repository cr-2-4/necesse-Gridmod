package colox.gridmod.paint;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import colox.gridmod.util.ConfigPaths;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;

public final class PaintState {
    public static boolean enabled = false;
    private static int brush = 1;
    public static float a = 0.25f, r = 0.2f, g = 0.8f, b = 1.0f;

    private static final Map<Long, String> painted = new HashMap<>();
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
        String cat = normalizeCategory(categoryId);
        long k = key(tx, ty);
        String prev = painted.put(k, cat);
        if (prev == null || !prev.equals(cat)) dirty = true;
    }

    @Deprecated
    public static void add(int tx, int ty) {
        add(tx, ty, colox.gridmod.paint.PaintCategory.defaultCategory().id());
    }

    public static void remove(int tx, int ty) {
        if (painted.remove(key(tx,ty)) != null) dirty = true;
    }

    public static String getCategory(int tx, int ty) {
        String cat = painted.get(key(tx, ty));
        return (cat == null) ? colox.gridmod.paint.PaintCategory.defaultCategory().id() : cat;
    }

    public static List<PaintEntry> iterateSnapshot() {
        List<PaintEntry> out = new ArrayList<>(painted.size());
        for (Map.Entry<Long, String> entry : painted.entrySet()) {
            long k = entry.getKey();
            int x = (int)(k >> 32);
            int y = (int)k;
            out.add(new PaintEntry(x, y, entry.getValue()));
        }
        return out;
    }

    public static PaintEntry getPaintEntry(int tx, int ty) {
        String cat = painted.get(key(tx, ty));
        if (cat == null) return null;
        return new PaintEntry(tx, ty, cat);
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
                    painted.put(key(x,y), normalizeCategory(cat));
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

            StringBuilder sb = new StringBuilder(painted.size() * 16);
            for (Map.Entry<Long, String> entry : painted.entrySet()) {
                long k = entry.getKey();
                int x = (int)(k >> 32);
                int y = (int) k;
                sb.append(x).append(',').append(y).append(',').append(entry.getValue()).append(';');
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

    private static String normalizeCategory(String categoryId) {
        colox.gridmod.paint.PaintCategory cat = colox.gridmod.paint.PaintCategory.byId(categoryId);
        return cat.id();
    }

    public static final class PaintEntry {
        public final int x;
        public final int y;
        public final String categoryId;
        public PaintEntry(int x, int y, String categoryId) {
            this.x = x;
            this.y = y;
            this.categoryId = categoryId == null ? colox.gridmod.paint.PaintCategory.defaultCategory().id() : categoryId;
        }
    }
}
