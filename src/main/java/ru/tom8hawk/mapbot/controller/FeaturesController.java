package ru.tom8hawk.mapbot.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.tom8hawk.mapbot.service.FeaturesService;

@RestController
public class FeaturesController {
    @Autowired
    private FeaturesService featuresService;

    @GetMapping({"/features"})
    public String getData() {
        return this.featuresService.getJsonDataString();
    }
}