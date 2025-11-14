package colox.gridmod.util;

import necesse.engine.GlobalData;
import necesse.engine.network.client.Client;
import necesse.engine.state.MainGame;
import necesse.engine.state.State;

/**
 * Resolves a stable key for the currently loaded world so we can store data per save rather than per profile.
 */
public final class WorldKeyProvider {
    private static final String DEFAULT_KEY = "global";

    private WorldKeyProvider() {}

    public static String currentWorldKey() {
        try {
            State state = GlobalData.getCurrentState();
            if (state instanceof MainGame) {
                Client client = ((MainGame) state).getClient();
                if (client != null) {
                    long id = client.getWorldUniqueID();
                    if (id > 0) {
                        return Long.toString(id);
                    }
                }
            }
        } catch (Throwable ignored) {}
        return DEFAULT_KEY;
    }
}
