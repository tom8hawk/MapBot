package ru.tom8hawk.mapbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class MapApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Moscow"));
        SpringApplication.run(MapApplication.class, args);
    }
}