package ru.tom8hawk.mapbot.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.tom8hawk.mapbot.component.InitDataValidator;
import ru.tom8hawk.mapbot.service.FeaturesMapService;
import ru.tom8hawk.mapbot.service.FeatureService;

@RestController
public class FeatureController {

    private final FeatureService featureService;
    private final FeaturesMapService featuresMapService;
    private final InitDataValidator initDataValidator;

    @Autowired
    public FeatureController(
            FeatureService featureService,
            FeaturesMapService featuresMapService,
            InitDataValidator initDataValidator) {

        this.featureService = featureService;
        this.featuresMapService = featuresMapService;
        this.initDataValidator = initDataValidator;
    }

    @GetMapping("/features")
    public ResponseEntity<String> getFeatures() {
        return ResponseEntity.ok(featuresMapService.getFeaturesMapString());
    }

    @PostMapping("/update")
    public ResponseEntity<Void> updateFeature(@RequestParam String initData, @RequestParam long pointId) {
        if (!initDataValidator.checkData(initData)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        featureService.update(pointId)
                .ifPresent(featuresMapService::update);

        return ResponseEntity.ok().build();
    }
}