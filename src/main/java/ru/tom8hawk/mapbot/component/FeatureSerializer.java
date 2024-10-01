package ru.tom8hawk.mapbot.component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;
import ru.tom8hawk.mapbot.model.Feature;
import ru.tom8hawk.mapbot.model.Geometry;
import ru.tom8hawk.mapbot.model.Properties;
import ru.tom8hawk.mapbot.model.User;

import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class FeatureSerializer {

    private final ObjectMapper objectMapper;
    private final FeatureConfig featureConfig;

    public FeatureSerializer(ObjectMapper objectMapper, FeatureConfig featureConfig) {
        this.objectMapper = objectMapper;
        this.featureConfig = featureConfig;
    }

    public Feature deserialize(JsonNode featureNode) {
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
            Map<String, String> properties = featureNode.get("properties").properties().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().asText()));

            feature.setProperties(new Properties(properties));
        }

        return feature;
    }

    public JsonNode serialize(Feature feature) {
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
        Properties properties = feature.getProperties();

        if (properties.getDescription() == null) {
            User creator = feature.getCreator();

            if (creator != null) {
                String username = creator.getTelegramUsername();

                if (username == null || username.isEmpty()) {
                    username = String.valueOf(feature.getCreator().getTelegramId());
                }

                String description = featureConfig.formatDate(feature.getCreatedAt()) + " @" + username;
                Date modifiedAt = feature.getModifiedAt();

                if (modifiedAt != null) {
                    description += "<br>[обновлено " + featureConfig.formatDate(modifiedAt) + "]";
                }

                properties.setDescription(description);
            }
        }

        properties.getPropertiesMap().forEach(propertiesNode::put);
        featureNode.set("properties", propertiesNode);

        return featureNode;
    }
}