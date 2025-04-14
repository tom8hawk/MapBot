package ru.tom8hawk.mapbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.tom8hawk.mapbot.constants.FeatureType;
import ru.tom8hawk.mapbot.model.Feature;
import ru.tom8hawk.mapbot.model.Geometry;
import ru.tom8hawk.mapbot.model.Properties;
import ru.tom8hawk.mapbot.model.User;
import ru.tom8hawk.mapbot.repository.FeatureRepository;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class FeaturesMapService {

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("d MMM", new Locale("ru"));

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final ArrayNode FEATURES_ARRAY = OBJECT_MAPPER.createArrayNode();
    private static final ObjectNode FEATURES_MAP = OBJECT_MAPPER.createObjectNode();

    private final FeatureRepository featureRepository;

    @Getter
    private String featuresMapString;

    @PostConstruct
    private void init() {
        FEATURES_MAP.put("type", "FeatureCollection");

        for (Feature feature : featureRepository.findAll()) {
            FEATURES_ARRAY.add(serialize(feature));
        }

        FEATURES_MAP.set("features", FEATURES_ARRAY);
        featuresMapString = FEATURES_MAP.toString();
    }

    public void display(Feature feature) {
        FEATURES_ARRAY.add(serialize(feature));
        featuresMapString = FEATURES_MAP.toString();
    }

    public void remove(Feature feature) {
        remove(feature, true);
    }

    private boolean remove(Feature feature, boolean updateMap) {
        long featureId = feature.getId();

        for (int i = FEATURES_ARRAY.size() - 1; i >= 0; i--) {
            JsonNode featureNode = FEATURES_ARRAY.get(i);

            if (featureNode.get("id").asLong() == featureId) {
                FEATURES_ARRAY.remove(i);

                if (updateMap) {
                    featuresMapString = FEATURES_MAP.toString();
                }

                return true;
            }
        }

        return false;
    }

    public void update(Feature feature) {
        if (remove(feature, false)) {
            display(feature);
        }
    }

    public void importFeatures(File file) throws IOException {
        JsonNode featuresNode = OBJECT_MAPPER.readTree(file).get("features");

        if (featuresNode != null && featuresNode.isArray()) {
            List<Feature> features = new ArrayList<>();

            for (JsonNode featureNode : featuresNode) {
                features.add(deserialize(featureNode));
                FEATURES_ARRAY.add(featureNode);
            }

            featureRepository.saveAll(features);
            featuresMapString = FEATURES_MAP.toString();
        }
    }

    public InputStream exportFeatures() {
        byte[] data = featuresMapString.replace("<br>", " ").getBytes();
        return new ByteArrayInputStream(data);
    }

    private Feature deserialize(JsonNode featureNode) {
        Feature feature = new Feature();
        JsonNode geometryNode = featureNode.get("geometry");

        if (geometryNode != null) {
            Geometry geometry = new Geometry();
            geometry.setGeometryType(geometryNode.get("type").asText());

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

        JsonNode propertiesNode = featureNode.get("properties");

        if (propertiesNode != null) {
            Map<String, String> properties = propertiesNode.properties().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().asText()));

            feature.setProperties(new Properties(properties));
        }

        return feature;
    }

    private JsonNode serialize(Feature feature) {
        ObjectNode featureNode = OBJECT_MAPPER.createObjectNode();

        featureNode.put("type", "Feature");
        featureNode.put("id", feature.getId());

        ObjectNode geometryNode = OBJECT_MAPPER.createObjectNode();
        Geometry geometry = feature.getGeometry();
        geometryNode.put("type", geometry.getGeometryType());

        double[] coordinates = geometry.getCoordinates();
        ArrayNode coordinatesArray = OBJECT_MAPPER.createArrayNode();

        if (coordinates.length > 2) {
            ArrayNode nestedCoordinatesArray = OBJECT_MAPPER.createArrayNode();

            for (int i = 0; i < coordinates.length; i += 2) {
                ArrayNode coordinatePair = OBJECT_MAPPER.createArrayNode();

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

        Properties properties = feature.getProperties();
        String description = properties.getDescription();
        User creator = feature.getCreator();

        if (creator != null) {
            String username = creator.getUsername();

            if (username == null || username.isEmpty()) {
                username = String.valueOf(feature.getCreator().getTelegramId());
            }

            description = DATE_FORMAT.format(feature.getCreatedAt()) + " @" + username;
        }

        if (description != null) {
            FeatureType featureType = feature.getFeatureType();

            if (featureType != null) {
                description += "<br>[" + featureType.getRussianName() + "]";
            }

            Date modifiedAt = feature.getModifiedAt();

            if (modifiedAt != null) {
                description += "<br>[обновлено " + DATE_FORMAT.format(modifiedAt) + "]";
            }

            properties.setDescription(description);
        }

        ObjectNode propertiesNode = OBJECT_MAPPER.createObjectNode();
        properties.getPropertiesMap().forEach(propertiesNode::put);
        featureNode.set("properties", propertiesNode);

        return featureNode;
    }
}