package ru.tom8hawk.mapbot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import ru.tom8hawk.mapbot.constants.FeatureStatus;
import ru.tom8hawk.mapbot.constants.FeatureType;

import java.util.Date;

@Getter
@Setter
@Entity
@Table(name = "features")
public class Feature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @Embedded
    @Column(nullable = false)
    private Geometry geometry;

    @Embedded
    @Column(nullable = false)
    private Properties properties;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeatureType featureType;

    @Enumerated(EnumType.STRING)
    private FeatureStatus status;

    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    private Date modifiedAt;

}