package com.example.addon.modules.automation.serverhelper;

public enum LeyuanWelcomeEntryMode {
    LEYUAN_CITY("乐源城入口"),
    BOOK_DIRECT("书本直达主城");

    private final String title;

    LeyuanWelcomeEntryMode(String title) {
        this.title = title;
    }

    @Override
    public String toString() {
        return title;
    }
}
