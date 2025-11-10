package colox.gridmod.paint;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import colox.gridmod.util.ConfigPaths;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;

public final class PaintState {
    public static boolean enabled = false;
    private static int brush = 1;
    public static float a = 0.25f, r = 0.2f, g = 0.8f, b = 1.0f;

    private static final Set<Long> painted = new HashSet<>();
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

    public static void add(int tx, int ty)    { if (painted.add(key(tx,ty)))   dirty = true; }
    public static void remove(int tx, int ty) { if (painted.remove(key(tx,ty))) dirty = true; }

    public static List<long[]> iterateSnapshot() {
        List<long[]> out = new ArrayList<>(painted.size());
        for (long k : painted) out.add(new long[]{ (int)(k >> 32), (int)k });
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
                    if (xy.length != 2) continue;
                    int x = Integer.parseInt(xy[0].trim());
                    int y = Integer.parseInt(xy[1].trim());
                    painted.add(key(x,y));
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

            StringBuilder sb = new StringBuilder(painted.size() * 10);
            for (long k : painted) {
                int x = (int)(k >> 32);
                int y = (int) k;
                sb.append(x).append(',').append(y).append(';');
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
}
