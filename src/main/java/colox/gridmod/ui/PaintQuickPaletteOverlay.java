package colox.gridmod.ui;

import java.awt.Rectangle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import colox.gridmod.config.GridConfig;
import colox.gridmod.paint.PaintCategory;
import necesse.gfx.Renderer;
import necesse.gfx.forms.Form;
import necesse.gfx.forms.components.FormCheckBox;
import necesse.gfx.forms.components.FormComponent;
import necesse.gfx.forms.components.FormContentBox;
import necesse.gfx.forms.components.FormCustomDraw;
import necesse.gfx.forms.components.FormLabel;
import necesse.gfx.forms.events.FormEventListener;
import necesse.gfx.forms.events.FormInputEvent;
import necesse.gfx.gameFont.FontOptions;

/**
 * Lightweight paint palette that lives alongside the game HUD so players can change
 * categories without opening the full Grid UI form.
 */
public final class PaintQuickPaletteOverlay {

    private static final IdentityHashMap<Object, PaletteForm> FORMS = new IdentityHashMap<>();

    private PaintQuickPaletteOverlay() {}

    public static void tick(boolean paintEnabled) {
        Object mgr = getActiveFormManager();
        if (!paintEnabled || mgr == null) {
            disposeAll();
            return;
        }

        // Clean up stale managers
        FORMS.entrySet().removeIf(entry -> {
            if (entry.getKey() == mgr) return false;
            detachForm(entry.getKey(), entry.getValue());
            return true;
        });

        PaletteForm form = FORMS.get(mgr);
        if (form == null) {
            form = new PaletteForm();
            if (!attachForm(mgr, form)) {
                form.dispose();
                return;
            }
            FORMS.put(mgr, form);
        }
        form.refreshState();
    }

    public static void disposeAll() {
        for (Map.Entry<Object, PaletteForm> entry : FORMS.entrySet()) {
            detachForm(entry.getKey(), entry.getValue());
        }
        FORMS.clear();
    }

    private static boolean attachForm(Object mgr, Form form) {
        if (invokeLogged(mgr, "addComponent", form)) {
            invokeLogged(form, "setHidden", false);
            return true;
        }
        Object list = getFieldVal(mgr, "components");
        if (list == null) list = callInstance(mgr, "getComponentList");
        if (list == null) return false;
        boolean added = tryAddToList(list, form);
        if (added) invokeLogged(form, "setHidden", false);
        return added;
    }

    private static void detachForm(Object mgr, Form form) {
        if (form == null) return;
        invokeLogged(mgr, "removeComponent", form);
        invokeLogged(form, "dispose");
    }

    // ------------------------------------------------------------
    // Palette form
    // ------------------------------------------------------------

    private static final class PaletteForm extends Form {
        private static final int WIDTH = 320;
        private static final int HEIGHT = 420;

        private final FormContentBox listBox;
        private final FormCheckBox hoverMaster;
        private final List<CategoryRow> rows = new ArrayList<>();
        private boolean ignoreEvents;

        PaletteForm() {
            super(WIDTH, HEIGHT);
            this.drawBase = true;
            this.drawEdge = true;
            this.setPosition(18, 120);

            addComponent(new FormLabel("Paint types", new FontOptions(16), FormLabel.ALIGN_LEFT, 12, 6));
            listBox = addComponent(new FormContentBox(0, 26, getWidth(), getHeight() - 26));

            hoverMaster = addTo(listBox, new FormCheckBox("Show tile type on hover", 12, 6, GridConfig.isHoverLabelsEnabled()));
            hoverMaster.onClicked(masterListener());

            int y = 32;
            for (PaintCategory category : PaintCategory.values()) {
                CategoryRow row = new CategoryRow(category, y);
                rows.add(row);
                y += 26;
            }
            int contentHeight = Math.max(y + 12, listBox.getHeight());
            listBox.setContentBox(new Rectangle(0, 0, listBox.getWidth(), contentHeight));
            refreshState();
        }

        private FormEventListener<FormInputEvent<FormCheckBox>> masterListener() {
            return e -> {
                if (ignoreEvents) return;
                GridConfig.setHoverLabelsEnabled(hoverMaster.checked);
                refreshState();
            };
        }

        void refreshState() {
            ignoreEvents = true;
            hoverMaster.checked = GridConfig.isHoverLabelsEnabled();
            PaintCategory active = GridConfig.getActivePaintCategory();
            for (CategoryRow row : rows) {
                row.sync(active, hoverMaster.checked);
            }
            ignoreEvents = false;
        }

        private void handleSelect(CategoryRow row) {
            if (ignoreEvents) {
                row.select.checked = row.category == GridConfig.getActivePaintCategory();
                return;
            }
            row.select.checked = true;
            GridConfig.setActivePaintCategory(row.category);
            refreshState();
        }

        private void handleHoverToggle(CategoryRow row) {
            if (ignoreEvents) {
                row.hoverToggle.checked = GridConfig.isHoverCategoryAllowed(row.category);
                return;
            }
            GridConfig.setHoverCategoryAllowed(row.category, row.hoverToggle.checked);
        }

        private final class CategoryRow {
            final PaintCategory category;
            final FormCheckBox select;
            final FormCheckBox hoverToggle;

            CategoryRow(PaintCategory category, int y) {
                this.category = category;
                addTo(listBox, new ColorSwatch(category, 8, y + 4, 18, 18));

                int labelWidth = getWidth() - 140;
                this.select = addTo(listBox, new FormCheckBox(category.label(), 36, y, labelWidth, false));
                this.select.onClicked((FormEventListener<FormInputEvent<FormCheckBox>>) e -> handleSelect(this));

                int hoverX = getWidth() - 94;
                this.hoverToggle = addTo(listBox, new FormCheckBox("hover", hoverX, y, 70, false));
                this.hoverToggle.onClicked((FormEventListener<FormInputEvent<FormCheckBox>>) e -> handleHoverToggle(this));
            }

            void sync(PaintCategory active, boolean hoverMasterEnabled) {
                this.select.checked = (category == active);
                this.hoverToggle.checked = GridConfig.isHoverCategoryAllowed(category);
                this.hoverToggle.setActive(hoverMasterEnabled);
            }
        }

        private <T extends FormComponent> T addTo(FormContentBox box, T comp) {
            box.addComponent(comp);
            return comp;
        }

        private final class ColorSwatch extends FormCustomDraw {
            private final PaintCategory category;

            ColorSwatch(PaintCategory category, int x, int y, int w, int h) {
                super(x, y, w, h);
                this.category = category;
                this.canBePutOnTopByClick = false;
            }

            @Override
            public boolean shouldUseMouseEvents() {
                return false;
            }

            @Override
            public void draw(necesse.engine.gameLoop.tickManager.TickManager tickManager,
                             necesse.entity.mobs.PlayerMob player,
                             java.awt.Rectangle renderBox) {
                GridConfig.PaintColor color = GridConfig.getPaintColor(category);
                Renderer.initQuadDraw(this.width, this.height)
                        .color(color.r, color.g, color.b, 1f)
                        .draw(getX(), getY());
                Renderer.initQuadDraw(this.width, 1).color(0f, 0f, 0f, 0.6f).draw(getX(), getY());
                Renderer.initQuadDraw(this.width, 1).color(0f, 0f, 0f, 0.6f).draw(getX(), getY() + this.height - 1);
                Renderer.initQuadDraw(1, this.height).color(0f, 0f, 0f, 0.6f).draw(getX(), getY());
                Renderer.initQuadDraw(1, this.height).color(0f, 0f, 0f, 0.6f).draw(getX() + this.width - 1, getY());
            }
        }
    }

    // ------------------------------------------------------------
    // Reflection helpers (trimmed copy of GridUI helpers)
    // ------------------------------------------------------------

    private static Object getActiveFormManager() {
        Object state = callStatic("necesse.engine.GlobalData", "getCurrentState");
        if (state == null) return null;
        return callInstance(state, "getFormManager");
    }

    private static boolean tryAddToList(Object list, Object form) {
        String[] methods = {"addComponent", "add", "addLast", "push", "offer"};
        for (String name : methods) {
            if (invokeLogged(list, name, form)) return true;
        }
        return false;
    }

    private static Object callStatic(String cls, String method) {
        try {
            Class<?> c = Class.forName(cls);
            Method m = findMethod(c, method);
            if (m == null) return null;
            m.setAccessible(true);
            return m.invoke(null);
        } catch (Throwable t) {
            System.out.println("[PaintPalette] " + cls + "." + method + "() FAILED: " + t);
            return null;
        }
    }

    private static Object callInstance(Object obj, String method) {
        if (obj == null) return null;
        try {
            Method m = findMethod(obj.getClass(), method);
            if (m == null) return null;
            m.setAccessible(true);
            return m.invoke(obj);
        } catch (Throwable t) {
            System.out.println("[PaintPalette] " + obj.getClass().getName() + "." + method + "() FAILED: " + t);
            return null;
        }
    }

    private static Object getFieldVal(Object obj, String field) {
        if (obj == null) return null;
        try {
            Field f;
            try { f = obj.getClass().getField(field); }
            catch (NoSuchFieldException e) { f = obj.getClass().getDeclaredField(field); }
            f.setAccessible(true);
            return f.get(obj);
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean invokeLogged(Object obj, String method, Object... args) {
        if (obj == null) return false;
        try {
            Method chosen = null;
            outer:
            for (Method m : obj.getClass().getMethods()) {
                if (!m.getName().equals(method)) continue;
                Class<?>[] params = m.getParameterTypes();
                if (params.length != args.length) continue;
                for (int i = 0; i < params.length; i++) {
                    if (!paramCompatible(params[i], args[i])) continue outer;
                }
                chosen = m;
                break;
            }
            if (chosen == null) return false;
            chosen.setAccessible(true);
            chosen.invoke(obj, args);
            return true;
        } catch (Throwable t) {
            System.out.println("[PaintPalette] " + obj.getClass().getName() + "." + method + "(�?�) FAILED: " + t);
            return false;
        }
    }

    private static boolean paramCompatible(Class<?> need, Object have) {
        if (have == null) return !need.isPrimitive();
        Class<?> hc = have.getClass();
        if (need.isAssignableFrom(hc)) return true;
        if (need.isPrimitive()) {
            Class<?> boxed = PRIMITIVE_MAP.get(need);
            return boxed != null && boxed.isAssignableFrom(hc);
        }
        return false;
    }

    private static Method findMethod(Class<?> c, String name, Class<?>... params) {
        try {
            return c.getMethod(name, params);
        } catch (NoSuchMethodException ignored) {}
        for (Method m : c.getMethods()) {
            if (!m.getName().equals(name)) continue;
            if (params.length != m.getParameterCount()) continue;
            return m;
        }
        return null;
    }

    private static final Map<Class<?>, Class<?>> PRIMITIVE_MAP = new HashMap<>();
    static {
        PRIMITIVE_MAP.put(boolean.class, Boolean.class);
        PRIMITIVE_MAP.put(byte.class, Byte.class);
        PRIMITIVE_MAP.put(short.class, Short.class);
        PRIMITIVE_MAP.put(int.class, Integer.class);
        PRIMITIVE_MAP.put(long.class, Long.class);
        PRIMITIVE_MAP.put(float.class, Float.class);
        PRIMITIVE_MAP.put(double.class, Double.class);
        PRIMITIVE_MAP.put(char.class, Character.class);
    }
}
