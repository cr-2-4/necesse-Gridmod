package colox.gridmod.paint;

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
        ensureDir();
        File file = fileFor(name);

        try {
            List<long[]> pts = PaintState.iterateSnapshot();
            if (pts.isEmpty()) {
                System.out.println("[GridMod] No painted tiles to save.");
                return;
            }

            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
            for (long[] p : pts) {
                int x = (int)p[0], y = (int)p[1];
                if (x < minX) minX = x;
                if (y < minY) minY = y;
            }

            StringBuilder sb = new StringBuilder(pts.size() * 8);
            for (long[] p : pts) {
                int x = (int)p[0] - minX;
                int y = (int)p[1] - minY;
                sb.append(x).append(',').append(y).append(';');
            }

            SaveData sd = new SaveData("paint_blueprint");
            sd.addSafeString("points", sb.toString());
            sd.addInt("normX", minX);
            sd.addInt("normY", minY);
            sd.saveScript(file);

            System.out.println("[GridMod] Saved blueprint: " + file.getAbsolutePath());
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /** Load relative blueprint as dx,dy offsets. */
    public static List<int[]> loadRelative(String name) {
        ensureDir();
        File file = fileFor(name);
        List<int[]> rel = new ArrayList<>();
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
                    if (xy.length != 2) continue;
                    int dx = Integer.parseInt(xy[0].trim());
                    int dy = Integer.parseInt(xy[1].trim());
                    rel.add(new int[]{dx, dy});
                }
            }
            System.out.println("[GridMod] Loaded blueprint '" + name + "' tiles=" + rel.size());
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
            System.out.println("[GridMod] Created empty blueprint: " + f.getAbsolutePath());
            return true;
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }

    public static boolean deleteBlueprint(String name) {
        ensureDir();
        File f = fileFor(name);
        if (!f.exists()) {
            System.out.println("[GridMod] deleteBlueprint: not found: " + f.getAbsolutePath());
            return false;
        }
        boolean ok = f.delete();
        System.out.println("[GridMod] deleteBlueprint '" + name + "': " + ok);
        return ok;
    }

    public static boolean renameBlueprint(String oldName, String newName) {
        ensureDir();
        File src = fileFor(oldName);
        File dst = fileFor(newName);
        if (!src.exists()) {
            System.out.println("[GridMod] renameBlueprint: source missing: " + src.getAbsolutePath());
            return false;
        }
        if (dst.exists()) {
            System.out.println("[GridMod] renameBlueprint: target exists: " + dst.getAbsolutePath());
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
        System.out.println("[GridMod] renameBlueprint '" + oldName + "' -> '" + newName + "': " + ok);
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
            System.out.println("[GridMod] saveSelectionAs: empty selection.");
            return 0;
        }
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        for (long[] p : absPoints) {
            int x = (int)p[0], y = (int)p[1];
            if (x < minX) minX = x;
            if (y < minY) minY = y;
        }
        StringBuilder sb = new StringBuilder(absPoints.size() * 8);
        for (long[] p : absPoints) {
            int x = (int)p[0] - minX;
            int y = (int)p[1] - minY;
            sb.append(x).append(',').append(y).append(';');
        }
        try {
            SaveData sd = new SaveData("paint_blueprint");
            sd.addSafeString("points", sb.toString());
            sd.addInt("normX", minX);
            sd.addInt("normY", minY);
            sd.saveScript(file);
            System.out.println("[GridMod] Saved selection '" + name + "' tiles=" + absPoints.size());
            return absPoints.size();
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
            System.out.println("[GridMod] Created empty GLOBAL blueprint: " + f.getAbsolutePath());
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
            List<long[]> pts = PaintState.iterateSnapshot();
            StringBuilder sb = new StringBuilder(pts.size() * 10);
            for (long[] p : pts) {
                int x = (int)p[0];
                int y = (int)p[1];
                sb.append(x).append(',').append(y).append(';');
            }
            SaveData sd = new SaveData("paint_blueprint");
            sd.addSafeString("points", sb.toString());
            sd.addSafeString("type", "global");
            sd.addInt("count", pts.size());
            sd.saveScript(f);
            count = pts.size();
            System.out.println("[GridMod] Global saved '" + name + "' tiles=" + count + " path=" + f.getAbsolutePath());
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return count;
    }

    public static int loadGlobal(String name) {
        ensureGlobalDir();
        File f = globalFileFor(name);
        if (!f.exists()) {
            System.out.println("[GridMod] loadGlobal not found: " + f.getAbsolutePath());
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
                    if (xy.length != 2) continue;
                    int x = Integer.parseInt(xy[0].trim());
                    int y = Integer.parseInt(xy[1].trim());
                    PaintState.add(x, y);
                    restored++;
                }
            }
            PaintState.saveIfDirty();
            System.out.println("[GridMod] Global loaded '" + name + "' tiles=" + restored);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return restored;
    }

    public static boolean deleteGlobal(String name) {
        ensureGlobalDir();
        File f = globalFileFor(name);
        if (!f.exists()) {
            System.out.println("[GridMod] deleteGlobal: not found: " + f.getAbsolutePath());
            return false;
        }
        boolean ok = f.delete();
        System.out.println("[GridMod] deleteGlobal '" + name + "': " + ok);
        return ok;
    }

    public static boolean renameGlobal(String oldName, String newName) {
        ensureGlobalDir();
        File src = globalFileFor(oldName);
        File dst = globalFileFor(newName);
        if (!src.exists()) {
            System.out.println("[GridMod] renameGlobal: source missing: " + src.getAbsolutePath());
            return false;
        }
        if (dst.exists()) {
            System.out.println("[GridMod] renameGlobal: target exists: " + dst.getAbsolutePath());
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
        System.out.println("[GridMod] renameGlobal '" + oldName + "' -> '" + newName + "': " + ok);
        return ok;
    }

    // Legacy single-file helpers (kept)
    private static File legacyGlobalFile() { return new File(DIR, LEGACY_GLOBAL_NAME + ".gridpaint"); }
    public static boolean hasLegacyGlobal() { ensureDir(); return legacyGlobalFile().exists(); }
    public static void saveLegacyGlobal() {
        ensureDir();
        File file = legacyGlobalFile();
        try {
            List<long[]> pts = PaintState.iterateSnapshot();
            StringBuilder sb = new StringBuilder(pts.size() * 10);
            for (long[] p : pts) {
                int x = (int)p[0];
                int y = (int)p[1];
                sb.append(x).append(',').append(y).append(';');
            }
            SaveData sd = new SaveData("paint_blueprint");
            sd.addSafeString("points", sb.toString());
            sd.addSafeString("type", "global");
            sd.addInt("count", pts.size());
            sd.saveScript(file);
            System.out.println("[GridMod] Legacy _global saved tiles=" + pts.size());
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
                    if (xy.length != 2) continue;
                    int x = Integer.parseInt(xy[0].trim());
                    int y = Integer.parseInt(xy[1].trim());
                    PaintState.add(x, y);
                    restored++;
                }
            }
            PaintState.saveIfDirty();
            System.out.println("[GridMod] Legacy _global restored tiles=" + restored);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return restored;
    }

    static String safeName(String name) {
        return name.replaceAll("[^a-zA-Z0-9-_\\.]", "_");
    }
}
