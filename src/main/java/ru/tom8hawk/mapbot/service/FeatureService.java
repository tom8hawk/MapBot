package ru.tom8hawk.mapbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.tom8hawk.mapbot.model.Feature;
import ru.tom8hawk.mapbot.model.Geometry;
import ru.tom8hawk.mapbot.model.User;
import ru.tom8hawk.mapbot.repository.FeatureRepository;
import ru.tom8hawk.mapbot.util.DateUtil;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Service
public class FeatureService {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ObjectNode jsonData = objectMapper.createObjectNode();
    private final ArrayNode featuresArray = objectMapper.createArrayNode();

    @Autowired
    private FeatureRepository featureRepository;

    @Getter
    private String jsonDataString;

    @PostConstruct
    public void init() {
        jsonData.put("type", "FeatureCollection");

        for (Feature feature : featureRepository.findAll()) {
            featuresArray.add(serializeFeature(feature));
        }

        jsonData.set("features", featuresArray);
        jsonDataString = jsonData.toString();
    }

    public void display(Feature feature) {
        JsonNode featureNode = serializeFeature(feature);
        featuresArray.add(featureNode);
        jsonDataString = jsonData.toString();
    }

    public void update(long featureId) {
        featureRepository.findById(featureId).ifPresent(feature -> {
            Map<String, String> properties = feature.getProperties();
            properties.put("marker-color", "#9c6c");

            feature.setModifiedAt(new Date());
            featureRepository.save(feature);

            remove(feature);
            featuresArray.add(serializeFeature(feature));
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
        JsonNode rootNode = objectMapper.readTree(file);

        if (rootNode.has("features") && rootNode.get("features").isArray()) {
            List<Feature> features = new ArrayList<>();

            for (JsonNode featureNode : rootNode.get("features")) {
                featuresArray.add(featureNode);
                features.add(deserializeFeature(featureNode));
            }

            featureRepository.saveAll(features);
            jsonDataString = jsonData.toString();
        }
    }

    private Feature deserializeFeature(JsonNode featureNode) {
        Feature feature = new Feature();

        if (featureNode.has("geometry")) {
            JsonNode geometryNode = featureNode.get("geometry");
            Geometry geometry = new Geometry();
            geometry.setType(geometryNode.get("type").asText());

            if (geometryNode.get("coordinates").isArray()) {
                JsonNode coordinatesArray = geometryNode.get("coordinates");
                double[] coordinates;

                if (coordinatesArray.get(0).isArray()) {
                    JsonNode nestedArray = coordinatesArray.get(0);
                    coordinates = new double[nestedArray.size() * 2];

                    for (int i = 0; i < nestedArray.size(); ++i) {
                        JsonNode coordinatePair = nestedArray.get(i);
                        coordinates[i * 2] = coordinatePair.get(0).asDouble();
                        coordinates[i * 2 + 1] = coordinatePair.get(1).asDouble();
                    }
                } else {
                    coordinates = new double[coordinatesArray.size()];

                    for (int i = 0; i < coordinatesArray.size(); ++i) {
                        coordinates[i] = coordinatesArray.get(i).asDouble();
                    }
                }

                geometry.setCoordinates(coordinates);
            }

            feature.setGeometry(geometry);
        }

        if (featureNode.has("properties")) {
            Map<String, String> properties = new HashMap<>();

            featureNode.get("properties").fields().forEachRemaining(entry ->
                    properties.put(entry.getKey(), entry.getValue().asText()));

            feature.setProperties(properties);
        }

        return feature;
    }

    private JsonNode serializeFeature(Feature feature) {
        ObjectNode featureNode = objectMapper.createObjectNode();

        featureNode.put("type", "Feature");
        featureNode.put("id", feature.getId());

        ObjectNode geometryNode = objectMapper.createObjectNode();
        Geometry geometry = feature.getGeometry();
        geometryNode.put("type", geometry.getType());

        double[] coordinates = geometry.getCoordinates();
        ArrayNode coordinatesArray = objectMapper.createArrayNode();

        if (coordinates.length > 2) {
            ArrayNode nestedCoordinatesArray = objectMapper.createArrayNode();

            for (int i = 0; i < coordinates.length; i += 2) {
                ArrayNode coordinatePair = objectMapper.createArrayNode();

                coordinatePair.add(coordinates[i]);
                coordinatePair.add(coordinates[i + 1]);

                nestedCoordinatesArray.add(coordinatePair);
            }

            coordinatesArray.add(nestedCoordinatesArray);
        } else {
            for (double coordinate : coordinates) {
                coordinatesArray.add(coordinate);
            }
        }

        geometryNode.set("coordinates", coordinatesArray);
        featureNode.set("geometry", geometryNode);

        ObjectNode propertiesNode = objectMapper.createObjectNode();
        Map<String, String> properties = feature.getProperties();

        if (!properties.containsKey("description")) {
            User creator = feature.getCreator();

            if (creator != null) {
                String username = creator.getTelegramUsername();

                if (username == null || username.isEmpty()) {
                    username = String.valueOf(feature.getCreator().getTelegramId());
                }

                String description = DateUtil.formatDate(feature.getCreatedAt()) + " @" + username;
                Date modifiedAt = feature.getModifiedAt();

                if (modifiedAt != null) {
                    description += "<br>[обновлено " + DateUtil.formatDate(modifiedAt) + "]";
                }

                properties.put("description", description);
            }
        }

        feature.getProperties().forEach(propertiesNode::put);
        featureNode.set("properties", propertiesNode);

        return featureNode;
    }
}