package com.creatorsettlement.infrastructure.persistence.creator;

import com.creatorsettlement.domain.model.creator.Creator;

class CreatorMapper {

    private CreatorMapper() {
    }

    static CreatorJpaEntity toEntity(Creator creator) {
        return CreatorJpaEntity.of(creator.name());
    }
}
