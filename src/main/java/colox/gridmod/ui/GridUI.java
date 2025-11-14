package colox.gridmod.ui;

import java.lang.reflect.*;
import java.util.IdentityHashMap;
import java.util.Map;

/* ... header comment unchanged ... */

public final class GridUI {

    // *** One GridUIForm per live FormManager (menu vs game)
    private static final Map<Object, GridUIForm> FORMS_BY_MANAGER = new IdentityHashMap<>(); // ***

    private GridUI() {}

    // ---------------------- NEW: public visibility helper ----------------------
    /** @return true if the Grid UI form for the current state exists and is visible. */
    public static boolean isOpen() {
        Object mgr = getActiveFormManager();
        if (mgr == null) return false;
        GridUIForm form = FORMS_BY_MANAGER.get(mgr);
        if (form == null) return false;
        Boolean hidden = (Boolean) callInstance(form, "isHidden");
        // If we cannot reflect isHidden(), default to "open" since form exists.
        return hidden == null ? true : !hidden;
    }
    // ---------------------------------------------------------------------------

    public static void ensureBuiltAndShow() {
        Object mgr = getActiveFormManager();
        if (mgr == null) {
            return;
        }

        GridUIForm form = FORMS_BY_MANAGER.get(mgr);
        if (form == null) {
            form = new GridUIForm();
            FORMS_BY_MANAGER.put(mgr, form);

            if (!scheduleAdd(mgr, form)) {
                if (!invokeLogged(mgr, "addComponent", form)) {
                    Object list = getFieldVal(mgr, "components");
                    if (list == null) list = callInstance(mgr, "getComponentList");
                    if (list == null || !tryAddToList(list, form)) {
                        return;
                    }
                }
            }
        }

        centerForm(form);
        invokeLogged(form, "setHidden", false);
        invokeLogged(mgr, "setCurrent", form);
        invokeLogged(form, "makeCurrent");
    }

    public static void toggle() {
        Object mgr = getActiveFormManager();
        if (mgr == null) return;
        GridUIForm form = FORMS_BY_MANAGER.get(mgr);
        if (form == null) {
            ensureBuiltAndShow();
            return;
        }
        Boolean hidden = (Boolean) callInstance(form, "isHidden");
        boolean next = hidden != null ? !hidden : false;
        invokeLogged(form, "setHidden", next);
        if (!next) {
            centerForm(form);
            invokeLogged(mgr, "setCurrent", form);
            invokeLogged(form, "makeCurrent");
        }
    }

    public static void closeInCurrentState() {
        Object mgr = getActiveFormManager();
        if (mgr == null) return;
        GridUIForm form = FORMS_BY_MANAGER.remove(mgr);
        if (form == null) return;

        if (!invokeLogged(mgr, "removeComponent", form)) {
            if (!invokeLogged(mgr, "close", form)) {
                invokeLogged(mgr, "remove", form);
            }
        }
        invokeLogged(form, "dispose");
    }

    public static void tryClose(GridUIForm form) {
        Object mgr = getActiveFormManager();
        if (mgr == null) return;
        GridUIForm mapped = FORMS_BY_MANAGER.get(mgr);
        if (mapped != form) {
            invokeLogged(mgr, "removeComponent", form);
            invokeLogged(form, "dispose");
            return;
        }
        closeInCurrentState();
    }

    // ================================ INTERNALS =============================

    private static Object getActiveFormManager() {
        Object state = callStatic("necesse.engine.GlobalData", "getCurrentState");
        if (state == null) return null;
        return callInstance(state, "getFormManager");
    }

    private static boolean scheduleAdd(Object mgr, Object form) {
        try {
            Method setTimeout = findMethod(mgr.getClass(), "setTimeout", Runnable.class, long.class);
            if (setTimeout == null) {
                return false;
            }
            final Object managerRef = mgr;
            final Object formRef = form;
            Runnable r = () -> {
                if (!invokeLogged(managerRef, "addComponent", formRef)) {
                    Object list = getFieldVal(managerRef, "components");
                    if (list == null) list = callInstance(managerRef, "getComponentList");
                    if (list == null || !tryAddToList(list, formRef)) {
                        return;
                    }
                }
                centerForm(formRef);
                invokeLogged(formRef, "setHidden", false);
                invokeLogged(managerRef, "setCurrent", formRef);
                invokeLogged(formRef, "makeCurrent");
            };
            setTimeout.setAccessible(true);
            setTimeout.invoke(mgr, r, 0L);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static void centerForm(Object form) {
        try {
            Method setPosMiddle = findMethod(form.getClass(), "setPosMiddle", int.class, int.class);
            int w = 640, h = 360;
            Object window = callStatic("necesse.engine.window.WindowManager", "getWindow");
            if (window != null) {
                Method getHudWidth  = findMethod(window.getClass(), "getHudWidth");
                Method getHudHeight = findMethod(window.getClass(), "getHudHeight");
                if (getHudWidth != null)  w = (Integer) getHudWidth.invoke(window);
                if (getHudHeight != null) h = (Integer) getHudHeight.invoke(window);
            }
            if (setPosMiddle != null) {
                setPosMiddle.setAccessible(true);
                setPosMiddle.invoke(form, w / 2, h / 2);
            }
        } catch (Throwable t) {
        }
    }

    private static boolean tryAddToList(Object list, Object form) {
        String[] m = { "addComponent", "add", "addLast", "push", "offer" };
        for (String name : m) if (invokeLogged(list, name, form)) return true;
        return false;
    }

    // ============================= REFLECTION HELPERS =======================

    private static final Map<Class<?>, Class<?>> PRIMITIVE_MAP = Map.of(
        boolean.class, Boolean.class,
        byte.class,    Byte.class,
        short.class,   Short.class,
        int.class,     Integer.class,
        long.class,    Long.class,
        float.class,   Float.class,
        double.class,  Double.class,
        char.class,    Character.class
    );

    private static boolean paramCompatible(Class<?> need, Object have) {
        if (have == null) return !need.isPrimitive();
        Class<?> hc = have.getClass();
        if (need.isAssignableFrom(hc)) return true;
        Class<?> boxed = need.isPrimitive() ? PRIMITIVE_MAP.get(need) : null;
        return boxed != null && boxed.isAssignableFrom(hc);
    }

    private static Method findMethod(Class<?> c, String name, Class<?>... wanted) {
        try {
            Method m = c.getMethod(name, wanted);
            if (m != null) return m;
        } catch (NoSuchMethodException ignored) {}
        outer:
        for (Method m : c.getMethods()) {
            if (!m.getName().equals(name)) continue;
            Class<?>[] p = m.getParameterTypes();
            if (p.length != wanted.length) continue;
            for (int i = 0; i < p.length; i++) {
                if (p[i].equals(wanted[i])) continue;
                if (p[i].isPrimitive() && PRIMITIVE_MAP.get(p[i]) == wanted[i]) continue;
                if (!p[i].isPrimitive() && wanted[i].isPrimitive() && PRIMITIVE_MAP.get(wanted[i]) == p[i]) continue;
                if (!p[i].isAssignableFrom(wanted[i])) continue outer;
            }
            return m;
        }
        return null;
    }

    private static Object callStatic(String cls, String method) {
        try {
            Class<?> c = Class.forName(cls);
            Method m = findMethod(c, method);
            m.setAccessible(true);
            return m.invoke(null);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Object callInstance(Object obj, String method) {
        if (obj == null) return null;
        try {
            Method m = findMethod(obj.getClass(), method);
            m.setAccessible(true);
            return m.invoke(obj);
        } catch (Throwable t) {
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
                Class<?>[] p = m.getParameterTypes();
                if (p.length != args.length) continue;
                for (int i = 0; i < p.length; i++) {
                    if (!paramCompatible(p[i], args[i])) continue outer;
                }
                chosen = m; break;
            }
            if (chosen == null) {
                return false;
            }
            chosen.setAccessible(true);
            chosen.invoke(obj, args);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
