package ru.tom8hawk.mapbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.tom8hawk.mapbot.MapConstants;
import ru.tom8hawk.mapbot.component.FeatureSerializer;
import ru.tom8hawk.mapbot.model.Feature;
import ru.tom8hawk.mapbot.repository.FeatureRepository;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

@Service
public class FeatureService {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final ObjectNode jsonData = objectMapper.createObjectNode();
    private static final ArrayNode featuresArray = objectMapper.createArrayNode();

    private final FeatureRepository featureRepository;
    private final FeatureSerializer featureSerializer;

    @Getter
    private String jsonDataString;

    @Autowired
    public FeatureService(FeatureRepository featureRepository) {
        this.featureRepository = featureRepository;
        this.featureSerializer = new FeatureSerializer(objectMapper);
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
            feature.getProperties().setMarkerColor(MapConstants.MARKER_COLOR);

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

    public void importFeatures(File file) throws IOException {
        JsonNode featuresNode = objectMapper.readTree(file).get("features");

        if (featuresNode != null && featuresNode.isArray()) {
            List<Feature> features = new ArrayList<>();

            for (JsonNode featureNode : featuresNode) {
                features.add(featureSerializer.deserialize(featureNode));
                featuresArray.add(featureNode);
            }

            featureRepository.saveAll(features);
            jsonDataString = jsonData.toString();
        }
    }

    public InputStream exportFeatures() {
        byte[] data = jsonDataString.replace("<br>", " ").getBytes();
        return new ByteArrayInputStream(data);
    }
}