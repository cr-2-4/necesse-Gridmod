package colox.gridmod.paint;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import colox.gridmod.paint.BlueprintPlacement.BlueprintTile;

public final class DefaultBlueprintRegistry {
    private DefaultBlueprintRegistry() {}

    private static final String DEFAULT_PREFIX = "_default:";
    private static final String MANIFEST_FILE = "manifest.txt";
    private static final String[] RESOURCE_ROOTS = new String[]{
            "/defaults/",
            "/resources/defaults/",
            "defaults/",
            "resources/defaults/"
    };

    public static final class DefaultBlueprint {
        public final String id;
        public final String file;
        public final String name;
        public final String description;

        DefaultBlueprint(String id, String file, String name, String description) {
            this.id = id;
            this.file = file;
            this.name = name;
            this.description = description;
        }
    }

    private static final List<DefaultBlueprint> defaults = initDefaults();

    private static List<DefaultBlueprint> initDefaults() {
        try (InputStream in = openManifest()) {
            if (in == null) return Collections.emptyList();
            List<DefaultBlueprint> out = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    String[] parts = line.split("\\|", 4);
                    if (parts.length < 3) continue;
                    String id = parts[0].trim();
                    String file = parts[1].trim();
                    String name = parts[2].trim();
                    String desc = parts.length >= 4 ? parts[3].trim() : "";
                    out.add(new DefaultBlueprint(id, file, name, desc));
                }
            }
            return Collections.unmodifiableList(out);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    public static List<DefaultBlueprint> values() {
        return defaults;
    }

    public static DefaultBlueprint find(String id) {
        if (id == null) return null;
        for (DefaultBlueprint bp : defaults) {
            if (bp.id.equals(id)) return bp;
        }
        return null;
    }

    public static List<BlueprintTile> load(String id) {
        DefaultBlueprint info = find(id);
        if (info == null) return Collections.emptyList();
        try (InputStream in = openResource(info.file)) {
            if (in == null) return Collections.emptyList();
            return PaintBlueprints.loadFromStream(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    public static String selectionKey(DefaultBlueprint info) {
        return DEFAULT_PREFIX + info.id;
    }

    public static boolean isDefaultKey(String value) {
        return value != null && value.startsWith(DEFAULT_PREFIX);
    }

    public static String keyToId(String value) {
        if (!isDefaultKey(value)) return null;
        return value.substring(DEFAULT_PREFIX.length());
    }

    public static String canonicalKey(String value) {
        if (value == null) return null;
        if (isDefaultKey(value)) return value;
        if (value.startsWith("_default_")) {
            String id = value.substring("_default_".length());
            DefaultBlueprint bp = find(id);
            if (bp != null) return selectionKey(bp);
        }
        return value;
    }

    private static InputStream openResource(String relativePath) throws IOException {
        for (String root : RESOURCE_ROOTS) {
            InputStream in = tryOpen(root + relativePath);
            if (in != null) return in;
        }
        return null;
    }

    private static InputStream openManifest() throws IOException {
        return openResource(MANIFEST_FILE);
    }

    private static InputStream tryOpen(String path) {
        InputStream in = DefaultBlueprintRegistry.class.getResourceAsStream(path.startsWith("/") ? path : ("/" + path));
        if (in != null) return in;
        ClassLoader loader = DefaultBlueprintRegistry.class.getClassLoader();
        if (loader != null) {
            String normalized = path.startsWith("/") ? path.substring(1) : path;
            in = loader.getResourceAsStream(normalized);
        }
        return in;
    }
}
