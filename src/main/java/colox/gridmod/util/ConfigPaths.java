package colox.gridmod.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class ConfigPaths {
    private ConfigPaths() {}

    // …/Necesse/mods-data/colox.gridmod/
    public static Path modDataDir() {
        String os = System.getProperty("os.name","").toLowerCase();
        Path base;
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            base = appData != null
                    ? Paths.get(appData, "Necesse", "mods-data", "colox.gridmod")
                    : Paths.get(System.getProperty("user.home"), "AppData", "Roaming", "Necesse", "mods-data", "colox.gridmod");
        } else if (os.contains("mac")) {
            base = Paths.get(System.getProperty("user.home"), "Library", "Application Support", "Necesse", "mods-data", "colox.gridmod");
        } else {
            base = Paths.get(System.getProperty("user.home"), ".config", "Necesse", "mods-data", "colox.gridmod");
        }
        try { Files.createDirectories(base); } catch (Exception ignored) {}
        return base;
    }

    public static Path paintFile()    { return modDataDir().resolve("paint_state.txt"); }
    public static Path settingsFile() { return modDataDir().resolve("grid_settings.txt"); }

    /** …/mods-data/colox.gridmod/blueprints/ */
    public static Path blueprintsDir() {
        Path p = modDataDir().resolve("blueprints");
        try { Files.createDirectories(p); } catch (Exception ignored) {}
        return p;
    }

    /** …/mods-data/colox.gridmod/global-blueprints/ */
    public static Path globalBlueprintsDir() {
        Path p = modDataDir().resolve("global-blueprints");
        try { Files.createDirectories(p); } catch (Exception ignored) {}
        return p;
    }
}
