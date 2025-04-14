package ru.tom8hawk.mapbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.tom8hawk.mapbot.model.TempFeature;

@Repository
public interface TempFeatureRepository extends JpaRepository<TempFeature, Long> {
}