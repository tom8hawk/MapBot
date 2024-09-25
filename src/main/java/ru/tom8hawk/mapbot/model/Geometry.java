package ru.tom8hawk.mapbot.model;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Embeddable
public class Geometry {
    private String type;
    private double[] coordinates;
}