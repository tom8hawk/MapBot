package ru.tom8hawk.mapbot.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.tom8hawk.mapbot.component.InitDataValidator;
import ru.tom8hawk.mapbot.constants.FeatureStatus;
import ru.tom8hawk.mapbot.service.FeatureService;
import ru.tom8hawk.mapbot.service.FeaturesMapService;

@RestController
@RequestMapping("/api/v1/features")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class FeatureController {

    private final FeatureService featureService;
    private final FeaturesMapService featuresMapService;
    private final InitDataValidator initDataValidator;

    @GetMapping
    public ResponseEntity<String> getFeatures(@RequestParam String initData) {
        if (!initDataValidator.checkData(initData)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(featuresMapService.getFeaturesMapString());
    }

    @PostMapping("/ripped")
    public ResponseEntity<Void> rippedFeature(@RequestParam String initData, @RequestParam long pointId) {
        return updateFeatureStatus(initData, pointId, FeatureStatus.RIPPED);
    }

    private ResponseEntity<Void> updateFeatureStatus(String initData,
                                                      long pointId,
                                                      FeatureStatus featureStatus) {

        if (!initDataValidator.checkData(initData)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        featureService.updateStatus(pointId, featureStatus).ifPresent(featuresMapService::update);
        return ResponseEntity.ok().build();
    }

}