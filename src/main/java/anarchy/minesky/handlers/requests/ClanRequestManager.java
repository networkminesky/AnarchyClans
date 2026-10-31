package anarchy.minesky.handlers.requests;

import anarchy.minesky.AnarchyClans;
import java.util.EnumMap;
import java.util.Map;

public class ClanRequestManager {
    private final Map<ClanRequestType, ClanRequestHandler> handlers = new EnumMap<>(ClanRequestType.class);

    public ClanRequestManager(AnarchyClans plugin) {
        for (ClanRequestType type : ClanRequestType.values()) {
            handlers.put(type, new ClanRequestHandler(plugin, type));
        }
    }

    public ClanRequestHandler getHandler(ClanRequestType type) {
        return handlers.get(type);
    }
}