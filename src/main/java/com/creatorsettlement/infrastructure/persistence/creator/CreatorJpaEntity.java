package com.creatorsettlement.infrastructure.persistence.creator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "creator")
public class CreatorJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "creator_id")
    private Long id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    protected CreatorJpaEntity() {
    }

    public static CreatorJpaEntity of(String name) {
        CreatorJpaEntity entity = new CreatorJpaEntity();
        entity.name = name;
        return entity;
    }

    public Long getId() {
        return id;
    }

    String getName() {
        return name;
    }
}
