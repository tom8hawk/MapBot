package ru.tom8hawk.mapbot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.util.Date;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "features")
public class Feature {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User creator;

    @Embedded
    private Geometry geometry;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "properties", joinColumns = @JoinColumn(name = "feature_id"))
    @MapKeyColumn(name = "field")
    @Column(name = "data")
    private Map<String, String> properties;

    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
}