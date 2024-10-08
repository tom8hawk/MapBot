package ru.tom8hawk.mapbot.component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;
import ru.tom8hawk.mapbot.MapConstants;
import ru.tom8hawk.mapbot.model.Feature;
import ru.tom8hawk.mapbot.model.Geometry;
import ru.tom8hawk.mapbot.model.Properties;
import ru.tom8hawk.mapbot.model.User;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class FeatureSerializer {

    private static final SimpleDateFormat dateFormat =
            new SimpleDateFormat("d MMM", new Locale("ru"));

    private final ObjectMapper objectMapper;

    public FeatureSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Feature deserialize(JsonNode featureNode) {
        Feature feature = new Feature();
        JsonNode geometryNode = featureNode.get("geometry");

        if (geometryNode != null) {
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

        JsonNode propertiesNode = featureNode.get("properties");

        if (propertiesNode != null) {
            Map<String, String> properties = propertiesNode.properties().stream()
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
                String username = creator.getUsername();

                if (username == null || username.isEmpty()) {
                    username = String.valueOf(feature.getCreator().getTelegramId());
                }

                String description = dateFormat.format(feature.getCreatedAt()) + " @" + username;
                Date modifiedAt = feature.getModifiedAt();

                if (modifiedAt != null) {
                    description += "<br>[обновлено " + dateFormat.format(modifiedAt) + "]";
                }

                properties.setDescription(description);
            }
        }

        properties.getPropertiesMap().forEach(propertiesNode::put);
        featureNode.set("properties", propertiesNode);

        return featureNode;
    }
}