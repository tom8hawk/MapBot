package ru.tom8hawk.mapbot.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum FeatureType {
    LEAFLET("Листовка", "\uD83D\uDCC4", "#9c6c"),
    STICKER("Стикер", "\uD83C\uDFF7", "#bf15bf");

    private final String russianName;

    private final String emoji;

    private final String color;

}