package ru.tom8hawk.mapbot.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.tom8hawk.mapbot.service.FeatureService;
import ru.tom8hawk.mapbot.util.InitDataValidator;

@RestController
public class FeatureController {

    @Autowired
    private FeatureService featureService;

    @Autowired
    private InitDataValidator initDataValidator;

    @GetMapping("/features")
    public String getData() {
        return featureService.getJsonDataString();
    }

    @PostMapping("/update")
    public boolean updateFeature(@RequestParam String initData, @RequestParam long pointId) {
        if (initDataValidator.checkData(initData)) {
            featureService.update(pointId);
            return true;
        }

        return false;
    }
}