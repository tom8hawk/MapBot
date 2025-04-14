package ru.tom8hawk.mapbot.constants;

import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public final class MapConstants { // TODO change to Config

    public static final List<String> ADMINISTRATORS;

    static {
        ADMINISTRATORS = List.of("1234567890");
    }

}