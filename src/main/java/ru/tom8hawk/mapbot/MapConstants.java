package ru.tom8hawk.mapbot;

import java.util.List;

public final class MapConstants {

    public static final String MARKER_COLOR;

    public static final List<String> ADMINISTRATORS;

    static {
        MARKER_COLOR = "#bf15bf";
        ADMINISTRATORS = List.of("1234567890");
    }

    private MapConstants() {
        throw new AssertionError("This class cannot be instantiated");
    }
}