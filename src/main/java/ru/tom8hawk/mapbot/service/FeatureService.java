package ru.tom8hawk.mapbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.tom8hawk.mapbot.constants.FeatureStatus;
import ru.tom8hawk.mapbot.model.Feature;
import ru.tom8hawk.mapbot.repository.FeatureRepository;

import java.util.Date;
import java.util.Optional;

@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class FeatureService {

    private final FeatureRepository featureRepository;

    public Optional<Feature> findById(Long featureId) {
        return featureRepository.findById(featureId);
    }

    public Feature save(Feature newFeature) {
        return featureRepository.save(newFeature);
    }

    public void delete(Feature feature) {
        featureRepository.delete(feature);
    }

    public Optional<Feature> updateStatus(long featureId, FeatureStatus status) {
        return featureRepository.findById(featureId).map(feature -> {
            feature.setModifiedAt(new Date());
            feature.setStatus(status);
            return featureRepository.save(feature);
        });
    }

}