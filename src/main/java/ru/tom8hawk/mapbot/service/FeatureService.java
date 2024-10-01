package ru.tom8hawk.mapbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.tom8hawk.mapbot.component.FeatureConfig;
import ru.tom8hawk.mapbot.component.FeatureSerializer;
import ru.tom8hawk.mapbot.model.Feature;
import ru.tom8hawk.mapbot.repository.FeatureRepository;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Service
public class FeatureService {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final ObjectNode jsonData = objectMapper.createObjectNode();
    private static final ArrayNode featuresArray = objectMapper.createArrayNode();

    private final FeatureRepository featureRepository;
    private final FeatureSerializer featureSerializer;

    private final FeatureConfig featureConfig;

    @Getter
    private String jsonDataString;

    @Autowired
    public FeatureService(FeatureRepository featureRepository, FeatureConfig featureConfig) {
        this.featureRepository = featureRepository;
        this.featureConfig = featureConfig;
        this.featureSerializer = new FeatureSerializer(objectMapper, featureConfig);
    }

    @PostConstruct
    public void init() {
        jsonData.put("type", "FeatureCollection");

        for (Feature feature : featureRepository.findAll()) {
            featuresArray.add(featureSerializer.serialize(feature));
        }

        jsonData.set("features", featuresArray);
        jsonDataString = jsonData.toString();
    }

    public void display(Feature feature) {
        featuresArray.add(featureSerializer.serialize(feature));
        jsonDataString = jsonData.toString();
    }

    public void update(long featureId) {
        featureRepository.findById(featureId).ifPresent(feature -> {
            feature.setModifiedAt(new Date());
            feature.getProperties().setMarkerColor(featureConfig.getMarkerColor());

            featureRepository.save(feature);
            remove(feature);

            featuresArray.add(featureSerializer.serialize(feature));
            jsonDataString = jsonData.toString();
        });
    }

    public void remove(Feature feature) {
        Iterator<JsonNode> itr = featuresArray.iterator();

        while (itr.hasNext()) {
            JsonNode featureNode = itr.next();

            if (featureNode.get("id").asInt() == feature.getId()) {
                itr.remove();
                break;
            }
        }

        jsonDataString = jsonData.toString();
    }

    public void importFromFile(File file) throws IOException {
        JsonNode featuresNode = objectMapper.readTree(file).get("features");

        if (featuresNode != null && featuresNode.isArray()) {
            List<Feature> features = new ArrayList<>();

            for (JsonNode featureNode : featuresNode) {
                featuresArray.add(featureNode);
                features.add(featureSerializer.deserialize(featureNode));
            }

            featureRepository.saveAll(features);
            jsonDataString = jsonData.toString();
        }
    }
}