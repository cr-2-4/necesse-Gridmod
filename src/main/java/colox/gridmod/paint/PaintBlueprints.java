package colox.gridmod.paint;

import colox.gridmod.config.GridConfig;
import colox.gridmod.util.ConfigPaths;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/*
// ===========================================================================
// PURPOSE: Relative + Global blueprint IO.
// Phase 5 addition: saveSelectionAs(...) that saves from an arbitrary subset.
// SEARCH ANCHORS:
//   [BP] relative save/load
//   [BP] selection save
//   [BP] globals
// ===========================================================================
*/
public final class PaintBlueprints {
    // Relative blueprints folder
    private static final File DIR = ConfigPaths.blueprintsDir().toFile();
    // Global blueprints folder (absolute snapshots)
    private static final File GLOBAL_DIR = ConfigPaths.globalBlueprintsDir().toFile();
    // Legacy single-file anchor
    private static final String LEGACY_GLOBAL_NAME = "_global";

    private PaintBlueprints() {}

    private static void ensureDir()        { if (!DIR.exists())        DIR.mkdirs(); }
    private static void ensureGlobalDir()  { if (!GLOBAL_DIR.exists()) GLOBAL_DIR.mkdirs(); }

    // ---------- [BP] relative save/load --------------------------------------

    private static File fileFor(String name) {
        return new File(DIR, safeName(name) + ".gridpaint");
    }

    /** Save all current paint as relative (unchanged behavior). */
    public static void saveBlueprint(String name) {
        List<long[]> selection = SelectionState.getSelectedPoints();
        if (!selection.isEmpty()) {
            saveBlueprintFromSelection(name, selection);
            return;
        }
        saveEntireBlueprint(name);
    }

    private static void saveEntireBlueprint(String name) {
        ensureDir();
        File file = fileFor(name);

        try {
            List<PaintState.PaintEntry> pts = PaintState.iterateSnapshot();
            if (pts.isEmpty()) {
                return;
            }

            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
            for (PaintState.PaintEntry p : pts) {
                int x = p.x, y = p.y;
                if (x < minX) minX = x;
                if (y < minY) minY = y;
            }

            StringBuilder sb = new StringBuilder(pts.size() * 8);
            for (PaintState.PaintEntry p : pts) {
                int x = p.x - minX;
                int y = p.y - minY;
                sb.append(x).append(',').append(y).append(',').append(p.categoryId).append(';');
            }

            SaveData sd = new SaveData("paint_blueprint");
            sd.addSafeString("points", sb.toString());
            sd.addInt("normX", minX);
            sd.addInt("normY", minY);
            sd.saveScript(file);

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /** Load relative blueprint as dx,dy offsets. */
    public static List<colox.gridmod.paint.BlueprintPlacement.BlueprintTile> loadRelative(String name) {
        ensureDir();
        File file = fileFor(name);
        List<colox.gridmod.paint.BlueprintPlacement.BlueprintTile> rel = new ArrayList<>();
        if (!file.exists()) {
            System.err.println("[GridMod] Blueprint not found: " + file.getAbsolutePath());
            return rel;
        }
        try {
            LoadData ld = new LoadData(file);
            String points = ld.getSafeString("points", "");
            if (!points.isEmpty()) {
                String[] pairs = points.split(";");
                for (String pair : pairs) {
                    if (pair.isEmpty()) continue;
                    String[] xy = pair.split(",");
                    if (xy.length < 2) continue;
                    int dx = Integer.parseInt(xy[0].trim());
                    int dy = Integer.parseInt(xy[1].trim());
                    String cat = (xy.length >= 3) ? xy[2].trim() : GridConfig.getActivePaintCategory().id();
                    rel.add(new colox.gridmod.paint.BlueprintPlacement.BlueprintTile(dx, dy, cat));
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return rel;
    }

    public static String[] listBlueprints() {
        ensureDir();
        File[] files = DIR.listFiles((dir, n) -> n.endsWith(".gridpaint"));
        if (files == null) return new String[0];
        List<String> out = new ArrayList<>(files.length);
        for (File f : files) {
            String base = f.getName().substring(0, f.getName().length() - ".gridpaint".length());
            if (LEGACY_GLOBAL_NAME.equals(base)) continue; // hide legacy from relative list
            if (base.isEmpty()) {
                f.delete();
                continue;
            }
            out.add(base);
        }
        return out.toArray(new String[0]);
    }

    public static boolean exists(String name) {
        ensureDir();
        return fileFor(name).exists();
    }

    public static boolean createEmpty(String name) {
        ensureDir();
        File f = fileFor(name);
        if (f.exists()) return false;
        try {
            SaveData sd = new SaveData("paint_blueprint");
            sd.addSafeString("points", "");
            sd.addInt("normX", 0);
            sd.addInt("normY", 0);
            sd.saveScript(f);
            return true;
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }

    public static boolean deleteBlueprint(String name) {
        ensureDir();
        if (name == null || name.isBlank()) {
            return false;
        }
        File f = fileFor(name);
        if (!f.exists()) {
            return false;
        }
        boolean ok = f.delete();
        return ok;
    }

    public static boolean renameBlueprint(String oldName, String newName) {
        ensureDir();
        File src = fileFor(oldName);
        File dst = fileFor(newName);
        if (!src.exists()) {
            return false;
        }
        if (dst.exists()) {
            return false;
        }
        boolean ok;
        try {
            ok = src.renameTo(dst);
            if (!ok) {
                java.nio.file.Files.copy(src.toPath(), dst.toPath());
                ok = src.delete();
            }
        } catch (IOException e) {
            e.printStackTrace();
            ok = false;
        }
        return ok;
    }

    // ---------- [BP] selection save (Phase 5) --------------------------------

    /**
     * Saves a relative blueprint from an arbitrary subset of ABSOLUTE points.
     * Returns number of tiles written.
     */
    public static int saveSelectionAs(String name, Collection<long[]> absPoints) {
        ensureDir();
        File file = fileFor(name);
        if (absPoints == null || absPoints.isEmpty()) {
            return 0;
        }
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        for (long[] p : absPoints) {
            int x = (int)p[0], y = (int)p[1];
            if (x < minX) minX = x;
            if (y < minY) minY = y;
        }
        StringBuilder sb = new StringBuilder(absPoints.size() * 12);
        java.util.HashSet<Long> selected = new java.util.HashSet<>(absPoints.size());
        for (long[] p : absPoints) selected.add(key((int)p[0], (int)p[1]));
        int written = 0;
        for (PaintState.PaintEntry entry : PaintState.iterateSnapshot()) {
            long k = key(entry.x, entry.y);
            if (!selected.contains(k)) continue;
            int x = entry.x - minX;
            int y = entry.y - minY;
            sb.append(x).append(',').append(y).append(',').append(entry.categoryId).append(';');
            written++;
        }
        try {
            SaveData sd = new SaveData("paint_blueprint");
            sd.addSafeString("points", sb.toString());
            sd.addInt("normX", minX);
            sd.addInt("normY", minY);
            sd.saveScript(file);
            return written;
        } catch (Throwable t) {
            t.printStackTrace();
            return 0;
        }
    }

    // ---------- [BP] globals --------------------------------------------------

    private static File globalFileFor(String name) {
        return new File(GLOBAL_DIR, safeName(name) + ".gridpaint");
    }

    public static String[] listGlobalBlueprints() {
        ensureGlobalDir();
        File[] files = GLOBAL_DIR.listFiles((dir, n) -> n.endsWith(".gridpaint"));
        if (files == null) return new String[0];
        String[] names = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            String nm = files[i].getName();
            names[i] = nm.substring(0, nm.length() - ".gridpaint".length());
        }
        return names;
    }

    public static boolean globalExists(String name) {
        ensureGlobalDir();
        return globalFileFor(name).exists();
    }

    public static boolean createGlobalEmpty(String name) {
        ensureGlobalDir();
        File f = globalFileFor(name);
        if (f.exists()) return false;
        try {
            SaveData sd = new SaveData("paint_blueprint");
            sd.addSafeString("points", "");
            sd.addSafeString("type", "global");
            sd.addInt("count", 0);
            sd.saveScript(f);
            return true;
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }

    public static int saveGlobal(String name) {
        ensureGlobalDir();
        File f = globalFileFor(name);
        int count = 0;
        try {
            List<PaintState.PaintEntry> pts = PaintState.iterateSnapshot();
            StringBuilder sb = new StringBuilder(pts.size() * 14);
            for (PaintState.PaintEntry p : pts) {
                sb.append(p.x).append(',').append(p.y).append(',').append(p.categoryId).append(';');
            }
            SaveData sd = new SaveData("paint_blueprint");
            sd.addSafeString("points", sb.toString());
            sd.addSafeString("type", "global");
            sd.addInt("count", pts.size());
            sd.saveScript(f);
            count = pts.size();
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return count;
    }

    public static int loadGlobal(String name) {
        ensureGlobalDir();
        File f = globalFileFor(name);
        if (!f.exists()) {
            return 0;
        }
        int restored = 0;
        try {
            LoadData ld = new LoadData(f);
            String points = ld.getSafeString("points", "");
            PaintState.clear();
            if (!points.isEmpty()) {
                String[] pairs = points.split(";");
                for (String pair : pairs) {
                    if (pair.isEmpty()) continue;
                    String[] xy = pair.split(",");
                    if (xy.length < 2) continue;
                    int x = Integer.parseInt(xy[0].trim());
                    int y = Integer.parseInt(xy[1].trim());
                    String cat = (xy.length >= 3) ? xy[2].trim() : GridConfig.getActivePaintCategory().id();
                    PaintState.add(x, y, cat);
                    restored++;
                }
            }
            PaintState.saveIfDirty();
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return restored;
    }

    public static boolean deleteGlobal(String name) {
        ensureGlobalDir();
        File f = globalFileFor(name);
        if (!f.exists()) {
            return false;
        }
        boolean ok = f.delete();
        return ok;
    }

    public static boolean renameGlobal(String oldName, String newName) {
        ensureGlobalDir();
        File src = globalFileFor(oldName);
        File dst = globalFileFor(newName);
        if (!src.exists()) {
            return false;
        }
        if (dst.exists()) {
            return false;
        }
        boolean ok;
        try {
            ok = src.renameTo(dst);
            if (!ok) {
                java.nio.file.Files.copy(src.toPath(), dst.toPath());
                ok = src.delete();
            }
        } catch (IOException e) {
            e.printStackTrace();
            ok = false;
        }
        return ok;
    }

    // Legacy single-file helpers (kept)
    private static File legacyGlobalFile() { return new File(DIR, LEGACY_GLOBAL_NAME + ".gridpaint"); }
    public static boolean hasLegacyGlobal() { ensureDir(); return legacyGlobalFile().exists(); }
    public static void saveLegacyGlobal() {
        ensureDir();
        File file = legacyGlobalFile();
        try {
            List<PaintState.PaintEntry> pts = PaintState.iterateSnapshot();
            StringBuilder sb = new StringBuilder(pts.size() * 14);
            for (PaintState.PaintEntry p : pts) {
                sb.append(p.x).append(',').append(p.y).append(',').append(p.categoryId).append(';');
            }
            SaveData sd = new SaveData("paint_blueprint");
            sd.addSafeString("points", sb.toString());
            sd.addSafeString("type", "global");
            sd.addInt("count", pts.size());
            sd.saveScript(file);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
    public static int loadLegacyGlobal() {
        ensureDir();
        File file = legacyGlobalFile();
        if (!file.exists()) return 0;
        int restored = 0;
        try {
            LoadData ld = new LoadData(file);
            String points = ld.getSafeString("points", "");
            PaintState.clear();
            if (!points.isEmpty()) {
                String[] pairs = points.split(";");
                for (String pair : pairs) {
                    if (pair.isEmpty()) continue;
                    String[] xy = pair.split(",");
                    if (xy.length < 2) continue;
                    int x = Integer.parseInt(xy[0].trim());
                    int y = Integer.parseInt(xy[1].trim());
                    String catId = (xy.length >= 3) ? xy[2].trim() : GridConfig.getActivePaintCategory().id();
                    PaintState.add(x, y, catId);
                    restored++;
                }
            }
            PaintState.saveIfDirty();
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return restored;
    }

    static String safeName(String name) {
        return name.replaceAll("[^a-zA-Z0-9-_\\.]", "_");
    }

    private static void saveBlueprintFromSelection(String name, List<long[]> selection) {
        ensureDir();
        File file = fileFor(name);

        try {
            if (selection.isEmpty()) {
                return;
            }

            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
            for (long[] pt : selection) {
                int x = (int)pt[0];
                int y = (int)pt[1];
                if (x < minX) minX = x;
                if (y < minY) minY = y;
            }

            StringBuilder sb = new StringBuilder(selection.size() * 8);
            for (long[] pt : selection) {
                int tx = (int)pt[0];
                int ty = (int)pt[1];
                PaintState.PaintEntry entry = PaintState.getPaintEntry(tx, ty);
                if (entry == null) continue;
                int x = tx - minX;
                int y = ty - minY;
                sb.append(x).append(',').append(y).append(',').append(entry.categoryId).append(';');
            }

            SaveData sd = new SaveData("paint_blueprint");
            sd.addSafeString("points", sb.toString());
            sd.addInt("normX", minX);
            sd.addInt("normY", minY);
            sd.saveScript(file);

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static long key(int x, int y) {
        return ((long)x << 32) | (y & 0xffffffffL);
    }
}
