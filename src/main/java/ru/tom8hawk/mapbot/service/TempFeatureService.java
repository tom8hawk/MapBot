package ru.tom8hawk.mapbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.tom8hawk.mapbot.model.TempFeature;
import ru.tom8hawk.mapbot.repository.TempFeatureRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class TempFeatureService {

    private final TempFeatureRepository tempFeatureRepository;

    public Optional<TempFeature> findById(Long tempFeatureId) {
        return tempFeatureRepository.findById(tempFeatureId);
    }

    public TempFeature save(TempFeature newTempFeature) {
        return tempFeatureRepository.save(newTempFeature);
    }

    public void delete(TempFeature tempFeature) {
        tempFeatureRepository.delete(tempFeature);
    }

}