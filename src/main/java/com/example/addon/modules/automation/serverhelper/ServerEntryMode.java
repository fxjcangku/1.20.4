package com.example.addon.modules.automation.serverhelper;

public enum ServerEntryMode {
    DIRECT("直接进入"),
    MENU_TRANSFER("菜单传送"),
    SUBSERVER_NETWORK("子服网络"),
    LEYUAN_CUSTOM("乐源服定制");

    private final String title;

    ServerEntryMode(String title) {
        this.title = title;
    }

    @Override
    public String toString() {
        return title;
    }
}
