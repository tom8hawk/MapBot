package ru.tom8hawk.mapbot.model;

import jakarta.persistence.*;

import java.util.HashMap;
import java.util.Map;

@Embeddable
public class Properties {

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "properties", joinColumns = @JoinColumn(name = "feature_id"))
    @MapKeyColumn(name = "field")
    @Column(name = "data")
    private final Map<String, String> propertiesMap;

    public Properties() {
        this.propertiesMap = new HashMap<>();
    }

    public Properties(Map<String, String> propertiesMap) {
        this.propertiesMap = propertiesMap;
    }

    public String getDescription() {
        return propertiesMap.get("description");
    }

    public void setDescription(String description) {
        propertiesMap.put("description", description);
    }

    public String getMarkerColor() {
        return propertiesMap.get("marker-color");
    }

    public void setMarkerColor(String color) {
        propertiesMap.put("marker-color", color);
    }

    public Map<String, String> getPropertiesMap() {
        return propertiesMap;
    }
}