package colox.gridmod.paint;

import java.util.*;
import java.awt.Point;

import colox.gridmod.config.GridConfig;

/**
 * SelectionState
 * - Modes: NONE / RECT / EDGE (stroke) / EDGE_FILL (stroke + polygon fill) / LASSO_FILL (polygon)
 * - EDGE: selects tiles crossed by the stroke.
 * - EDGE_FILL: behaves like EDGE during drag, but on release also fills the polygon traced by the stroke path.
 * - LASSO_FILL: polygon lasso using tile-center point-in-polygon.
 *
 * Search anchors:
 *   // [SEL] public API
 *   // [SEL] compute selection
 *   // [SEL] helpers
 */
public final class SelectionState {

    public enum Mode { NONE, RECT, EDGE, EDGE_FILL, LASSO_FILL }

    // -------- Singleton --------
    private static Mode mode = Mode.NONE;

    // Drag state
    private static boolean dragging = false;
    private static int dragStartTx, dragStartTy;
    private static int dragEndTx, dragEndTy;

    // Stroke tiles for EDGE / EDGE_FILL
    private static final LinkedHashSet<Long> lassoStroke = new LinkedHashSet<>();
    private static int lastDragTx, lastDragTy;

    // Polygon path tiles for EDGE_FILL / LASSO_FILL (store tile centers along the path)
    private static final ArrayList<Point> lassoPath = new ArrayList<>();

    // Computed selection
    private static final LinkedHashSet<Long> selected = new LinkedHashSet<>();
    private static int lastCount = 0;

    private SelectionState() {}

    // ==========================================================
    // [SEL] public API
    // ==========================================================

    public static Mode getMode() { return mode; }

    /** Sets the mode; also clears any current selection. */
    public static void setMode(Mode m) {
        if (m == null) m = Mode.NONE;
        mode = m;
        clear();
    }

    public static boolean isActive() { return mode != Mode.NONE; }

    public static void beginDrag(int tx, int ty) {
        if (mode == Mode.NONE) return;
        dragging = true;
        dragStartTx = tx;
        dragStartTy = ty;
        dragEndTx = tx;
        dragEndTy = ty;

        lassoStroke.clear();
        lassoPath.clear();
        lastDragTx = tx;
        lastDragTy = ty;

        switch (mode) {
            case EDGE:
                lassoStroke.add(key(tx, ty));
                break;
            case EDGE_FILL:
                lassoStroke.add(key(tx, ty));
                lassoPath.add(new Point(tx, ty));
                break;
            case LASSO_FILL:
                lassoPath.add(new Point(tx, ty));
                break;
            default:
                break;
        }
    }

    public static void updateDrag(int tx, int ty) {
        if (!dragging || mode == Mode.NONE) return;
        dragEndTx = tx;
        dragEndTy = ty;

        switch (mode) {
            case EDGE:
                addLineTiles(lastDragTx, lastDragTy, tx, ty, lassoStroke);
                break;
            case EDGE_FILL: { // <- braces give this case its own scope
                addLineTiles(lastDragTx, lastDragTy, tx, ty, lassoStroke);
                // also keep polygon path of tile centers
                Point lastPt = lassoPath.isEmpty() ? null : lassoPath.get(lassoPath.size() - 1);
                if (lastPt == null || lastPt.x != tx || lastPt.y != ty) {
                    lassoPath.add(new Point(tx, ty));
                }
                break;
            }
            case LASSO_FILL: {
                Point last = lassoPath.isEmpty() ? null : lassoPath.get(lassoPath.size() - 1);
                if (last == null || last.x != tx || last.y != ty) {
                    lassoPath.add(new Point(tx, ty));
                }
                break;
            }
            default:
                break;
        }
        lastDragTx = tx;
        lastDragTy = ty;
    }

    public static void endDrag() {
        if (!dragging) return;
        dragging = false;
        computeSelection();
    }

    /** Clears current selection and any in-progress drag. */
    public static void clear() {
        dragging = false;
        lassoStroke.clear();
        lassoPath.clear();
        selected.clear();
        lastCount = 0;
        notifyChange();
    }

    public static boolean isDragging() { return dragging; }

    public static int getDragStartTx() { return dragStartTx; }
    public static int getDragStartTy() { return dragStartTy; }
    public static int getDragEndTx()   { return dragEndTx; }
    public static int getDragEndTy()   { return dragEndTy; }

    /** Live hover stroke (EDGE/EDGE_FILL). */
    public static List<long[]> getHoverStrokePoints() {
        ArrayList<long[]> out = new ArrayList<>(lassoStroke.size());
        for (long k : lassoStroke) out.add(new long[]{ (int)(k >> 32), (int)k });
        return out;
    }

    /** Live lasso polygon path (EDGE_FILL/LASSO_FILL). */
    public static List<Point> getLassoPath() {
        return Collections.unmodifiableList(lassoPath);
    }

    public static int getSelectedCount() { return lastCount; }

    /** Snapshot of selected absolute tiles as long[] {x,y}. */
    public static List<long[]> getSelectedPoints() {
        ArrayList<long[]> out = new ArrayList<>(selected.size());
        for (long k : selected) out.add(new long[]{ (int)(k >> 32), (int)k });
        return out;
    }

    /** True if a given tile is currently selected. */
    public static boolean isTileSelected(int tx, int ty) { return selected.contains(key(tx, ty)); }

    public static void refreshSelection() {
        if (!isActive()) return;
        computeSelection();
    }

    // ==========================================================
    // [SEL] compute selection
    // ==========================================================

    private static void computeSelection() {
        selected.clear();

        // Build quick lookup of painted tiles
        HashSet<Long> painted = new HashSet<>();
        PaintLayerFilter filter = GridConfig.getPaintLayerFilter();
        for (PaintState.PaintEntry p : PaintState.iterateSnapshot()) {
            if (!filter.matches(p.layer)) continue;
            painted.add(key(p.x, p.y));
        }
        if (painted.isEmpty()) {
            lastCount = 0;
            notifyChange();
            return;
        }

        switch (mode) {
            case RECT: {
                int x0 = Math.min(dragStartTx, dragEndTx);
                int y0 = Math.min(dragStartTy, dragEndTy);
                int x1 = Math.max(dragStartTx, dragEndTx);
                int y1 = Math.max(dragStartTy, dragEndTy);
                for (int ty = y0; ty <= y1; ty++) {
                    for (int tx = x0; tx <= x1; tx++) {
                        long k = key(tx, ty);
                        if (painted.contains(k)) selected.add(k);
                    }
                }
                break;
            }
            case EDGE: {
                for (long k : lassoStroke) {
                    if (painted.contains(k)) selected.add(k);
                }
                break;
            }
            case EDGE_FILL: {
                // 1) edge hits
                for (long k : lassoStroke) {
                    if (painted.contains(k)) selected.add(k);
                }
                // 2) polygon fill of the stroke path
                if (lassoPath.size() >= 3) {
                    List<Point> poly = closedCopy(lassoPath);
                    for (long k : painted) {
                        int tx = (int)(k >> 32);
                        int ty = (int)k;
                        if (pointInPolygon(tx + 0.5, ty + 0.5, poly)) selected.add(k);
                    }
                }
                break;
            }
            case LASSO_FILL: {
                if (lassoPath.size() >= 3) {
                    List<Point> poly = closedCopy(lassoPath);
                    for (long k : painted) {
                        int tx = (int)(k >> 32);
                        int ty = (int)k;
                        if (pointInPolygon(tx + 0.5, ty + 0.5, poly)) selected.add(k);
                    }
                }
                break;
            }
            default: break;
        }

        lastCount = selected.size();
        notifyChange();
    }

    private static Runnable changeListener = () -> {};

    public static void setChangeListener(Runnable listener) {
        changeListener = (listener == null) ? () -> {} : listener;
    }

    private static void notifyChange() {
        try { changeListener.run(); } catch (Throwable ignored) {}
    }

    // ==========================================================
    // [SEL] helpers
    // ==========================================================

    private static List<Point> closedCopy(List<Point> path) {
        if (path.isEmpty()) return Collections.emptyList();
        Point f = path.get(0);
        Point l = path.get(path.size() - 1);
        if (f.x == l.x && f.y == l.y) return path;
        ArrayList<Point> c = new ArrayList<>(path);
        c.add(new Point(f.x, f.y));
        return c;
    }

    private static long key(int x, int y) { return ((long)x << 32) | (y & 0xffffffffL); }

    /** Adds all grid tiles touched by a straight line from (x0,y0) to (x1,y1). */
    private static void addLineTiles(int x0, int y0, int x1, int y1, LinkedHashSet<Long> out) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        int x = x0, y = y0;
        out.add(key(x, y));
        while (x != x1 || y != y1) {
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x += sx; }
            if (e2 <  dx) { err += dx; y += sy; }
            out.add(key(x, y));
        }
    }

    private static boolean pointInPolygon(double x, double y, List<Point> poly) {
        // Standard ray-casting algorithm on tile centers
        boolean inside = false;
        int n = poly.size();
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double xi = poly.get(i).x + 0.5;
            double yi = poly.get(i).y + 0.5;
            double xj = poly.get(j).x + 0.5;
            double yj = poly.get(j).y + 0.5;

            boolean intersect = ((yi > y) != (yj > y))
                    && (x < (xj - xi) * (y - yi) / (yj - yi + 1e-7) + xi);
            if (intersect) inside = !inside;
        }
        return inside;
    }
}
