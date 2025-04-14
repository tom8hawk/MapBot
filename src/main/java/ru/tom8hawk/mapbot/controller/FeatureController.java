package ru.tom8hawk.mapbot.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.tom8hawk.mapbot.component.InitDataValidator;
import ru.tom8hawk.mapbot.service.FeatureService;
import ru.tom8hawk.mapbot.service.FeaturesMapService;

@RestController
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class FeatureController {

    private final FeatureService featureService;
    private final FeaturesMapService featuresMapService;
    private final InitDataValidator initDataValidator;

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