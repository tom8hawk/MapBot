package ru.tom8hawk.mapbot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import ru.tom8hawk.mapbot.constants.FeatureType;

import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long telegramId;

    @Column(unique = true)
    private String username;

    @Enumerated(EnumType.STRING)
    private FeatureType defaultFeatureType;

    @OneToMany(mappedBy = "creator", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Feature> features;

    @OneToMany(mappedBy = "creator", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TempFeature> tempFeatures;

}